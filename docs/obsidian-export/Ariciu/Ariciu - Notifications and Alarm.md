---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Notifications and Alarm

Related: [[Ariciu - Reminder Lifecycle]], [[Ariciu - Background Work and Recovery]], [[Ariciu - Geofencing and Permissions]].

Source: `feature/delivery/platform/AndroidDelivery.kt` (`AndroidReminderNotifications`, `quietNotificationAction`) and `DeliveryWork.kt` (`NotificationActionReceiver`).

## Actual channels and behavior

| Intensity | Channel ID | Implemented request |
| --- | --- | --- |
| Silent | reminders_silent_v1 | IMPORTANCE_LOW, null sound, vibration disabled, setSilent; Done/Next time; dismissible. |
| Normal | normal_reminders | Existing IMPORTANCE_HIGH channel; user/default sound and vibration settings; Done/Next time; dismissible. |
| Urgent | reminders_urgent_v1 | Separate IMPORTANCE_HIGH channel plus ongoing=true; Done/Next time. No distinct custom louder sound or stronger vibration pattern is configured. |
| Alarm | reminders_alarm_v1 | IMPORTANCE_HIGH, default alarm ringtone with USAGE_ALARM/SONIFICATION, vibration pattern [0,700,250,700,250,1000], ongoing=true; Stop/Done/Snooze 10 min. |

All use a private lock-screen visibility request, notification/reminder or alarm category, a native monochrome small icon, no auto-cancel on tap, and a delete intent meaning Next time. HIGH importance and priority are requests; channel settings, DND, user controls and OS/OEM behavior determine actual alerts. On modern Android even ongoing notifications may be dismissible. Silent's defaults must not be confused with an ability to override every user/OEM channel change.

Channel settings are user-controlled after creation. Recreating a channel does not reset their choices. Changing an existing reminder's intensity can cancel an old-channel notification before posting the replacement. Existing Normal channel identity is preserved across upgrades.

## Identity and PendingIntents

One stable Android notification tag per parent: `reminder:<reminderId>`, integer ID 1. New occurrences replace the previous visible alert for that reminder. Extras carry ariciu_generation, ariciu_occurrence and ariciu_number.

Action URI: `ariciu://occurrence/<encoded parent requestId>/<occurrence>/<action>`. Extras carry request_id/occurrence; action enum is the Intent action. Broadcast PendingIntents are explicit and immutable. Activity PendingIntents use `ariciu://reminder/<encoded id>`, EXTRA_REMINDER_ID and CLEAR_TOP/SINGLE_TOP to open/highlight the item. Chain actions address the parent, not one store.

Same occurrence number suppresses repeated alerting when updating an existing visible notification. The persistent engine checks **generation and occurrence**, independently of this presentation-level suppression. Cancellation removes only reminder tags that are no longer marked visible.

## Alarm Stop and Snooze

FLAG_INSISTENT requests repeating system notification sound for a new, non-quiet Alarm occurrence. No MediaPlayer, custom audio service, exact alarm, full-screen activity, FGS launch, DND bypass or volume forcing is used. Opening the notification window can stop the repeating sound. No guarantee of continuous sound, full-screen presentation or a particular vibration strength is made.

For any valid notification action, the receiver first finds a matching active generation/token, clones its notification, removes INSISTENT and adds ONLY_ALERT_ONCE. This requests immediate silencing before WorkManager persists the semantic action. The engine then handles Stop/Done/Snooze/Next time. After Stop, the same Alarm is quiet and retains Done/Snooze actions. A timed snooze creates a later token; swipe instead means next visit.

Boot restoration of an already-delivered alarm is quiet. A pending alert that was never successfully delivered can still alert when access returns. Stop-before-persistence crash recovery and receiver/posting races need real/stress validation; they are not proved by a successful action-intent test.

## Supported fallback and tests

The supported Alarm fallback is an actionable prominent notification. If app-wide notification access or the Alarm channel itself is disabled, no such alert can be posted; reminders show blocked setup help until access is restored. Full-screen special access is not requested; the app's location-reminder purpose is not treated as an automatic alarm-clock eligibility exemption. If Android limits audio, Ariciu keeps supported notification/actions rather than adding prohibited workarounds.

Automatically tested: channel defaults/flags/actions on an emulator, injected worker crossing, swipe/Next time/Done and persisted Alarm Stop/Snooze; lifecycle/token guards are unit tested. Not verified: real-phone audio/vibration, heads-up, locked-screen behavior, full background timing, DND/OEM differences. Original planning notes proposing stronger/non-dismissible Urgent and full-screen Alarm are historical; see [[Ariciu - Historical Notes]].


## Phone review update (17 October)

Urgent still requests high importance and ongoing=true. Android 14+ permits individual unlocked swipes; the delete intent persists Next time through the real receiver/worker. No automatic repost or false call/media exemption. Help now states this directly. Only Android 17 runtime was available in this environment; older runtime and Samsung locked/Clear all behavior need phone testing. See [[Ariciu - Phone Review 1]].
