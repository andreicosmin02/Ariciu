# Urgent re-alerts and temporary Alarm retirement — rc4

> Historical implementation-batch report. Subsequent owner-authorized consolidation made `master` authoritative and published [v1.1-rc4](https://github.com/andreicosmin02/Ariciu/releases/tag/v1.1-rc4). See [publication evidence and cleanup plan](GITHUB_RELEASE_RC4.md). The implementation and emulator evidence below are unchanged; initial owner phone feedback is positive, with targeted checks still pending.

17 October 2026. Branch `fix/urgent-realerts`, from clean rc3 `fix/phone-review-2` at `8f11976`. Candidate **1.1-rc4 / code 5**, Room **5**. Nothing merged or published. This owner-directed behavior supersedes rc3’s Urgent swipe-to-Next-time policy; historical Samsung rc2 observations remain valid for that old version.

## Behavior and UX

The available levels are **Silent, Normal and Urgent**. Silent/Normal keep their channels and swipe-to-Next-time behavior. Urgent keeps its existing HIGH/ongoing channel and a single standard alert on its first occurrence. Done and Next time remain in the notification.

An individual Urgent swipe now sends distinct `DISMISS`, not `NEXT_TIME`. It hides the alert, retains TRIGGERED/unresolved state and persists a deadline five minutes from receiver receipt. At/after the deadline one new alert attempt uses the existing notification tag/channel and occurrence. It says “Still pending. Choose Done or Next time.”; it does not claim another crossing. Each subsequent swipe starts another delayed attempt. A visible alert has no retry deadline/job and does not sound repeatedly. There is no continuous sound or periodic worker.

The editor describes re-alerting before Urgent selection. Active cards show the scheduled time or waiting-for-access state, Mark done and Next time. Mark done completes the reminder; Next time ends this occurrence and uses the existing conservative opposite-crossing rearm. Both clear deadlines and schedule asynchronous notification/job/registration cleanup. Unresolved Urgent remains unresolved across ENTER/EXIT, Every visit and chain union transitions; position and event ordering still update. Once explicitly handled, normal rearming rules apply. No global geofence initial trigger change.

Alarm is temporarily unavailable for new reminders and every intensity picker. `ALARM`, its channel, data, Stop/Done/Snooze actions and ten-minute snooze implementation remain. Existing Alarm reminders can still be edited/saved as Alarm. The editor explains the retirement and offers explicit Urgent conversion through Alert. A pending legacy snooze retains its existing deadline as an Urgent wait; later Urgent swipes use five minutes. Saved Alarm data is never silently converted. An unsaved restored/new Alarm draft becomes Urgent for review before saving. Reconsidering Alarm is future work, not part of this batch.

## Smallest reliable extension

- Room 4→5 adds `urgentRetryAtMs` (wall-clock deadline) and `notificationAttempt`, both zero by default. Exported schemas 1–4 and migrations remain intact; schema 5 is added. The migration also holds existing visible/unanswered Urgent occurrences in TRIGGERED where rc3 Every visit had rearmed them. It does not revive previously resolved Next time/completed rows or change Alarm state, store snapshots or registration identities.
- No new phase/table/dependency/service/permission. `snoozeUntilMs` remains legacy Alarm’s separate deadline. The shipping delay is `URGENT_RE_ALERT_DELAY_MS = 300_000`; `DeliveryEngine` constructor injection permits shorter deterministic timer tests. No user setting or broadcast-supplied delay.
- `onUrgentDismiss` checks active phase, generation (engine), occurrence, visibility and alert attempt. Duplicate swipes do not extend a deadline; an older attempt cannot dismiss a re-alert. Due processing increments only the attempt, never the geofence occurrence. Explicit Done/Next time address the unresolved occurrence, so a valid user resolution can win even if a due attempt just advanced; older generations/occurrences remain rejected.
- `UrgentRetryWork` uses one unique delayed `OneTimeWorkRequest` per parent request/generation/occurrence/attempt, KEEP, with no network constraint. It persists enqueue/cancel operations before worker completion, removes obsolete jobs and avoids cancelling the currently executing timer. A worker rechecks persisted identity/deadline; a clock moving backwards uses inexact backoff rather than alerting early. Recovery also consumes due deadlines and reconstructs missing jobs.
- Notification outbox semantics remain: persist visible/pending before posting, clear pending only on success, stable tag plus occurrence/attempt metadata suppresses duplicate delivery. A blocked channel or missing app notification access leaves one due attempt in the outbox; reopening after access is restored dispatches it without a catch-up burst or permission polling.
- Final Urgent validation/post happens inside a Room transaction so card completion/edit/delete between the earlier snapshot and posting cannot publish a stale attempt. Generation-conditional writes remain. Card Next time uses a Room transaction and rotates generation, matching safe card completion cleanup. Retry cancellation is asynchronous; a racing old job cannot revive a resolved row.
- Existing visible rc3 Urgent notifications need new delete intents. A marker detects only those older visible alerts and quietly rebinds them on delivery/reconciliation. Missing platform notifications are not unconditionally reposted; later genuine retries remain audible requests. Normal/Silent and legacy Alarm explicit PendingIntent identities are retained; only DISMISS adds an alert-attempt suffix.

Room, WorkManager and the Android notification service cannot share an atomic transaction. Reconciliation/startup/boot/package replacement repairs scheduling gaps and the outbox; a killed receiver before its command is durably enqueued cannot guarantee recovery of that swipe. Never claim absolute exactly-once or immediate cleanup.

## Verification

Final command passed: `ANDROID_SERIAL=emulator-5554 ./gradlew -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest`. Results: **97 unit tests, 44 connected Android 17 emulator tests, zero failed/skipped; lint 0 errors/39 warnings; debug assembly/install/signature passed**. Connected gate completed in 1m52s. The retention option kept emulator app/data installed after tests; only the emulator was connected. No phone instrumentation.

Coverage includes first Urgent post and one standard alert request; distinct swipe intent, five-minute receipt-time deadline, duplicates; two successive real WorkManager delayed re-alerts with constructor-configured 6s waits; visible-alert suppression; stale attempts/generations/occurrences; card/notification Done and Next time cancelling waits; Every visit ENTER/EXIT and chain holding; due-outbox denied-access recovery; completion between due snapshot and final post; 1→5/2→5/4→5 schema validation, legacy Alarm preservation/conversion and Room reopen; immediate-on-save followed by real delayed retries; quiet old-notification rebinding. Unit denied-access/channel refusal tests inject those boundary failures; actual permission/channel checks are described separately below.

Actual rc3→rc4 emulator APK upgrade preserved all four existing reminders and core fields; schema 5 was validated. Seven UI captures in [design/screenshots/urgent-realerts](design/screenshots/urgent-realerts) cover both themes, pending actions, legacy Alarm and the three-level selector. A separately executed native-window test at system font scale 2.0 passed 1/1 and adds an eighth capture; scrolling/selection/Save remained reachable. System font scale was restored.

Intermediate findings: the first failing regression proved rc3 Every visit rearmed unanswered Urgent; the fix holds it. An old-notification fixture raced with automatic reconciliation, which repaired it before the assertion; using the runtime mutex made that controlled test deterministic (targeted rerun 1/1, then full 44 passed). An AppOps-only denial fixture did not revoke modern notification permission; it was replaced with host-driven actual runtime permission checks because revocation kills the instrumentation process. One earlier run lost the emulator connection when the emulator stopped; it was not counted passed. The data was retained and the final headless emulator run completed successfully. No production delays were introduced.

## Actual Android 17 emulator recovery observation

A harmless new single ENTER/Every visit reminder used the existing opt-in foreground check with emulator-provided coordinates, not real GPS. It posted the first actual Urgent alert. Individual notification-shade swipe persisted TRIGGERED/INSIDE, occurrence 1/attempt 0, hidden/not pending, a five-minute deadline and exactly one unfinished timer.

Outside instrumentation, actual notification permission revocation killed the app process. The exact generation/occurrence/attempt/deadline and one timer survived emulator reboot and an in-place same-key APK replacement. The explicit user-set denied permission also persisted across another reboot/replacement. At the shipping five-minute deadline, due processing advanced to attempt 1 in the same occurrence and retained one visible/pending outbox attempt, with no timer and no posted notification. Restoring permission and reopening dispatched exactly one fixture alert, HIGH/ongoing on reminders_urgent_v1, and cleared the outbox. No new crossing or continuous location request was used for this re-alert.

A second actual swipe scheduled another deadline/job for attempt 1. Disabling only Urgent through stock Android Settings produced channel importance NONE while Normal remained HIGH, and the focused card showed Re-alert waiting. Card Next time cleared the deadline, cancelled the unfinished timer and persisted WAITING_NEXT_VISIT; subsequent card Mark done completed only the harmless fixture. The original effective notification grant, enabled/HIGH Urgent channel and font scale 1.0 were restored. No pending fixture timer/alert remained. The notification-navigation flags/extra were also exercised; a bare shell launch without the content-intent flags merely brought the existing Create task forward and was corrected in the diagnostic driver.

Emulator testing note: an AppOps-only toggle was not a runtime-permission denial. A bare pm revoke without user-set was not retained through this emulator reboot/update path; the documented user-set denial recipe was used for the final check. These are test-setup observations, not evidence that Ariciu can grant permissions or override user denial. Runtime permission and original effective enabled state were restored; no such operations occurred on the Samsung.

## Android limitations and sources

Five minutes is an approximate eligibility time, not an exact alarm or guaranteed delivery time. WorkManager can be delayed by Doze, job quotas, battery restrictions and Samsung/other OEM policies. Notification access, channel settings, DND and OS alerting controls govern sound/pop-ups. Force stop prevents normal work until reopening. A deadline uses the device wall clock (as legacy snooze does), so manual clock changes can move its perceived due time. No exact alarm, foreground service, full-screen activity, call/media exemption, wake-lock loop, continuous sound or GPS polling is introduced. Core remains local with zero recurring cost.

Official sources checked 17 October:

- [WorkManager delayed work and timing limits](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work#delayed)
- [Unique work and cancellation](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work)
- [Persistent background work](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Android 14+ ongoing notification dismissal](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)
- [Notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)

Samsung S26 Ultra / Android 16 / One UI 8.5 is not connected for this batch. Historical rc2 swipe evidence is in PHONE_REVIEW_2.md; it cannot verify the new rc4 policy. Real GPS/geofence delivery, Samsung lock/Clear all behavior, sound/vibration, long idle/OEM delay and TalkBack remain phone checks.

## Physical-phone retest

1. Install rc4 over the current app with the same signing key. Never uninstall/clear storage. Confirm existing reminders, including any Alarm, remain.
2. At a safe stationary location, create a new single Arrive reminder, radius 200–500m, Every visit, Urgent; explicitly enable Notify me now while keeping Ariciu open. Or trigger it by a real crossing. Expect one standard alert with Done/Next time.
3. Swipe it unlocked. Confirm Re-alert scheduled plus Mark done/Next time on the card. Leave/reenter if convenient: the unresolved occurrence must remain one. Around five minutes or later, expect one fresh alert; leave it visible and check that it does not keep alerting. Swipe again and repeat.
4. During a wait, use card Next time; confirm the deadline disappears and no delayed retry returns. Leave/return for a new visit, then test Mark done during another pending wait. Old notification actions must not revive completed/newer occurrences.
5. With explicit owner cooperation, test an ordinary app close, reboot/unlock and upgrade during a wait. Test disabled Urgent channel/app notification permission: no alert while blocked, truthful waiting status, then one pending alert after restoring access and reopening. Never change the owner's settings/process/clock without permission during assisted diagnostics.
6. Existing Alarm: edit/save unchanged; verify legacy actions if desired, then explicitly select Urgent and save. Keep existing snooze timing where present. New pickers and Settings offer only Silent/Normal/Urgent. Review both themes/larger text and TalkBack.

## Commits

- `0e11336`: durable Urgent deadline/attempt, distinct dismissal, guarded engine/outbox/unique work, Room 5 migration and focused lifecycle/integration coverage.
- `43973e5`: waiting card actions/status, three choices, legacy Alarm conversion UX and screenshot/large-text coverage; rc4/code 5.
- `525b609`: original documentation/evidence commit on fix/urgent-realerts. The later authorized merge/pre-release is recorded separately in GITHUB_RELEASE_RC4.md.
