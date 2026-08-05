# Ariciu

## Purpose

Ariciu is an offline-first Android app for location-based reminders. A reminder will eventually contain a geographic point, radius, ENTER or EXIT transition, notification intensity, and repeat behavior.

## Technical foundation

- Kotlin, Jetpack Compose, Material 3, single-activity architecture.
- Add ViewModel, StateFlow, and coroutines when screen state or asynchronous work justifies them.
- Add Room later for persistence, Google Play Services `GeofencingClient` later for geofencing, and `NotificationCompat` later for notifications.
- Prefer simple constructor-based dependency injection; introduce Hilt only when complexity justifies it.

## Hard constraints

- No backend, Firebase, subscriptions, mandatory account, or cloud storage.
- Do not add paid or usage-billed APIs without explicit approval.
- Core behavior must work locally.
- Support Google Play Services devices initially.
- Respect Android's 100-active-geofence limit; never claim events are perfectly immediate or exact.

## Development workflow

- Work in one small, reviewable step; stop after that milestone.
- Inspect relevant files before editing. Do not silently rename packages, classes, fields, or routes.
- Prefer compile-ready Kotlin, small diffs, existing dependencies, and no premature abstractions.
- Keep application-owned models independent from Compose, Android, and MapLibre where practical.
- Use TDD for application-owned behavior. Do not fake tests for remote tiles, native rendering, or network behavior.
- Run focused tests, then broader relevant checks. Explain what automation proves and what needs manual device testing.
- Show the exact diff after each step.

## Verification

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Run only relevant checks for a narrow step, but finish production changes with `:app:assembleDebug`.

## Gradle

- Use the existing version catalog; do not upgrade unrelated dependencies.
- After Gradle or version-catalog changes, Android Studio may need **Sync Project with Gradle Files** even if command-line Gradle succeeds.
- Do not recommend invalidating caches unless ordinary Gradle sync fails.

## Current status

Read `docs/PROJECT_STATUS.md` for current implementation state. Verify it against the repository before relying on it.
