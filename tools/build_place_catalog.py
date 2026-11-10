#!/usr/bin/env python3
"""Generate the offline ODbL place catalogue from a Geofabrik Romania .osm.pbf.
Development tool only: pip install osmium (BSD-2-Clause); no app/network dependency.
Usage: python tools/build_place_catalog.py INPUT.osm.pbf OUTPUT_DIR
Writes OUTPUT_DIR/towns.tsv (all towns/villages), OUTPUT_DIR/grid/c<ix>_<iy>.tsv (everything else, one file per
0.25 degree cell) and OUTPUT_DIR/grid/index.txt (list of cells that exist).
Row: id <TAB> name <TAB> kind(town|mall|store) <TAB> address <TAB> lat <TAB> lon <TAB> brand <TAB> category
Includes nodes and closed ways, not relations. The brand tag is kept as searchable text ("Lidl" works for any brand).
"""
import sys, math, hashlib
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
import osmium

CELL = 0.25
CATEGORIES = ['supermarket', 'pharmacy', 'fuel', 'bank', 'post', 'mall', 'food', 'health', 'hardware', 'bakery', 'town']
SHOP = {'supermarket': 'supermarket', 'convenience': 'supermarket', 'mall': 'mall', 'doityourself': 'hardware',
        'hardware': 'hardware', 'bakery': 'bakery'}
AMENITY = {'pharmacy': 'pharmacy', 'fuel': 'fuel', 'bank': 'bank', 'atm': 'bank', 'post_office': 'post',
           'restaurant': 'food', 'cafe': 'food', 'fast_food': 'food', 'hospital': 'health', 'clinic': 'health'}
PLACES = {'city', 'town', 'village', 'hamlet'}


def classify(t):
    if t.get('place') in PLACES:
        return 'town'
    return SHOP.get(t.get('shop', '')) or AMENITY.get(t.get('amenity', ''))


rows = []


class Catalog(osmium.SimpleHandler):
    def add(self, obj, lat, lon, prefix):
        t = dict(obj.tags)
        category = classify(t)
        if not category or not (20 < lon < 30.5 and 43.5 < lat < 49):
            return
        brand = t.get('brand', '').strip()
        name = (t.get('name') or brand or t.get('operator', '')).strip()
        if not name:
            return
        address = ', '.join(filter(None, [t.get('addr:city', ''), ' '.join(filter(None, [t.get('addr:street', ''), t.get('addr:housenumber', '')]))]))
        kind = 'town' if category == 'town' else ('mall' if category == 'mall' else 'store')
        rows.append([prefix + str(obj.id), name, kind, address, f'{lat:.6f}', f'{lon:.6f}', brand if category != 'mall' else '', category])

    def node(self, n):
        if n.location.valid():
            self.add(n, n.location.lat, n.location.lon, 'n')

    def way(self, w):
        if 'place' in w.tags or not w.is_closed() or classify(dict(w.tags)) in (None, 'town'):
            return
        coords = [(n.lat, n.lon) for n in w.nodes if n.location.valid()]
        if coords:
            self.add(w, sum(x[0] for x in coords) / len(coords), sum(x[1] for x in coords) / len(coords), 'w')


def key(r):
    return (r[7], (r[6] or r[1]).lower())


src = Path(sys.argv[1]); out_dir = Path(sys.argv[2])
Catalog().apply_file(str(src), locations=True, idx='sparse_file_array')
# Prefer area objects; drop a node/area duplicate of the same brand and category within ~100 m (hash grid, not O(n^2)).
seen = defaultdict(list)
out = []
for row in sorted(rows, key=lambda r: (r[7], not r[0].startswith('w'), r[0])):
    lat, lon = float(row[4]), float(row[5])
    if row[7] != 'town':
        cx, cy = int(lon / 0.002), int(lat / 0.001)
        if any(key(o) == key(row) and math.hypot((lat - float(o[4])) * 111000, (lon - float(o[5])) * 78000) < 100
               for dx in (-1, 0, 1) for dy in (-1, 0, 1) for o in seen[(cx + dx, cy + dy)]):
            continue
        seen[(cx, cy)].append(row)
    out.append(row)

header = f'# OpenStreetMap contributors, ODbL 1.0. Geofabrik Romania extract. Generated {datetime.now(timezone.utc).date()}.\n'
fmt = lambda rs: header + '\n'.join('\t'.join(str(v).replace('\t', ' ').replace('\n', ' ') for v in r).rstrip() for r in rs) + '\n'
(out_dir / 'grid').mkdir(parents=True, exist_ok=True)
for old in (out_dir / 'grid').glob('*'):
    old.unlink()
cells = defaultdict(list)
towns = []
for r in sorted(out, key=lambda r: r[0]):
    if r[7] == 'town':
        towns.append(r)
    else:
        cells[(int(math.floor(float(r[5]) / CELL)), int(math.floor(float(r[4]) / CELL)))].append(r)
(out_dir / 'towns.tsv').write_text(fmt(towns))
for (ix, iy), rs in cells.items():
    (out_dir / 'grid' / f'c{ix}_{iy}.tsv').write_text(fmt(rs))
(out_dir / 'grid' / 'index.txt').write_text('\n'.join(f'{ix}_{iy}' for ix, iy in sorted(cells)) + '\n')
print('Rows:', len(out), 'Towns:', len(towns), 'Cells:', len(cells))
print('Per category:', {c: sum(r[7] == c for r in out) for c in CATEGORIES})
print('Source SHA256:', hashlib.sha256(src.read_bytes()).hexdigest())
