# Real-phone debugging and stabilization

Updated 2026-10-17 for Ariciu 1.1-rc4. Current owner-directed Urgent re-alert policy and Alarm retirement: [URGENT_RE_ALERTS.md](URGENT_RE_ALERTS.md). Start with [handoff](CODEX_HANDOFF.md), [status](PROJECT_STATUS.md) and [audit/limits](STABILIZATION_AUDIT.md). The first owner phone review and its implemented rc2 fixes are recorded in [PHONE_REVIEW_1.md](PHONE_REVIEW_1.md). The second review and limited actual rc2 Samsung swipe verification are in [PHONE_REVIEW_2.md](PHONE_REVIEW_2.md); rc3 phone retest is pending. Do not manufacture a bug list from hypotheses.

## Workflow

1. Capture expected/actual behavior, repeatable steps, device/Android/app version, intensity/transition/repeat/radius, foreground/background/lock state and permissions/channels/Location/battery/connectivity. For UI issues capture theme, font/display scaling and keyboard/accessibility context. Keep locations private.
2. Inspect current Git/worktree/source, preserve user data and isolate the failing boundary: UI/draft → Room → registration/capabilities → GMS event → lifecycle → outbox → Android notification → action/work recovery.
3. Reproduce on an appropriate emulator/test device. A source concern is a hypothesis; an emulator result is not a phone result. Do not clear storage/uninstall/reset databases to make a failure disappear.
4. Add a meaningful failing regression test for application-owned behavior when possible. For OS/native behavior, retain a repeatable device scenario; do not fake network/tile tests.
5. Fix the root cause in the existing architecture, then check adjacent flows and accessibility/themes where affected. Preserve ID+generation writes, occurrence guards, ledger/outbox and migration history.
6. Run focused checks, relevant broader gates, lint and debug assembly. Commit/push focused authorized feature-branch changes; update handoff/evidence. A phone issue remains awaiting phone retest when only emulator/model checks pass.

## First-pass diagnosis

| Symptom | First boundary to inspect |
| --- | --- |
| Save/edit/data loss | `RemindersViewModel`, draft validity/errors, repository `saveEdited`, Room migrations. |
| Needs setup/not monitored | `deliveryCapabilities`, app/channel access, precise/background/device Location/GMS; registration/error. |
| Partial chain/no expected store | Fixed selected snapshot, 40-store bound, 100-area allocation and monitored IDs. |
| No arrival/departure alert | Receiver/WorkManager, last accepted position/boot/elapsed time, phase and outbox; distinguish no event from rejected event. |
| Duplicate/stale action | Generation/occurrence/tag identity, duplicate guard, outbox repost versus new visit. |
| Stop/Snooze unreliable | Immediate quiet request, persisted command, quiet/deadline/token, worker delay and clock changes. |
| Blank offline map | Theme/bounds/zoom/completion, hosted/display resource compatibility, HTTP budget and Native lifecycle. |
| Search gap | Local catalogue/date/normalization versus missing source data; no remote paid fallback. |
| Unreachable/confusing UI | Font/display size, keyboard/insets/scroll, dialog/route and status copy. |

## Safe diagnostic commands

Use SDK adb on PATH and explicitly select the authorized test device. Replace the placeholder with its serial; do not default to a daily-use phone.

```bash
git status --short --branch
git log -8 --oneline
adb devices -l
export ARICIU_TEST_SERIAL="YOUR_TEST_DEVICE_SERIAL"
adb -s "$ARICIU_TEST_SERIAL" shell dumpsys package ro.andrei.ariciu
adb -s "$ARICIU_TEST_SERIAL" shell cmd appops get ro.andrei.ariciu
adb -s "$ARICIU_TEST_SERIAL" logcat -d -s AriciuDelivery:V AndroidRuntime:E
adb -s "$ARICIU_TEST_SERIAL" shell dumpsys notification
adb -s "$ARICIU_TEST_SERIAL" shell dumpsys jobscheduler
```

`AriciuDelivery` logs enqueue/worker failures, not every event. There is no dedicated debug export/event timeline. Dumps/logs can contain unrelated/private data: inspect locally and retain only a redacted relevant excerpt, never raw databases/signing keys/private coordinates in Git. Android Studio Database Inspector can inspect a debug process; raw `.db` copies can miss WAL transactions. Do not mutate the user's Room data for debugging.

Inspect reminder generation/phase/position/occurrence, notificationVisible/Pending, quiet/snoozeUntilMs, registration/error, chain per-store state and `registered_geofences`. The ledger describes intended/cleanup identity, not proof of live GMS registration. WorkManager/Room/platform side effects are separate commits.

## Verification commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
# With a deliberately chosen emulator/test device:
ANDROID_SERIAL="$ARICIU_TEST_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

Do not run app-data-writing instrumentation on the owner's daily-use phone without evaluating the fixtures. Use disposable data for kill/reboot/force-stop, clock changes and migrations. Force stop differs from ordinary process death and can suppress work until reopening.

Real geofence tests must cross well beyond a sensible 200–500m starting radius and allow minutes. Registration has initial trigger 0; already-inside registration should not invent a visit. EXIT repeats need a return; Every visit needs the opposite crossing. Do not test while driving yourself. For Alarm compare channel/DND/volume/lock/panel behavior rather than introduce full-screen/FGS/exact-alarm workarounds. [Phone checklist](PHONE_VERIFICATION.md) remains the field guide.

## Issue record

Record: title/date/timezone; expected/actual; steps/frequency; device/build/settings; reminder scenario/status; reproduction evidence; confirmed root cause; fix/test commit; executed checks; phone retest result and remaining uncertainty. A fuller copyable template is in the [knowledge base](<obsidian-export/Ariciu/Ariciu - Phone Issue Template.md>).

No new external service, dependency cost, broad redesign or coverage expansion is implied by a bug report. Recheck current official platform/provider rules before changing sensitive behavior.
