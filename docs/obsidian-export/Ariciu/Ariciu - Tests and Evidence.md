---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/urgent-realerts
version: 1.1-rc4
---

# Tests and evidence

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

## Current rc4 update

Current rc4 gate: **97 unit / 44 emulator tests passed**, lint **0 errors/39 warnings**, assembly/install/signature passed. Native system-font 2.0 UI passed 1/1 separately; actual shipping five-minute retry survived process death/reboot/APK replacement, queued safely with denied permission, and posted once on restore. Actual blocked Urgent channel/card resolution also checked. Failure diagnosis and phone limits are in the current record. Earlier counts below remain dated historical results. See [[Ariciu - Urgent Re-alerts]].


Related: [[Ariciu - Development and Builds]], [[Ariciu - Current Status]], [[Ariciu - Debugging Playbook]].

## First phone-review rc2 gate — 17 October

**68 unit / 23 connected Android 17 emulator tests passed**, zero failures/errors/skips. Lint 0 errors/39 warnings; debug assembly/install/signature verification passed. Command: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug`.

See [[Ariciu - Phone Review 1]] for regression scope, actual native screenshots, intermediate failures corrected and phone retest. Swiftshader software GPU, Medium_Phone API37.2; older Android runtimes and Samsung S26 Ultra/One UI 8.5/Android 16 behavior remain unverified. No physical phone/data modified. The following rc1 evidence is historical and is not proof for rc2 changes.

## Historical rc1 gate — 16 October

On **2026-10-16**, the recorded full unit + connected emulator + lint + debug assembly command passed. On **2026-10-17**, the earlier documentation audit read the stored XML/lint reports and compared source; it did **not** rerun application checks or phone tests.

| Layer | Recorded result | What it establishes |
| --- | --- | --- |
| JUnit | 64 tests; zero failures/errors/skips | The listed local model/adapter behavior under test. |
| Android instrumentation | 19 tests; zero failures/errors/skips, Android 17 Medium_Phone emulator | Tested Room, platform notification construction and Compose interactions. |
| Android lint | 0 errors/fatal, 39 warnings | Static checks passed; warnings remain. |
| Debug assembly | Passed; subsequently rebuilt/signature-verified without source changes | Installable debug artifact for this environment. |
| Visual/native map review | Both themes, larger text; one downloaded region rendered disconnected | Those captured layouts and that region worked on the emulator. |
| Physical phone | No dated observations supplied | Delivery/audio/OEM/accessibility not established. |

Local report paths: `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/`, `app/build/reports/lint-results-debug.xml` and the corresponding HTML reports. They are generated and may be replaced by later runs. Unit XML timestamps begin `2026-10-16T18:56:53`; connected aggregate begins `2026-10-16T18:57:32`. Do not treat an old report as proof for newly edited code.

## Historical rc1 unit inventory

All are under `app/src/test/java/ro/andrei/ariciu/`.

| Class | Tests | Focus |
| --- | ---: | --- |
| ChainLifecycleTest | 3 | Union/overlap/per-store order and completion semantics. |
| DeliveryEngineTest | 11 | Registration/capacity, actions/outbox/recovery and intensity behavior. |
| ReminderLifecycleTest | 9 | Once/repeat/ENTER/EXIT, duplicate/order/token and Alarm states. |
| RadiusGeometryTest | 8 | Geographic radius boundary/normalization. |
| RadiusMapGeometryTest | 4 | Geometry adapter shape. |
| RadiusSelectionTest | 5 | Presets/slider and validation. |
| SelectedCoordinatesFormatterTest | 3 | Stable coordinates/locale behavior. |
| DownloadBudgetTest | 2 | Local byte/request budget behavior. |
| PlaceCatalogTest | 2 | Accent/address matching and chain selection. |
| ReminderDraftTest | 6 | Draft validation/conversion, including chain constraints. |
| ReminderPresentationTest | 4 | Status/label presentation. |
| RemindersViewModelTest | 4 | Draft/save/restore/navigation identity with fake persistence. |
| PaletteContrastTest | 1 | Selected foreground/background contrast pairs. |
| ThemePreferenceTest | 2 | Appearance choice resolution. |

## Historical rc1 instrumentation inventory

All are under `app/src/androidTest/java/ro/andrei/ariciu/`.

| Class | Tests | Focus |
| --- | ---: | --- |
| NotificationDeliveryTest | 2 | Constructed channels/notifications/actions and Alarm controls. |
| DeliverySetupScreenTest | 1 | Setup/settings interaction. |
| LocationPickerScreenTest | 3 | Compose map/selection controls. |
| ReleaseCandidateFlowTest | 1 | Private search → chain → Alarm creation, themes and 2× Compose text. |
| ReminderEditorScreenTest | 3 | Editor choices/input/save behavior. |
| ReminderDatabaseTest | 1 | Durable Room round trip. |
| ReminderMigrationTest | 5 | 1→4, 2→4, reopen and generation/stale-write protection. |
| AriciuAppTest | 2 | Routing/draft/list flows. |
| ThemePreferencesTest | 1 | Locally persisted appearance. |

Migration/concurrency tests use controlled/sequential interleavings; they are not a live multi-process stress test. Snooze/model restoration checks are not a full reboot/process-kill audio test. Download-budget unit tests do not prove all native requests obey a budget across pauses/retries. No golden-image test or real remote tile assertion is invented.

## Historical rc1 manual evidence and remaining work

RC screenshots are checked in; see [[Ariciu - Design and Accessibility]]. One light pack recorded 31 protected vector tiles and 396 protected style/font/sprite resources and rendered roads/labels with emulator Wi-Fi/mobile disabled. Attribution and pin selection were visible. This does not prove other regions/themes or fully offline positioning.

Still not verified: actual arrivals/departures while locked/backgrounded, force stop/reboot/update recovery on a phone, audio/vibration/DND, OEM battery restrictions, wall-clock changes during snooze, TalkBack/Switch Access, external keyboard and large display scaling usability. Follow [docs/PHONE_VERIFICATION.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/PHONE_VERIFICATION.md) and record results with [[Ariciu - Phone Issue Template]].

Lint warning groups include dependency/version advisories, LogNotTimber, ModifierParameter, RedundantLabel, UnusedResources and UseKtx. The audit does not convert those 39 warnings into runtime bug reports or authorize bulk upgrades.

## Second phone review

Historical rc3 gate: **82 unit / 35 emulator instrumentation tests passed**, lint **0 errors / 39 warnings**, assembly/install/signature passed. Both raw and Gradle connected runners passed. Current checks, 19 new screenshots, actual foreground provider/manual simulated-position outcomes and limits are in [[Ariciu - Phone Review 2]]. Historical sections above do not prove new code. Actual Samsung rc2 swipe lifecycle was inspected read-only; all rc3 instrumentation targets only the emulator.

## rc4 consolidation and pre-release

Authoritative baseline is now master, tag v1.1-rc4, with an actual verified attached debug APK. [[Ariciu - Releases and Signing]] records the authorized merge, asset download/checksum/certificate, fresh 97-unit/44-emulator gate, signing/licensing limits and pending phone checks. Repository visibility is unchanged by documentation work; no production key or app-wide licence was introduced. Earlier dated implementation/source evidence above retains its own scope.

## Related reading

[Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md) · [Development and Builds](Ariciu%20-%20Development%20and%20Builds.md) · [Current Status](Ariciu%20-%20Current%20Status.md) · [Debugging Playbook](Ariciu%20-%20Debugging%20Playbook.md) · [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) · [Design and Accessibility](Ariciu%20-%20Design%20and%20Accessibility.md) · [Phone Issue Template](Ariciu%20-%20Phone%20Issue%20Template.md) · [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md) · [Releases and Signing](Ariciu%20-%20Releases%20and%20Signing.md)
