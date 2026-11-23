"""Run A08 Room persistence checks without downloading or loading a neural model."""
import argparse
import json
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--app-apk', type=Path)
    parser.add_argument('--test-apk', type=Path)
    args = parser.parse_args()
    if bool(args.app_apk) != bool(args.test_apk): parser.error('Supply both APK paths together')
    adb = [args.adb, '-s', args.serial]
    def run(*command, timeout=180):
        result = subprocess.run(adb + list(command), check=True, capture_output=True, timeout=timeout)
        return result.stdout.decode('utf-8', errors='replace')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.unlink(missing_ok=True)
    if args.app_apk:
        print(run('install', '-r', str(args.app_apk)), flush=True)
        print(run('install', '-r', str(args.test_apk)), flush=True)
    output = run('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                 'com.galeriara.data.vectors.RoomVectorRepositoryInstrumentedTest',
                 'com.galeriara.test/androidx.test.runner.AndroidJUnitRunner', timeout=300)
    args.output.with_suffix('.instrumentation.txt').write_text(output, 'utf-8')
    print(output, flush=True)
    if 'OK (12 tests)' not in output or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:
        raise RuntimeError('A08 Room tests did not pass; inspect instrumentation output')
    report = json.loads(run('exec-out', 'run-as', 'com.galeriara', 'cat', 'files/a08/android_report.json'))
    if report['status'] != 'passed' or report['bitwise_round_trip'] is not True:
        raise RuntimeError('A08 report did not confirm lossless persistence')
    report['passed_tests'] = 12
    args.output.write_text(json.dumps(report, indent=2) + '\n', 'utf-8')
    print('Verified A08 report:', args.output)


if __name__ == '__main__':
    main()
