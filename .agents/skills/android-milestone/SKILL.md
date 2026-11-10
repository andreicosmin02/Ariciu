---
name: android-milestone
description: Implement an Ariciu feature batch or behavior change in Kotlin/Compose. Not for planning-only requests or trivial doc edits.
---

1. Read AGENTS.md constraints, the relevant source/tests, and docs/PROJECT_STATUS.md only if status matters. Trust verified source over docs; report discrepancies.
2. Set acceptance criteria. Decide routine matters yourself; ask only about material product/UX, cost, privacy, Android-compromise or blocking questions (see docs/AGENT_GUIDE.md).
3. Inspect existing types and boundaries first. Add a failing focused test before app-owned logic; do not fake native map or geofence validation with mocks.
4. Implement completely with minimal diffs, constructor injection and existing libraries. Keep domain code free of Compose/vendor APIs.
5. For permissions, geofencing, notifications or background work, read current official Android docs and separate OS limits from bugs.
6. Room changes need migration + migration test. Consider process death and duplicate events.
7. Run `$android-verification`. Update docs/PROJECT_STATUS.md only with verified facts. Keep commits focused; never merge, publish or do destructive Git actions without authorization.
