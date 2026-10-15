# Project status

## Current implementation

The autonomous Codex workflow is configured. The first feature batch provides a map-first reminder creation and local management flow. Location-based delivery is implemented and ready for physical-phone validation. The runtime uses Google Play services geofences, persistent lifecycle state, WorkManager recovery, and Normal notifications.

## User-facing behavior

- The app opens on the MapLibre map around Brașov (`45.6579`, `25.6012`, zoom `13.0`). A center marker and radius polygon show the chosen area.
- Radius selection is 100–1000 metres in 100m intervals, default 200m.
- **Use this location** opens the Material 3 reminder editor. Users enter a title, choose **Arrive** (ENTER) or **Leave** (EXIT), and optionally repeat every visit. Defaults are ENTER and once-only.
- Saving requires a nonblank title and valid location/radius. Titles are trimmed.
- **Your reminders** opens locally saved reminders. Users can edit the title, place/radius, transition, and repeat choice, or delete with confirmation.
- Cancelling a named draft or replacing it with another reminder asks before discarding changes. Android Back returns from location changes to the pending editor.
- Draft fields, route, and save/edit identity use `SavedStateHandle`; the picker retains coordinates/radius with saveable Compose state. This supports recreation through Android saved-state restoration, not a guarantee of drafts surviving force-stop or task removal.
- Save retries reuse the same ID and repeated save taps are ignored while saving. Load/save/delete failures show actionable errors; failed saves retain the draft.
- **Delivery setup** explains and stages precise foreground location, background access, device Location, notification/channel access, and Play services readiness. No permission is requested automatically on first launch.
- Reminder cards show waiting, triggered, next-visit, or completed lifecycle state alongside registered, blocked, failed, pending, or capacity-limited monitoring status. Missing capabilities have actionable setup shortcuts.
- ENTER alerts on arrival; EXIT on departure. Once stays triggered until Done or Next time. Every visit rearms after the opposite crossing. Next time/swipe dismisses the current occurrence and waits for a new visit; Done completes either repeat choice.
- Normal notifications show the title and crossing context, with Done and Next time actions that work through persistent work while the UI is closed. Tapping opens and highlights the relevant reminder.
- Initial registration does not immediately alert for an already-inside location. Editing replaces its geofence generation and preserves occurrence/completion state. Duplicate broadcasts and stale generations/actions are rejected.

## Architecture and storage

- Kotlin, Compose, Material 3, single activity; packages and existing picker result types are preserved.
- Plain Kotlin `Reminder`, `ReminderDraft`, transition/repeat enums, and `ReminderRepository` remain independent of Android, Compose, Room, and MapLibre.
- `RemindersViewModel` owns asynchronous storage, draft state, and the three-screen flow. Dependencies use constructor injection and an activity ViewModel factory.
- Room stores reminders locally in `reminders.db`. Schema version 2 and version 1 are exported in `app/schemas/`. Migration 1→2 preserves reminders and adds lifecycle/outbox/registration fields and a registration ledger. Migration and concurrent edit/delete protection passed instrumentation tests.
- The plain Kotlin delivery engine uses Room transactions, generation-conditional writes, persistent occurrence tokens, and an outbox for notification posting. Stable notification tags prevent multiple visible notifications per reminder.
- Play services registers both ENTER/EXIT for up to 100 active areas. Reconciliation journals additions and retries obsolete removals, replaces edited registrations, and exposes failures/overflow. WorkManager persists event/action commands separately from registration retries.
- Boot/package-update receivers and app/settings return reconcile registrations. No continuous GPS, polling loop, tracking foreground service, exact alarm, Alarm intensity, or chain-based reminder was added.
- Android cloud backup and device transfer exclude app data; `allowBackup` is false to preserve local-only storage.
- MapLibre uses the OpenFreeMap Positron style. SpatialK/MapLibre types remain at the map boundary. Map tiles need internet for uncached areas; local saved reminders do not.
- No GPS/current-location map shortcut is implemented. Staged delivery permissions are implemented; device positioning is delegated to Play services.

## Verification (2026-10-15)

Executed successfully for this delivery batch:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug
```

- `./gradlew :app:lintDebug` also passed. Its advisory findings include available dependency updates, starter unused resources, style suggestions, and standard Android Log usage; no unrelated upgrades or logging dependency were added.
- **45 unit tests passed**: existing model/ViewModel/geometry tests plus lifecycle, ENTER/EXIT repeat semantics, stale/duplicate events and actions, permission blocking, registration/removal failures, recovery, and 100-area synchronization planning.
- **12 instrumentation tests passed** on the Android 17 emulator: existing picker/editor/flow/storage tests; version-1→2 migration/schema validation; editing triggered/completed reminders; stale engine-write protection; staged-permission explanations; and actual Android notification posting plus receiver PendingIntents for swipe, Next time, and Done through WorkManager with injected crossing commands.
- The emulator disconnected during an earlier test installation; restarting the same AVD without wiping restored testing. A migration test return-type issue was fixed and the full suite rerun successfully.
- A manual emulator smoke check with delivery permissions granted saved a temporary reminder and reached **Area monitoring registered** through the actual Play services client. The temporary reminder was then deleted. This validates API registration success on that emulator, not positioning or physical delivery.
- These checks do **not** prove actual Play services positioning, real boundary crossings, process-kill/reboot delivery, sound/heads-up presentation, or OEM background behavior. No physical-phone testing was performed.
- [Physical-phone checklist](PHONE_VERIFICATION.md) covers background ENTER/EXIT, Once/Every visit, actions/swipe, editing/deletion, reboot/update/process recovery, permissions/channel changes, and restrictions/connectivity.
- Android Studio may need **Sync Project with Gradle Files** after adding the catalog dependencies.

## Product decisions and assumptions

- The product owner chose a **map-first home screen** over a list-first home screen.
- Initial defaults: ENTER, once-only, 200m radius; every-visit repeat is optional. Registering while already inside does not create an immediate occurrence.
- Normal notification intensity is implemented. Alarm and chain-based reminders remain outside MVP; no custom audio playback is used.
- Zero recurring financial cost is permanent guidance. Play services location uses the royalty-free Android SDK license, WorkManager and Room test support use Apache 2.0, and no credit card, Cloud billing account, paid key, subscription, or metered API is introduced. See the dependency review for evidence and limits.

## Remaining limitations and next work

- Real geofence delivery and reboot recovery need phone verification before treating the MVP as validated. Android/OEM restrictions, positioning, network-assisted location, and WorkManager quotas may delay or suppress work. Force-stopped apps need reopening. Heads-up alerts are user/OS controlled.
- Crossings missed while permissions or location services were unavailable cannot be reconstructed; visit gating is deliberately conservative to avoid duplicate occurrences.
- Room, WorkManager, Play services, and the notification service do not share one transaction. Persistent registration bookkeeping, outbox flags, stable IDs, and reconciliation repair interrupted operations when Android next runs the app/work. There is no absolute exactly-once or immediate-delivery guarantee.
- Failed registration receives bounded exponential retries (up to five attempts), followed by user Retry, app resume, boot, or update recovery. More than 100 active reminders show overflow status in deterministic reminder-ID order.
- Saving edits preserves completion. Creating a separate new reminder is currently the way to restart a completed task; Alarm, Silent/Urgent intensity, and chained reminders remain outside this batch.
- Next priority: execute the physical-phone checklist, address reproduced device-specific failures, and refine delivery diagnostics/UX from those results before adding advanced features.
- [Architecture, reliability boundaries, dependency licenses/costs, and official sources](DELIVERY_ARCHITECTURE.md).

## Relevant files

- Workflow: `AGENTS.md`, `.agents/skills/android-milestone/SKILL.md`, `.agents/skills/android-verification/SKILL.md`
- Entry point: `app/src/main/java/ro/andrei/ariciu/MainActivity.kt`
- Flow/state: `app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/`
- Domain/storage contract: `app/src/main/java/ro/andrei/ariciu/feature/reminders/model/`
- Delivery lifecycle/engine, platform adapters/receivers/work, and setup UI: `app/src/main/java/ro/andrei/ariciu/feature/delivery/`
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
