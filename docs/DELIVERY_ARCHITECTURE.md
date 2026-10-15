# Reminder delivery

## Lifecycle and identity

The existing Reminder table remains the source of truth. Schema 2 adds lifecycle, position, occurrence, notification-outbox, registration, and error fields. Migration 1→2 retains every version-1 reminder and assigns a monitoring generation. Exported schemas are versioned; there is no destructive fallback.

- WAITING: eligible for the chosen crossing.
- TRIGGERED: a current occurrence exists. ONCE stays here until Done or Next time; EVERY_VISIT becomes eligible after the opposite crossing.
- WAITING_NEXT_VISIT: Next time/swipe was selected while still on the triggering side. An opposite crossing rearms WAITING.
- COMPLETED: Done disables the reminder and removes its geofence.

Both ENTER and EXIT are monitored for every active reminder. EXIT repeat therefore observes a return before another departure. The most recent position and monotonic event timestamp/boot count reject duplicate and older crossings. A notification occurrence has a persistent incrementing token. Old action tokens cannot dismiss or complete newer occurrences.

Geofence request IDs combine reminder ID and generation. Editing replaces the generation while preserving occurrence/completion state. PendingIntent action URIs also contain generation, occurrence, and action. Room updates from the engine use a generation condition: interrupted platform calls cannot resurrect deleted rows or overwrite a newer edit.

Initial geofence triggers are disabled: registration/reconciliation is not a new visit, even when already inside. Recovery preserves visit eligibility conservatively. Crossings that Android never reports cannot be reconstructed; an opposite crossing may be needed before a later occurrence.

## Registration and background work

`DeliveryEngine` is plain Kotlin behind store, geofence, and notification boundaries. `RoomDeliveryStore` uses Room transactions for lifecycle mutations. The Android runtime serializes delivery operations with one process mutex; Room serializes concurrent UI edits.

A persistent registration ledger records planned request IDs **before** adding them. Reconciliation removes obsolete IDs before adding eligible IDs. Stable IDs replace existing Play services registrations instead of multiplying them. The ledger survives deletion and interrupted platform calls until cleanup succeeds. At most 100 active areas are selected in deterministic reminder-ID order; overflow reminders show an actionable limit status and become eligible when capacity is freed.

Geofence and notification-action receivers persist commands into a unique WorkManager event chain. Lifecycle commands and registration reconciliation have separate queues, so a backoff for failed registration does not block Done/Next time. Registration calls have a 20-second timeout; failed reconciliation receives up to five attempts with exponential backoff starting at 30 seconds, then waits for a user retry, app resume, boot, or package update. This is event-driven work, not location polling or a recurring job.

Boot and package replacement enqueue reconciliation. App startup/resume, settings return, foreground location-setting changes, saving/editing/deletion, actions, and Play services errors also reconcile. Reboot can restore unresolved notifications. Force stop and restrictive OS/OEM policies can suppress this work until the app is reopened.

## Notifications and crash boundaries

Normal notifications use a user-controlled high-importance channel, NotificationCompat, a private lock-screen presentation, immutable action/activity PendingIntents, and a small monochrome icon. The geofence PendingIntent is explicitly targeted and mutable because Play services supplies event extras. Internal receivers are not exported. Tapping a notification opens the reminder list and scrolls to/highlights the matching reminder without discarding an existing editor draft.

Done completes the reminder. Next time and the notification delete intent share the same idempotent action. Each reminder uses one stable Android notification tag; a new visit replaces any unanswered older occurrence. An update/retry of the same occurrence suppresses another alert; a new occurrence can alert again, subject to channel, DND, and OS behavior.

Lifecycle updates first commit a pending-notification outbox flag. The flag is cleared only after posting succeeds. The OS notification service and Room cannot share a transaction: an interrupted post may be retried, using the same tag and occurrence identity. This provides recoverable, idempotent notification identity, not an absolute exactly-once guarantee across all system failures. WorkManager persists a receiver command before finishing `goAsync`; an unrecoverable failure to enqueue is logged rather than falsely marked delivered. Saving to Room and scheduling WorkManager are also separate commits; startup/boot reconciliation repairs an interrupted scheduling window.

## Permissions and platform limits

Setup is user initiated, with explanations and a Not now option. Fine/coarse foreground location are requested together; background access is a separate step (Android 10 runtime request; Android 11+ Settings with the localized all-the-time label). Notifications use POST_NOTIFICATIONS on Android 13+. Device Location, Google Play services availability, app-level notification access, and the Normal channel are checked before registration. Missing capabilities mark active reminders blocked and prevent new registrations. Permissions/settings are checked on resume and on every delivery job; platform failures remain visible and retryable.

Android 16 job runtime quotas also affect WorkManager. Android 17 background audio restrictions reinforce using standard notification-channel sound rather than custom playback. No continuous GPS, foreground tracking service, exact alarm, full-screen intent, custom audio loop, backend, or network API key is used. Play services may use network-assisted positioning, so entirely disconnected detection and perfectly immediate/exact boundary delivery are not guaranteed.

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
