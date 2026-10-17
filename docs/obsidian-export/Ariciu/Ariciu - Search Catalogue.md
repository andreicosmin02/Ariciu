---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-2
version: 1.1-rc3
---

# Romania catalogue, search and data generation

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Chain Reminders]], [[Ariciu - Sources and Licensing]], [[Ariciu - Maintenance and Extension]].

Sources: [feature/places/LocalPlaces.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/places/LocalPlaces.kt), [PlaceCatalog.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/places/PlaceCatalog.kt); [app/src/main/assets/places/romania.tsv](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/places/romania.tsv), [NOTICE.md](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/places/NOTICE.md); [tools/build_place_catalog.py](https://github.com/andreicosmin02/Ariciu/blob/master/tools/build_place_catalog.py).

## Data and provenance

26,065 rows, including 231 malls; the original 25,834 rows are unchanged. OSM replication timestamp 2026-10-15T20:21:00Z; Geofabrik Romania extract downloaded 2026-10-16, rebuilt 2026-10-17. Raw PBF SHA256 is recorded in NOTICE.md. The normalized shipped TSV hash at review is `035189f011258310a54bd3b80bf816eb7311ce7a8903480f23a75616a24eba0c`.

Each TSV row has ID, name, kind, address, latitude, longitude and optional chain. Empty chain can be omitted at the end; the loader defaults it to an empty string. No user search history or OSM contributor metadata is included. Catalogue is a shipped ODbL derivative, independently available as a plain TSV in source and inside the APK.

## Search mechanics

`LocalPlaces.load` reads on Dispatchers.IO and caches a parsed list in the process. `CatalogPlace` is a plain model. Search normalizes Unicode accents and case, splits punctuation/whitespace into tokens and requires every query token to prefix a name/address token. Exact names, whole name words, name prefixes and address matches rank in that order; haversine distance and stable ID break ties. At most 30 results. Empty query returns none. UI computation runs on Dispatchers.Default with loading/failure/no-result states.

This is a local linear scan with normalization/ranking, not an FTS database, online autocomplete or a full address parser. No Geocoder, Nominatim, Overpass, Google Places or server sees query text. Map navigation afterwards still requests tiles when needed.

## Included/excluded geography

Generator includes named city/town/village/hamlet nodes and named supermarket/convenience/mall nodes/closed ways. Store name can fall back to brand. Ways use an average of available vertices, not an entrance or exact polygon centroid. Broad geographic bounds filter obvious outside records; border spillover may remain. Relations are not handled. Missing OSM tags/addresses/shops and changed shops remain gaps.

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


## Phone review correction (17 October)

The importer now includes named `shop=mall` nodes/closed ways, including AFI Brașov, Cotroceni and Ploiești from the same source. Malls never become supermarket-chain areas. Queries require whole-token prefixes; ranking prefers exact names, whole name words, name prefixes, then address matches, with distance/ID tie breakers. Accents and punctuation normalize locally. See [[Ariciu - Phone Review 1]].

## Second phone review — rc3

Search progress now distinguishes local catalogue load, active current-query work and completed results. Revision/cancellation guards prevent an old result overwriting a new query; clearing returns to idle and query restores across recreation. Retry is explicit. No ranking/data/remote-service change. See [[Ariciu - Phone Review 2]].

## Related reading

[Chain Reminders](Ariciu%20-%20Chain%20Reminders.md) · [Sources and Licensing](Ariciu%20-%20Sources%20and%20Licensing.md) · [Maintenance and Extension](Ariciu%20-%20Maintenance%20and%20Extension.md) · [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) · [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md)
