---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Maps, radius selection and current position

Related: [[Ariciu - Search Catalogue]], [[Ariciu - Offline Maps]], [[Ariciu - Design and Accessibility]].

Sources: `feature/locationpicker/ui/LocationPickerScreen.kt`, `CurrentPosition.kt`, `RadiusSelection.kt`, `geometry/RadiusGeometry.kt`, `map/RadiusMapGeometry.kt`.

## Display boundary

MapLibre Compose 0.13.1 renders the map; SpatialK Position/Polygon types remain at the map boundary. SelectedCoordinates and SelectedLocation are plain Kotlin. There is no Google Maps Platform map or Places dependency.

Camera starts at Brașov unless a saved/initial selection exists. SnapshotFlow observes native camera movement; when it stops, the camera target becomes the selected point. Search/current-position results animate to zoom 15. Confirmation is disabled while moving. Map-load failure changes guidance text; a blank/missing tile still needs provider/cache/connectivity diagnosis.

The area boundary uses 64 spherical destination points plus the repeated first point, Earth radius 6,371,000m and normalized longitude. GeoJSON gets longitude/latitude in its expected order. Fill opacity is 0.14 and outline width 2dp. This visualization is not proof of Play services accuracy.

Presets: 100/200/500m; slider: 100–1000m in 100m steps, default 200. Coordinates are secondary, formatted to five decimal places with locale-independent decimal points. Search result names are not persisted into reminder labels.

## Styles and attribution

Display URIs: `asset://map/ariciu-graphite.json` and `asset://map/ariciu-light.json`. Dark is the custom graphite adaptation; light is bundled OpenFreeMap Positron. Dark/light have 47/55 style layers at this baseline. TileJSON is OpenFreeMap /planet; glyphs and sprites are provider-hosted; natural-earth raster background is another source.

Visible OpenMapTiles/OpenStreetMap credits remain, along with the native attribution control. Logo and imperial scale are disabled. Notices beside the styles cover OpenFreeMap, upstream code and design licences. Asset availability does not make all referenced map resources offline.

## My location

The button makes no request until pressed. An explanation precedes the first coarse+fine foreground permission request; later denial can direct to app settings. Approximate access is sufficient for centring. Reminder setup separately requires precise/background permission.

`currentMapPosition` checks permission and device Location, uses FusedLocationProviderClient.getCurrentLocation with HIGH_ACCURACY, allows fixes up to 30 seconds old, requests an 18-second native duration and wraps it in a 20-second coroutine timeout. CancellationTokenSource is cancelled with the coroutine. A null fix, exception, disabled Location or denial has a user-facing alternative (search/manual map).

The picker scope cancels on leaving composition. No continuous update subscription, blue-dot tracking loop, current-position stream or foreground GPS service exists. Native positioning quality/network assistance on a real phone is not validated by UI screenshots.

## Compact screens

Create uses a draggable Material 3 standard bottom sheet over the map, with an explicit expand/collapse action. Its summary height follows scaled content; expanded content scrolls within the available viewport. The primary action is outside the sheet. Coordinates are secondary behind Show coordinates. Search and a labeled 52dp My location icon share a padded row, bounded to 640dp.


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
