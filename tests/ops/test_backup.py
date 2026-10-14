import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
FAKE = '''#!/usr/bin/env python3
import os, sys
from pathlib import Path
name = Path(sys.argv[0]).name
with open(os.environ['BACKUP_TEST_LOG'], 'a') as log:
    log.write(name + '\\n')
if os.environ.get('BACKUP_FAIL') == name:
    sys.exit(7)
if name == 'pg_dump':
    path = next(a.split('=', 1)[1] for a in sys.argv if a.startswith('--file='))
    Path(path).write_bytes(b'PGDMP-test')
'''


class BackupTest(unittest.TestCase):
    def run_backup(self, folder, failure=''):
        binaries = folder / 'bin'
        binaries.mkdir(exist_ok=True)
        for command in ['pg_dump', 'pg_restore', 'createdb', 'dropdb', 'psql']:
            executable = binaries / command
            executable.write_text(FAKE)
            executable.chmod(0o755)
        return subprocess.run(['bash', str(ROOT / 'ops/postgres/backup.sh')], capture_output=True, text=True,
                              env={**os.environ, 'PATH': str(binaries) + ':' + os.environ['PATH'],
                                   'LAPTOPGG_BACKUP_DIR': str(folder / 'backups'),
                                   'BACKUP_TEST_LOG': str(folder / 'calls'), 'BACKUP_FAIL': failure})

    def test_verified_archive_is_private_and_old_archives_are_pruned(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            backups = folder / 'backups'
            backups.mkdir()
            old = backups / 'laptopgg-old.dump'
            old.write_text('old backup')
            os.utime(old, (0, 0))
            result = self.run_backup(folder)
            self.assertEqual(result.returncode, 0, result.stderr)
            archives = list(backups.glob('laptopgg-*.dump'))
            self.assertEqual(len(archives), 1)
            self.assertEqual(stat.S_IMODE(archives[0].stat().st_mode), 0o600)
            self.assertFalse(old.exists())
            self.assertIn('pg_restore', (folder / 'calls').read_text())
            self.assertIn('dropdb', (folder / 'calls').read_text())

    def test_failed_dump_or_restore_keeps_existing_backups_and_cleans_partial_files(self):
        for failure in ['pg_dump', 'pg_restore']:
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temporary:
                folder = Path(temporary)
                backups = folder / 'backups'
                backups.mkdir()
                old = backups / 'laptopgg-old.dump'
                old.write_text('old backup')
                os.utime(old, (0, 0))
                result = self.run_backup(folder, failure)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(old.read_text(), 'old backup')
                self.assertEqual(list(backups.glob('.laptopgg-backup.*')), [])
                self.assertIn('dropdb', (folder / 'calls').read_text())


if __name__ == '__main__':
    unittest.main()
