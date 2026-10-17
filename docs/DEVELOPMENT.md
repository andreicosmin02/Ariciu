# Build and test Ariciu

Current application baseline: **1.1-rc4 / code 5 / Room 5**, on `master`. No API key, backend configuration or application account is needed. Obsidian and Codex are optional.

## Requirements

| Component | Project requirement / source |
| --- | --- |
| Android Studio | A version compatible with Android Gradle Plugin **9.3.1** and SDK 37 |
| Gradle | **9.5.0**, via the checked-in wrapper; distribution checksum is pinned |
| Gradle daemon JDK | **25**, requested by `gradle/gradle-daemon-jvm.properties` |
| Android SDK | Platform **37**, Build Tools **36.0.0**, Platform Tools (`adb`); Emulator if using an AVD |
| Android device | Minimum **Android 8.0/API 26**, compatible graphics and **Google Play services** |

Java source/target compatibility is **11**; this is not the JDK used to run Gradle. The project configures Foojay toolchain resolution and daemon download URLs; provisioning can require internet. Prefer the requested JDK 25 rather than changing the wrapper or dependencies to fit a local installation. [AGP compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes) lists Build Tools 36.0.0; the project’s JDK choice is stricter than AGP’s general minimum.

MapLibre’s native libraries are dependencies, not source built here; no project NDK/CMake build step is configured. Optional catalogue regeneration separately needs Python 3 and PyOsmium; it is not required to build the bundled catalogue.

## Clone and configure

```bash
git clone https://github.com/andreicosmin02/Ariciu.git
cd Ariciu
```

Repository access follows its visibility. Open the project root in Android Studio, install the required SDK packages through SDK Manager, select JDK 25 for Gradle where needed, and run **Sync Project with Gradle Files**. Initial Gradle/JDK/Maven dependency downloads need internet.

Android Studio normally creates an ignored `local.properties` containing `sdk.dir` for your SDK. For command-line setup, configure `ANDROID_HOME` to your own SDK installation, or create that ignored file with the `sdk.dir` value shown by SDK Manager. Do not copy another developer’s SDK path or commit `local.properties`. Make the SDK’s `platform-tools` available on PATH for `adb` examples.

Check the wrapper/toolchain and build:

```bash
./gradlew --version
./gradlew :app:assembleDebug
```

The standard output is **`app/build/outputs/apk/debug/app-debug.apk`**. The named GitHub release asset is a separately named verified candidate; Gradle does not promise to create that filename.

Examples use a POSIX shell. On Windows use `gradlew.bat` and your shell’s environment-variable syntax. Keep the checked-in toolchain/version catalogue; after build configuration changes, sync normally before considering cache cleanup.

## Unit tests, lint and reports

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Unit XML: `app/build/test-results/testDebugUnitTest/`; HTML: `app/build/reports/tests/testDebugUnitTest/`. Lint: `app/build/reports/lint-results-debug.html` and `.xml`. Reports are generated, not source evidence for later code changes. Use `./gradlew :app:testDebugUnitTest --rerun` when an explicit fresh unit execution is needed.

## Instrumentation on a selected test device

Use a disposable test emulator with a **Google Play** image for Play-services flows, compatible graphics, and location support. A physical test device also needs Google Play services. The minimum SDK is not a claim that every older runtime has been tested.

Instrumentation fixtures write application data. Disconnect daily-use devices before starting tests; never run these fixtures against someone’s installed reminders without explicit permission. List devices and replace the placeholder below with the deliberately chosen test-device serial:

```bash
adb devices -l
export ARICIU_TEST_SERIAL="YOUR_TEST_DEVICE_SERIAL"
adb -s "$ARICIU_TEST_SERIAL" get-state
ANDROID_SERIAL="$ARICIU_TEST_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

Connected XML reports are under `app/build/outputs/androidTest-results/connected/`. Record the selected device/Android version and actual results. If no suitable test device is available, state that instrumentation was not run.

Emulator tests cover model/Room/platform adapters and UI interactions; simulated positions do not certify real GPS, geofence timing, OEM battery behavior, sound/vibration or accessibility on a phone. Use the [physical verification checklist](PHONE_VERIFICATION.md) for real ENTER/EXIT visits and background/lock/reboot scenarios. Ordinary geofence registration does not fabricate an initial crossing; the optional foreground arrival check is a separate explicit flow.

## Installation and signing

The [verified rc4 release](RELEASE_CANDIDATE.md) uses the existing published **debug certificate**. A fresh local build normally uses your own debug key and may be unable to update that installation. Do not uninstall or clear reminder storage to work around a signature mismatch. Use a disposable emulator for an independently signed build; preserve the existing app and investigate compatibility before updating a phone.

Do not commit debug/production keystores, passwords or generated APKs. No production signing key or Play publication is configured by this guide. See [release/signing evidence](GITHUB_RELEASE_RC4.md) and [licensing review](LICENSING.md).

## Contribute

Follow [CONTRIBUTING.md](../CONTRIBUTING.md), the [architecture](DELIVERY_ARCHITECTURE.md), and the [documentation index](README.md). Reliability, data preservation, offline-first behavior and zero recurring cost are the project’s boundaries.
