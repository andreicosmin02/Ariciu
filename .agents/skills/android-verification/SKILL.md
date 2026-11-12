---
name: android-verification
description: Verify an Arichu code, test, manifest, resource or Gradle change before declaring it done, or diagnose a failing build/test.
---

1. `git diff --check`, `git status --short`, review the diff for broad edits, renames, dependency bumps, secrets.
2. Run narrow first, compact output (full log only on failure):
   - `./gradlew --console=plain -q :app:testDebugUnitTest --tests '<Class>' 2>&1 | tail -n 40`
   - then `:app:testDebugUnitTest` without `--tests`.
   - `:app:connectedDebugAndroidTest` only with an attached device/emulator.
3. Production code, resources, manifest or Gradle: finish with `:app:assembleDebug` (same flags). A green build does not prove geofence or notification delivery.
4. If blocked (SDK, network, device), diagnose and repair within authorization; report the blocker and what *was* verified. Never report an unrun check as passed.
5. Geofence/notification work: list manual physical-device steps (UI closed, permissions, location off, duplicate broadcasts, Done/Next time, repeat after leaving, restart).
6. Room change: inspect migration and tests. Permission change: check staged request flow.
7. Report: changed files; commands with pass/fail/not-run; risks; next manual check.
