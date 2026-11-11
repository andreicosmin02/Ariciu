#!/usr/bin/env python3
"""Generate an offline ODbL place catalogue (one country or region) from a Geofabrik .osm.pbf.
Development tool only: pip install osmium (BSD-2-Clause); no app/network dependency.
Usage: python tools/build_place_catalog.py INPUT.osm.pbf OUTPUT_DIR [SOURCE_LABEL]
(tools/build_place_packs.py runs this for every European country and zips the results for the app's "Search data".)
Writes OUTPUT_DIR/towns.tsv (all towns/villages), OUTPUT_DIR/grid/c<ix>_<iy>.tsv (everything else, one file per
0.25 degree cell) and OUTPUT_DIR/grid/index.txt (list of cells that exist).
Row: id <TAB> name <TAB> kind(town|mall|store) <TAB> address <TAB> lat <TAB> lon <TAB> brand <TAB> category [<TAB> folded name, towns.tsv only]
Includes nodes and closed ways, not relations. The brand tag is kept as searchable text ("Lidl" works for any brand).
"""
import os, sys, math, hashlib, unicodedata
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
import osmium
import osmium.filter

CELL = 0.25
CATEGORIES = ['supermarket', 'pharmacy', 'fuel', 'bank', 'post', 'mall', 'food', 'health', 'hardware', 'bakery', 'town']
SHOP = {'supermarket': 'supermarket', 'convenience': 'supermarket', 'mall': 'mall', 'doityourself': 'hardware',
        'hardware': 'hardware', 'bakery': 'bakery'}
AMENITY = {'pharmacy': 'pharmacy', 'fuel': 'fuel', 'bank': 'bank', 'atm': 'bank', 'post_office': 'post',
           'restaurant': 'food', 'cafe': 'food', 'fast_food': 'food', 'hospital': 'health', 'clinic': 'health'}
PLACES = {'city', 'town', 'village', 'hamlet'}


def fold(text):
    """Accent-free lowercase, like the app's foldForSearch; towns.tsv carries it as a 9th column so the app can scan millions of towns without parsing them."""
    return ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn').lower()


def classify(t):
    if t.get('place') in PLACES:
        return 'town'
    return SHOP.get(t.get('shop', '')) or AMENITY.get(t.get('amenity', ''))


rows = []


def add(obj, lat, lon, prefix):
    t = dict(obj.tags)
    category = classify(t)
    if not category:
        return
    brand = t.get('brand', '').strip()
    name = (t.get('name') or brand or t.get('operator', '')).strip()
    if not name:
        return
    address = ', '.join(filter(None, [t.get('addr:city', ''), ' '.join(filter(None, [t.get('addr:street', ''), t.get('addr:housenumber', '')]))]))
    kind = 'town' if category == 'town' else ('mall' if category == 'mall' else 'store')
    rows.append([prefix + str(obj.id), name, kind, address, f'{lat:.6f}', f'{lon:.6f}', brand if category != 'mall' else '', category])


def collect(path):
    """Named nodes and closed ways with a catalogue tag; the C++ key filter keeps Python out of the untagged majority."""
    fp = osmium.FileProcessor(path, osmium.osm.NODE | osmium.osm.WAY).with_locations(os.environ.get('NODE_INDEX', 'flex_mem')) \
        .with_filter(osmium.filter.KeyFilter('shop', 'amenity', 'place'))
    for o in fp:
        if o.is_node():
            if o.location.valid():
                add(o, o.location.lat, o.location.lon, 'n')
        elif 'place' not in o.tags and o.is_closed() and classify(dict(o.tags)) not in (None, 'town'):
            coords = [(n.lat, n.lon) for n in o.nodes if n.location.valid()]
            if coords:
                add(o, sum(x[0] for x in coords) / len(coords), sum(x[1] for x in coords) / len(coords), 'w')


def key(r):
    return (r[7], (r[6] or r[1]).lower())


src = Path(sys.argv[1]); out_dir = Path(sys.argv[2]); label = sys.argv[3] if len(sys.argv) > 3 else 'Geofabrik extract'
collect(str(src))
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

header = f'# OpenStreetMap contributors, ODbL 1.0. {label}. Generated {datetime.now(timezone.utc).date()}.\n'
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
(out_dir / 'towns.tsv').write_text(fmt([r + [fold(r[1])] for r in towns]))
for (ix, iy), rs in cells.items():
    (out_dir / 'grid' / f'c{ix}_{iy}.tsv').write_text(fmt(rs))
(out_dir / 'grid' / 'index.txt').write_text('\n'.join(f'{ix}_{iy}' for ix, iy in sorted(cells)) + '\n')
print('Rows:', len(out), 'Towns:', len(towns), 'Cells:', len(cells))
print('Per category:', {c: sum(r[7] == c for r in out) for c in CATEGORIES})
print('Source SHA256:', hashlib.sha256(src.read_bytes()).hexdigest())
