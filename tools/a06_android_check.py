"""Run the two A06 reference tests on an explicitly selected Android device."""
import argparse
import hashlib
import json
from pathlib import Path
import posixpath
import shlex
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--dense', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--remote-dir', help='Optional writable device directory owned/readable by the app')
    parser.add_argument('--app-apk', type=Path)
    parser.add_argument('--test-apk', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    metadata = json.loads((root / 'app/src/androidTest/assets/a06/fixture.json').read_text('utf-8'))
    for path, key in [(args.model, 'model_sha256'), (args.dense, 'dense_float32_sha256')]:
        with path.open('rb') as stream:
            checksum = hashlib.file_digest(stream, 'sha256').hexdigest()
        if checksum != metadata[key]:
            parser.error(f'Component does not match the independent fixture: {path.name}')
    if bool(args.app_apk) != bool(args.test_apk):
        parser.error('Supply both APK paths together')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.unlink(missing_ok=True)
    adb = [args.adb, '-s', args.serial]

    def run(*command, timeout=180):
        result = subprocess.run(adb + list(command), check=True, capture_output=True, timeout=timeout)
        return result.stdout.decode('utf-8', errors='replace')

    if args.app_apk:
        print(run('install', '-r', str(args.app_apk)), flush=True)
        print(run('install', '-r', str(args.test_apk)), flush=True)
    run('shell', 'run-as', 'com.galeriara', 'mkdir', '-p', 'files/a06')
    if args.remote_dir:
        if not args.remote_dir.startswith('/'):
            parser.error('--remote-dir must be an absolute Android path')
        run('shell', 'mkdir', '-p', shlex.quote(args.remote_dir))
    extra = []
    for local, name, argument in [(args.model, 'model_quint8_avx2.onnx', 'modelPath'),
                                   (args.dense, 'clip_multilingual_dense.f32', 'densePath')]:
        if args.remote_dir:
            remote = posixpath.join(args.remote_dir, name)
            print(run('push', str(local), remote), flush=True)
            extra += ['-e', argument, shlex.quote(remote)]
        else:
            with local.open('rb') as stream:
                subprocess.run(adb + ['exec-in', 'run-as', 'com.galeriara', 'dd',
                                     f'of=files/a06/{name}', 'bs=65536'],
                               stdin=stream, check=True, capture_output=False, timeout=180)
    classes = ','.join('com.galeriara.data.encoding.' + name for name in
                       ['CasedWordPieceTokenizerInstrumentedTest', 'ClipTextModelInstrumentedTest'])
    output = run('shell', 'am', 'instrument', '-w', '-r', *extra, '-e', 'class', classes,
                 'com.galeriara.test/androidx.test.runner.AndroidJUnitRunner', timeout=600)
    args.output.with_suffix('.instrumentation.txt').write_text(output, 'utf-8')
    print(output, flush=True)
    if 'OK (2 tests)' not in output or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:
        raise RuntimeError('A06 Android checks did not pass; inspect instrumentation output')
    report = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a06/android_report.json'))
    tokenizer = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a06/tokenizer_report.json'))
    if report['status'] != 'passed' or tokenizer['status'] != 'passed' or report['model_sha256'] != metadata['model_sha256']:
        raise RuntimeError('Report/component mismatch')
    report['tokenizer'] = tokenizer
    args.output.write_text(json.dumps(report, indent=2) + '\n', 'utf-8')
    print('Verified A06 report:', args.output)


if __name__ == '__main__':
    main()
