# Search data packs (Europe)

Owner-authorized 2026-11-11: map search works anywhere in Europe by downloading per-country place lists, instead of a search server. Still free, no backend, no account, no new permission, no Room change.

## How it works
- **Data**: one zip per country (`places-<code>.zip`: `towns.tsv`, `grid/index.txt`, `grid/c<lon>_<lat>.tsv`, same row format as the bundled Romania catalogue), plus `index.tsv` (what the app reads) and `index.json`, as assets of the GitHub release **`places-data`** (public, not marked latest). The release is updated in place; a changed pack hash in `index.tsv` means "update available".
- **App** (`feature/places/PlacePacks.kt` pure logic + tests, `PlacePackManager.kt`, `LocalPlaces.kt`, Settings > Search data): the user taps Load list / Download / Update / Remove. Download is sha256- and size-checked, only the pack layout is unpacked (no path escapes, size-limited), then swapped in atomically into `files/places/<code>/`. Free space is checked first. Searching reads the bundled Romania data plus every installed pack, all offline. An installed `ro` pack replaces the bundled Romania data.
- **Network**: only github.com release files, only on a button tap, one pack at a time, a User-Agent that names the app. Nothing about the user's location is sent.
- **Rebuild**: `python tools/build_place_packs.py OUT_DIR` (needs `pip install osmium`, ~20 GB free disk, downloads each Geofabrik extract once, sequentially). Then replace the assets: `gh release upload places-data OUT_DIR/*.zip OUT_DIR/index.* --clobber`.

## Limits and known gaps
- Europe = the Geofabrik `europe/` country extracts without the overlapping regional ones (alps, dach, britain-and-ireland, great-britain). Russia is not under `europe/` at Geofabrik and is not included. Turkey, Ukraine, Belarus, Georgia, Moldova, Kosovo are.
- Pack bounding boxes are rough (0.25 degree cells); they only drive the "no search data for X yet" hint.
- Borders: a place near a border may appear in two packs; results are de-duplicated by OSM id.
- An update is only as fresh as the last rebuild; there is no scheduled job.
- Licence: ODbL 1.0, © OpenStreetMap contributors (see the release notes and `app/src/main/assets/places/NOTICE.md`).
