---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Chain reminders and shared geofence allocation

Related: [[Ariciu - Search Catalogue]], [[Ariciu - Reminder Lifecycle]], [[Ariciu - Geofencing and Permissions]].

Sources: `feature/delivery/model/ChainLifecycle.kt`, `DeliveryEngine.kt`, `feature/remindereditor/ui/ReminderEditorScreen.kt`, Room saveEdited.

## Product contract

“Near any Kaufland store” means **any of the selected listed stores**, not every branch in Romania/worldwide. Create offers Place/Chain before selection. New or explicitly refreshed snapshots choose at most 40 listed stores within 25 km of the pin. View selected stores opens a real map, with an accessible list of addresses/distances. Existing snapshots remain unchanged until refreshed and distant selections show a warning. Refresh/change the anchor when travelling. No continuous GPS or automatic roaming allocation exists.

The parent Reminder stores chainName, anchor/radius and the selected ChainArea list. A ChainArea carries OSM ID, coordinates, last known position, elapsed time and boot epoch. Every store uses the parent's radius, transition/repeat and intensity. One parent lifecycle, occurrence counter and notification tag represent the task.

## Events as a union of areas

`crossChain` rejects completed reminders, unknown store IDs, same-side duplicate events and non-newer timestamps on the same store/boot. It updates the target store, then treats the chain as INSIDE if **any stored area** is INSIDE, otherwise OUTSIDE. UNKNOWN areas are not counted as inside.

The aggregate ENTER/EXIT is fed to the existing parent lifecycle. Parent time is advanced with max(event time, previous parent time+1) so valid different-store events are not lost solely to a parent timestamp collision; per-store guards remain the real event ordering checks.

Overlapping selected stores therefore count as one continuous visit. Leaving A while still inside B does not rearm arrival. Once/Done apply to the whole task. Every visit rearms after the union is left; another store visit can then produce a new occurrence. Missed exits or stored INSIDE state for a no-longer-monitored area can conservatively hold the union inside; investigate with per-area state rather than inventing missing crossings.

## Monitoring identities and capacity

Single place request ID: reminderId:generation. Chain area request ID: reminderId:generation:areaId. Action ID still addresses reminderId:generation plus the occurrence token.

`allocateAreas` sorts parents by reminder ID and takes one area from each in rounds until capacity 100. Each chain's area order follows its stored nearest-store selection. It does not rank by a live device position or share/deduplicate the same physical store between different reminders; those requests consume separate slots.

No selected capacity → LIMIT_REACHED; some areas fit → PARTIAL; all successful → REGISTERED. Completion/deletion frees slots; reconciliation promotes other areas. The UI warns partial coverage but does not display per-store actual registration counts or marks in the review list.

## Refresh and persistence

Selection is stored with the reminder; dataset updates alone do not mutate it. Changing the pin clears draft chain areas, and the editor repopulates after loading the local catalogue. Saving/refreshing changes the parent generation, journals/adds current areas and removes obsolete IDs. Existing completion/occurrence state is preserved. Strict encoded area state survives Room recreation.

Automatically tested: union overlap, duplicate/out-of-order store events, Once/Done across stores, fair capacity/partial promotion, one notification identity and DB reopening/title edits. Not verified: real trips between stores, stale catalogue correction, missing exit recovery, actual 100-area Play services stress and automatic travel behavior (which is not implemented).


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
