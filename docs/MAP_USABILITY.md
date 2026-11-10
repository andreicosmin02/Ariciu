# Map usability (Phase 1)

Authorized by the owner on 2026-11-10 (batch scope: icons, POI markers, reminders map, map controls, nearest-place label, area budget).
Constraints unchanged: free only, no backend/paid API, no new permissions, offline-first, no Room schema change, 100-geofence limit.
No permission, geofencing, alarm or notification behaviour was changed, so no platform-limit review was needed.

## What exists
- `AriciuIcons`: Store, Mall, Town, Pin, Arrive, Depart, BellSilent/Normal/Urgent, Clock, ZoomIn/Out, Compass, Layers. Hand-drawn 24dp strokes in the existing style, so no third-party asset and no provenance entry. `material-icons-extended` not added. Icons not yet used in screens: Arrive, Depart, BellSilent, BellNormal (available for later).
- Create: clustered catalogue markers (`PlaceMarkers.kt`) from zoom 12, filtered to a padded viewport box (max 1,500 rows), Shops/Malls/Towns chips (in the options sheet), tap a marker to centre it as the reminder location, offline nearest-place label, `N/100 areas used`.
- Reminders: List/Map toggle. Pins and radius circles for active reminders; status shown by colour, glyph and a text legend; tap a pin, then Open.
- Settings: `N/100 areas used`. This is the last recorded registration state, not a live read of Play services. PARTIAL / LIMIT_REACHED are treated as full.
- Map controls: zoom in/out, compass reset, recenter (Create: my location; Reminders: show all). Each target is at least 48dp and labelled.

## Measured (Android emulator Medium_Phone, API 37, debug build, swiftshader; not a phone)
- Catalogue load (26,065 rows, first read): 474 ms.
- Viewport filter + GeoJSON for zoom 14 around Brasov: 65-230 ms, 2-9 KB JSON. Recomputed only when the camera leaves the last padded box.

## Needs a physical phone
Marker/cluster performance on real GPUs, touch accuracy, glyph (cluster count) rendering offline, all geofence delivery.
Screenshots: `docs/design/screenshots/map-1/`.
