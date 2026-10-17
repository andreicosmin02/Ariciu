# Project status

## Current direction — 2026-10-17

**Ariciu 1.1-rc2 / version code 3**, branch **`fix/phone-review-1`**, based on `feat/phone-release-candidate` at `62557c5`. This is the first physical-phone-review stabilization batch, not a new broad feature phase. Nothing merged into master or published. The graphite/slate-blue identity, warm light theme, System/Light/Dark and Create / Reminders / Settings remain.

The original Obsidian review and all eight screenshots in its capture sequence were inspected before edits. [Review dispositions, root causes and retest checklist](PHONE_REVIEW_1.md) cover every item. Physical-phone resolution is **awaiting owner retest**; screenshots of rc1 do not establish rc2 reliability.

## Verified implementation

- Active reminder cards expose Done, including waiting and snoozed tasks. A Room transaction checks occurrence, retains task data, marks completion/inactive, clears notification/snooze state and rotates generation. Existing callback/reconciliation cleans notifications and registrations; stale engine writes cannot undo completion. Completed edits stay completed.
- Active/Completed selection has checkmark/color/text and sticks while scrolling; large text stacks the choices. Card status labels align; alert intensity has its own line.
- Create has a padded search row bounded to 640dp and adjacent labeled 52dp My location icon. Single place / Supermarket chain is offered before search/confirmation. A compact draggable Material 3 sheet expands via drag or a labeled action, with scrollable options and primary action outside it. Coordinates are secondary. Download explanation distinguishes map roads/labels from offline search and geofence coverage.
- Private search matches token prefixes and ranks exact names/name words ahead of address-only matches, then distance/ID. The same original OSM extract adds 231 malls, including AFI Brașov, Cotroceni and Ploiești: 26,065 total, all original 25,834 rows unchanged. Source timestamp remains 2026-10-15T20:21Z, rebuilt 17 October; no remote search or newer-data claim.
- New/explicitly refreshed chain snapshots select up to 40 listed stores **within 25 km** of the pin. Persisted old selections stay intact until refreshed; distant selections show a warning. Real map preview and accessible list show selected store areas. The actual reminder radius remains 100–1000m per store, and the app-wide geofence limit remains 100.
- Editor uses smaller headings/input hierarchy and tighter spacing with sp scaling retained. Save stays fixed. Settings uses consistent surfaces/insets, contained expandable help, clear monitoring retry/offline-map descriptions and app-wide notification settings for generic setup.
- Urgent retains high importance/ongoing request. Android 14+ allows individual unlocked dismissal; swiping is Next time. Copy explains the limitation. No full-screen/FGS/exact-alarm/audio-loop workaround.
- Original adaptive vector fox and separate monochrome icon, consistent with the existing palette. No external image dependency/licence.

## Architecture and preserved boundaries

Kotlin/Compose/Material 3, activity ViewModel/StateFlow/SavedStateHandle, constructor injection, Room, WorkManager, Play services geofencing and MapLibre remain. **Room 4 and exported schemas/additive migrations 1–4 are unchanged.** No permissions, dependency versions, remote services, paid keys, backend, accounts or recurring costs added.

Generation/occurrence guards, notification tags/outbox, journal-before-add/removal retry, both ENTER/EXIT and disabled initial triggers remain. Platform side effects, Room and WorkManager are separate commits; cleanup/delivery can be delayed and recovery is best-effort. No perfectly immediate or absolute exactly-once promise. Preserve the installed app and debug signing identity; never uninstall/clear storage to fix an upgrade.

Offline maps retain explicit approximately 4×4 km/theme/zoom 10–14 packs, maximum three/one active, pause/resume/delete, bounded provider traffic and visible failures. Search remains Romania-first and incomplete. Offline basemap caching does not guarantee disconnected Play services positioning.

## Verification

Current rc2 commands, counts and screenshots: [PHONE_REVIEW_1.md](PHONE_REVIEW_1.md). Final gate: **68 unit / 23 connected emulator tests passed**, lint 0 errors/39 warnings, debug assembly/install/signature verification passed. Do not reuse rc1 results to validate rc2.

Historical rc1 gate on 16 October: 64 unit / 19 Android 17 emulator tests, lint 0 errors/39 warnings, debug assembly/install passed. One actual light pack rendered roads/labels with emulator networking off. These older results are retained as history, not physical-phone validation.

## Next priority

Install rc2 over rc1 with the same signing key and run the [targeted phone retest](PHONE_REVIEW_1.md#phone-retest). Continue field testing of notification audio/lock behavior, real crossings, rearming, reboot/update, OEM battery restrictions, TalkBack and larger font/display settings. No physical phone was modified by this development batch. See [handoff](CODEX_HANDOFF.md), [source audit](STABILIZATION_AUDIT.md) and [phone checklist](PHONE_VERIFICATION.md).
