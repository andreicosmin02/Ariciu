---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/urgent-realerts
version: 1.1-rc4
---

# Notifications and Alarm

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Reminder Lifecycle]], [[Ariciu - Background Work and Recovery]], [[Ariciu - Geofencing and Permissions]].

Source: [feature/delivery/platform/AndroidDelivery.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/platform/AndroidDelivery.kt) (`AndroidReminderNotifications`, `quietNotificationAction`) and [DeliveryWork.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/platform/DeliveryWork.kt) (`NotificationActionReceiver`).

## Actual channels and behavior

| Intensity | Channel ID | Implemented request |
| --- | --- | --- |
| Silent | reminders_silent_v1 | IMPORTANCE_LOW, null sound, vibration disabled, setSilent; Done/Next time; dismissible. |
| Normal | normal_reminders | Existing IMPORTANCE_HIGH channel; user/default sound and vibration settings; Done/Next time; dismissible. |
| Urgent | reminders_urgent_v1 | Separate IMPORTANCE_HIGH channel plus ongoing=true; Done/Next time; each swipe persists a five-minute delayed re-alert until explicit resolution. No distinct custom louder sound or stronger vibration pattern is configured. |
| Legacy Alarm (existing only) | reminders_alarm_v1 | IMPORTANCE_HIGH, default alarm ringtone with USAGE_ALARM/SONIFICATION, vibration pattern [0,700,250,700,250,1000], ongoing=true; Stop/Done/Snooze 10 min. |

All use a private lock-screen visibility request, notification/reminder or alarm category, a native monochrome small icon, no auto-cancel on tap. Urgent delete intents mean DISMISS and a delayed re-alert; other intensities retain Next time. HIGH importance and priority are requests; channel settings, DND, user controls and OS/OEM behavior determine actual alerts. On modern Android even ongoing notifications may be dismissible. Silent's defaults must not be confused with an ability to override every user/OEM channel change.

Channel settings are user-controlled after creation. Recreating a channel does not reset their choices. Changing an existing reminder's intensity can cancel an old-channel notification before posting the replacement. Existing Normal channel identity is preserved across upgrades.

## Identity and PendingIntents

One stable Android notification tag per parent: `reminder:<reminderId>`, integer ID 1. New occurrences replace the previous visible alert for that reminder. Extras carry ariciu_generation, ariciu_occurrence, ariciu_number and ariciu_attempt; an Urgent marker supports quiet upgrade refresh.

Action URI: `ariciu://occurrence/<encoded parent requestId>/<occurrence>/<action>`. DISMISS additionally appends /attempt to the URI and carries notification_attempt; explicit action URIs stay unchanged. Extras carry request_id/occurrence; action enum is the Intent action. Broadcast PendingIntents are explicit and immutable. Activity PendingIntents use `ariciu://reminder/<encoded id>`, EXTRA_REMINDER_ID and CLEAR_TOP/SINGLE_TOP to open/highlight the item. Chain actions address the parent, not one store.

Same occurrence and alert attempt suppress repeated alerting when updating an existing visible notification. The persistent engine checks **generation and occurrence**, independently of this presentation-level suppression. Cancellation removes only reminder tags that are no longer marked visible.

## Current Urgent policy and temporary Alarm retirement

Only Silent, Normal and Urgent are offered. Existing ALARM data/actions/channel remain supported; editing keeps Alarm unless explicitly converted. A pending legacy snooze keeps its deadline on Urgent conversion. New/restored unsaved Alarm drafts become Urgent for review. No persisted Alarm is silently converted.

Urgent DISMISS persists urgentRetryAtMs with its occurrence/attempt; one unique delayed job advances only notificationAttempt and requests a fresh standard alert. A visible alert has no retry job. Older dismissal attempts cannot dismiss the re-alert. Explicit Done/Next time resolve the same occurrence even while waiting. Unresolved Urgent is held across Every visit and chain crossings. Cards show due/waiting status and both actions. Existing visible old-version Urgent alerts are quietly rebound; fresh retries remain audible requests. Permission/channel denial retains one pending outbox attempt without polling or catch-up bursts.

See [[Ariciu - Urgent Re-alerts]] for source, migration, executed evidence, limits and retest. No continuous audio, exact alarm, foreground service, fake call/media/full-screen or GPS loop.

## Alarm Stop and Snooze

FLAG_INSISTENT requests repeating system notification sound for a new, non-quiet Alarm occurrence. No MediaPlayer, custom audio service, exact alarm, full-screen activity, FGS launch, DND bypass or volume forcing is used. Opening the notification window can stop the repeating sound. No guarantee of continuous sound, full-screen presentation or a particular vibration strength is made.

For explicit notification actions (not DISMISS), the receiver first finds a matching active generation/token, clones its notification, removes INSISTENT and adds ONLY_ALERT_ONCE. This requests immediate silencing before WorkManager persists the semantic action. The engine then handles Stop/Done/Snooze/Next time. After Stop, the same Alarm is quiet and retains Done/Snooze actions. A timed snooze creates a later token; swipe instead means next visit.

Boot restoration of an already-delivered alarm is quiet. A pending alert that was never successfully delivered can still alert when access returns. Stop-before-persistence crash recovery and receiver/posting races need real/stress validation; they are not proved by a successful action-intent test.

## Supported fallback and tests

The supported Alarm fallback is an actionable prominent notification. If app-wide notification access or the Alarm channel itself is disabled, no such alert can be posted; reminders show blocked setup help until access is restored. Full-screen special access is not requested; the app's location-reminder purpose is not treated as an automatic alarm-clock eligibility exemption. If Android limits audio, Ariciu keeps supported notification/actions rather than adding prohibited workarounds.

Automatically tested: channel defaults/flags/actions on an emulator, injected worker crossing, swipe/Next time/Done and persisted Alarm Stop/Snooze; lifecycle/token guards are unit tested. Not verified: real-phone audio/vibration, heads-up, locked-screen behavior, full background timing, DND/OEM differences. Original planning notes proposing stronger/non-dismissible Urgent and full-screen Alarm are historical; see [[Ariciu - Historical Notes]].


## Historical first phone review (rc2)

Urgent still requests high importance and ongoing=true. Android 14+ permits individual unlocked swipes; the delete intent persists Next time through the real receiver/worker. No automatic repost or false call/media exemption. Help now states this directly. Only Android 17 runtime was available in this environment; older runtime and Samsung locked/Clear all behavior need phone testing. See [[Ariciu - Phone Review 1]].

## Historical second phone review — rc3

The notification small-icon resource now contains a dedicated transparent white 24dp fox, independent of the unchanged adaptive launcher. Actual Android 17 light/dark shades reviewed; Samsung rendering awaits rc3 retest. Read-only rc2 Samsung ADB confirmed HIGH/ongoing/deleteIntent. Owner swipe produced WAITING_NEXT_VISIT, INSIDE, occurrence 1, no visible/pending notification and REGISTERED monitoring. Android 14+ permits individual unlocked dismissal; locked/Clear all remain phone-test items. No repost/exemption workaround. [[Ariciu - Phone Review 2]] records limits without private content.

## Related reading

[Reminder Lifecycle](Ariciu%20-%20Reminder%20Lifecycle.md) · [Background Work and Recovery](Ariciu%20-%20Background%20Work%20and%20Recovery.md) · [Geofencing and Permissions](Ariciu%20-%20Geofencing%20and%20Permissions.md) · [Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md) · [Historical Notes](Ariciu%20-%20Historical%20Notes.md) · [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) · [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md)
