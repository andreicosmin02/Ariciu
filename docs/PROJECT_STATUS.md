# Project status

## Current implementation

Feature expansion is paused. Ariciu now has a dark-first UI/UX redesign with Create / Reminders / Settings navigation, locally persisted System / Light / Dark appearance, a custom graphite map, and guided reminder editing. The next priority is product-owner design feedback and usability/accessibility review.

Location-based delivery from the preceding batch remains implemented: Play services geofences, Room lifecycle state, persistent WorkManager commands/recovery, and Normal notifications. Real-phone delivery validation remains outstanding.

## User-facing behavior

- **Create** opens a large map around Brașov with a centre marker and area boundary. A persistent selection sheet offers 100m / 200m / 500m presets; More distances reveals the full 100–1000m slider. Default is 200m. Coordinates are secondary under Location details.
- **Use this place** opens a guided editor: reminder text, a place summary, explicit Arrive/Leave cards, and Just once/Every visit choices. Save stays at the bottom; content scrolls and the choices stack with larger text.
- **Reminders** separates Active and Completed. Cards lead with the task, crossing, repeat/area, and a short status. Setup problems have contextual help rather than raw platform diagnostics.
- **Settings** combines appearance and staged delivery permissions, device Location, notification/channel access, Play services readiness, and retry. More detailed platform limitations are under How reminders work. No permission is automatically requested on first launch.
- Appearance defaults to System. Light and Dark override the device explicitly and persist locally. Dynamic wallpaper color is removed. Graphite surfaces, warm white text, and a slate-blue accent define the dark palette; light uses warm paper surfaces and charcoal text.
- Named drafts survive tab switches; Create resumes editing. Map selection uses saveable screen state across tabs. The existing SavedStateHandle draft/edit/save-identity behavior remains.
- Editing/deletion, discard confirmation, validation, retries, notification actions, and the lifecycle semantics remain intact. Editing preserves completion and occurrence state.
- ENTER alerts on arrival; EXIT on departure. Once waits for Done or Next time after triggering. Every visit rearms after the opposite crossing. Next time/swipe waits for a new visit; Done completes the reminder.
- Notification taps open and highlight the relevant reminder in the list. Registration does not immediately alert merely because the device is already inside.

## Architecture and storage

- Kotlin, Compose, Material 3, single activity; packages and existing picker result types are preserved.
- Plain Kotlin `Reminder`, `ReminderDraft`, transition/repeat enums, and `ReminderRepository` remain independent of Android, Compose, Room, and MapLibre.
- `RemindersViewModel` owns asynchronous storage, draft state, and the existing route flow. Create / Reminders / Settings map onto MAP / LIST / DELIVERY; EDITOR is a focused secondary screen. Dependencies use constructor injection and an activity ViewModel factory.
- Room stores reminders locally in `reminders.db`. Schema version 2 and version 1 are exported in `app/schemas/`. Migration 1→2 preserves reminders and adds lifecycle/outbox/registration fields and a registration ledger. Migration and concurrent edit/delete protection passed instrumentation tests.
- The plain Kotlin delivery engine uses Room transactions, generation-conditional writes, persistent occurrence tokens, and an outbox for notification posting. Stable notification tags prevent multiple visible notifications per reminder.
- Play services registers both ENTER/EXIT for up to 100 active areas. Reconciliation journals additions and retries obsolete removals, replaces edited registrations, and exposes failures/overflow. WorkManager persists event/action commands separately from registration retries.
- Boot/package-update receivers and app/settings return reconcile registrations. No continuous GPS, polling loop, tracking foreground service, exact alarm, Alarm intensity, or chain-based reminder was added.
- Android cloud backup and device transfer exclude app data; `allowBackup` is false to preserve local-only storage.
- MapLibre uses Positron in light mode and a bundled, licensed OpenFreeMap Dark adaptation in dark mode, with clearer roads and labels. Map tiles/fonts/sprites still come from the same free provider. Source credits/notices are retained. SpatialK/MapLibre types remain at the map boundary.
- `ThemePreferences` uses local SharedPreferences and StateFlow. No new dependency, service, billing requirement, Room schema change, or delivery algorithm was introduced by the redesign. The map-style asset and native Compose/vector controls require no image-generation API or remote font.
- Map tiles need internet for uncached areas; local saved reminders do not.
- No GPS/current-location map shortcut is implemented. Staged delivery permissions are implemented; device positioning is delegated to Play services.

## Verification (2026-10-15, redesign)

Executed successfully:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug
```

- **52 unit tests passed**: existing delivery/lifecycle/ViewModel/geometry behavior, theme preference resolution, palette role-pair text contrast (at least 4.5:1), and honest user-facing reminder statuses.
- **15 emulator tests passed** on Android 17: existing migration/storage/notification-action tests, updated map/editor/navigation flow, explicit theme switching, tab/draft preservation, theme persistence across store instances, permission explanations, and a 2× text editor flow with reachable choices and a visible Save action.
- Android lint and debug assembly passed. Advisory lint findings include existing dependency-update/resource/logging suggestions and component-parameter style advice; no unrelated dependency upgrades were made.
- The emulator disconnected during some APK-install attempts. A clean boot without wiping data restored final instrumentation verification.
- [Design rationale, sources, accessibility limits, and actual dark/light screenshots](design/DESIGN.md).
- These checks do not establish real Play services positioning, real boundary crossings, process-kill/reboot delivery, sound/heads-up behavior, OEM battery behavior, or usability with older adults. Physical-phone TalkBack/Switch Access and user testing remain outstanding.
- Background-delivery verification remains covered by [the phone checklist](PHONE_VERIFICATION.md).

## Product decisions and assumptions

- UI/UX redesign is the top priority; feature expansion stays paused.
- Identity: dark-first charcoal/graphite, restrained slate blue, no green identity, no wallpaper-driven palette, no playful/flashy decoration.
- Primary navigation: Create / Reminders / Settings. System appearance is the default; the user can override it locally.
- Existing reminder defaults remain ENTER, once-only, and 200m. Alarm, chain-based reminders, and additional notification intensities are outside this work.
- Zero recurring financial cost remains permanent guidance. The existing OpenFreeMap provider and style licensing/terms were reviewed before bundling a dark variant. No paid font, external design SDK, geocoder, new provider, key, billing account, or metered service was introduced.

## Remaining limitations and next work

- Real geofence delivery and reboot recovery need phone verification before treating the MVP as validated. Android/OEM restrictions, positioning, network-assisted location, and WorkManager quotas may delay or suppress work. Force-stopped apps need reopening. Heads-up alerts are user/OS controlled.
- Crossings missed while permissions or location services were unavailable cannot be reconstructed; visit gating is deliberately conservative to avoid duplicate occurrences.
- Room, WorkManager, Play services, and the notification service do not share one transaction. Persistent registration bookkeeping, outbox flags, stable IDs, and reconciliation repair interrupted operations when Android next runs the app/work. There is no absolute exactly-once or immediate-delivery guarantee.
- Failed registration receives bounded exponential retries (up to five attempts), followed by user Retry, app resume, boot, or update recovery. More than 100 active reminders show overflow status in deterministic reminder-ID order.
- Saving edits preserves completion. Creating a separate new reminder is currently the way to restart a completed task; Alarm, Silent/Urgent intensity, and chained reminders remain outside this batch.
- Next priority: incorporate product-owner feedback on the visual system and creation flow, then evaluate larger text/TalkBack and observed usability on a physical phone. Delivery validation remains necessary before treating the MVP as validated; feature expansion stays paused.
- [Architecture, reliability boundaries, dependency licenses/costs, and official sources](DELIVERY_ARCHITECTURE.md).

## Relevant files

- Workflow: `AGENTS.md`, `.agents/skills/android-milestone/SKILL.md`, `.agents/skills/android-verification/SKILL.md`
- Design system, theme preference, and shared native components: `app/src/main/java/ro/andrei/ariciu/ui/`
- Dark basemap and notices: `app/src/main/assets/map/`
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
