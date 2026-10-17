---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Data, persistence and migrations

Related: [[Ariciu - Reminder Lifecycle]], [[Ariciu - Chain Reminders]], [[Ariciu - Tests and Evidence]].

Source: `app/src/main/java/ro/andrei/ariciu/feature/reminders/data/ReminderDatabase.kt`. Exported schemas: `app/schemas/ro.andrei.ariciu.feature.reminders.data.ReminderDatabase/{1,2,3,4}.json`.

## Domain contracts

`Reminder`: ID, trimmed title, SelectedLocation, transition, repeat, DeliveryState, registration/error, intensity, optional chainName and ChainArea list. `ReminderDraft` has defaults ENTER / ONCE / NORMAL and a nullable location; `toDraft`/`toReminder` preserve choices. `ReminderRepository` exposes observeReminders, save and delete without Room/vendor types.

Validation requires nonblank title, finite coordinates in geographic bounds and a 100–1000m radius divisible by 100. Chain drafts require a nonblank name, 1–40 unique valid `n`/`w` numeric OSM IDs and finite valid store coordinates. No arbitrary user text belongs in the chain serialization delimiters.

## Room database

Database file: `reminders.db`, current version **4**. Two tables:

| Field group in reminders | Storage and meaning |
| --- | --- |
| id/title | TEXT primary key and user task. |
| latitude/longitude/radiusMeters | REAL/REAL/INTEGER anchor or single place and per-area radius. |
| transition/repeat | Enum names stored as TEXT. |
| generation | TEXT edit/monitoring identity. |
| phase/position/occurrence | Parent lifecycle, last known side and incrementing alert token. |
| notificationVisible/notificationPending | INTEGER booleans: unresolved current notification and outbox work. |
| lastEventElapsedMs/lastEventBoot | Monotonic accepted-crossing ordering across boot epochs. |
| registration/deliveryError | Enum state and nullable actionable error. |
| intensity/quiet/snoozeUntilMs | Alert type, stopped audio state and wall-clock snooze deadline (0 means none). |
| chainName/chainAreas | Nullable name and encoded TEXT store snapshot/state. |

`registered_geofences` has one TEXT primary-key `requestId`. It is a cleanup/journaling ledger, not a separate list of user reminders or a guarantee that Play services still holds the IDs.

Chain state is **not JSON or a Room relation table**. Each area is `id,latitude,longitude,position,elapsedMs,boot`; areas are separated by semicolons. Decoder uses strict split/enum/number parsing. Changing this format requires compatibility/migration work.

## Migration history

| Version change | Additive transformation |
| --- | --- |
| 1→2 | Lifecycle/position/outbox/order/registration/error/generation columns and registration ledger; existing rows get random monitoring generations. |
| 2→3 | intensity defaults NORMAL; quiet defaults 0; snoozeUntilMs defaults 0. |
| 3→4 | Nullable chainName and empty-default chainAreas. |

All migrations are registered in the production builder. No destructive fallback. Version-1 and version-2 fixtures are tested through version 4; direct version-3 upgrade is not a separate named migration test.

## Writes and preservation

`RoomReminderRepository.save` assigns a UUID generation and calls transactional `saveEdited`, then schedules reconciliation. Existing completion, occurrence, notification state, timestamps, quiet and snooze are preserved. Parent position becomes UNKNOWN when the anchor/radius/chain name or ordered store IDs change. When the parent is not moved and chain name is unchanged, matching unchanged store coordinates reuse stored per-store state; otherwise incoming area state is used. This is more precise than assuming all per-store fields are always reset on every edit.

`RoomDeliveryStore.put` calls an UPDATE conditioned on reminder ID and generation. A late worker write cannot resurrect a deleted row or overwrite a newer edit. `withTransaction` provides lifecycle transaction boundaries. The DAO list is title-sorted case-insensitively, then by ID.

Deletion removes the reminder row immediately. The ledger intentionally remains until a later successful platform removal. No user deletion is treated as completion history.

## Other local stores

Appearance: SharedPreferences file `appearance`, key `theme`. Draft/route/edit/save identity: SavedStateHandle, not Room. Map screen selection: saveable Compose state. Offline map resources/regions: MapLibre Native database, separate from Room. WorkManager maintains its own job database. Do not assume one transaction or backup covers all of these.

Cloud backup/device transfer are excluded in manifest/XML. There is no in-app user-data export; uninstalling or clearing storage loses local reminders.


## Phone review update (17 October)

`ReminderDao.complete(id, occurrence)` is transactional: retain current row data, check occurrence, mark completed/inactive, clear alert/snooze state and rotate generation. Older worker writes cannot undo it. Repository invokes the existing reconciliation callback for notification/registration cleanup. Room remains version 4. See [[Ariciu - Phone Review 1]].
