---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Offline map packs and provider limits

Related: [[Ariciu - Maps and Current Location]], [[Ariciu - Sources and Licensing]], [[Ariciu - Risks and Technical Debt]].

Sources: `feature/offlinemap/OfflineMaps.kt`, `DownloadBudget.kt`; picker download confirmation and Settings pack rows.

## User-visible contract

Explicit user-initiated approximately 4×4 km packs around a pin, zooms 10–14, pixel ratio 1, for the selected Light or Dark theme. At most three regions, one download at a time. Coordinates and theme form the default pack name. Settings shows progress, resource bytes in decimal MB, complete/paused/error state, Pause/Resume and Delete. No rename/custom-region-size/view-pack navigation is implemented.

Normal flow pauses downloads when the containing screen is disposed or lifecycle reaches ON_STOP, and each resume has a 90-second pause watchdog. Resume continues an incomplete native region; it is not an automatic background download service. Session IDs prevent an old watchdog from cancelling a later resumed session. Callback timing around pending creation remains an audit risk, not a verified universal pause guarantee.

Downloads outside latitude [-80,80] or longitude [-179,179], non-finite positions, unopened storage, full three-pack capacity or another active creation/download are rejected. Initial native region-list failures are surfaced; the singleton does not implement a general reload/retry state machine.

## Why display and download style differ

Display uses bundled asset:// styles. Native pack definitions use `https://tiles.openfreemap.org/styles/dark` or `/styles/positron`; those styles reference the same map resource URLs. Asset-only native download definitions stalled at zero resources in implementation testing; using the matching hosted style fixed the observed case. Do not repeat the asset:// download-definition approach without native evidence.

Changing a bundled style's font/sprite/source URLs can break this sharing even if normal online rendering works. Review both styles and pack behavior together.

## Implementation and safety budgets

`OfflineMaps` is a process singleton. MapLibre.getInstance is called **before** installing the HTTP client; reversing this once caused a reproduced initialization crash, now fixed. OfflineManager persists region definitions/resources; in-memory flows expose UI state.

MapNetwork installs the existing OkHttp 4.12 client integration with maxRequests=4, maxRequestsPerHost=4 and a unique Ariciu User-Agent. Its interceptor reads responses and feeds a synchronized DownloadBudget while a download is active. A resumed session starts with stored resource bytes; the session's tile-response counter starts at zero. `.pbf` requests include glyph responses as well as vector tiles. This is not a complete cumulative unique-tile accounting system.

| Guard | Current value |
| --- | --- |
| HTTP byte budget | IOException above 45,000,000 counted bytes; includes response retries during the active session. |
| HTTP .pbf response budget | IOException above 2,500 responses in that active session. |
| Native status pause | Pause when incomplete region reaches 40,000,000 resource bytes or 2,000 completed tiles. |
| Resume resource-size guard | Refuse an already stored >=40,000,000-byte pack. |
| Time guard | Pause an active session after 90 seconds; explicit Resume. |

The native Mapbox tile-limit callback is handled but is not relied upon to limit OpenFreeMap. In-flight requests, cancellation, cached/shared resources and pause/resume accounting still need integration stress verification. Budget unit tests prove application counters, not every native/network/provider condition.

Recorded OpenFreeMap terms used by this implementation (reviewed 2026-10-16): up to 3,000 tiles **or** 50MB per download, at most four parallel requests, identifying User-Agent; no key/billing account. Recheck before changing limits/provider. No prohibited public OSM raster bulk download endpoint is used.

## Storage and offline boundaries

Native region metadata/resources are separate from Room (observed native file `files/mbgl-offline.db` on the emulator). Per-region MB describes resources, not total app disk use: cache, shared resources and database overhead differ. Phone storage settings exposes the total. Delete releases the region; shared/ambient cached tiles may remain. The controller does not currently expose cache eviction/compaction controls.

A map pack does not cache all countries, arbitrary overview/deep zooms or both themes. The local search catalogue is separately bundled. Saved reminder lifecycle remains local; Play services positioning can still depend on network assistance.

Emulator verified 2026-10-16: one real light region completed with 31 protected vector tiles and 396 protected style/font/sprite resources; roads/labels and pin selection rendered with Wi-Fi/mobile data disabled. This was not a native download golden test, proof for all regions/themes, or a physical-phone positioning test.

![[Attachments/rc-offline-light.png]]


## Phone review update (17 October)

The download explanation now distinguishes roads/labels/basemap from the bundled offline search catalogue. Packs do not add searchable stores or change geofence coverage. Settings explains explicit resume and the monitoring retry action separately. See [[Ariciu - Phone Review 1]].
