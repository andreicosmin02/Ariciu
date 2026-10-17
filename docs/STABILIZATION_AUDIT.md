# Stabilization audit and known boundaries

Source audit **2026-10-17**, baseline app commit `011746c` / Ariciu 1.1-rc1. Documentation only; no production fixes made. No actual phone bug report or dated physical-phone verification has yet been supplied. Stored automated results are green; a source-derived risk below is **not** a reproduced critical defect.

## Established implementation/platform limits

- Android/GMS can delay or miss crossings; WorkManager Snooze/recovery is inexact. Force stop can suppress jobs/receivers until reopening; missed crossings cannot be reconstructed. No absolute exactly-once/immediate/full-screen promise.
- Urgent differs by its high-importance channel/ongoing request; there is **no custom stronger tone or vibration pattern than Normal**. Modern Android can dismiss it; swipe is Next time. Timed Snooze is Alarm-only.
- Alarm requests repeating system notification sound/strong vibration, subject to channels/DND/OS. Opening notifications can stop repeat sound. No full-screen/FGS/exact alarm/custom audio-loop service.
- Romania search is dated/incomplete towns/selected shops/known shop addresses, not a full address geocoder. Chains use up to 40 selected stores around a pin, explicit refresh, no automatic roaming; globally at most 100 actual areas. Missed exits can conservatively delay union rearming.
- Offline packs are small/theme-specific/zoom-limited and user driven. Displayed resource MB is not exact total filesystem use; shared/ambient cache may remain after deletion. Download byte/status and tile-response counts differ across resume/cache/retries. Offline maps do not guarantee disconnected positioning.
- Completed reminders stay completed on edit; no restart or in-app Done workflow. No backup/export/import/cloud sync, non-GMS delivery or catalogue network refresh. Installation updates require the same signing key; do not uninstall to fix a signature error.

## Confirmed wording/maintenance debt

- `ReminderPresentation.kt`: generic triggered text can say “Choose Done or Next time” for Alarm, which exposes Stop/Done/Snooze; snooze copy does not calculate remaining time, and partial monitoring can outrank snooze status.
- `DeliverySetupScreen.kt`: generic notification setup opens the Normal channel even if app-wide notification access is blocked; per-intensity links are available in expanded alert settings. Confirm the reported OS route before adjusting it.
- Catalogue date/count/provenance and UI strings require coordinated updates; generator output does not update every NOTICE/UI date. Chain TEXT decoding is strict, with no reported corrupt row at this handoff.
- Lint has 39 advisory warnings, including version suggestions, LogNotTimber, ModifierParameter, RedundantLabel, UnusedResources and UseKtx. Large platform/database files and unused legacy resources are maintenance debt, not a reason for a rewrite or unrelated upgrades.

## Unreproduced source hypotheses

| Area / source | Hypothesis and focused investigation |
| --- | --- |
| `OfflineMaps.kt` | Pending Native create callback might resume after screen-exit pause; failed singleton region-list load may not reload on reopen. Delay callbacks/load on disposable data and inspect actual status/traffic. |
| `DownloadBudget.kt`, `OfflineMaps.kt` | Resume initializes byte count from stored bytes but resets `.pbf` response count; glyphs share that extension, and in-flight/shared-cache behavior differs from resource status. Validate small bounded failure/resume scenarios before claiming a lifetime pack traffic cap. |
| `ChainLifecycle.kt`, `DeliveryEngine.kt` | An INSIDE store later unmonitored under partial allocation may keep union INSIDE. Reproduce allocation changes/missed exit and examine rearming. |
| `ReminderDatabase.kt` | Same-ID store-coordinate refresh and incoming per-store state interact with parent movement detection based on selected IDs. Test controlled old/new coordinates without changing completion semantics. |
| `AndroidDelivery.kt` | Immediate quiet reads/clones/notifies a matching notification; another post can race between read and update. Use controlled interleaving and verify newer occurrence identity. |
| `DeliveryWork.kt`, engine/platform | Stop silence precedes action persistence; platform post/cancel/add/remove can finish around edit/delete/crash. Room conditional writes protect data, but cannot make separate OS/WorkManager commits atomic. Fault-inject recovery on disposable data. |
| Snooze recovery | Clock changes, old-generation wakes and multiple queued deadlines need device scenarios beyond model tests. Test forward/backward time, edit/delete/reboot while snoozed. |

Prioritize the owner's reproducible problem. Confirm a hypothesis before changing behavior, add a focused regression/device case and update this audit when evidence changes. See [stabilization workflow](STABILIZATION.md), [delivery architecture](DELIVERY_ARCHITECTURE.md) and [evidence](PROJECT_STATUS.md).
