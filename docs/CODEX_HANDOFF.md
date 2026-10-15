# Codex handoff

Prepared 2026-10-15. Start with `AGENTS.md`, [project status](PROJECT_STATUS.md), and [design direction/screenshots](design/DESIGN.md); verify them against Git and source. This handoff changes documentation only.

## Branch and local work

- Continue on **`feat/considered-ui-redesign`**, tracking `origin/feat/considered-ui-redesign`. Do not start from `master`: this branch includes creation, delivery, and redesign work that has not been merged there.
- Latest application commit: **`3a2f5a7`** (redesign/navigation/theme/tests). Previous documentation/screenshots: **`4fa643e`**. This handoff is a subsequent documentation commit; use `git log -4 --oneline` for its hash.
- Delivery baseline: `ad0290d` (lifecycle/registration), `a170154` (notifications/recovery), `e334adf` (setup/status). Creation: `9e91f09`; autonomous workflow: `dd0649f`.
- At preparation, three unrelated files were already staged: `.idea/codeStyles/Project.xml`, `.idea/codeStyles/codeStyleConfig.xml`, `.idea/deviceManager.xml`. Preserve them; do not unstage, discard, or include them in task commits without authorization. Recheck `git status` for later user changes.
- Commit/push completed authorized work to a feature branch. No merging into `master`, force-pushing, branch deletion, or release publishing without authorization.

## Implemented

- Map-first selection, local Room reminders, creation/editing/deletion, saved-state draft restoration, validation/error handling.
- Persistent ENTER/EXIT and ONCE/EVERY_VISIT lifecycle; Play services geofences, registration reconciliation/failures/100-area overflow, WorkManager commands/retries, boot/update recovery.
- Normal notifications with Done, Next time, swipe-as-Next-time, and a tap destination that highlights the reminder. Staged permission explanations/settings shortcuts and actionable delivery status.
- Create / Reminders / Settings tabs; focused editor with explicit arrival/repeat cards and fixed Save action; radius presets plus slider; Active/Completed lists; combined appearance/delivery Settings.
- Locally persisted System / Light / Dark, custom palettes/type/shapes/icons, dark map style and license notices, actual emulator screenshots. No new dependency or service was added for the redesign.

## Decisions to preserve

- **Feature expansion is paused.** Continue design refinement and usability/accessibility validation; no owner feedback on the finished redesign is recorded yet. Work autonomously on routine engineering, asking only for meaningful product/design preferences or necessary authorization.
- Identity: premium graphite/near-black, restrained slate blue, warm paper light theme. System is the default. No green identity, wallpaper-driven palette, gradients, playful decoration, or generic component-demo styling. Keep the UX simple, calm, and understandable to older/non-technical people.
- Zero recurring financial cost is mandatory. No backend, Firebase, mandatory account, or cloud storage. Before any new library/provider/service, verify licensing, pricing, limits, and billing requirements; paid keys, credit cards, subscriptions, or metered services require explicit approval. Existing provider evidence/notices are in the design and delivery docs; recheck terms before future changes. A free tier is not a permanent zero-cost guarantee.
- Defaults remain ENTER / ONCE / 200m. ONCE does not recur without Next time; EVERY_VISIT needs an opposite crossing before another occurrence. EXIT needs a return then another departure. Done completes; Next time/swipe waits for a distinct visit. Editing preserves completion/occurrence state. Registration itself does not trigger an initial alert.
- Core data stays local; uncached map areas need internet and Play services positioning can depend on network assistance. Do not promise immediate/exact/guaranteed geofence delivery or heads-up notifications. Alarm, chains, Silent/Urgent, continuous GPS, polling, and tracking services remain outside this work.

## Boundaries and important files

Paths below are relative to `app/src/main/java/ro/andrei/ariciu/` unless stated otherwise.

| Area | Boundary / entry points |
| --- | --- |
| App wiring | `MainActivity.kt`: constructor injection, theme collection, notification intents. Package/application ID remains `ro.andrei.ariciu`; min SDK 26, compile/target SDK 37. |
| Flow | `feature/reminders/ui/AriciuApp.kt`, `RemindersViewModel.kt`: routes MAP/LIST/DELIVERY/EDITOR, SavedStateHandle draft/save identity. Preserve internal route names and draft guards. |
| Domain/storage | `feature/reminders/model/`; `feature/reminders/data/ReminderDatabase.kt`: Room v2, exported v1/v2 schemas in `app/schemas/`, explicit 1→2 migration. No destructive migration. |
| Delivery | `feature/delivery/model/ReminderLifecycle.kt`, `DeliveryEngine.kt`: plain Kotlin state machine/ports. Android adapters/receivers/work in `feature/delivery/platform/`; permission UI in `feature/delivery/ui/`. |
| Design | `ui/theme/`, `ui/components/`; picker/editor/reminder screens in their existing feature packages. `ThemePreferences` uses local SharedPreferences/StateFlow. |
| Map | MapLibre/SpatialK stay at map boundaries; `app/src/main/assets/map/` contains the adapted dark style and licenses. Keep attribution. Light uses existing OpenFreeMap Positron. |

Protect generation-conditional Room writes, registration ledger, occurrence tokens, notification outbox/stable tags, and separate event/reconciliation queues. They prevent stale edits/actions, duplicate notifications, and orphan registrations. Room/platform calls cannot be one transaction; recovery is best-effort, not exactly-once. See [delivery architecture](DELIVERY_ARCHITECTURE.md). Use the repository Android implementation/verification skills when changing application code; avoid unrelated upgrades or added architecture.

## Verification and remaining work

Last successful redesign command:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug
```

- **52 unit tests + 15 Android 17 emulator tests passed**, zero failures/errors/skips. Covers lifecycle/duplicate events/registration, Room migration/concurrent edits, notification actions, ViewModel, theme persistence, tested palette contrast, navigation/drafts, and a 2× text editor flow.
- Lint/build passed; lint has 36 advisory warnings. Stored reports were rechecked for this handoff; tests/build were not rerun for documentation-only changes. Regenerate build artifacts as needed.
- No failing test or unresolved reproducible app bug is recorded. This does not establish bug-free or physical-device behavior. Emulator install disconnects occurred; a clean boot without wiping data restored the successful run.
- Physical-phone delivery, reboot/process recovery, OEM restrictions, sound/heads-up behavior, TalkBack/Switch Access, other screen sizes/display scaling, and observed usability with older users remain unverified. Follow [the phone checklist](PHONE_VERIFICATION.md); its “Delivery setup” now lives in Settings. Screenshots are review artifacts, not golden tests or proof of accessibility.
- No place search/address lookup/current-location shortcut. Completed reminders have no restart control; creating another reminder is the current workaround. Missed crossings cannot be reconstructed. Registration retries are bounded; OS restrictions/force stop can prevent work until reopening.

## Next priorities

1. Incorporate owner feedback on slate blue, map/sheet balance, editor density, and creation-flow clarity; retain the agreed identity and navigation.
2. Validate and fix accessibility/layout issues on a phone, especially large text, TalkBack order/actions, and readable maps. Record observed results without overstating emulator coverage.
3. Complete real-phone delivery verification and repair reproducible failures while preserving lifecycle semantics. Resume feature expansion only when the owner changes the current priority.
