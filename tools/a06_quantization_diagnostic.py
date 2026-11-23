"""Diagnose the rejected ARM64 U8S8 graph on the current CPU without selecting it."""
import argparse
import json
from pathlib import Path
from a05_visual_reference import download, digest, compare


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--contract', type=Path, required=True)
    parser.add_argument('--tokenizer-cases', type=Path, required=True)
    parser.add_argument('--work-dir', type=Path, required=True)
    args = parser.parse_args()
    import numpy as np
    import onnx
    import onnxruntime as ort
    from onnx import helper, numpy_helper, TensorProto

    contract = json.loads(args.contract.read_text('utf-8'))
    source = next(w for w in contract['weights'] if w['path'] == 'onnx/model_qint8_arm64.onnx')
    work = args.work_dir.resolve()
    original = work / 'source' / source['path']
    url = f'https://huggingface.co/{source["repo"]}/resolve/{source["revision"]}/{source["path"]}'
    download(url, original, source['published_sha256'])
    graph = onnx.load(str(original))
    first = next(n for n in graph.graph.node if n.op_type == 'MatMulInteger')
    initializers = {t.name: numpy_helper.to_array(t) for t in graph.graph.initializer}
    graph.graph.output.extend([
        helper.make_tensor_value_info(first.input[0], TensorProto.UINT8, None),
        helper.make_tensor_value_info(first.input[2], TensorProto.UINT8, None),
        helper.make_tensor_value_info(first.output[0], TensorProto.INT32, None),
    ])
    diagnostic = work / 'diagnostic-integer.onnx'
    onnx.save(graph, str(diagnostic))
    options = ort.SessionOptions()
    options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_DISABLE_ALL
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    session = ort.InferenceSession(str(diagnostic), sess_options=options, providers=['CPUExecutionProvider'])
    cases = json.loads(args.tokenizer_cases.read_text('utf-8'))['cases']
    case = next(c for c in cases if c['id'] == 'turkish_query')
    ids = np.asarray([case['input_ids']], dtype=np.int64)
    mask = np.asarray([case['attention_mask']], dtype=np.int64)
    hidden, activation, zero, observed = session.run(
        ['last_hidden_state', first.input[0], first.input[2], first.output[0]],
        {'input_ids': ids, 'attention_mask': mask})
    weights = initializers[first.input[1]]
    weight_zero = initializers[first.input[3]]
    exact = (activation.astype(np.int32) - zero.astype(np.int32)) @ (weights.astype(np.int32) - weight_zero.astype(np.int32))
    dense = np.fromfile(work / 'clip_multilingual_dense.f32', dtype='<f4').reshape(512, 768)
    pooled = (hidden.astype(np.float64) * mask[..., None]).sum(1) / mask.sum(1)[:, None]
    vector = (pooled @ dense.astype(np.float64).T).astype(np.float32)
    reference = np.fromfile(work / 'fixtures/turkish_query-dynamic-pytorch.f32', dtype='<f4')
    count = int(np.count_nonzero(exact != observed))
    report = {'status': 'diagnostic_only', 'selected_for_application': False,
              'model_sha256': digest(original), 'source': dict(source, downloaded=True, locally_verified=True), 'onnxruntime': ort.__version__,
              'query': case['text'], 'first_integer_node': first.name,
              'integer_elements': int(observed.size), 'different_integer_elements': count,
              'max_integer_abs_error': int(np.max(np.abs(exact - observed))),
              'final_vector_vs_fp32': compare(reference, vector),
              'interpretation': 'Nonzero integer differences support x86 U8S8 saturation; this is not an ARM result.',
              'runtime_reference': 'https://onnxruntime.ai/docs/performance/model-optimizations/quantization.html'}
    (work / 'quantization_diagnostic.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print(json.dumps(report, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
