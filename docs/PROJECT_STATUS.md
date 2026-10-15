# Project status

## Current implementation

The autonomous Codex workflow is configured. The first feature batch provides a map-first reminder creation and local management flow. Geofencing and notification delivery are not implemented yet.

## User-facing behavior

- The app opens on the MapLibre map around Brașov (`45.6579`, `25.6012`, zoom `13.0`). A center marker and radius polygon show the chosen area.
- Radius selection is 100–1000 metres in 100m intervals, default 200m.
- **Use this location** opens the Material 3 reminder editor. Users enter a title, choose **Arrive** (ENTER) or **Leave** (EXIT), and optionally repeat every visit. Defaults are ENTER and once-only.
- Saving requires a nonblank title and valid location/radius. Titles are trimmed.
- **Your reminders** opens locally saved reminders. Users can edit the title, place/radius, transition, and repeat choice, or delete with confirmation.
- Cancelling a named draft or replacing it with another reminder asks before discarding changes. Android Back returns from location changes to the pending editor.
- Draft fields, route, and save/edit identity use `SavedStateHandle`; the picker retains coordinates/radius with saveable Compose state. This supports recreation through Android saved-state restoration, not a guarantee of drafts surviving force-stop or task removal.
- Save retries reuse the same ID and repeated save taps are ignored while saving. Load/save/delete failures show actionable errors; failed saves retain the draft.
- Saved reminder screens explicitly say delivery is not enabled yet. Repeat/transition choices are stored preferences; they do not trigger notifications yet.

## Architecture and storage

- Kotlin, Compose, Material 3, single activity; packages and existing picker result types are preserved.
- Plain Kotlin `Reminder`, `ReminderDraft`, transition/repeat enums, and `ReminderRepository` remain independent of Android, Compose, Room, and MapLibre.
- `RemindersViewModel` owns asynchronous storage, draft state, and the three-screen flow. Dependencies use constructor injection and an activity ViewModel factory.
- Room stores reminders locally in `reminders.db`. Schema version 1 is exported and checked in under `app/schemas/`; there is no earlier database schema to migrate. Future changes require migrations, without destructive fallback.
- Android cloud backup and device transfer exclude app data; `allowBackup` is false to preserve local-only storage.
- MapLibre uses the OpenFreeMap Positron style. SpatialK/MapLibre types remain at the map boundary. Map tiles need internet for uncached areas; local saved reminders do not.
- No GPS/current-location feature or location runtime permissions exist yet.

## Verification (2026-10-15)

Executed successfully for this batch:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug
```

- 29 unit tests passed: existing geometry, formatting, and radius behavior; reminder validation, defaults, normalization, duplicate-save prevention, failed-save retry, saved-state restoration, update, and deletion.
- 7 instrumented tests passed on the Android 17 emulator: picker controls/result callback, editor fields/choices, connected map-to-editor-to-list/edit flow, and actual Room persistence across database close/reopen plus update/delete.
- The tests do not prove remote tiles load, native map appearance is correct on every device, actual process-kill restoration, or geofence/notification delivery. No physical-device testing was performed.
- Espresso 3.5.1 failed on the emulator due to a removed `InputManager.getInstance` API. Updating the affected test catalog entries to Espresso 3.7.0 and AndroidX JUnit 1.3.0 fixed the executed UI tests. Other existing versions were preserved.
- Room 2.8.4 and KSP 2.3.6 were added. The older attempted KSP version could not use AGP’s built-in Kotlin source registration; 2.3.6 compiled successfully.
- Android Studio may need **Sync Project with Gradle Files** after these catalog/build changes.

## Product decisions and assumptions

- The product owner chose a **map-first home screen** over a list-first home screen.
- Initial defaults: ENTER, once-only, 200m radius; every-visit repeat is optional.
- Alarm notification intensity and chain-based reminders remain outside MVP. Normal supported Android notifications are the next delivery foundation.
- No backend, account, Firebase, cloud storage, subscriptions, or billed APIs were introduced.

## Next substantial feature batch

Implement supported notifications and staged foreground/background location permission requests, then geofence registration and reliable recovery. Include registration failure states, the 100-active-geofence limit, duplicate-event handling, once/repeat semantics, process/app/device restart recovery, and honest background-delivery limitations. Verify current official Android/Google documentation before implementation and physically test background delivery.

## Relevant files

- Workflow: `AGENTS.md`, `.agents/skills/android-milestone/SKILL.md`, `.agents/skills/android-verification/SKILL.md`
- Entry point: `app/src/main/java/ro/andrei/ariciu/MainActivity.kt`
- Flow/state: `app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/`
- Domain/storage contract: `app/src/main/java/ro/andrei/ariciu/feature/reminders/model/`
- Room storage: `app/src/main/java/ro/andrei/ariciu/feature/reminders/data/ReminderDatabase.kt`
- Editor: `app/src/main/java/ro/andrei/ariciu/feature/remindereditor/ui/ReminderEditorScreen.kt`
- Location picker/model/geometry/map boundary: `app/src/main/java/ro/andrei/ariciu/feature/locationpicker/`
- Tests: corresponding `feature/` packages under `app/src/test/` and `app/src/androidTest/`

## Documentation consulted

- [Codex repository instructions](https://learn.chatgpt.com/docs/agent-configuration/agents-md) and [repository skills](https://learn.chatgpt.com/docs/build-skills)
- [Room releases and setup](https://developer.android.com/jetpack/androidx/releases/room)
- [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) and [KSP releases](https://github.com/google/ksp/releases)
- [Android backup controls](https://developer.android.com/identity/data/autobackup)
- [AndroidX Test compatibility fix](https://developer.android.com/jetpack/androidx/releases/test)
