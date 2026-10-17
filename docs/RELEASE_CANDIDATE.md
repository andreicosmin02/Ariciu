# Ariciu 1.1-rc3 — second phone-review retest

Install `app/build/outputs/apk/debug/Ariciu-1.1-rc3-debug.apk` over the existing app using the same debug signing key. **Do not uninstall or clear storage.** Application ID `ro.andrei.ariciu`, Room 4 and migrations remain unchanged. This is a local debug retest candidate, not a published release.

Current version **1.1-rc3 / code 4**, branch `fix/phone-review-2`, based on `fix/phone-review-1` at `1b2eb13`. [All nine review dispositions, evidence and retest steps](PHONE_REVIEW_2.md). No merge into master or release publication.

Changes: explicit cancellation-safe local-search progress; connected Active/Completed tabs; outlined Mark done; Create spacing and reachable compact layouts; Settings status beside Review; dedicated transparent fox small icon; an off-by-default foreground arrival check for new single-place ENTER reminders. The optional check saves first, requests one bounded precise fix, and uses the real lifecycle/outbox only when confidently inside. Failures keep the reminder saved. Ordinary geofence registration still uses initialTrigger0.

| Area | Supported limits |
| --- | --- |
| Search | 26,065 local Romania towns/shops/malls, including 231 malls; incomplete addresses. No remote search. |
| Chains | New/refreshed snapshots: up to 40 listed stores within 25km. Existing snapshots stay intact until explicit refresh. Shared Android limit: 100 monitored areas. |
| Maps | New detail may need internet. Explicit packs: about 4×4km, theme-specific, zoom 10–14, up to three packs. Basemap downloads add no search or reminder coverage. |
| Delivery | Precise/background location, device Location, Play services and notification access are needed for normal background monitoring. Android/OEM restrictions can delay/prevent delivery. Offline maps do not guarantee disconnected positioning. |
| Immediate check | New single ENTER only; foreground, precise access and selected notification channel required. Total bound 20s, evidence age≤10s, accuracy≤50m and distance+2×accuracy≤radius. Conservative heuristic, not a guaranteed boundary or dispatch time. No continuous GPS or replay on reconciliation. |
| Urgent/Alarm | High importance/ongoing and repeat notification sound remain subject to Android/channels/DND. Android 14+ permits individual unlocked dismissal; swipe means Next time. No guaranteed full-screen or immediate alarm. |

Final 17 October gate: **82 unit tests, 35 Android 17 emulator tests, lint 0 errors/39 warnings, debug assembly and signature verification passed**. Both raw instrumentation and the Gradle connected runner passed the full 35-test suite. Actual screenshots reviewed in both themes, at enlarged text and on a short 320dp-wide emulator; native light/dark notification shades included. Actual foreground Play-services wiring was manually exercised with simulated emulator positions: outside saved without an alert; inside persisted occurrence 1 and posted Urgent through the real outbox. Older Android runtimes, real GPS/crossings/audio/TalkBack and new Samsung rendering remain unverified.

Read-only actual Samsung S26 Ultra / Android 16 / One UI 8.5 rc2 diagnostics confirmed HIGH/ongoing Urgent and correct Next time lifecycle after the owner's unlocked swipe. The phone disconnected and the owner requested emulator-only continuation. No rc3 phone upgrade/retest occurred; no data/settings/process/permission changes or phone instrumentation were performed.

## Current local artifact

`Ariciu-1.1-rc3-debug.apk` and `app-debug.apk` are identical. Size: **74,660,892 bytes**. SHA256: `bbdba130e31a8437f7a9628eae7f54796369338e745b29c5ca529122bf5e27cd`. Android build-tools apksigner verified the existing debug certificate (SHA256 `3064d0124e266f0d7cf26c742b7ab50a01afd118bf03e40cf731a3b2d765ad2c`). Generated APKs are ignored by Git; a fresh checkout builds with `./gradlew :app:assembleDebug`. Rebuilding can change bytes; verify the artifact being installed.

Historical rc1/rc2 artifact hashes remain in Git history. Follow the [second-review phone checklist](PHONE_REVIEW_2.md#focused-phone-checklist), then the broader [phone verification](PHONE_VERIFICATION.md). No post-fix physical-phone resolution is claimed.
