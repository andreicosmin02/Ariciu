---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Romania catalogue, search and data generation

Related: [[Ariciu - Chain Reminders]], [[Ariciu - Sources and Licensing]], [[Ariciu - Maintenance and Extension]].

Sources: `feature/places/LocalPlaces.kt`, `PlaceCatalog.kt`; `app/src/main/assets/places/romania.tsv`, `NOTICE.md`; `tools/build_place_catalog.py`.

## Data and provenance

25,834 rows. OSM replication timestamp 2026-10-15T20:21:00Z; Geofabrik Romania extract downloaded/built 2026-10-16. Raw PBF SHA256 is recorded in NOTICE.md. The normalized shipped TSV hash at review is `b6c51e76f6fc0a242458c5518d210aba47ff47ab1037be43b64786613eafc467`.

Each TSV row has ID, name, kind, address, latitude, longitude and optional chain. Empty chain can be omitted at the end; the loader defaults it to an empty string. No user search history or OSM contributor metadata is included. Catalogue is a shipped ODbL derivative, independently available as a plain TSV in source and inside the APK.

## Search mechanics

`LocalPlaces.load` reads on Dispatchers.IO and caches a parsed list in the process. `CatalogPlace` is a plain model. Search normalizes Unicode accents and case, splits whitespace into query words and requires all words in name+address. Exact normalized name matches rank first, then haversine distance from the current map centre, then stable ID. At most 30 results. Empty query returns none. UI computation runs on Dispatchers.Default with loading/failure/no-result states.

This is a local linear scan with normalization/ranking, not an FTS database, online autocomplete or a full address parser. No Geocoder, Nominatim, Overpass, Google Places or server sees query text. Map navigation afterwards still requests tiles when needed.

## Included/excluded geography

Generator includes named city/town/village/hamlet nodes and named supermarket/convenience nodes/closed ways. Store name can fall back to brand. Ways use an average of available vertices, not an entrance or exact polygon centroid. Broad geographic bounds filter obvious outside records; border spillover may remain. Relations are not handled. Missing OSM tags/addresses/shops and changed shops remain gaps.

Nearby store duplicates with the same chain/name are removed within an approximate 100m threshold, preferring ways. This heuristic can merge nearby real branches or miss naming variants; it is not an authoritative retailer directory.

Seven recognized chains and shipped counts: Kaufland 200, Lidl 390, Carrefour 370, Auchan 84, Penny 384, Mega Image 436, Profi 1,684. These are catalogue entries, not completeness claims or automatically active geofences.

## Regeneration workflow

Development-only PyOsmium parses a lawful free regional PBF. The app never downloads/imports that large extract at runtime.

```bash
python3 -m venv /tmp/ariciu-catalog-tools
/tmp/ariciu-catalog-tools/bin/pip install osmium
/tmp/ariciu-catalog-tools/bin/python tools/build_place_catalog.py /path/to/romania.osm.pbf app/src/main/assets/places/romania.tsv
```

Before shipping regenerated data: verify provider/licence terms, source timestamp/hash, row/chain counts, coordinate bounds, duplication heuristic, file encoding and representative searches. Update NOTICE.md, UI date strings in picker/editor/Settings and status/evidence docs together. The script prints counts/hash and puts the UTC generation date in the header; it does not automatically update all these other metadata locations. Do not check the huge raw extract, temporary virtualenv or personal files into Git.

Existing chain snapshots remain until explicitly refreshed; app update alone does not silently change selected stores. Automatically tested: normalization, address matching, nearby ranking and nearest-chain selection on small fixtures. Full catalogue freshness/completeness, every result and generator-scale performance are not automatically established.
