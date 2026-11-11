# Map usability (Phase 1)

Authorized by the owner on 2026-11-10 (batch scope: icons, POI markers, reminders map, map controls, nearest-place label, area budget).
Constraints unchanged: free only, no backend/paid API, no new permissions, offline-first, no Room schema change, 100-geofence limit.
No permission, geofencing, alarm or notification behaviour was changed, so no platform-limit review was needed.

## What exists
- `AriciuIcons`: Store, Mall, Town, Pin, Arrive, Depart, BellSilent/Normal/Urgent, Clock, ZoomIn/Out, Compass, Layers. Hand-drawn 24dp strokes in the existing style, so no third-party asset and no provenance entry. `material-icons-extended` not added. Icons not yet used in screens: Arrive, Depart, BellSilent, BellNormal (available for later).
- Create: superseded by map search, see "Map search (2026-11-10)" below. Item 2 (always-on catalogue markers) no longer exists.
- Reminders: List/Map toggle. Pins and radius circles for active reminders; status shown by colour, glyph and a text legend; tap a pin, then Open.
- Settings: `N/100 areas used`. This is the last recorded registration state, not a live read of Play services. PARTIAL / LIMIT_REACHED are treated as full.
- Map controls: zoom in/out, compass reset, recenter (Create: my location; Reminders: show all). Each target is at least 48dp and labelled.

## Measured (Android emulator Medium_Phone, API 37, debug build, swiftshader; not a phone)
- Catalogue load (26,065 rows, first read): 474 ms.
- Viewport filter + GeoJSON for zoom 14 around Brasov: 65-230 ms, 2-9 KB JSON. Recomputed only when the camera leaves the last padded box.

## Map search (2026-11-10, replaces item 2)
Plan: [MAP_SEARCH_PLAN.md](MAP_SEARCH_PLAN.md). Multi-place reminders reuse chainAreas: [DELIVERY_ARCHITECTURE.md](DELIVERY_ARCHITECTURE.md).
- Catalogue: Geofabrik romania-261108 (SHA256 and ODbL notice in `app/src/main/assets/places/NOTICE.md`, `docs/third-party/provenance.json`). 52,533 rows, 11 categories, brand kept as searchable text. `towns.tsv` (13,971 rows) plus 482 grid cells of 0.25 degrees.
- Search: field at the top, category chips, free text (name, brand, category words in English and Romanian, address, accent-insensitive). At most 40 results within 25 km of the reference point; a town can match by name from anywhere. Results are clustered category pins plus a list in the bottom sheet (peek or half height). Tapping a pin selects its row and the other way round. "Search this area" appears after the map moves.
- Reference point is the My location fix when there is one, else the map centre. The nearest result is selected and the camera fits the results. Long-press drops a manual pin and returns to the crosshair mode. The clear button also returns to it.
- Two actions: "Remind me at this place" (title and point prefilled) and "Remind me at all N places" (1-40 areas, label in chainName). Both are blocked with a message when they would exceed the shared 100 areas.
- Removed: the chain chips, the chain dialog, StorePreviewScreen, the old search dialog and romania.tsv. Legacy chain reminders still load, display and edit (instrumented migration and release-flow tests pass).

Measured on the Android emulator (API 37, swiftshader; not a phone):
- Added APK size about 0.83 MB (assets deflate to about 1.35 MB, previously 0.53 MB). Budget 5 MB.
- Cold catalogue load: first search around Brasov reads 12 grid cells (about 1,500 rows, pre-folded for search) in 44 ms for a category chip, 150-350 ms when free text also loads the towns file. Budget 500 ms.
- Ranking: 2-94 ms warm for 1,500 candidates (a 3.4 s first version was fixed by pre-folding text and precompiling regexes); first query after launch can take about 270 ms while the JIT warms. Budget 100 ms warm.

Not verified: accuracy circle and My location (no fix was provided to the emulator), TalkBack, rotating the map with two fingers, offline tile behaviour.

## Needs a physical phone
Pin and cluster rendering on real GPUs (a long-running emulator session once corrupted pin sprites; a fresh one rendered them correctly), search speed on a mid-range phone, touch accuracy, glyph (cluster count) rendering offline, all geofence delivery.
Screenshots: `docs/design/screenshots/map-1/`.

## Download area (2026-11-11, branch feat/map-offline-download)
Free, no new dependency, permission, host or backend. Uses OfflineMaps, MapNetwork and DownloadBudget.
- **Limits** (OpenFreeMap: <= 3,000 tiles or 50 MB per download, <= 4 parallel requests, unique User-Agent). App margins unchanged: hard stops 2,500 tiles / 45 MB (`DownloadBudget`), plans sized to 2,000 tiles / 40 MB. Never raise them. Up to three areas; at the limit the user picks one to replace (confirmation by choosing it).
- **UI:** "Download area" in the map controls (48dp, icon + label) shows the planned box on the map, an estimate and Download/Cancel; progress, cancel (removes the partial pack), failures and retry are inline. Saved areas of the current theme are thin outlines. When the view is inside a complete same-theme pack the control reads "Downloaded". Packs are theme-specific and the panel says so. Settings still manages and deletes.
- **Sizing** (`TileMath`, `planDownload`): the largest box that fits 2,000 tiles / 40 MB at zoom 10-14, centred on the visible map and with its shape, found by bisection. The camera then zooms in or out by itself to show the whole box (Cancel returns to the previous view); there is no "zoom in" refusal. Saved areas are drawn as faint dashed outlines, the plan as the only strong outline.
- **Estimate:** tiles x bytes per tile + fonts/styling overhead. Defaults are conservative (350 KB per tile, 20 MB overhead); once a pack completed, the densest measured value (+20%) and measured overhead are used. Labelled as an estimate.

Measured on real OpenFreeMap downloads (emulator, 8 packs):
| City | tiles | bytes per tile | total |
|---|---|---|---|
| Brasov RO | 8 | 85 KB | 18.7 MB |
| Vienna AT | 8 | 262 KB | 20.1 MB |
| Lisbon PT | 14 | 166 KB | 20.3 MB |
| Warsaw PL | 6 / 24 | 208 / 178 KB | 19.3 / 22.3 MB |
| Paris FR | 6 | 336 KB | 20.0 MB |
| Berlin DE | 9 | 181 KB | 19.7 MB |
| Madrid ES | 7 | n/a | 19.6 MB |
| Rome IT (dark) | 9 | 182 KB | 7.7 MB |
Planned tile counts matched actual counts in every pack. A first estimate was 3.6 MB because the ~18 MB of fonts and styling per pack was missing, and a Vienna-sized average underestimated Paris; both are fixed. Cancel, replace at three areas, offline failure ("paused after 90 seconds", Try again resumed and completed) and the Downloaded state were each exercised on the emulator.

Not verified: a pack at the hard limits with measured (not default) tile sizes, phone download speed, storage pressure, and layouts at 2x text and 320dp are usable but the map area is small there.
