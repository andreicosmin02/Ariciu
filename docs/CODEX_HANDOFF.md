# Codex handoff — phone debugging and stabilization

Updated **2026-10-17**, checked against source, Git, schemas, assets and stored reports. The next conversation focuses on **problems the owner discovers while using Ariciu on an Android phone**. Do not start another broad feature-expansion phase by default.

## Start safely

Read [AGENTS.md](../AGENTS.md), [PROJECT_STATUS.md](PROJECT_STATUS.md), [STABILIZATION.md](STABILIZATION.md), [STABILIZATION_AUDIT.md](STABILIZATION_AUDIT.md) and [design/DESIGN.md](design/DESIGN.md). Consult [delivery architecture](DELIVERY_ARCHITECTURE.md) and [feature/licensing evidence](FEATURE_COMPLETION.md) for the affected subsystem.

```bash
git status --short --branch
git log -8 --oneline
```

Continue from **feat/phone-release-candidate**, based on the redesign branch at `de9644e`, not master. Preserve staged/uncommitted work. At this audit's start HEAD was `42f77a3` and the worktree was clean. The handoff commit itself is later in history; inspect Git for its exact hash.

| Commit | Baseline |
| --- | --- |
| `42f77a3` | Previous release-candidate evidence/phone checklist. |
| `011746c` | Latest application code: private Romania search, My location, offline maps and UI polish. |
| `4e959b9` | Intensities, snooze, Room 4 and multi-store lifecycle/allocation. |
| `92ba3e9` | Feature-completion priority and owner-requested IDE ignores. |

Owner authorizes autonomous diagnosis, fixes, relevant tests, focused commits and pushes on feature branches. Ask only about meaningful product/architecture/platform compromises or genuine blockers. No merge, force push, deletion of branches/work/user data or release publication. Preserve graphite/slate-blue, warm light mode, System/Light/Dark and Create / Reminders / Settings. Zero recurring cost and local core operation remain non-negotiable.

The three previously uncommitted IDE files remain local and ignored: `.idea/codeStyles/Project.xml`, `.idea/codeStyles/codeStyleConfig.xml`, `.idea/deviceManager.xml`. Do not stage unrelated IDE settings or secrets.

## Current implementation and approved boundaries

**Ariciu 1.1-rc1 / code 2**, min API 26, compile/target 37. Single Compose/Material 3 app with activity ViewModel, StateFlow/SavedStateHandle, constructor injection, Room and WorkManager. No backend/account/cloud storage/billing key/paid API.

- Local create/edit/delete, drafts/restoration, Active/Completed and notification destinations. Completed reminders stay completed on edit; no restart/export UI.
- Silent: low channel/no audio-vibration defaults; Done/Next time; swipe is Next time. Normal retains `normal_reminders`. Urgent: high importance/ongoing request with Done/Next time; **no stronger custom tone/vibration than Normal**; modern Android may allow swipe.
- Alarm: explicit selection, system notification repeat-sound request/strong vibration, Stop/Done/ten-minute Snooze. Snooze is Alarm-only/inexact. Immediate matching-alert silence precedes persisted action work. No full-screen/FGS/exact-alarm/app playback workaround; channel/DND/OS controls alerting, and opening the panel may stop repeat sound.
- Romania-first scope was expressly approved: 25,834 OSM-derived town/shop rows, seven supermarket chains, source timestamp 2026-10-15T20:21Z, built 16 October. Queries are local; not every street/store/address or worldwide coverage. No Nominatim/Overpass/Geocoder/paid Places endpoint.
- Chains persist up to 40 nearest listed stores around a selected pin. One parent lifecycle/notification; Done/Once apply to the whole chain. Every visit rearms after leaving the store-area union. Selection refresh is explicit; no automatic roaming/continuous GPS. At most 100 actual geofence areas globally, fair rounds and visible partial/overflow statuses.
- My location is explicit foreground positioning, approximate allowed for map centring, max fix age 30s/timeout 20s. Monitoring separately requires precise/background permission, device Location, GMS and notifications.
- Offline maps: user-initiated approximately 4×4 km/theme/zoom 10–14 packs, max three/one active; progress/resource MB, pause/resume/delete. Screen-exit/background and 90-second pause, four requests, identifying User-Agent and conservative budgets. Display styles are bundled; Native definitions use matching hosted styles because `asset://` stalled in prior native verification. Cache/shared resources and resume counters have documented limits; do not call displayed MB exact disk usage or claim offline positioning is guaranteed.

The Romania scope and supported Alarm fallback need no repeated product approval. Expanding coverage/services/full-screen behavior/public release is a separate decision.

## Protect these invariants

Room version **4**, exported schemas **1–4**, registered additive migrations 1→2→3→4; no destructive fallback. Chain state is encoded delimiter TEXT, not JSON. Edits change generation and preserve completion/occurrence/quiet/snooze; parent movement and per-store preservation have precise conditions in `saveEdited` (do not assume all stores reset on every move).

Engine writes are ID+generation-conditional; actions require generation+occurrence+visible state. Journal request IDs before adding; retain obsolete IDs until removal succeeds. Preserve notification outbox and stable tag/token identity, transaction/process mutex boundaries, both ENTER/EXIT registrations and disabled initial triggers. WorkManager, Room and platform services cannot share an atomic commit; recovery is best-effort, not absolute exactly once.

## Where to investigate

All Kotlin paths below are relative to `app/src/main/java/ro/andrei/ariciu/`.

| Concern | Relevant files |
| --- | --- |
| Wiring/draft/routes | `MainActivity.kt`; `feature/reminders/ui/AriciuApp.kt`, `RemindersViewModel.kt` |
| Persistence/migrations/conditional writes | `feature/reminders/data/ReminderDatabase.kt`; `app/schemas/` |
| Event/action/chain/allocation logic | `feature/delivery/model/ReminderLifecycle.kt`, `ChainLifecycle.kt`, `DeliveryEngine.kt` |
| GMS/channels/audio/actions/capabilities | `feature/delivery/platform/AndroidDelivery.kt` |
| Receivers/queues/retries/snooze/recovery | `feature/delivery/platform/DeliveryWork.kt` |
| Settings/setup | `feature/delivery/ui/DeliverySetupScreen.kt` |
| Picker/one-shot positioning | `feature/locationpicker/ui/LocationPickerScreen.kt`, `CurrentPosition.kt` |
| Catalogue | `feature/places/PlaceCatalog.kt`, `LocalPlaces.kt`; `app/src/main/assets/places/`; `tools/build_place_catalog.py` |
| Offline lifecycle/HTTP limits | `feature/offlinemap/OfflineMaps.kt`, `DownloadBudget.kt`; `app/src/main/assets/map/` |
| Editor/status/design | `feature/remindereditor/ui/ReminderEditorScreen.kt`; `feature/reminders/ui/ReminderPresentation.kt`; `ui/theme/`, `ui/components/` |

## Evidence and uncertainty

Last full run **2026-10-16**: **64 unit tests, 19 Android 17 emulator tests, zero failures/errors/skips; lint zero errors and 39 warnings; debug assembly/install passed**. The debug APK was subsequently rebuilt/signature-verified without source changes. On 17 October this documentation task inspected stored reports; it did **not** rerun application checks.

Recorded emulator review covers both themes/enlarged text and one completed light map pack (31 protected vector tiles + 396 style/font/sprite resources) rendered with Wi-Fi/mobile disabled. These are not proof of phone background detection, real audio/vibration, reboot/process/OEM behavior, fully offline positioning or accessibility usability. No dated physical-phone verification or actual field bug report has yet been supplied.

Current generated APK and hash are in [RELEASE_CANDIDATE.md](RELEASE_CANDIDATE.md). Updates must keep the same signing key/app ID; do not uninstall/clear storage to fix installation failures. Reminders have no cloud backup/export.

[STABILIZATION_AUDIT.md](STABILIZATION_AUDIT.md) separates confirmed limitations/copy debt from **unreproduced hypotheses**: offline pending callbacks/load recovery/resume accounting, partial-chain stale INSIDE state, same-ID coordinate refresh, notification quiet/post races and separate-commit/snooze clock recovery. None is a confirmed critical phone defect. Prioritize the reported experience, reproduce first, then add targeted regressions.

## Verification and debugging

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
# Only with an appropriate emulator/test device available:
./gradlew :app:connectedDebugAndroidTest
```

Use `$android-milestone` for Android implementation and `$android-verification` for Android/code/test/build changes. Keep the daemon JDK 25 / source Java 11 distinction; existing Gradle/catalog versions suffice. Do not run data-writing instrumentation on the owner's daily-use phone casually. Non-destructive diagnosis commands, report template and pipeline isolation are in [STABILIZATION.md](STABILIZATION.md). Use [PHONE_VERIFICATION.md](PHONE_VERIFICATION.md) for real-phone retests; compilation alone does not resolve a field issue.

## Knowledge transfer

[Documentation index](README.md) links repository essentials. [Obsidian export](obsidian-export/README.md) contains **25 linked notes and 8 copied RC screenshots** with product, implementation, tests, sources and maintenance detail. Existing vault `Personal Projects/Ariciu` was located but is read-only under current sandbox permissions, so originals were preserved and the export is ready to merge. The next session must remain functional without that vault.

This handoff changes documentation only. No application functionality, schemas, tests, Gradle settings or assets were modified.
