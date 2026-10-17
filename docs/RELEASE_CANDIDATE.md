# Ariciu 1.1-rc2 — first phone-review retest

Install `app/build/outputs/apk/debug/Ariciu-1.1-rc2-debug.apk` over the existing app using the same debug signing key. **Do not uninstall or clear storage.** Application ID and Room 4/migrations are unchanged. No account, key or payment is required. This is a debug retest candidate, not a published release.

Current version **1.1-rc2 / code 3**, branch `fix/phone-review-1`, based on `feat/phone-release-candidate` at `62557c5`. All 11 review dispositions and targeted phone steps are in [PHONE_REVIEW_1.md](PHONE_REVIEW_1.md). No merge into master or release publication.

Changes include safe Done on cards, clearer lists/intensity, better local AFI search with 231 malls, early Place/Chain choice, draggable Create sheet, compact scalable editor, dedicated store map/list, bounded nearby chain refresh, clearer Settings/offline explanations and an original fox icon.

| Area | Supported limits |
| --- | --- |
| Search | 26,065 local Romania towns/shops/malls; incomplete addresses. OSM source 15 Oct 2026, rebuilt 17 Oct. No remote search. |
| Chains | New/refreshed snapshots: up to 40 listed stores within 25 km. Existing snapshots stay intact until explicit refresh. Shared Android limit: 100 monitored areas. |
| Maps | New detail may need internet. Explicit packs: about 4×4 km, theme-specific, zoom 10–14, up to three packs. Basemap downloads add no search or reminder coverage. |
| Delivery | Precise/background location, device Location, Play services and notification access are needed. Android/OEM restrictions can delay/prevent delivery. Offline maps do not guarantee disconnected positioning. |
| Urgent/Alarm | High importance/ongoing and repeat notification sound remain requests subject to Android/channels/DND. Android 14+ permits individual unlocked dismissal; swipe means Next time. No guaranteed full-screen or immediate alarm. |

Final 17 October gate passed **68 unit tests, 23 Android 17 emulator tests, lint (0 errors/39 warnings) and debug assembly**. Installation and signature verification passed. Actual screenshots include both themes, large text, native store-map roads/labels, List and fox. Older Android runtimes and physical-phone behavior were not verified. The reporting phone is Samsung S26 Ultra / One UI 8.5 / Android 16; rc2 retest is pending.

## Current local artifact

`Ariciu-1.1-rc2-debug.apk` and `app-debug.apk` are identical. Size: **74,603,860 bytes**. SHA256: `900aaf63c75893bb51821b256ea724ce31101c1b51fd337a41d332c47127c40e`. Debug signature verified with Android build-tools apksigner. Generated APKs are ignored by Git; a fresh checkout must build its own with `./gradlew :app:assembleDebug`. Rebuilding can change bytes; verify the artifact being installed.

The previous rc1/code2 artifact and its historical checksum remain documented in Git history. Gradle regenerates its output directory; the previous convenience APK is no longer present there. The installed phone app/data were not touched. Follow [targeted phone retest](PHONE_REVIEW_1.md#phone-retest), then the broader [phone checklist](PHONE_VERIFICATION.md). No post-fix physical-phone resolution is claimed.
