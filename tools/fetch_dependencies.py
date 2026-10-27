"""Fetch original upstream artifacts over verified HTTPS; never edit or bundle them."""
from pathlib import Path
import hashlib, json, urllib.request
root = Path(__file__).resolve().parents[1]
(root / 'libs').mkdir(exist_ok=True)
for entry in json.loads((root / 'tools/dependencies.json').read_text()):
    target = root / 'libs' / entry['file']
    data = target.read_bytes() if target.exists() else urllib.request.urlopen(entry['url'], timeout=120).read()
    if hashlib.sha256(data).hexdigest() != entry['sha256']:
        raise SystemExit('Checksum mismatch: ' + entry['file'])
    if not target.exists():
        target.write_bytes(data)
    print('Verified ' + entry['file'])
