---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Debugging playbook

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Phone Issue Template]], [[Ariciu - Risks and Technical Debt]], [[Ariciu - Code Map]]. Repository-independent essential version: [docs/STABILIZATION.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/STABILIZATION.md).

## Begin with the reported experience

1. Record the expected/actual behavior, reproducible steps, phone/Android/app version, intensity/repeat/transition/radius, app foreground/background/locked state and relevant permission/channel/device Location settings. Preserve the user's reminders.
2. Inspect branch/worktree/history and the relevant implementation. Separate a source hypothesis, an emulator reproduction and a physical observation.
3. Identify the failing boundary before changing code. Add a failing application-owned regression test where possible; for native/OS problems, record a reproducible device scenario instead of a fake test.
4. Fix the smallest coherent cause, then check adjacent working flows and both themes/large text if UI changed.
5. Run relevant tests/lint/assembly; record exact results/remaining phone verification. Commit/push a focused feature-branch change, never master merge or force push.

A late reminder is not automatically a notification bug. A saved reminder is not automatically registered. A registered geofence is not proof that Android delivered a crossing. Diagnose the pipeline in order.

## Isolate the boundary

| Symptom | Inspect first | Distinguish |
| --- | --- | --- |
| Cannot save / data disappears | Draft validity, ViewModel errors, Room repository/migrations | Validation, stale edit, local write failure, incompatible update. |
| “Needs setup” / no monitoring | Settings capability checks, registration/error and channel blocking | Precise/background/device Location/GMS/app access versus just a muted channel. |
| Monitoring incomplete | Allocation and ledger | 100 total areas, 40 selected stores, partial chain versus failed registration. |
| No boundary alert | Event receiver/queue, accepted position/time/boot, lifecycle | Unreported crossing versus rejected duplicate versus blocked notification. |
| Repeated or stale alert | Generation/occurrence, outbox and platform tag | New visit versus repost; never remove guards to force success. |
| Stop/Snooze problem | Receiver immediate quiet, persisted command, quiet/deadline/token, worker wake | System sound behavior, enqueue delay, stale action or wall-clock change. |
| Chain does not rearm | Per-store INSIDE/OUTSIDE/UNKNOWN and selected/monitored snapshot | Overlap, missed exit or unmonitored store state. |
| Search misses a place | TSV/source date/normalization | Missing catalogue data versus matching defect; no paid lookup fallback. |
| Blank or partial offline map | Pack theme/bounds/zoom/completion/resources, HTTP budgets | Cache versus protected offline region; network/provider versus render/lifecycle issue. |
| Controls unreachable | Font/display scale, keyboard/insets, route/dialog state | Device sizing/accessibility versus layout regression. |

## Non-destructive local checks

From repository root, with the SDK's `adb` on PATH and a deliberately selected device:

```bash
git status --short --branch
git log -8 --oneline
adb devices -l
adb shell dumpsys package ro.andrei.ariciu
adb shell cmd appops get ro.andrei.ariciu
adb shell dumpsys notification
adb shell dumpsys jobscheduler
adb logcat -d -s AriciuDelivery:V AndroidRuntime:E
```

For multiple devices use `adb -s DEVICE_SERIAL ...`. Package/appops output helps verify grants and installed version; notification/job dumps help inspect platform state. `AriciuDelivery` logs persistence/worker failures, not every lifecycle event. There is no dedicated in-app debug export or full event timeline at this baseline. Logs/dumps may contain unrelated/private content; inspect locally, redact and share only the necessary excerpt. Do not commit raw phone databases, logs or locations.

For a debug build, Android Studio Database Inspector can inspect the running Room database. Account for WAL files; a raw `.db` copy alone may miss recent transactions. Inspect id/generation/phase/position/occurrence/visible/pending/quiet/snooze/registration/error, chain state and the ledger. Do not edit the user's database to manufacture a reproduction.

## Recovery experiments

Use a disposable emulator profile before force-stop/process-kill/reboot, permission revocation, clock changes, migration fixtures or fault injection. Preserve the daily-use phone installation. Android force stop differs from ordinary process death and may suppress receivers/jobs until reopening. Emulated location proves event handling under controlled conditions, not actual background positioning.

For geofencing, registration initial triggers are disabled. Start outside/inside appropriately and cross well beyond the radius, allowing minutes. Test ENTER and EXIT with Once/Every visit separately, including a return/opposite crossing. Never test while driving yourself. Use the nontechnical phone checklist for real evidence.

For Alarm, compare channel sound, alarm volume, DND, lock state and panel opening. No full-screen/FGS/exact-alarm workaround is authorized. For Snooze, compare persisted wall-clock deadline and worker execution, not only a stopwatch. For offline maps, test one small pack/theme at a time, normal screen exit/background, failure/resume/delete and disconnected rendering.

## Close a fix with evidence

Record root cause, changed paths, regression case, executed counts, screenshots if relevant and what the phone must retest. Keep [docs/PROJECT_STATUS.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/PROJECT_STATUS.md), [docs/CODEX_HANDOFF.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/CODEX_HANDOFF.md) and [[Ariciu - Current Status]] consistent. Do not mark a user issue resolved solely because compilation succeeds.

## Related reading

[Phone Issue Template](Ariciu%20-%20Phone%20Issue%20Template.md) · [Risks and Technical Debt](Ariciu%20-%20Risks%20and%20Technical%20Debt.md) · [Code Map](Ariciu%20-%20Code%20Map.md) · [Current Status](Ariciu%20-%20Current%20Status.md)
