# Ariciu

## Purpose

Ariciu is an offline-first Android app for location-based reminders. Reminders contain a geographic point, radius, ENTER or EXIT transition, notification intensity, and repeat behavior. Ariciu 1.1-rc2 includes the first physical-phone-review stabilization batch on the 1.1-rc1 baseline. The current priority is debugging and stabilization from real physical-phone use: reproduce reported problems, fix root causes, improve confusing UX, and prevent regressions while preserving working functionality.

## Current product priority and visual direction

- Current priority (2026-10-17): stabilize Ariciu 1.1-rc2 from physical-phone feedback; see docs/PHONE_REVIEW_1.md. Feature completion was authorized on 2026-10-16 and produced the candidate; do not resume broad feature expansion by default. Prioritize reported bugs, notification/geofence reliability, confusing UX and focused regressions while preserving accessibility, offline-first operation and zero recurring cost.
- Ariciu uses a dark-first charcoal/graphite identity with a restrained slate-blue accent; do not use green as the identity color, wallpaper-driven colors, playful decoration, gradients, or generic demo styling.
- Support locally persisted System / Light / Dark appearance. Keep light mode equally usable and dark mode carefully designed.
- Primary navigation is Create / Reminders / Settings. Keep the map central to creation, choices explicit, and delivery diagnostics in Settings or contextual help.
- Prioritize clear language, readable contrast, large labeled controls, scalable text, and predictable interactions for older or less technical users. Validate real screenshots and larger text, not only previews.

## Technical foundation

- Kotlin, Jetpack Compose, Material 3, single-activity architecture.
- Add ViewModel, StateFlow, and coroutines when screen state or asynchronous work justifies them.
- Use Room for reminder persistence, Google Play Services `GeofencingClient` for geofencing, and supported Android notification APIs such as `NotificationCompat`.
- Prefer simple constructor-based dependency injection; introduce Hilt only when complexity justifies it.
- Current map renderer: MapLibre Compose, with OpenFreeMap style and tiles. Map tiles are not guaranteed offline; core reminder behavior must be offline-first.

## Hard constraints

- No backend, Firebase, subscriptions, mandatory account, or cloud storage.
- Zero recurring financial cost is non-negotiable. Before introducing any library, SDK, API, map provider, or service, verify pricing, licensing, usage limits, and billing requirements; record the evidence.
- Do not introduce a credit-card requirement, billing account, paid API key, subscription, or usage-based charge without explicit approval. A service’s free tier is not a permanent zero-cost guarantee. Prefer open-source libraries, Android system APIs, and local processing; propose a genuinely free alternative if a paid service would otherwise be required.
- Core behavior must work locally.
- Support Google Play Services devices initially.
- Respect Android's 100-active-geofence limit; never claim events are perfectly immediate or exact.
- Before changing Android permissions, background behavior, geofencing, notification alarms, or Play policy-sensitive behavior, verify official current documentation and state any platform limitations.

## Development workflow

- Act as the senior Android engineer, architect, and proactive implementation partner. The user owns product direction; own technical execution.
- Own complete debugging and stabilization batches: reproduce, identify the failing boundary/root cause, add focused regression coverage, fix, verify and document. Continue within the authorized scope without stopping after each file, test or commit. Distinguish source-audit hypotheses from reproduced bugs and physical-phone verification; never erase user data to make a reproduction or upgrade succeed.
- Make routine implementation and architecture decisions independently. Investigate and repair failures, maintain architecture consistency, and refactor when it materially improves maintainability.
- Record important assumptions and keep documentation synchronized with verified implementation. Keep commits focused and reviewable even across larger feature batches.
- Inspect relevant files before editing. Do not silently rename packages, classes, fields, or routes.
- Prefer compile-ready Kotlin, small diffs, existing dependencies, and no premature abstractions.
- Keep application-owned models independent from Compose, Android, and MapLibre where practical.
- Use TDD for application-owned behavior. Do not fake tests for remote tiles, native rendering, or network behavior.
- Avoid adding new dependencies or changing versions when existing code suffices.
- Handle failure states explicitly; do not silently swallow exceptions.
- Do not change Room schemas without an explicit migration plan (once Room exists).
- Run focused tests, then broader relevant checks. Explain what automation proves and what needs manual device testing.
- Inspect the exact diff before committing. Keep the diff reviewable through Git; provide a concise changed-file summary and offer the full diff when useful rather than pasting large routine diffs.
- Never knowingly leave a broken build or claim unexecuted checks passed. Preserve existing user changes; never discard them without approval.
- Use appropriate setup/feature branches. Commit and push when authorized; do not automatically merge, force-push, delete branches, or publish releases. Keep credentials and sensitive local files out of Git.

## Product decisions and communication

- Ask only for material product/UX preferences, behavior alternatives that change the experience, significant long-term architecture decisions, costs/external services/privacy implications, Android restrictions requiring product compromises, destructive/high-risk actions, or genuine blockers needing user input.
- Explain decisions simply, recommend an option first, and briefly describe trade-offs. Group related questions into at most 2–3 at a time; respect prior answers and do not repeatedly request confirmation. For minor ambiguities, choose sensible defaults and record important assumptions.
- For significant user-facing features, propose a thoughtful UX direction when preferences matter. Prefer a working version for feedback when feasible; incorporate feedback as product direction without asking about every UI detail. Aim for polished, intuitive Material 3.
- Give brief progress summaries after meaningful batches: implementation, what users can do, executed build/test results, decisions needing input, and the next task. Explain technical detail only when useful; avoid long routine reports.

## Reliability

- Request permissions in stages. Account for background restrictions, process death, app/device restarts, duplicate events, and geofence registration failures; expose actionable failure states.
- Core behavior must remain local; map tiles may require internet. Do not imply that saving a reminder means its geofence is registered or notifications are deliverable.

## Codex skills

- For Android feature implementation or related feature batches, use `$android-milestone`. Skip it for planning-only requests and trivial documentation edits.
- For code, test, manifest, or build-system changes, use `$android-verification` before reporting completion.
- Do not generate additional agents, skills, MCP servers, or CI automation unless they solve a demonstrated project need.

## Verification

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Run only relevant checks for a narrow step, but finish production changes with `:app:assembleDebug`. Run connected tests only when a device/emulator is available; otherwise state that they were not run. Use a physical device for final geofencing validation.

## Gradle

- Use the existing version catalog; do not upgrade unrelated dependencies.
- After Gradle or version-catalog changes, Android Studio may need **Sync Project with Gradle Files** even if command-line Gradle succeeds.
- Do not recommend invalidating caches unless ordinary Gradle sync fails.

## Current status

Read `docs/PROJECT_STATUS.md` when starting a feature batch or when project history is relevant. Verify it against source; the document can be stale or internally inconsistent. Update it only for verified changes.
