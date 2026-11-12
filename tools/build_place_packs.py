#!/usr/bin/env python3
"""Build the downloadable "Search data" packs: one zip per European country plus index.json.
Development tool only (needs: pip install osmium; network to download.geofabrik.de; ~20 GB free disk for the largest country).
Usage: python tools/build_place_packs.py OUT_DIR [code ...]
For each country: download the Geofabrik extract, verify its MD5, run tools/build_place_catalog.py, zip towns.tsv + grid/,
delete the extract. Re-running skips packs already listed in OUT_DIR/index.json (delete an entry to rebuild it).
Pack layout (what the app unpacks into files/places/<code>/): towns.tsv, grid/index.txt, grid/c<lonCell>_<latCell>.tsv.
index.tsv (read by the app) and index.json (same data) list the packs: {"version":1,"generated":"YYYY-MM-DD","packs":[{code,name,file,bytes,sha256,rows,bbox:[w,s,e,n],source,generated}]}
Each extract is downloaded once, one after another (Geofabrik fair use); do not run this in a loop.
"""
import hashlib, json, os, shutil, subprocess, sys, urllib.request, zipfile
from datetime import datetime, timezone
from pathlib import Path

# code, Geofabrik europe/<id>, display name. Overlapping regional extracts (alps, dach, britain-and-ireland, great-britain) are left out.
COUNTRIES = [
    ('ad', 'andorra', 'Andorra'), ('al', 'albania', 'Albania'), ('at', 'austria', 'Austria'), ('ba', 'bosnia-herzegovina', 'Bosnia and Herzegovina'),
    ('be', 'belgium', 'Belgium'), ('bg', 'bulgaria', 'Bulgaria'), ('by', 'belarus', 'Belarus'), ('ch', 'switzerland', 'Switzerland'),
    ('cy', 'cyprus', 'Cyprus'), ('cz', 'czech-republic', 'Czechia'), ('de', 'germany', 'Germany'), ('dk', 'denmark', 'Denmark'),
    ('ee', 'estonia', 'Estonia'), ('es', 'spain', 'Spain'), ('fi', 'finland', 'Finland'), ('fo', 'faroe-islands', 'Faroe Islands'),
    ('fr', 'france', 'France'), ('gb', 'united-kingdom', 'United Kingdom'), ('ge', 'georgia', 'Georgia'), ('gg', 'guernsey-jersey', 'Guernsey and Jersey'),
    ('gr', 'greece', 'Greece'), ('hr', 'croatia', 'Croatia'), ('hu', 'hungary', 'Hungary'), ('ie', 'ireland-and-northern-ireland', 'Ireland'),
    ('im', 'isle-of-man', 'Isle of Man'), ('is', 'iceland', 'Iceland'), ('it', 'italy', 'Italy'), ('li', 'liechtenstein', 'Liechtenstein'),
    ('lt', 'lithuania', 'Lithuania'), ('lu', 'luxembourg', 'Luxembourg'), ('lv', 'latvia', 'Latvia'), ('mc', 'monaco', 'Monaco'),
    ('md', 'moldova', 'Moldova'), ('me', 'montenegro', 'Montenegro'), ('mk', 'macedonia', 'North Macedonia'), ('mt', 'malta', 'Malta'),
    ('nl', 'netherlands', 'Netherlands'), ('no', 'norway', 'Norway'), ('pl', 'poland', 'Poland'), ('pt', 'portugal', 'Portugal'),
    ('pt-azores', 'azores', 'Azores'), ('ro', 'romania', 'Romania'), ('rs', 'serbia', 'Serbia'), ('se', 'sweden', 'Sweden'),
    ('si', 'slovenia', 'Slovenia'), ('sk', 'slovakia', 'Slovakia'), ('tr', 'turkey', 'Turkey'), ('ua', 'ukraine', 'Ukraine'),
    ('xk', 'kosovo', 'Kosovo'),
]
HERE = Path(__file__).resolve().parent
BASE = 'https://download.geofabrik.de/europe/'


def fetch(url, dest):
    req = urllib.request.Request(url, headers={'User-Agent': 'arichu-place-packs/1 (+https://github.com/andreicosmin02/Arichu)'})
    with urllib.request.urlopen(req, timeout=120) as r, open(dest, 'wb') as f:
        shutil.copyfileobj(r, f, 1 << 20)
        return r.geturl()


def file_hash(path, algo):
    h = hashlib.new(algo)
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def bbox_of(d):
    xs, ys = [], []
    for s in (d / 'grid' / 'index.txt').read_text().split():
        ix, iy = s.split('_'); xs.append(int(ix)); ys.append(int(iy))
    w, e, s_, n = min(xs) * 0.25, (max(xs) + 1) * 0.25, min(ys) * 0.25, (max(ys) + 1) * 0.25
    for line in (d / 'towns.tsv').read_text().splitlines():
        f = line.split('\t')
        if len(f) > 5 and not line.startswith('#'):
            lat, lon = float(f[4]), float(f[5]); w, e, s_, n = min(w, lon), max(e, lon), min(s_, lat), max(n, lat)
    return [round(w, 3), round(s_, 3), round(e, 3), round(n, 3)]


def build(code, gid, name, out, work, py):
    pbf = work / f'{gid}.osm.pbf'
    final = fetch(f'{BASE}{gid}-latest.osm.pbf', pbf)
    want = urllib.request.urlopen(urllib.request.Request(final + '.md5', headers={'User-Agent': 'arichu-place-packs/1'}), timeout=60).read().split()[0].decode()
    got = file_hash(pbf, 'md5')
    if want != got:
        raise RuntimeError(f'md5 mismatch for {gid}: {got} != {want}')
    src_name = final.rsplit('/', 1)[1]
    catalog = work / code
    shutil.rmtree(catalog, ignore_errors=True)
    idx = work / 'nodes.idx'  # file-backed node locations: a country like France or Germany needs ~12 GB, too much RAM
    idx.unlink(missing_ok=True)
    try:
        subprocess.run([py, str(HERE / 'build_place_catalog.py'), str(pbf), str(catalog), f'Geofabrik {name} extract ({src_name})'],
                       check=True, env={**os.environ, 'NODE_INDEX': f'sparse_file_array,{idx}'})
    finally:
        idx.unlink(missing_ok=True)
    pbf.unlink()
    rows = sum(len([l for l in p.read_text().splitlines() if l and l[0] != '#']) for p in [catalog / 'towns.tsv', *(catalog / 'grid').glob('c*.tsv')])
    zpath = out / f'places-{code}.zip'
    with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        z.write(catalog / 'towns.tsv', 'towns.tsv')
        z.write(catalog / 'grid' / 'index.txt', 'grid/index.txt')
        for p in sorted((catalog / 'grid').glob('c*.tsv')):
            z.write(p, f'grid/{p.name}')
    box = bbox_of(catalog)
    shutil.rmtree(catalog)
    return {'code': code, 'name': name, 'file': zpath.name, 'bytes': zpath.stat().st_size, 'sha256': file_hash(zpath, 'sha256'),
            'rows': rows, 'bbox': box, 'source': src_name, 'generated': str(datetime.now(timezone.utc).date())}


def write_tsv(index, path):
    """The app reads this flat file (no JSON library needed): code, name, file, bytes, sha256, rows, west, south, east, north, generated."""
    lines = [f"# arichu place packs v1 {index.get('generated', '')}"]
    for p in index['packs']:
        w, s, e, n = p['bbox']
        lines.append('\t'.join(map(str, [p['code'], p['name'], p['file'], p['bytes'], p['sha256'], p['rows'], w, s, e, n, p['generated']])))
    path.write_text('\n'.join(lines) + '\n')


def main():
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    only = set(sys.argv[2:])
    work = Path(os.environ.get('PACK_WORK', out / 'work')); work.mkdir(parents=True, exist_ok=True)
    index_path = out / 'index.json'
    index = json.loads(index_path.read_text()) if index_path.exists() else {'version': 1, 'packs': []}
    done = {p['code']: p for p in index['packs']}
    for code, gid, name in COUNTRIES:
        if (only and code not in only) or code in done:
            continue
        try:
            print(f'== {code} {name}', flush=True)
            done[code] = build(code, gid, name, out, work, sys.executable)
            print('   ', done[code]['rows'], 'rows', done[code]['bytes'], 'bytes', flush=True)
        except Exception as e:  # keep going; the failed country is listed at the end
            print(f'FAILED {code}: {e}', flush=True)
            (work / f'{gid}.osm.pbf').unlink(missing_ok=True)
        index = {'version': 1, 'generated': str(datetime.now(timezone.utc).date()), 'packs': sorted(done.values(), key=lambda p: p['name'])}
        index_path.write_text(json.dumps(index, indent=1, ensure_ascii=False) + '\n')
        write_tsv(index, out / 'index.tsv')
    missing = [c for c, _, _ in COUNTRIES if c not in done and (not only or c in only)]
    print('Missing:', missing or 'none')


if __name__ == '__main__':
    main()
