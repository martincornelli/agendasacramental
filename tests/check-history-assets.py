"""Verify the packaged artifact, not just generated files in the build folder."""
import sys
import zipfile
from pathlib import Path

shared = [
    'participation-history.mjs', 'history-workbook.mjs', 'history-organizations.mjs',
    'history-workbook-worker.js', 'vendor/xlsx-0.20.3.full.min.js', 'vendor/SHEETJS-LICENSE.txt',
]
local = ['index.html', 'bootstrap.js', 'import.mjs', 'import.css']
for name in sys.argv[1:]:
    artifact = Path(name)
    prefix = 'base/assets/history/' if artifact.suffix == '.aab' else 'assets/history/'
    with zipfile.ZipFile(artifact) as package:
        for filename in shared + local:
            source = Path('docs' if filename in shared else 'app/src/main/assets/history') / filename
            assert package.read(prefix + filename) == source.read_bytes(), f'{artifact}: missing or stale {filename}'
    print(f'{artifact.name}: all {len(shared) + len(local)} history assets present and current')
if len(sys.argv) < 2:
    raise SystemExit('Pass one or more APK/AAB paths to verify')
