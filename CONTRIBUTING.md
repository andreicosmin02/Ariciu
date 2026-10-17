# Contributing to Ariciu

Ariciu is in physical-phone testing and stabilization. Focus on reproducible bugs, reminder reliability, accessibility and clear UX before expanding scope. Codex and Obsidian are not required.

## Set up and propose a change

Follow [Build and test](docs/DEVELOPMENT.md) for Android Studio, JDK 25, SDK 37, local SDK configuration and the Gradle wrapper. Read the [current status](docs/PROJECT_STATUS.md) and [architecture](docs/DELIVERY_ARCHITECTURE.md).

Create a focused branch from the latest clean master (in your fork or an authorized checkout):

```bash
git switch master
git pull --ff-only
git switch -c fix/short-description
```

Discuss substantial product changes, dependencies, service costs and privacy implications first. Submit a pull request targeting `master` with the problem, resulting behavior, relevant screenshots, tests actually run and remaining device checks. Preserve unrelated changes and commit history; do not force-push shared branches. Maintainers review and merge; a PR is not authorization to publish an APK.

Ariciu-owned material is under [Apache-2.0](LICENSE), with the [scope and third-party exclusions](docs/LICENSING.md) explained separately. Submit work you have the rights to contribute; identify third-party sources and preserve their notices. Apache-2.0 section 5 addresses intentional contributions unless explicitly stated otherwise. No separate contributor agreement is introduced.

## Engineering principles

- Keep reminders and lifecycle offline-first. No backend, Firebase, mandatory account, subscription or recurring paid service. Verify cost/licence/privacy evidence before proposing dependencies or providers.
- Use existing Kotlin/Compose/Material 3 patterns, constructor injection, ViewModel/StateFlow and Room. Keep application-owned models independent from Android/Compose where practical. Prefer small, readable changes and the existing version catalogue.
- Preserve generation/occurrence guards, notification identities/outbox, geofence registration journal/recovery, saved chain snapshots and the **100-area** limit.
- Never clear user data to fix a bug or make a migration pass. Room changes need an explicit additive migration plan, registered migrations, exported new schema and tests using older fixtures; preserve prior schemas/history.
- Check current official Android documentation before changing permissions, background work or notification behavior. Respect channel/DND controls and platform limits; do not promise exact geofence/retry timing or introduce unsupported full-screen/foreground-service workarounds.
- Preserve graphite/slate identity, both themes, scalable text and accessible controls. Inspect real screen captures when UI changes; a preview alone is insufficient.

## Verification

Use focused regression tests for meaningful application-owned behavior, ideally reproducing the failure first. Finish Android changes with:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Run relevant [instrumentation on a deliberately selected disposable test device](docs/DEVELOPMENT.md#instrumentation-on-a-selected-test-device). Never default to a daily-use phone. Clearly distinguish automated, emulator and physical-device evidence. Do not claim a phone bug resolved until the relevant phone scenario is confirmed; note unexecuted checks.

For documentation, validate Markdown links, wikilinks, attachments and referenced facts. Update the focused technical notes and current status where implementation changes; retain dates/version scopes of historical reports. The [portable knowledge base](docs/obsidian-export/README.md) is optional and edited in the repository, never through access to another person’s vault.

## Report a bug

Include app version, Android/device model, expected versus observed behavior, reproducible steps and frequency. For delivery issues add arrival/departure, repeat/intensity/radius, card status, foreground/background/lock state and relevant permission/channel/location/battery settings. For UI issues add theme, font/display size and a redacted screenshot if useful. See the [issue template](docs/obsidian-export/Ariciu/Ariciu%20-%20Phone%20Issue%20Template.md).

Share only necessary redacted diagnostics. Do not post exact personal locations, reminder text, raw databases, unrelated notification content, ADB pairing credentials, signing keys or tokens. Check `git diff --check`, the full diff and `git status` before committing; keep `local.properties`, private `.obsidian/` settings, credentials and generated outputs out of Git.

Automation-specific guidance lives in [AGENTS.md](AGENTS.md); ordinary contributors can use this guide directly.
