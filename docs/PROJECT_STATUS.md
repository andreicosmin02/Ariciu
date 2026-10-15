# Project status

## Current milestone

The MapLibre location-selection spike and result contract are complete. A title-only reminder editor is the current app entry point. The autonomous Codex workflow and repository skills are installed.

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
- `MainActivity` launches the title-only `ReminderEditorScreen`; the picker is not connected yet.

## Important boundaries

- MapLibre is the renderer; OpenFreeMap currently provides the style and tiles.
- `SelectedCoordinates`, `SelectedLocation`, radius selection, and radius geometry are application-owned.
- SpatialK and MapLibre types are confined to the map-integration boundary.
- The map needs internet for uncached or new areas.
- GPS and Android location runtime permissions are not implemented.
- Room, a complete reminder editor, geofencing, and notifications are not implemented.

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

Previous checks were recorded as passing; this is historical evidence, not a claim that setup changes reran them:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Re-run these before relying on this snapshot.

## Reminder Creation

- `ReminderEditorScreen` provides a title-only editor shell with a single-line Material 3 text field.
- The title is held in local `rememberSaveable` Compose state.
- `MainActivity` currently launches `ReminderEditorScreen` directly.
- The completed location picker is not connected to the editor yet.

## Development workflow and next feature batch

- `AGENTS.md` and `.agents/skills/` now define autonomous feature-batch development with product-owner input for material decisions.
- The setup ZIP was adapted to remove its mandatory small-milestone stops; repository skills follow the official Codex repository-local `.agents/skills/` convention.
- Product direction: map-first home screen. Choose a place, then enter reminder details; provide access to saved reminders from the map.
- Next batch: connect location selection and reminder editing, add ENTER/EXIT choices, and persist/manage reminders locally with Room.
- Initial defaults: ENTER and once-only. Alarm intensity and chained reminders remain outside MVP.
- Subsequent batch: staged permissions, supported notifications, geofence registration/recovery, and duplicate-event handling. Saved reminders must not be presented as actively monitored before this is implemented.
