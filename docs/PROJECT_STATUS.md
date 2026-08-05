# Project status

## Current milestone

The MapLibre location-selection feasibility spike is complete.

## Implemented behavior

- MapLibre Compose renders the map using the OpenFreeMap Positron style.
- The initial camera opens around Brașov (`45.6579`, `25.6012`, zoom `13.0`).
- A fixed Compose center marker sits above the movable map.
- After camera movement settles, the camera target is copied into application-owned `SelectedCoordinates` and displayed with deterministic five-decimal formatting.
- The Material 3 slider stores an `Int` radius in metres: 100–1000m in 100m intervals, default 200m.
- Plain Kotlin builds a closed 64-segment spherical radius boundary (65 coordinates including the repeated first point).
- A map-boundary adapter converts that ring to a SpatialK GeoJSON polygon.
- MapLibre renders the polygon with a low-opacity fill and a 2dp outline. It updates after a radius change or settled camera-center change.

## Important boundaries

- MapLibre is the renderer; OpenFreeMap currently provides the style and tiles.
- `SelectedCoordinates`, radius selection, and radius geometry are application-owned.
- SpatialK and MapLibre types are confined to the map-integration boundary.
- The map needs internet for uncached or new areas.
- GPS and Android location runtime permissions are not implemented.
- Room, a reminder editor, geofencing, and notifications are not implemented.

## Relevant files

- Screen: `app/src/main/java/ro/andrei/ariciu/LocationPickerScreen.kt`
- Activity entry point: `app/src/main/java/ro/andrei/ariciu/MainActivity.kt`
- Coordinates: `app/src/main/java/ro/andrei/ariciu/SelectedCoordinates.kt`
- Coordinate formatting: `app/src/main/java/ro/andrei/ariciu/SelectedCoordinatesFormatter.kt`
- Radius selection: `app/src/main/java/ro/andrei/ariciu/RadiusSelection.kt`
- Radius geometry: `app/src/main/java/ro/andrei/ariciu/RadiusGeometry.kt`
- GeoJSON adapter: `app/src/main/java/ro/andrei/ariciu/RadiusMapGeometry.kt`
- Unit tests: `app/src/test/java/ro/andrei/ariciu/`
- Compose instrumentation test: `app/src/androidTest/java/ro/andrei/ariciu/ExampleInstrumentedTest.kt`
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

Immediate next step:

- introduce a plain `SelectedLocation` result containing `latitude`, `longitude`, and `radiusMeters`;
- add a Material 3 **Use this location** button;
- emit the current value through a callback;
- do not add navigation, Room, GPS, permissions, or geofencing in that step.
