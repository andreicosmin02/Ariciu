---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Location, geofencing and staged permissions

Related: [[Ariciu - Background Work and Recovery]], [[Ariciu - Maps and Current Location]], [[Ariciu - Debugging Playbook]].

Sources: `feature/delivery/platform/AndroidDelivery.kt`, `DeliveryWork.kt`, `feature/delivery/ui/DeliverySetupScreen.kt`, `app/src/main/AndroidManifest.xml`.

## Capabilities

`Context.deliveryCapabilities()` checks precise foreground access, background access (API 29+), device Location, Play services availability and app-wide notification access. POST_NOTIFICATIONS is checked on API 33+. It separately gathers intensity channels with IMPORTANCE_NONE. Global ready does not mean every selected intensity is allowed or every fence is registered.

Registration requires global readiness and excludes reminders whose channel is blocked. Missing capabilities become BLOCKED with actionable text. Inactive/completed reminders become INACTIVE. Status cards prioritize completion, setup, failure/partial/capacity and then delivery state.

| Permission | Purpose and request flow |
| --- | --- |
| INTERNET | Existing map/provider traffic; not required for local catalogue queries or Room. |
| ACCESS_COARSE_LOCATION + ACCESS_FINE_LOCATION | Requested together from an explicit explanation. Approximate is sufficient for My location; precise is required for reminder monitoring. |
| ACCESS_BACKGROUND_LOCATION | Separate request on API 29; API 30+ opens app settings with Android's localized all-the-time label. |
| POST_NOTIFICATIONS | User-initiated runtime request on API 33+; older versions still require enabled app/channel settings. |
| VIBRATE | Normal manifest permission for notification vibration, not another runtime prompt. |
| RECEIVE_BOOT_COMPLETED | Schedules registration/notification recovery after boot. |

No full-screen, exact-alarm, FGS, background tracking or location-polling permission/service is added. Backup/device-transfer exclusions preserve local data ownership but also mean no automatic restoration after uninstall.

## Platform registrations

`PlayServicesGeofenceBackend` uses the existing `LocationServices.getGeofencingClient`. Each area has a never-expiring circular fence with **both ENTER and EXIT**, even if the reminder wants only one. Both are necessary for visit rearming. Notification responsiveness is requested as 60,000ms; this is not an accuracy/delivery deadline. `setInitialTrigger(0)` avoids alerts simply from registration while already inside.

The geofence PendingIntent targets the non-exported receiver explicitly and is mutable on API 31+ because Play services fills event data. Action/activity intents are immutable and separate. Removing uses request-ID lists.

The engine, not the platform adapter, selects up to 100 actual areas in fair deterministic rounds. Chains can be PARTIAL. IDs are journaled before add; obsolete removal must succeed before their ledger entries are forgotten. Registration calls are bounded by a worker timeout and retries.

## Receiving events

`GeofenceReceiver` parses GeofencingEvent. Errors enqueue `unavailable`. Valid ENTER/EXIT events include request IDs, transition, location elapsed realtime when present (otherwise SystemClock), and boot count. The receiver uses goAsync until WorkManager enqueue completion; it does not run the domain engine inline. Non-exported does not prevent an explicitly authorized Play services PendingIntent from being delivered.

`rememberDeliveryCapabilities` refreshes on resume and observes LocationManager.MODE_CHANGED_ACTION dynamically while UI is alive. There is no manifest-only location-mode receiver or continuous watcher while the UI is gone. Workers recheck capabilities when they run.

## Reliability limits

Android/Play services can delay or miss crossings; Wi-Fi/network assistance, weak signals, device settings, OEM battery restrictions and force stop matter. Reopening repairs what can be synchronized, not crossings that were never observed. No real-phone timing, reception or battery behavior is established at this handoff. Recheck official current documentation before changing these platform boundaries; source links are in [[Ariciu - Sources and Licensing]].
