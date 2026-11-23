"""Export the pinned CLIP visual branch and validate one controlled image on CPU."""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import time
import urllib.request


def digest(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            hasher.update(chunk)
    return hasher.hexdigest()


def download(url: str, target: Path, expected: str) -> None:
    if target.is_file() and digest(target) == expected:
        print(f'Using verified {target.name}', flush=True)
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    partial = target.with_suffix(target.suffix + '.partial')
    request = urllib.request.Request(url, headers={'User-Agent': 'GalerideAra-A05'})
    with urllib.request.urlopen(request, timeout=60) as response, partial.open('wb') as output:
        count = 0
        reported = 0
        while chunk := response.read(1024 * 1024):
            output.write(chunk)
            count += len(chunk)
            if count - reported >= 64 * 1024 * 1024:
                print(f'Downloaded {count // (1024*1024)} MiB: {target.name}', flush=True)
                reported = count
    actual = digest(partial)
    if actual != expected:
        raise ValueError(f'Checksum mismatch for {target.name}: {actual}')
    partial.replace(target)
    print(f'Verified {target.name}: {actual}', flush=True)


def compare(reference, actual):
    import numpy as np
    a = np.asarray(reference, dtype=np.float64).reshape(-1)
    b = np.asarray(actual, dtype=np.float64).reshape(-1)
    if a.shape != (512,) or b.shape != (512,) or not np.isfinite(b).all():
        raise ValueError('Invalid embedding shape or non-finite output')
    na, nb = np.linalg.norm(a), np.linalg.norm(b)
    if min(na, nb) <= 0:
        raise ValueError('Zero-norm embedding')
    return {'cosine': float(np.dot(a,b)/(na*nb)), 'relative_l2': float(np.linalg.norm(a-b)/na),
            'max_abs_error': float(np.max(np.abs(a-b))), 'reference_norm': float(na), 'actual_norm': float(nb)}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--contract',type=Path,required=True)
    parser.add_argument('--work-dir',type=Path,required=True)
    parser.add_argument('--prepare-only',action='store_true')
    args=parser.parse_args()
    contract=json.loads(args.contract.read_text(encoding='utf-8'))
    work=args.work_dir.resolve(); work.mkdir(parents=True,exist_ok=True)
    repo=contract['selection']['image_repository']
    weight=next(x for x in contract['weights'] if x['repo']==repo)
    model_dir=work/'source';model_dir.mkdir(exist_ok=True)
    for name in ['config.json','preprocessor_config.json']:
        source=next(x for x in contract['source_files'] if x['repo']==repo and x['path']=='0_CLIPModel/'+name)
        download(source['url'],model_dir/name,source['sha256'])
    checkpoint=model_dir/'model.safetensors'
    download(f'https://huggingface.co/{repo}/resolve/{weight["revision"]}/{weight["path"]}',checkpoint,weight['published_sha256'])
    if args.prepare_only:
        return

    import numpy as np
    import torch
    from PIL import Image, ImageOps
    import onnx
    import onnxruntime as ort
    from safetensors import safe_open
    from transformers import CLIPVisionConfig, CLIPVisionModelWithProjection, CLIPImageProcessor

    torch.set_num_threads(2)
    torch.manual_seed(0)
    config=json.loads((model_dir/'config.json').read_text(encoding='utf-8'))
    vision_config=CLIPVisionConfig(**config['vision_config'])
    vision_config._attn_implementation='eager'
    model=CLIPVisionModelWithProjection(vision_config).eval().float()
    with safe_open(str(checkpoint),framework='pt',device='cpu') as tensors:
        keys=[k for k in tensors.keys() if k.startswith(('vision_model.','visual_projection.'))]
        state={key:tensors.get_tensor(key) for key in keys}
        # Older checkpoints persist this deterministic buffer; current Transformers does not.
        position_ids=state.pop('vision_model.embeddings.position_ids',None)
        if position_ids is not None:
            if not torch.equal(position_ids,model.vision_model.embeddings.position_ids):
                raise ValueError('Checkpoint position IDs disagree with the selected model geometry')
        model.load_state_dict(state,strict=True)
    del state
    print(f'Loaded {len(keys)} visual tensors with strict=True',flush=True)

    fixtures=work/'fixtures';fixtures.mkdir(exist_ok=True)
    image_path=fixtures/'a05-pattern.png'
    # Reproducible owned test image, independent of private/user photographs.
    yy,xx=np.indices((240,320))
    rgb=np.stack([xx*255//319,yy*255//239,((xx//20+yy//20)%2)*160+40],axis=-1).astype(np.uint8)
    Image.fromarray(rgb).save(image_path)
    with Image.open(image_path) as source_image:
        image=ImageOps.exif_transpose(source_image).convert('RGBA')
        background=Image.new('RGBA',image.size,(255,255,255,255))
        image=Image.alpha_composite(background,image).convert('RGB')
        processor=CLIPImageProcessor.from_dict(json.loads((model_dir/'preprocessor_config.json').read_text(encoding='utf-8')))
        pixel_values=processor(images=image,return_tensors='pt')['pixel_values'].float()
    assert tuple(pixel_values.shape)==(1,3,224,224)

    class VisualGraph(torch.nn.Module):
        def __init__(self,visual):
            super().__init__();self.visual=visual
        def forward(self,pixel_values):
            return self.visual(pixel_values=pixel_values,return_dict=False)[0]
    wrapper=VisualGraph(model).eval()
    with torch.inference_mode():
        reference=wrapper(pixel_values).numpy()
    assert reference.shape==(1,512) and np.isfinite(reference).all()
    model_path=work/'clip_vit_b32_visual_fp32.onnx'
    print('Exporting visual model (FP32, opset 17)...',flush=True)
    with torch.inference_mode():
        torch.onnx.export(wrapper,pixel_values,str(model_path),input_names=['pixel_values'],output_names=['image_features'],
                          dynamic_axes={'pixel_values':{0:'batch'},'image_features':{0:'batch'}},opset_version=17,
                          export_params=True,do_constant_folding=True,dynamo=False,external_data=False)
    onnx.checker.check_model(str(model_path),full_check=True)
    options=ort.SessionOptions();options.intra_op_num_threads=2
    options.graph_optimization_level=ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
    session=ort.InferenceSession(str(model_path),sess_options=options,providers=['CPUExecutionProvider'])
    inp,out=session.get_inputs(),session.get_outputs()
    assert len(inp)==len(out)==1
    assert (inp[0].name,inp[0].type,inp[0].shape)==('pixel_values','tensor(float)',['batch',3,224,224])
    assert (out[0].name,out[0].type,out[0].shape)==('image_features','tensor(float)',['batch',512])
    start=time.perf_counter();actual=session.run(['image_features'],{'pixel_values':pixel_values.numpy()})[0]
    elapsed=(time.perf_counter()-start)*1000
    metrics=compare(reference,actual)
    gate=contract['tolerances']['fp32_same_tensor']
    assert metrics['cosine']>=gate['min_cosine'] and metrics['relative_l2']<=gate['max_relative_l2'],metrics
    pixel_values.numpy().astype('<f4').tofile(fixtures/'input.f32')
    reference.astype('<f4').tofile(fixtures/'reference.f32')
    actual.astype('<f4').tofile(fixtures/'onnx.f32')
    model_sha=digest(model_path)
    fixture_meta={'model_sha256':model_sha,'image_sha256':digest(image_path),'input_sha256':digest(fixtures/'input.f32'),
                  'reference_sha256':digest(fixtures/'reference.f32'),'input_shape':[1,3,224,224],'output_shape':[1,512],
                  'dtype':'float32_little_endian','input_name':'pixel_values','output_name':'image_features',
                  'desktop_output':actual.reshape(-1).tolist(),'desktop_comparison':metrics,
                  'source_image':{'width':320,'height':240,'origin':'deterministic RGB test pattern; no private image'},
                  'tolerances':contract['tolerances']}
    (fixtures/'fixture.json').write_text(json.dumps(fixture_meta,indent=2)+'\n',encoding='utf-8')
    report={'status':'desktop_passed_android_pending','contract_version':contract['contract_version'],'source_checkpoint':dict(weight,locally_verified=True,downloaded=True),
            'artifact':{'filename':model_path.name,'sha256':model_sha,'bytes':model_path.stat().st_size,'opset':17,'dtype':'float32','visual_projection_included':True},
            'input':{'name':inp[0].name,'type':inp[0].type,'shape':inp[0].shape},'output':{'name':out[0].name,'type':out[0].type,'shape':out[0].shape},
            'comparison':metrics,'single_inference_ms':elapsed,'timing_is_benchmark':False,'android_verified':False,
            'environment':{p:importlib.metadata.version(p) for p in ['torch','torchvision','transformers','tokenizers','Pillow','numpy','onnx','onnxruntime','safetensors']},
            'fixture':{k:v for k,v in fixture_meta.items() if k not in ['desktop_output','tolerances']}}
    (work/'desktop_report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'status':'desktop_passed','model_sha256':model_sha,'bytes':model_path.stat().st_size,**metrics}),flush=True)


if __name__=='__main__':
    main()
