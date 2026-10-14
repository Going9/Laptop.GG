import os
from pathlib import Path
import shlex
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]


class DeployTest(unittest.TestCase):
    def test_success_restart_failure_and_failed_health_preserve_the_correct_release(self):
        workflow = (ROOT / '.github/workflows/deploy-web.yml').read_text()
        script = textwrap.dedent(workflow.split('      - name: Switch release and health check\n', 1)[1].split('          script: |\n', 1)[1])
        self.assertEqual(script.count('APP_DIR=/home/ubuntu/laptopgg'), 1)
        for mode in ['success', 'restart_failure', 'health_failure']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as temporary:
                folder = Path(temporary)
                for release in ['previous', 'new']:
                    directory = folder / 'releases' / release
                    directory.mkdir(parents=True)
                    (directory / f'app-{release}.jar').touch()
                previous = folder / 'releases/previous/app-previous.jar'
                (folder / 'app.jar').symlink_to(previous)
                binaries = folder / 'bin'
                binaries.mkdir()
                commands = {
                    'sudo': '#!/bin/sh\nexec "$@"\n',
                    'journalctl': '#!/bin/sh\nexit 0\n',
                    'curl': '#!/bin/sh\n[ "$DEPLOY_TEST_MODE" != health_failure ]\n',
                    'systemctl': '''#!/usr/bin/env python3
import os, sys
from pathlib import Path
counter = Path(os.environ['DEPLOY_TEST_COUNTER'])
count = int(counter.read_text()) if counter.exists() else 0
if sys.argv[1] == 'restart':
    counter.write_text(str(count + 1))
    sys.exit(1 if os.environ['DEPLOY_TEST_MODE'] == 'restart_failure' and count == 0 else 0)
if sys.argv[1] == 'is-active' and os.environ['DEPLOY_TEST_MODE'] == 'health_failure':
    sys.exit(1)
''',
                }
                for name, content in commands.items():
                    executable = binaries / name
                    executable.write_text(content)
                    executable.chmod(0o755)
                isolated = script.replace('${{ github.sha }}', 'new').replace('APP_DIR=/home/ubuntu/laptopgg', 'APP_DIR=' + shlex.quote(str(folder)))
                result = subprocess.run(['bash', '-c', isolated], capture_output=True, text=True,
                                        env={**os.environ, 'PATH': str(binaries) + ':' + os.environ['PATH'],
                                             'DEPLOY_TEST_MODE': mode, 'DEPLOY_TEST_COUNTER': str(folder / 'restarts')})
                target = (folder / 'app.jar').resolve()
                if mode == 'success':
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(target, folder / 'releases/new/app-new.jar')
                else:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertEqual(target, previous)
                    self.assertEqual((folder / 'restarts').read_text(), '2')


if __name__ == '__main__':
    unittest.main()
