import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ArtifactCleanupTest(unittest.TestCase):
    def test_cleanup_scopes_deletion_to_this_run_and_reports_api_failures(self):
        workflow = (ROOT / '.github/workflows/ci.yml').read_text()
        script = textwrap.dedent(workflow.split('  cleanup:\n', 1)[1].split('        run: |\n', 1)[1])
        for mode in ['artifacts', 'empty', 'list_failure', 'delete_failure']:
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as temporary:
                folder = Path(temporary)
                commands = folder / 'commands'
                gh = folder / 'gh'
                gh.write_text('''#!/bin/bash
printf '%s\n' "$*" >> "$CLEANUP_COMMANDS"
if [ "$2" = --paginate ]; then
  [ "$CLEANUP_MODE" != list_failure ] || exit 1
  [ "$CLEANUP_MODE" != empty ] || exit 0
  printf '101\n102\n'
else
  [ "$CLEANUP_MODE" != delete_failure ] || exit 1
fi
''')
                gh.chmod(0o755)
                result = subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', script],
                                        capture_output=True, text=True,
                                        env={**os.environ, 'PATH': str(folder) + ':' + os.environ['PATH'],
                                             'GITHUB_REPOSITORY': 'owner/repo', 'GITHUB_RUN_ID': '123',
                                             'CLEANUP_COMMANDS': str(commands), 'CLEANUP_MODE': mode})
                calls = commands.read_text().splitlines()
                self.assertTrue(calls[0].startswith('api --paginate repos/owner/repo/actions/runs/123/artifacts --jq '))
                self.assertEqual(result.returncode == 0, mode in ['artifacts', 'empty'], result.stderr)
                expected = ['api --method DELETE repos/owner/repo/actions/artifacts/101',
                            'api --method DELETE repos/owner/repo/actions/artifacts/102']
                self.assertEqual(calls[1:], expected if mode == 'artifacts' else expected[:1] if mode == 'delete_failure' else [])


if __name__ == '__main__':
    unittest.main()
