# Project status

## Current milestone

The MapLibre location-selection feasibility spike and result contract are complete.

## Implemented behavior

- MapLibre Compose renders the map using the OpenFreeMap Positron style.
- The initial camera opens around Brașov (`45.6579`, `25.6012`, zoom `13.0`).
- A fixed Compose center marker sits above the movable map.
- After camera movement settles, the camera target is copied into application-owned `SelectedCoordinates` and displayed with deterministic five-decimal formatting.
- The Material 3 slider stores an `Int` radius in metres: 100–1000m in 100m intervals, default 200m.
- Plain Kotlin builds a closed 64-segment spherical radius boundary (65 coordinates including the repeated first point).
- A map-boundary adapter converts that ring to a SpatialK GeoJSON polygon.
- MapLibre renders the polygon with a low-opacity fill and a 2dp outline. It updates after a radius change or settled camera-center change.
- `SelectedLocation` is a plain application-owned result containing the selected latitude, longitude, and radius.
- The Material 3 **Use this location** button emits that result through `LocationPickerScreen`'s required callback.
- `MainActivity` currently supplies a no-op callback because a reminder editor and navigation flow do not exist yet.

## Important boundaries

- MapLibre is the renderer; OpenFreeMap currently provides the style and tiles.
- `SelectedCoordinates`, `SelectedLocation`, radius selection, and radius geometry are application-owned.
- SpatialK and MapLibre types are confined to the map-integration boundary.
- The map needs internet for uncached or new areas.
- GPS and Android location runtime permissions are not implemented.
- Room, a reminder editor, geofencing, and notifications are not implemented.

## Relevant files

- Screen: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/LocationPickerScreen.kt`
- Activity entry point: `app/src/main/java/ro/andrei/ariciu/MainActivity.kt`
- Coordinates: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/model/SelectedCoordinates.kt`
- Picker result: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/model/SelectedLocation.kt`
- Coordinate formatting: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/SelectedCoordinatesFormatter.kt`
- Radius selection: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/RadiusSelection.kt`
- Radius geometry: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/geometry/RadiusGeometry.kt`
- GeoJSON adapter: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/map/RadiusMapGeometry.kt`
- Unit tests: `app/src/test/java/ro/andrei/ariciu/feature/locationpicker/`
- Compose instrumentation test: `app/src/androidTest/java/ro/andrei/ariciu/feature/locationpicker/ui/LocationPickerScreenTest.kt`
- Manifest: `app/src/main/AndroidManifest.xml`
- App dependencies: `app/build.gradle.kts`
- Version catalog: `gradle/libs.versions.toml`

## Verification

The currently passing checks are:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Re-run these before relying on this snapshot.

## Next chapter

Reminder Creation.

Immediate next step: create the reminder editor shell.
