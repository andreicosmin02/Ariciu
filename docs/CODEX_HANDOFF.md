# Codex handoff — first phone review

Updated **2026-10-17**. Current candidate **Ariciu 1.1-rc2 / code 3**, min API 26, compile/target 37. Work is on **`fix/phone-review-1`**, branched from `feat/phone-release-candidate` at `62557c5`; nothing merged/published. Inspect actual Git/history/worktree before proceeding.

## Start safely

Read `AGENTS.md`, `PROJECT_STATUS.md`, `PHONE_REVIEW_1.md`, `STABILIZATION.md`, `STABILIZATION_AUDIT.md`, `DELIVERY_ARCHITECTURE.md` and `design/DESIGN.md`. The owner authorizes autonomous focused debugging, implementation, verification, commits and feature-branch pushes. No merge, force-push, data/branch deletion or release publication. Preserve unrelated local work and installed reminder data.

Original phone review: existing Obsidian `Personal Projects/Ariciu/Ariciu - Review 1.1-rc1.md`; read fully with all six referenced unique screenshots plus two other captures. Original notes/screenshots remain unchanged/private. Every item is dispositioned in `PHONE_REVIEW_1.md`. Implemented changes await physical-phone retest; do not describe emulator evidence as real-world resolution.

## Changes to know

- Card Done works on any active task, including before an alert or while snoozed. `ReminderRepository.complete(id, occurrence)` → transactional `ReminderDao.complete` → shared `DeliveryState.completed()`. Completion clears alert/snooze, marks inactive and rotates generation to reject interrupted worker writes. Existing repository callback schedules reconcile/cancel/removal. Notification Done still requires matching generation/token/visible occurrence; old events/actions cannot revive completed tasks. Room schema remains 4.
- Sticky Active/Completed selector with explicit checkmark/text; stacked at larger text. Card labels align and intensity is separate.
- Create has bounded padded search + 52dp My location icon, early Place/Chain selection, compact draggable standard sheet with accessible expand/collapse and scrollable body. Fixed primary action is outside the sheet. Custom handle lives in sheet content; content expansion waits for layout when anchors change. Preserve sheet/draft restoration; tests must not assume a returned sheet is collapsed.
- Token-prefix private search replaces arbitrary substring matching. Exact/name matches precede address matches, then distance/ID. Catalogue adds 231 malls from the existing 316MB Geofabrik extract; 26,065 rows, original 25,834 unchanged. AFI Brașov/Cotroceni/Ploiești included. Mall rows never become chain stores. Original source timestamp 15 Oct; build 17 Oct does not mean fresher source. Notice/generator/UI updated together.
- New/explicitly refreshed chain snapshots choose at most 40 stores within 25 km. Old snapshots stay intact until refresh, with distant-store warning. Real MapLibre store-area map and accessible List alternative via `StorePreviewScreen.kt`. Saving retains normal generation/lifecycle safeguards. Shared capacity remains 100 actual areas.
- Smaller editor headings/body input and tighter cards without overriding font scaling. Consistent Settings panels and contained disclosures; clear retry/basemap explanations. Generic notification setup opens app-wide settings; intensity links still open channels.
- Original adaptive vector fox + monochrome silhouette. Existing notification icon/channel IDs are retained.

## Platform and reliability

Urgent uses high importance and ongoing=true; Android 14+ permits unlocked individual dismissal, documented protections apply while locked/Clear all. Existing delete intent means Next time; no repost loop/exemption workaround. Official docs rechecked 17 Oct and linked in the review record. Reporting device: Samsung S26 Ultra, One UI 8.5, Android 16; no post-fix physical retest yet. Only Android 17 emulator runtime was installed; older runtimes/Samsung behavior still need verification.

Alarm remains supported system notification repeat sound/strong vibration with Stop/Done/inexact 10-minute Snooze. No full-screen/FGS/exact alarms/custom playback. Channels/DND/OS govern alerting. Romania search is incomplete, chain refresh explicit, offline packs small/theme-specific; no network-independent-positioning guarantee. No dependency, permission, schema or recurring cost added.

Preserve ID+generation conditional writes, generation+occurrence+visible notification actions, journal-before-add and retained obsolete IDs, outbox/stable notification tag, process mutex/transactions, ENTER+EXIT and initial trigger 0. Platform/WorkManager/Room cannot share an atomic transaction. No destructive fallback, export/cloud backup or completed-reminder restart.

## Where to investigate

Kotlin under `app/src/main/java/ro/andrei/ariciu/`: wiring `MainActivity.kt`, `feature/reminders/ui/AriciuApp.kt` and `RemindersViewModel.kt`; persistence `feature/reminders/data/ReminderDatabase.kt`; lifecycle/allocation/engine `feature/delivery/model/`; channels/receivers/work `feature/delivery/platform/`; Create `feature/locationpicker/ui/LocationPickerScreen.kt`; catalogue/preview `feature/places/`; offline lifecycle/budgets `feature/offlinemap/`; editor `feature/remindereditor/ui/`; shared disclosures/icons/theme `ui/`.

## Evidence and continuation

Final executed counts/commands and actual emulator screenshots are recorded in `PHONE_REVIEW_1.md` and `docs/design/screenshots/review-1/`. Final gate passed 68 unit/23 connected tests, lint 0 errors/39 warnings, assembly/install/signature verification. Intermediate renderer/fixture failures and their corrections are recorded in the review. Retained-capture rerun passed six release/location tests. Manual emulator gestures and Android 2× system font were checked; actual phone retest remains pending.

Run focused tests, then `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`; connected tests only on a deliberately chosen emulator/test device. Do not run data-writing instrumentation on the owner's everyday phone. Use `$android-milestone` / `$android-verification`. Keep Gradle daemon JDK 25 vs source Java 11; no unrelated upgrades.

Install the rc2 APK using the same debug key/application ID. Artifact hash/size are in `RELEASE_CANDIDATE.md`. Never uninstall or clear storage to force an update. The next meaningful work is the owner's rc2 phone feedback and targeted retests, not speculative expansion.

`docs/obsidian-export/Ariciu/` remains the portable knowledge base. The actual existing vault was synchronized on 17 October using filesystem escalation: 26 focused notes/27 emulator images. Same-named notes matched the previous export before updates; originals/private review/eight phone captures were hash-checked unchanged. Compare same-named notes before future overwrite and preserve original planning/review notes. The repository handoff must remain usable without Obsidian.
