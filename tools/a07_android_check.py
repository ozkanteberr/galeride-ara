"""Verify the QueryEncoder adapter plus the A06 tokenizer/text regression tests."""
import argparse
import hashlib
import json
from pathlib import Path
import posixpath
import shlex
import subprocess


def checksum(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--dense', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--remote-dir')
    parser.add_argument('--app-apk', type=Path)
    parser.add_argument('--test-apk', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    manifest = json.loads((root / 'docs/MODEL_RUNTIME_MANIFEST.json').read_text('utf-8'))
    revision = manifest.pop('revision')
    actual = hashlib.sha256(json.dumps(manifest, sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()).hexdigest()
    if actual != revision:
        parser.error('Runtime manifest revision does not match its components')
    components = manifest['components']
    payloads = [(args.model, 'model_quint8_avx2.onnx', 'modelPath', components['text_onnx']['sha256']),
                (args.dense, 'clip_multilingual_dense.f32', 'densePath', components['dense']['runtime_sha256'])]
    for local, _, _, expected in payloads:
        if checksum(local) != expected:
            parser.error(f'Component checksum mismatch: {local.name}')
    if bool(args.app_apk) != bool(args.test_apk):
        parser.error('Supply both APK paths together')
    adb = [args.adb, '-s', args.serial]
    def run(*command, timeout=180):
        result = subprocess.run(adb + list(command), check=True, capture_output=True, timeout=timeout)
        return result.stdout.decode('utf-8', errors='replace')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.unlink(missing_ok=True)
    if args.app_apk:
        print(run('install', '-r', str(args.app_apk)), flush=True)
        print(run('install', '-r', str(args.test_apk)), flush=True)
    run('shell', 'run-as', 'com.galeriara', 'mkdir', '-p', 'files/a06', 'files/a07')
    if args.remote_dir:
        if not args.remote_dir.startswith('/'):
            parser.error('--remote-dir must be an absolute Android directory')
        run('shell', 'mkdir', '-p', shlex.quote(args.remote_dir))
    extra = []
    for local, name, argument, expected in payloads:
        remote = posixpath.join(args.remote_dir, name) if args.remote_dir else 'files/a06/' + name
        command = ['shell'] + ([] if args.remote_dir else ['run-as', 'com.galeriara'])
        try:
            matches = run(*command, 'sha256sum', shlex.quote(remote)).split()[0] == expected
        except (subprocess.CalledProcessError, IndexError):
            matches = False
        if matches:
            print('Using verified device file:', name, flush=True)
        elif args.remote_dir:
            print(run('push', str(local), remote), flush=True)
        else:
            with local.open('rb') as stream:
                subprocess.run(adb + ['exec-in', 'run-as', 'com.galeriara', 'dd', 'of=' + remote, 'bs=65536'],
                               stdin=stream, check=True, timeout=180)
        if args.remote_dir:
            extra += ['-e', argument, shlex.quote(remote)]
    classes = ','.join('com.galeriara.data.encoding.' + name for name in [
        'CasedWordPieceTokenizerInstrumentedTest', 'ClipTextModelInstrumentedTest', 'ClipQueryEncoderInstrumentedTest'])
    output = run('shell', 'am', 'instrument', '-w', '-r', *extra, '-e', 'class', classes,
                 'com.galeriara.test/androidx.test.runner.AndroidJUnitRunner', timeout=600)
    args.output.with_suffix('.instrumentation.txt').write_text(output, 'utf-8')
    print(output, flush=True)
    if 'OK (13 tests)' not in output or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:
        raise RuntimeError('A07/A06 Android checks failed; inspect instrumentation output')
    report = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a07/android_report.json'))
    previous = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a06/android_report.json'))
    if report['status'] != 'passed' or report['model_revision'] != revision or previous['status'] != 'passed':
        raise RuntimeError('Android report/revision mismatch')
    report['passed_tests'] = 13
    report['adapter_tests'] = 11
    report['a06_regression'] = {k: v for k, v in previous.items() if k != 'results'}
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', 'utf-8')
    print('Verified A07 report:', args.output)


if __name__ == '__main__':
    main()
