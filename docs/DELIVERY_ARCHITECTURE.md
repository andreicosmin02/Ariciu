# Reminder delivery

## Lifecycle and identity

The existing Reminder table remains the source of truth. Current schema is 4 (see [feature-completion evidence](FEATURE_COMPLETION.md)). Schema 2 added lifecycle, position, occurrence, notification-outbox, registration, and error fields. Migration 1→2 retains every version-1 reminder and assigns a monitoring generation. Exported schemas are versioned; there is no destructive fallback.

- WAITING: eligible for the chosen crossing.
- TRIGGERED: a current occurrence exists. ONCE stays here until Done or Next time; EVERY_VISIT becomes eligible after the opposite crossing.
- WAITING_NEXT_VISIT: Next time/swipe was selected while still on the triggering side. An opposite crossing rearms WAITING.
- COMPLETED: Done disables the reminder and removes its geofence.

Both ENTER and EXIT are monitored for every active reminder. EXIT repeat therefore observes a return before another departure. The most recent position and monotonic event timestamp/boot count reject duplicate and older crossings. A notification occurrence has a persistent incrementing token. Old action tokens cannot dismiss or complete newer occurrences.

Geofence request IDs combine reminder ID and generation. Editing replaces the generation while preserving occurrence/completion state. PendingIntent action URIs also contain generation, occurrence, and action. Room updates from the engine use a generation condition: interrupted platform calls cannot resurrect deleted rows or overwrite a newer edit.

Initial geofence triggers are disabled: registration/reconciliation is not a new visit, even when already inside. Recovery preserves visit eligibility conservatively. Crossings that Android never reports cannot be reconstructed; an opposite crossing may be needed before a later occurrence.

## Registration and background work

`DeliveryEngine` is plain Kotlin behind store, geofence, and notification boundaries. `RoomDeliveryStore` uses Room transactions for lifecycle mutations. The Android runtime serializes delivery operations with one process mutex; Room serializes concurrent UI edits.

A persistent registration ledger records planned request IDs **before** adding them. Reconciliation removes obsolete IDs before adding eligible IDs. Stable IDs replace existing Play services registrations instead of multiplying them. The ledger survives deletion and interrupted platform calls until cleanup succeeds. At most 100 actual areas are selected in deterministic rounds across reminder IDs, sharing capacity with chain store areas. Overflow and partial chain monitoring have distinct statuses and promote when capacity is freed.

Geofence and notification-action receivers persist commands into a unique WorkManager event chain. Lifecycle commands and registration reconciliation have separate queues, so a backoff for failed registration does not block Done/Next time. Registration calls have a 20-second timeout; failed reconciliation receives up to five attempts with exponential backoff starting at 30 seconds, then waits for a user retry, app resume, boot, or package update. This is event-driven work, not location polling or a recurring job.

Boot and package replacement enqueue reconciliation. App startup/resume, settings return, foreground location-setting changes, saving/editing/deletion, actions, and Play services errors also reconcile. Reboot can restore unresolved notifications. Force stop and restrictive OS/OEM policies can suppress this work until the app is reopened.

## Notifications and crash boundaries

Normal notifications use a user-controlled high-importance channel, NotificationCompat, a private lock-screen presentation, immutable action/activity PendingIntents, and a small monochrome icon. The geofence PendingIntent is explicitly targeted and mutable because Play services supplies event extras. Internal receivers are not exported. Tapping a notification opens the reminder list and scrolls to/highlights the matching reminder without discarding an existing editor draft.

Done completes the reminder. Next time and the notification delete intent share the same idempotent action. Each reminder uses one stable Android notification tag; a new visit replaces any unanswered older occurrence. An update/retry of the same occurrence suppresses another alert; a new occurrence can alert again, subject to channel, DND, and OS behavior.

Lifecycle updates first commit a pending-notification outbox flag. The flag is cleared only after posting succeeds. The OS notification service and Room cannot share a transaction: an interrupted post may be retried, using the same tag and occurrence identity. This provides recoverable, idempotent notification identity, not an absolute exactly-once guarantee across all system failures. WorkManager persists a receiver command before finishing `goAsync`; an unrecoverable failure to enqueue is logged rather than falsely marked delivered. Saving to Room and scheduling WorkManager are also separate commits; startup/boot reconciliation repairs an interrupted scheduling window.

## Permissions and platform limits

Setup is user initiated, with explanations and a Not now option. Fine/coarse foreground location are requested together; background access is a separate step (Android 10 runtime request; Android 11+ Settings with the localized all-the-time label). Notifications use POST_NOTIFICATIONS on Android 13+. Device Location, Google Play services availability, app-level notification access, and the selected intensity channel are checked before registration. Missing capabilities mark active reminders blocked and prevent new registrations. Permissions/settings are checked on resume and on every delivery job; platform failures remain visible and retryable.

Android 16 job runtime quotas also affect WorkManager. Android 17 background audio restrictions reinforce using standard notification-channel sound rather than custom playback. No continuous GPS, foreground tracking service, exact alarm, full-screen intent, app audio loop, backend, or network API key is used. Alarm requests repeating audio from the Android notification service. Play services may use network-assisted positioning, so entirely disconnected detection and perfectly immediate/exact boundary delivery are not guaranteed.

## Dependencies, licensing, and cost review (2026-10-15)

- `com.google.android.gms:play-services-location:21.3.0`: Maven POM declares the Android Software Development Kit License; the SDK license grants royalty-free Android development use. Geofencing setup requires the local Play services client and Android permissions, without API-key, Cloud-project, credit-card, billing-account, subscription, or metered-service setup. The applicable limit is 100 monitored geofences per app/device user. This is a proprietary Play services SDK, not an open-source positioning engine. It is distinct from billed Google Maps Platform Geolocation/Places services; those are not introduced.
- `androidx.work:work-runtime-ktx:2.10.1`: Maven POM declares Apache 2.0. Scheduling and persistence run on the device; no external service, key, billing account, subscription, or financial usage quota is involved. Android execution quotas still apply.
- `androidx.room:room-testing:2.8.4`: same Apache-licensed Room family already in use, added only for instrumented schema/migration verification; no hosted service or runtime billing.
- Existing MapLibre/OpenFreeMap dependencies and provider are preserved. No new map API, geocoder, service, or provider was added. No claim is made that any provider’s future terms are guaranteed unchanged.

License metadata was checked from the official Google Maven POMs before adding Play services location and WorkManager. Zero recurring cost is now an explicit permanent AGENTS.md constraint, including review before any future dependency/service addition.

## Official sources reviewed

- [Android geofencing and recovery guidance](https://developer.android.com/develop/sensors-and-location/location/geofencing)
- [GeofencingClient contract](https://developers.google.com/android/reference/com/google/android/gms/location/GeofencingClient)
- [Play services dependency setup](https://developers.google.com/android/guides/setup)
- [Android SDK license](https://developer.android.com/studio/terms)
- [Play services location POM](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-location/21.3.0/play-services-location-21.3.0.pom)
- [WorkManager POM](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/2.10.1/work-runtime-ktx-2.10.1.pom)
- [Background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) and [notification construction](https://developer.android.com/develop/ui/compose/notifications/create-notification)
- [Android 16 all-app behavior](https://developer.android.com/about/versions/16/behavior-changes-all) and [targeted changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 all-app behavior](https://developer.android.com/about/versions/17/behavior-changes-all) and [targeted changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions): location-mode changes are observed dynamically while the UI is active; background recovery does not rely on a manifest-only location-mode broadcast.

## Release-candidate extensions (2026-10-16)

See [feature-completion evidence](FEATURE_COMPLETION.md) for the current intensity, snooze, chain, map, licensing and platform decisions. Actions now include Alarm Stop and Snooze; per-store request IDs append a stable OSM identity to the parent generation. Action IDs continue to address the parent reminder and occurrence. Per-store positions/timestamps and a parent union-of-areas lifecycle persist in Room; Done completes the whole reminder. Unknown/missed crossings remain conservative. Chain snapshots refresh explicitly around the user's chosen pin; there is no background GPS tracking or worldwide store claim.

Snooze deadlines are local wall-clock timestamps backed by persistent inexact WorkManager jobs. Wake processing increments the occurrence token and resets alerting state; old buttons cannot affect the new alert. Reboot/app reconciliation repairs interrupted scheduling. Restored already-delivered alarms are quiet. The action receiver immediately requests silencing of the matching platform notification (generation and occurrence checked) before persisting its command, so Stop does not depend solely on delayed worker execution. Channel/DND/OS behavior still governs audio.

Schema 2→3 and 3→4 use additive migrations with exported schemas. Completed/triggered states, generations and registration ledger entries survive upgrades. Title edits preserve store positions and snooze state. Parent positioning resets when anchor/radius/chain name or ordered selected store IDs change, while completion/occurrence remain. When the parent is not moved and chain name is unchanged, matching unchanged store coordinates preserve stored per-store state; otherwise incoming store state is used. Do not assume every per-store position resets on every edit; inspect `saveEdited` before changing refresh behavior.

## Stabilization reading notes (2026-10-17)

The current priority is [real-phone debugging](STABILIZATION.md), with [known limitations and source hypotheses](STABILIZATION_AUDIT.md) kept distinct. Normal and Urgent share default/user-controlled alert behavior; Urgent adds an ongoing presentation request, not a custom stronger sound/vibration. Timed Snooze is Alarm-only.

Chain request IDs are `reminderId:generation:areaId`; actions address `reminderId:generation` plus occurrence. A changed area's crossing updates the parent union using an adjusted monotonic timestamp so valid same-time events from different stores are not rejected by the parent's ordering guard. UNKNOWN store positions do not count as INSIDE. Partial monitoring/missed exits can conservatively hold known INSIDE state; diagnose before changing this invariant.

Worker queues are unique `delivery-events` and `delivery-reconcile` chains with APPEND_OR_REPLACE. Snooze wake work uses `snooze:<requestId>:<occurrence>` with KEEP and a wall-clock-derived delay. Early wake after a backwards clock change retries; due deadlines are also recovered during processing/reconciliation. No exact scheduling guarantee. Boot requests forced reconciliation and quiet restoration; package replacement forces reconciliation without the boot restoration flag. WorkManager/Room/platform commit windows and immediate-quiet/post races require controlled device investigation, not claims of absolute exactly once.


## First phone review (1.1-rc2, 17 October)

Active cards now support explicit Done before any crossing, after triggering or while snoozed. `ReminderDao.complete` transaction checks the displayed occurrence against the current row, retains task data, marks completion/inactive and clears visible/pending/snooze state through `DeliveryState.completed()`. It assigns a new generation, so an interrupted older worker cannot overwrite completion. Repository schedules the existing reconciliation callback; notification cancellation and removal of all obsolete single/chain registrations follow the normal ledger/retry path. A stale card occurrence cannot complete a newer occurrence. Notification actions retain their generation/token/visible guards. No schema change.

New/explicitly refreshed chain snapshots are limited to 40 listed stores within 25 km of the chosen anchor. This is selection coverage, not the per-store geofence radius. Existing snapshots survive an upgrade until explicitly refreshed. The 100-area allocator and conservative parent union behavior are unchanged. Urgent ongoing/delete-intent behavior is preserved; official Android 14+ dismissibility and runtime evidence are in [the review record](PHONE_REVIEW_1.md).

## Second phone review (rc3)

The new explicit foreground arrival check is separate from registration; initialTrigger0 remains. Save returns its exact generation. ForegroundArrivalCheck checks a bounded precise fix and DeliveryWork.immediateArrival shares the worker mutex; DeliveryEngine.immediateArrival transaction accepts only a fresh/accurate first single ENTER and writes the normal occurrence/outbox. No raw location history/option column is added. Duplicate attempts, old generation, completed/edited/deleted row, chain/EXIT and subsequent occurrences are rejected. Next time/Every visit still require an opposite crossing. Initial checks never replay on reconcile; persisted outbox recovery remains. See PHONE_REVIEW_2.md for limits, actual Samsung swipe evidence and tests.
