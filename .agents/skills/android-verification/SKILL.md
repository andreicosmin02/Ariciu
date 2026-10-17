---
name: android-verification
description: Verify an Ariciu Android code, test, manifest, resources, or Gradle change before declaring it complete. Use after implementation or when asked to review a build or test failure.
---

# Verify an Ariciu change

1. Inspect `git diff --check`, `git status --short`, and the change's actual diff. Look for accidental broad edits, renamed paths, unrelated dependency upgrades, and secrets.
2. Run the smallest relevant tests first. For production source changes with unit tests, run `./gradlew :app:testDebugUnitTest`. For changed instrumented Compose tests, run `./gradlew :app:connectedDebugAndroidTest` only when an appropriate emulator/device is attached.
3. For production code, resources, manifest, or Gradle changes, run `./gradlew :app:assembleDebug` after focused tests. A successful build does not prove geofencing or notification delivery works.
4. When an Android SDK, Gradle setup, network, device, or system permission blocks a check, attempt diagnosis and repair within authorization; use the normal escalation mechanism for sandbox restrictions. Describe any remaining blocker and show what *was* verified. Never report a check as passed if it was not run.
5. For geofencing and notification work, state focused manual verification steps on a physical device, including UI closed, location/background permissions, location services disabled, duplicate broadcasts, reminder Done/Next time, repeat after leaving, and restart where applicable.
6. If a Room schema changed, inspect the migration and its tests. If the manifest or runtime permissions changed, check staged request flow and platform-specific limitations.
7. Finish with a compact report: changed-file summary; commands and pass/fail/not-run results; notable risks; exact next manual check. Commit/push when authorized by the session; existing authorization persists. Do not request repeated confirmation. Verification is a checkpoint within the feature batch, not a requirement to stop development.
