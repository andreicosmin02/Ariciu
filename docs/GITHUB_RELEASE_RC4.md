# rc4 consolidation and first GitHub pre-release

17 October 2026. The owner explicitly authorized a normal merge into `master` and a GitHub **pre-release**, not Google Play publication. **1.1-rc4 / code 5 / Room 5** remains an early testing build, not production-stable. `master` is now authoritative. The repository was **private at publication**; release/download access follows its visibility. Publication did not change visibility, create a production key or rewrite history. The later approved branch cleanup is recorded below.

## History and source audit

Started clean on `fix/urgent-realerts` at `525b6095745a5b5282a4ec8761813c81ba83a637`; fetched origin before mutation. Local and remote branch tips agreed. Full graph, merge bases and unique commits were checked: all eight development tips formed one ancestry chain above old `master` (`ea6218a`), with no independent work to rescue. Old master was 27 commits behind rc4.

[PR #1](https://github.com/andreicosmin02/Ariciu/pull/1) used a **normal merge**, preserving every original commit. Merge commit **`c8c47ce4642ba7f70ee0d61355bb5175e2334cc0`** has parents `ea6218a60157d0fa0f51ffb4ace0c16f560e8ee5` and `525b6095745a5b5282a4ec8761813c81ba83a637`. Its whole tree is exactly rc4's tree **`cc979fd02f8b0276f4d6e4318687668314f7a69c`**; no conflicts/source edits. The reviewed change from old master comprised 250 files, 38,231 insertions and 570 deletions, including the catalogue, tests, assets, documentation and all five exported Room schemas. Three inherited trailing spaces in the upstream dark-map licence were retained verbatim rather than changing legal text; the new docs diff passes whitespace checks.

The annotated tag **`v1.1-rc4`** points to that verified merge commit. A subsequent documentation-only PR adds the README, verified links and current handoff after actual publication. It does not change the application, Gradle configuration, permissions, migrations or release APK; later master documentation commits do not move the tag.

## Published release and verified artifact

- [Ariciu 1.1-rc4 — Phone Testing](https://github.com/andreicosmin02/Ariciu/releases/tag/v1.1-rc4), published 17 October 2026 at 20:06:54 UTC; prerelease=true, draft=false.
- [Attached Ariciu-1.1-rc4-debug.apk](https://github.com/andreicosmin02/Ariciu/releases/download/v1.1-rc4/Ariciu-1.1-rc4-debug.apk). It is a real uploaded GitHub Release asset, not an APK committed to Git or only mentioned in prose.
- **74,677,975 bytes**, application ID **`ro.andrei.ariciu`**, version **1.1-rc4**, version code **5**, minimum SDK **26**, target SDK **37**. Universal APK includes arm64-v8a, armeabi-v7a, x86 and x86_64; compatible device graphics/Google Play services remain required. Older-runtime coverage has not been established merely by minimum SDK.
- APK SHA256: **`f6bfa834034599addd680768d39e1e86caac96c32a9cd79052723be1f5863d78`**.
- Existing **Android Debug** certificate SHA256: **`3064d0124e266f0d7cf26c742b7ab50a01afd118bf03e40cf731a3b2d765ad2c`**; apksigner verifies v2 signing. No replacement signing key was generated/published. Locally rebuilt debug APKs can use a different developer key.

Before building, the documented local APK was preserved outside Git and checked with SHA256, aapt2 badging/permissions and apksigner. Debug assembly produced identical bytes. After publication, the attached asset was actually downloaded through authenticated GitHub CLI and independently matched the same byte count/checksum/certificate. The release API confirms its uploaded state and matching server SHA256. This verifies downloads for an authorized repository user; anonymous public download was not claimed while the repository was private at publication.

The published binary is byte-for-byte the original rc4 candidate, so it adds no permissions or migration changes during consolidation. The merged manifest also includes existing library permissions (for example WorkManager's WAKE_LOCK/FOREGROUND_SERVICE); these are not newly introduced app behavior. Room schemas/migrations 1–5 are preserved, with no destructive fallback. Debuggable signing is intentional for this testing release.

**Install over the existing same-signed debug app. Never uninstall or clear storage to update.** If Android reports signature/version incompatibility, preserve the installed app and investigate. The debug certificate matches the existing recorded candidate; no new Samsung install/signature inspection was performed during consolidation.

## Executed consolidation checks

These commands record the historical selected emulator; use [DEVELOPMENT.md](DEVELOPMENT.md) for current portable examples.

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest
./gradlew :app:testDebugUnitTest --rerun
```

Gate **passed in 1m41s**, followed by the explicitly rerun unit task **passed in 2s**. Results: **97 unit tests, 44 connected Android 17 emulator tests, zero failures/errors/skips; lint 0 errors/39 warnings**. APK build, package/version/permissions/signature and actual release download checks passed. Only deliberately selected `emulator-5554` was attached/targeted; no daily-use phone instrumentation, settings changes, data clear or uninstall. The application tree at the merge equals the tested tree; later changes are README/docs/logo export only.

Earlier rc4 mechanism evidence, shipping five-minute recovery and both-theme/large-text screenshots remain in [URGENT_RE_ALERTS.md](URGENT_RE_ALERTS.md). The owner reports **positive initial physical-phone feedback** on Samsung S26 Ultra / Android 16 / One UI 8.5, without detailed scenarios/results; do not upgrade this to comprehensive physical verification.

## Completed safe branch cleanup

Original audited tips below were all ancestors of rc4, each with **zero commits unique relative to rc4**. Local and origin refs matched. After PR #2 merged, all eight branch refs (including its docs commit on fix/urgent-realerts) were rechecked against latest clean master at `77ee6c9896ec3f4dffede22d0e01ae2369e704da`. The owner explicitly approved deletion. Atomic normal remote deletion and local `git branch -d`, followed by fetch/prune, left only master before new work began. Every deleted tip remains reachable from master; release/tag/APK were reverified unchanged.

| Branch | Audited source tip | Independent commits | Cleanup outcome |
| --- | --- | --- | --- |
| `master` | `ea6218a` before consolidation | 0 | Keep; authoritative baseline after merges |
| `chore/autonomous-codex-workflow` | `dd0649f` | 0 | Safely deleted locally and from origin |
| `feat/map-first-reminder-creation` | `9e91f09` | 0 | Safely deleted locally and from origin |
| `feat/reliable-reminder-delivery` | `e334adf` | 0 | Safely deleted locally and from origin |
| `feat/considered-ui-redesign` | `de9644e` | 0 | Safely deleted locally and from origin |
| `feat/phone-release-candidate` | `62557c5` | 0 | Safely deleted locally and from origin |
| `fix/phone-review-1` | `1b2eb13` | 0 | Safely deleted locally and from origin |
| `fix/phone-review-2` | `8f11976` | 0 | Safely deleted locally and from origin |
| `fix/urgent-realerts` | `525b609` | 0 | Safely deleted after PR #2 also merged |

All eight approved deletions completed without force deletion/push or history rewriting. Master, v1.1-rc4 and all history remain. Any future cleanup must recheck fresh refs and preserve new unique work; this record is not blanket deletion authorization.

## Documentation, privacy and next work

The root [README](../README.md) uses the original fox geometry exported to SVG and three actual inspected emulator captures (Create/map, pending Reminders, light Settings), with compact display widths. No private phone image, personal coordinates, vault originals or fabricated screenshot/badge is introduced. Release/image links must be checked from master after the docs merge. Required OSM/OpenMapTiles/style/catalogue notices remain. No application-wide licence is invented; production licensing/signing require deliberate future decisions.

Repository handoff/status/release instructions and the optional tracked knowledge base record the release. The portable collection requires no personal vault or synchronization workflow. No APK, key, private log or database snapshot is committed.

Remaining phone checks: [Urgent swipe/retry/Done/Next time and legacy Alarm](URGENT_RE_ALERTS.md#physical-phone-retest), real ENTER/EXIT crossings, overnight/Doze/Samsung lock/Clear all, sound/vibration, notification settings, TalkBack and older runtimes. WorkManager/geofence delays, OEM restrictions, DND/access and the 100-area limit remain; do not promise exact or safety-critical alerts. Offline map/search coverage remains bounded.

Future work: stabilize actual reported failures, reconsider genuine Alarm support separately, and plan production signing plus a data-preserving transition and project licensing before production distribution. No unrelated feature development is included here.

## Subsequent licensing; unchanged historical release

The owner later approved Apache-2.0 for Ariciu-owned material. [Current scope](LICENSING.md), root LICENSE/NOTICE and [companion third-party notices](third-party/README.md) are post-tag repository documentation. The tag remains at c8c47ce with its original tree; no root licence files were retroactively inserted. The release page, APK, checksum, debug certificate and package/version are unchanged. Full MapLibre BSD texts and version-matched Google OSS bundles are provided separately with installation documentation, addressing the identified companion-notice gaps without rebuilding the APK. No comprehensive native/transitive binary certification is claimed.
