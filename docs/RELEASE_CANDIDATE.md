# Ariciu 1.1-rc4 — Urgent re-alert retest

Install `app/build/outputs/apk/debug/Ariciu-1.1-rc4-debug.apk` over the existing app with the same debug signing key. **Do not uninstall or clear storage.** App ID `ro.andrei.ariciu` and signing identity remain. Room **5** has a tested additive 4→5 migration; earlier schemas/migrations and all reminder data remain.

Version **1.1-rc4 / code 5**, branch `fix/urgent-realerts`, from rc3 `8f11976`. [Policy, implementation, tests and phone checklist](URGENT_RE_ALERTS.md). Nothing merged/published; this is a local debug retest candidate.

Available levels: **Silent, Normal, Urgent**. Urgent keeps HIGH/ongoing and one standard first alert. A swipe schedules an inexact five-minute re-alert within the same unresolved occurrence; a visible alert does not keep alerting. Done/Next time explicitly resolve it, including from its waiting card. Deadlines/attempt tokens and unique delayed one-time WorkManager jobs survive/recover from interrupted execution. Crossing again does not silently clear/duplicate an unresolved Urgent. No exact alarm, periodic worker, foreground service, continuous sound/GPS or full-screen workaround.

Alarm is temporarily hidden from new choices. Existing ALARM records/actions/snooze remain; editors retain Alarm unless explicitly converted to Urgent. A pending legacy snooze keeps its current deadline on conversion. No silent data conversion or deletion.

The rc3 search/tabs/editor/Settings/fox/immediate-arrival improvements remain. Catalogue: 26,065 Romania entries, new/refreshed chains max 40 within 25km, shared 100-area monitoring limit and small theme-specific offline map packs are unchanged. Maps may need connectivity; offline positioning and prompt geofence delivery are not guaranteed. No dependency, permission, backend or recurring-cost addition.

Executed gate: **97 unit tests, 44 Android 17 emulator tests, lint 0 errors/39 warnings, debug assembly/install/signature passed**. An actual rc3→rc4 emulator upgrade preserved all four existing reminders/core fields; schema 4→5 validated. Both themes, 2× Compose text and native system font scale 2.0 were inspected/tested. Detailed manual recovery/permission observations and limits are in URGENT_RE_ALERTS.md. These do not establish physical Samsung behavior, real GPS/crossings, audible/vibration performance, TalkBack or older-runtime behavior.

## Current local artifact

`app-debug.apk` and `Ariciu-1.1-rc4-debug.apk` are identical: **74,677,975 bytes**, SHA256 `f6bfa834034599addd680768d39e1e86caac96c32a9cd79052723be1f5863d78`. Android apksigner verified the existing certificate SHA256 `3064d0124e266f0d7cf26c742b7ab50a01afd118bf03e40cf731a3b2d765ad2c`. Generated APKs are ignored by Git; fresh checkouts build with `./gradlew :app:assembleDebug`. Rebuilds can change bytes; verify the actual file installed. Earlier artifact metadata remains in Git history.

Samsung S26 Ultra / Android 16 / One UI 8.5 is disconnected for this batch. No phone upgrade, settings/data/permission/process changes or instrumentation occurred. Follow [rc4 phone retest](URGENT_RE_ALERTS.md#physical-phone-retest), then [broader verification](PHONE_VERIFICATION.md). Five minutes is approximate; Android/Doze/OEM/DND/channel choices can delay or suppress alerts. No physical-phone resolution is claimed.
