---
name: android-milestone
description: Implement complete Ariciu Android features or related feature batches in Kotlin and Compose. Use when asked to add or change application behavior; not for planning-only tasks or trivial documentation edits.
---

# Implement an Ariciu feature batch

1. Read the repository's `AGENTS.md`, relevant source/tests, and `docs/PROJECT_STATUS.md` if status informs this feature. If documentation and source differ, trust verified source and report the discrepancy.
2. Choose a meaningful feature batch and acceptance criteria from the user’s objective and verified repository state. Make routine technical decisions independently; record important assumptions. Ask only for material product, UX, long-term architecture, cost, privacy, Android compromise, high-risk, or genuinely blocking decisions, following AGENTS.md.
3. Inspect existing types, package paths, navigation boundaries, and dependencies before editing. Preserve names and behavior not expressly in scope.
4. For app-owned logic, add or adjust a focused failing unit test first where feasible. For Compose behavior, add a narrow UI test where feasible. Do not pretend native map rendering or geofencing is validated by a mock-only unit test.
5. Implement a complete, maintainable solution and fix related failures autonomously. For significant UI work, recommend a coherent Material 3 direction and use a working version for feedback when feasible. Keep domain behavior separate from Compose and vendor APIs where practical. Prefer constructor injection and existing libraries.
6. For permission, geofencing, notifications, background execution, SDK or Play-policy work, check current official Android/Google documentation before implementing; distinguish OS restrictions from bugs. Never promise perfectly timely geofence events.
7. Preserve offline-first reminder functionality and zero recurring service cost. Clearly flag internet-dependent map tile behavior and any proposed dependency with possible usage charges.
8. If Room schema changes, include an appropriate migration and migration tests, not destructive fallback. Account for process death and duplicate events when relevant.
9. Invoke `$android-verification` for changes affecting code, tests, manifests, Gradle or resources. Do not mark unrun checks as passing.
10. Synchronize `docs/PROJECT_STATUS.md` with verified changes and fix contradictory status entries. Inspect the exact diff and keep commits focused. Briefly summarize implementation, new user capabilities, executed verification, material questions, and next work. Continue to the next logical task within authorized scope without approval after every milestone; pause only for necessary input or authorization. Never merge, publish, or perform destructive Git actions without authorization.
