"""Run the fixed A05 instrumentation comparison on an explicitly selected device."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--model', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--remote-model', help='Optional writable device path; otherwise use app-private files')
    parser.add_argument('--app-apk', type=Path)
    parser.add_argument('--test-apk', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    metadata = json.loads((root / 'app/src/androidTest/assets/a05/fixture.json').read_text('utf-8'))
    with args.model.open('rb') as stream:
        checksum = hashlib.file_digest(stream, 'sha256').hexdigest()
    if checksum != metadata['model_sha256']:
        parser.error('Model does not match the checked-in fixture SHA-256')
    adb = [args.adb, '-s', args.serial]

    def run(*command, timeout=180):
        result = subprocess.run(adb + list(command), check=True, capture_output=True, timeout=timeout)
        return result.stdout.decode('utf-8', errors='replace')

    if bool(args.app_apk) != bool(args.test_apk):
        parser.error('Supply both APK paths together')
    if args.app_apk:
        print(run('install', '-r', str(args.app_apk)), flush=True)
        print(run('install', '-r', str(args.test_apk)), flush=True)
    run('shell', 'run-as', 'com.galeriara', 'mkdir', '-p', 'files/a05')
    if args.remote_model:
        # The caller explicitly supplies this device path, e.g. the app's SD-card directory.
        run('shell', 'mkdir', '-p', str(Path(args.remote_model).parent).replace('\\', '/'))
        print(run('push', str(args.model), args.remote_model), flush=True)
    else:
        with args.model.open('rb') as stream:
            subprocess.run(adb + ['exec-in', 'run-as', 'com.galeriara', 'sh', '-c',
                                 'cat > files/a05/clip_vit_b32_visual_fp32.onnx'],
                           stdin=stream, check=True, timeout=180)
    command = ['shell', 'am', 'instrument', '-w', '-r']
    if args.remote_model:
        command += ['-e', 'modelPath', args.remote_model]
    command += ['-e', 'class', 'com.galeriara.data.encoding.ClipVisualModelInstrumentedTest',
                'com.galeriara.test/androidx.test.runner.AndroidJUnitRunner']
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Remove any previous report so a failed run cannot appear successful.
    args.output.unlink(missing_ok=True)
    output = run(*command, timeout=600)
    args.output.with_suffix('.instrumentation.txt').write_text(output, encoding='utf-8')
    print(output, flush=True)
    if 'OK (1 test)' not in output or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:
        raise RuntimeError('Android reference comparison did not pass; inspect instrumentation output')
    report = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a05/android_report.json'))
    if report['status'] != 'passed' or report['model_sha256'] != checksum:
        raise RuntimeError('Android report/model mismatch')
    args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('Verified Android report:', args.output)


if __name__ == '__main__':
    main()
