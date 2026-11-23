"""Validate the pinned multilingual text body + masked mean + Dense on CPU."""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import sys
from concurrent.futures import ThreadPoolExecutor

# Reuse the checksum-verified download and vector comparison used in A05.
from a05_visual_reference import download, digest, compare


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--contract', type=Path, required=True)
    parser.add_argument('--tokenizer-cases', type=Path, required=True)
    parser.add_argument('--work-dir', type=Path, required=True)
    parser.add_argument('--prepare-only', action='store_true')
    args = parser.parse_args()
    contract = json.loads(args.contract.read_text('utf-8'))
    cases = json.loads(args.tokenizer_cases.read_text('utf-8'))
    work = args.work_dir.resolve()
    source = work / 'source'
    source.mkdir(parents=True, exist_ok=True)
    repo = contract['selection']['text_repository']
    revision = next(x['revision'] for x in contract['repositories'] if x['repo'] == repo)
    files = [(x['url'], source / x['path'], x['sha256'])
             for x in contract['source_files'] if x['repo'] == repo]
    needed = {'model.safetensors', '2_Dense/model.safetensors', contract['text']['published_graph']}
    weights = [x for x in contract['weights'] if x['repo'] == repo and x['path'] in needed]
    files += [(f'https://huggingface.co/{repo}/resolve/{revision}/{x["path"]}',
               source / x['path'], x['published_sha256']) for x in weights]
    with ThreadPoolExecutor(max_workers=2) as pool:
        results = [pool.submit(download, *item) for item in files]
        for result in results:
            result.result()
    if args.prepare_only:
        return
    (work / 'desktop_report.json').unlink(missing_ok=True)

    import numpy as np
    import torch
    import onnx
    import onnxruntime as ort
    from safetensors.torch import load_file
    from tokenizers import Tokenizer
    from transformers import DistilBertConfig, DistilBertModel

    torch.set_num_threads(2)
    config = DistilBertConfig.from_json_file(str(source / 'config.json'))
    config._attn_implementation = 'eager'
    model = DistilBertModel(config).eval().float()
    model.load_state_dict(load_file(str(source / 'model.safetensors')), strict=True)
    dense_state = load_file(str(source / '2_Dense/model.safetensors'))
    assert set(dense_state) == {'linear.weight'}, list(dense_state)
    dense = dense_state['linear.weight']
    assert tuple(dense.shape) == (512, 768) and torch.isfinite(dense).all()
    fixtures = work / 'fixtures'
    fixtures.mkdir(exist_ok=True)
    dense_path = work / 'clip_multilingual_dense.f32'
    dense.numpy().astype('<f4').tofile(dense_path)

    graph = source / contract['text']['published_graph']
    onnx.checker.check_model(str(graph), full_check=True)
    opts = ort.SessionOptions()
    opts.intra_op_num_threads = 2
    opts.inter_op_num_threads = 1
    opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
    session = ort.InferenceSession(str(graph), sess_options=opts, providers=['CPUExecutionProvider'])
    inputs = session.get_inputs()
    outputs = session.get_outputs()
    io = {'inputs': [{'name': x.name, 'type': x.type, 'shape': x.shape} for x in inputs],
          'outputs': [{'name': x.name, 'type': x.type, 'shape': x.shape} for x in outputs]}
    print(json.dumps(io), flush=True)
    assert {x.name for x in inputs} == {'input_ids', 'attention_mask'}, io
    assert all(x.type == 'tensor(int64)' and len(x.shape) == 2 for x in inputs), io
    assert len(outputs) == 1 and outputs[0].type == 'tensor(float)', io
    assert len(outputs[0].shape) == 3 and outputs[0].shape[2] == 768, io
    output_name = outputs[0].name
    tokenizer = Tokenizer.from_file(str(source / 'tokenizer.json'))
    tokenizer.enable_truncation(max_length=128, strategy='longest_first', direction='right')
    extra_texts = {
        'embedded_specials': 'önce[MASK]sonra[PAD][SEP][CLS][UNK]',
        'cleaned_special_is_ordinary': 'a[MA\x00SK]b',
        'unicode_spaces': 'araba\u00a0bir\u2007kedi\u202fve\u3000ev',
        'python_strip_controls': '\x1c\x1f araba \x1e',
        'private_unassigned': 'ar\ue000a\u0378ba',
        'astral_chinese': '车\U00020000🚗与 araba',
        'emoji_zwj': 'bir 👩\u200d💻 fotoğrafı',
        'word_length_boundary': 'a' * 100 + ' ' + 'a' * 101,
        'case_and_accent': 'İ I ı i café CAFE e\u0301 é',
        'removed_controls': 'ar\x0ba\x0cba\u0085\u200b',
    }
    for key, text in extra_texts.items():
        encoded = tokenizer.encode(text.strip())
        cases['cases'].append({'id': key, 'text': text, 'input_ids': encoded.ids,
                               'attention_mask': encoded.attention_mask})
    gate = contract['tolerances']['int8_text_vs_fp32']
    all_cases = []
    failures = []
    for case in cases['cases']:
        encoded = tokenizer.encode(case['text'].strip())
        assert encoded.ids == case['input_ids'] and encoded.attention_mask == case['attention_mask'], case['id']
        result = {'id': case['id'], 'text': case['text'], 'profiles': {}}
        vectors = {}
        for profile in ['dynamic', 'fixed_128']:
            ids = list(encoded.ids)
            mask = list(encoded.attention_mask)
            if profile == 'fixed_128':
                pad = 128 - len(ids)
                ids += [0] * pad
                mask += [0] * pad
            ids_array = np.asarray([ids], dtype=np.int64)
            mask_array = np.asarray([mask], dtype=np.int64)
            with torch.inference_mode():
                hidden = model(input_ids=torch.from_numpy(ids_array), attention_mask=torch.from_numpy(mask_array)).last_hidden_state
                expanded = torch.from_numpy(mask_array).unsqueeze(-1).float()
                pooled = (hidden * expanded).sum(1) / expanded.sum(1).clamp(min=1e-9)
                reference = torch.nn.functional.linear(pooled, dense).numpy()
            quantized_hidden = session.run([output_name], {'input_ids': ids_array, 'attention_mask': mask_array})[0]
            assert quantized_hidden.shape == (1, len(ids), 768) and np.isfinite(quantized_hidden).all()
            # Independent Float64 reduction avoids mirroring Kotlin's loop implementation.
            pooled_np = (quantized_hidden.astype(np.float64) * mask_array[..., None]).sum(1) / mask_array.sum(1)[:, None]
            actual = (pooled_np @ dense.numpy().astype(np.float64).T).astype(np.float32)
            metrics = compare(reference, actual)
            if metrics['cosine'] < gate['min_cosine'] or metrics['relative_l2'] > gate['max_relative_l2']:
                failures.append({'id': case['id'], 'profile': profile, 'metrics': metrics})
            prefix = f'{case["id"]}-{profile}'
            reference.reshape(-1).astype('<f4').tofile(fixtures / f'{prefix}-pytorch.f32')
            actual.reshape(-1).astype('<f4').tofile(fixtures / f'{prefix}-onnx.f32')
            result['profiles'][profile] = {'input_ids': ids, 'attention_mask': mask,
                'reference_file': f'{prefix}-pytorch.f32', 'desktop_file': f'{prefix}-onnx.f32',
                'comparison': metrics}
            vectors[profile] = reference
        equivalence = compare(vectors['dynamic'], vectors['fixed_128'])
        assert equivalence['cosine'] >= .99999 and equivalence['relative_l2'] <= .001, equivalence
        result['fp32_padding_equivalence'] = equivalence
        all_cases.append(result)
        print(json.dumps({'id': case['id'], 'profiles': {k: v['comparison'] for k, v in result['profiles'].items()}}), flush=True)
    if failures:
        failure_report = {'status': 'failed', 'actual_graph_io': io, 'failures': failures, 'cases': all_cases}
        (work / 'failed_reference_report.json').write_text(json.dumps(failure_report, ensure_ascii=False, indent=2) + '\n', 'utf-8')
        raise ValueError(f'INT8 parity failed in {len(failures)} / {len(all_cases) * 2} comparisons; see failed_reference_report.json')
    metadata = {'contract_version': contract['contract_version'], 'model_sha256': digest(graph),
        'dense_source_sha256': digest(source / '2_Dense/model.safetensors'),
        'dense_float32_sha256': digest(dense_path), 'dense_float32_shape': [512, 768],
        'tokenizer_sha256': digest(source / 'tokenizer.json'), 'vocab_sha256': digest(source / 'vocab.txt'),
        'actual_graph_io': io, 'output_name': output_name, 'cases': all_cases,
        'tolerances': contract['tolerances']}
    (fixtures / 'fixture.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    report = {'status': 'desktop_passed_android_pending', 'case_count': len(all_cases),
        'profile_count': 2, 'comparisons': len(all_cases) * 2,
        'worst_cosine': min(p['comparison']['cosine'] for c in all_cases for p in c['profiles'].values()),
        'worst_relative_l2': max(p['comparison']['relative_l2'] for c in all_cases for p in c['profiles'].values()),
        'environment': {p: importlib.metadata.version(p) for p in ['torch', 'transformers', 'tokenizers', 'numpy', 'onnx', 'onnxruntime', 'safetensors']},
        'sources': [dict(w, downloaded=True, locally_verified=True) for w in weights],
        'artifacts': {name: {'bytes': path.stat().st_size, 'sha256': digest(path)} for name, path in
                      [('text_onnx', graph), ('dense_f32', dense_path)]},
        'fixture': metadata, 'scope': 'numerical parity only; not Turkish retrieval quality or a performance benchmark'}
    (work / 'desktop_report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print(json.dumps({k: v for k, v in report.items() if k not in ['fixture', 'sources']}, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
