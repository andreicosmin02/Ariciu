---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-2
version: 1.1-rc3
---

# Development environment and builds

Related: [[Ariciu - Tests and Evidence]], [[Ariciu - Maintenance and Extension]], [[Ariciu - Current Status]].

## Reproducible configuration

One Android application module `:app`, namespace/application ID `ro.andrei.ariciu`, Kotlin/Compose/Material 3. Minimum SDK 26 (Android 8); compile/target SDK 37; version `1.1-rc2`, code 3. Android SDK/platform/build tools and a compatible Android Studio installation are required.

Use the checked-in wrapper: Gradle **9.5.0**, with distribution SHA configured. `gradle/gradle-daemon-jvm.properties` requests **JDK 25**. Java source/target compatibility is **11**; that is not the Gradle daemon JDK. Follow the existing toolchain rather than changing versions to resolve a local IDE mismatch. Android Gradle Plugin **9.3.1** uses built-in Kotlin; there is no separate kotlin-android plugin.

| Dependency/tool | Pinned version |
| --- | --- |
| Kotlin Compose plugin / Compose BOM | 2.2.10 / 2026.02.01 |
| KSP / Room | 2.3.6 / 2.8.4 |
| MapLibre Compose | 0.13.1 (resolved Native runtime 13.0.2 at the audited build) |
| Play services location / WorkManager | 21.3.0 / 2.10.1 |
| OkHttp / coroutines-test | 4.12.0 / 1.10.2 |
| Lifecycle / Activity Compose / core-ktx | 2.6.1 / 1.8.0 / 1.10.1 |
| JUnit / AndroidX JUnit / Espresso | 4.13.2 / 1.3.0 / 3.7.0 |

`gradle/libs.versions.toml` and resolved Gradle dependencies are authoritative. Older pins are not automatically defects; do not upgrade unrelated dependencies during stabilization. First dependency resolution needs internet; subsequent builds may use local caches. No map key, backend, billing account or login configuration is required.

## Commands from repository root

```bash
./gradlew --version
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
adb devices -l
./gradlew :app:connectedDebugAndroidTest
```

Run connected tests only on a deliberately selected emulator/test device; do not run app-data-writing instrumentation against the user's daily-use phone without understanding its fixtures. Full relevant gate after production changes:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug
```

When no device is available, run unit/lint/assembly and explicitly report connected tests unexecuted. After build/catalog changes, Android Studio may require **Sync Project with Gradle Files**. Do not default to deleting caches. Network/sandbox/SDK/JDK failures are environment issues to identify, not justification for unrelated dependency churn.

## APK and installation

Output: `app/build/outputs/apk/debug/app-debug.apk`. A convenience copy `Ariciu-1.1-rc2-debug.apk` currently exists alongside it; it is generated/local, not committed. Check [[Ariciu - Current Status]] for the recorded rebuilt artifact hash.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

Install updates with the **same signing key** and application ID. If Android reports a signature/version incompatibility, preserve the installed app and investigate. Do not uninstall or clear storage to force an update: reminders have no cloud backup/export. Do not copy debug signing keys into Git or the vault.

Release optimization is disabled in the current Gradle release type. This is an installable **debug candidate**, not a signed/published Play release. No release publication, production signing setup or merge is authorized by the handoff. Future distributable release work needs deliberate signing/version/policy review.


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].

## Second phone review — rc3

Current debug retest candidate is1.1-rc3/code 4 on fix/phone-review-2, not a published release. Build with the existing Gradle/dependencies; convenience filename Ariciu-1.1-rc3-debug.apk. Installation must update the existing package with the same key; never uninstall/clear storage. Target emulator-5554 explicitly for instrumentation/installation. User requested emulator-only continuation after Samsung disconnected. See [[Ariciu - Phone Review 2]].
