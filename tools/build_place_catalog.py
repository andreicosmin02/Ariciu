#!/usr/bin/env python3
"""Generate a small ODbL catalogue from an official Geofabrik Romania .osm.pbf.
Development tool only: pip install osmium (BSD-2-Clause); no app/network dependency.
Usage: python tools/build_place_catalog.py INPUT.osm.pbf OUTPUT.tsv
Includes place nodes and selected supermarket nodes/closed ways, not relations.
"""
import sys, math, hashlib
from datetime import datetime, timezone
from pathlib import Path
import osmium
CHAINS = ['Kaufland', 'Lidl', 'Carrefour', 'Auchan', 'Penny', 'Mega Image', 'Profi']
rows = []
class Catalog(osmium.SimpleHandler):
    def add(self, obj, lat, lon, prefix):
        t = dict(obj.tags); name = t.get('name', '')
        place = t.get('place') in ['city', 'town', 'village', 'hamlet']
        if not place and t.get('shop') not in ['supermarket', 'convenience']: return
        if not name: name = t.get('brand', '')
        if not name or not (20 < lon < 30.5 and 43.5 < lat < 49): return
        chain = next((c for c in CHAINS if c.lower() in (t.get('brand','')+' '+name).lower()), '')
        address = ', '.join(filter(None, [t.get('addr:city',''), ' '.join(filter(None, [t.get('addr:street',''), t.get('addr:housenumber','')]))]))
        rows.append([prefix+str(obj.id), name, 'town' if place else 'store', address, f'{lat:.6f}', f'{lon:.6f}', chain])
    def node(self, n):
        if n.location.valid(): self.add(n, n.location.lat, n.location.lon, 'n')
    def way(self, w):
        if w.tags.get('shop') not in ['supermarket', 'convenience'] or not w.is_closed(): return
        coords = [(n.lat,n.lon) for n in w.nodes if n.location.valid()]
        if coords: self.add(w, sum(x[0] for x in coords)/len(coords), sum(x[1] for x in coords)/len(coords), 'w')
Catalog().apply_file(sys.argv[1], locations=True, idx='sparse_file_array')
# Prefer area objects, remove nearby duplicate node/area representations of the same shop.
out = []
for row in sorted(rows, key=lambda r: (r[2], not r[0].startswith('w'), r[0])):
    if row[2]=='store' and any(old[2]=='store' and (row[6] or row[1])==(old[6] or old[1])
        and math.hypot((float(row[4])-float(old[4]))*111000,(float(row[5])-float(old[5]))*78000)<100 for old in out): continue
    out.append(row)
p=Path(sys.argv[2]);p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(f'# OpenStreetMap contributors, ODbL 1.0. Geofabrik Romania extract. Generated {datetime.now(timezone.utc).date()}.\n'+
    '\n'.join('\t'.join(str(v).replace('\t',' ').replace('\n',' ') for v in r).rstrip() for r in out)+'\n')
print('Rows:',len(out),'Chains:',{c:sum(r[6]==c for r in out) for c in CHAINS})
print('Source SHA256:',hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest())
