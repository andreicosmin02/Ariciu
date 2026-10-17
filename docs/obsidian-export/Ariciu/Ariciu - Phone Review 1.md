---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

Related: [[Ariciu - Review 1.1-rc1]] (private original in vault), [[Ariciu - Current Status]], [[Ariciu - Tests and Evidence]], [[Ariciu - Notifications and Alarm]]. Repository mirror: docs/PHONE_REVIEW_1.md. Original review/screenshots are preserved; only project emulator evidence is exported.

# First phone review — implementation and retest

Review received 2026-10-17 for **1.1-rc1**. Implementation candidate: **1.1-rc2 / code 3**, branch `fix/phone-review-1`, based on `feat/phone-release-candidate` at `62557c5`. No merge or publication. Original Obsidian `Ariciu - Review 1.1-rc1.md` was read completely, together with all six unique referenced screenshots and both additional screenshots from the same capture sequence. Originals remain private and unchanged.

## Findings and decisions

The phone captures establish UX friction and show Urgent notifications being swiped. They do not establish geofence timing, repeat accuracy, reboot recovery or audio reliability. Source audit and focused failing search tests separately reproduced substring relevance and unlimited-distance selection. Missing AFI malls were confirmed by comparing the shipped catalogue with the original local OSM extract.

| Review item | Disposition and implemented solution |
| --- | --- |
| 1. Search width / My location | Fixed in implementation: padded, bounded search row, full usable width and adjacent 52dp location icon with a spoken label. |
| 2. `afi` relevance / coverage | Fixed: whole-token/prefix matching; exact names, whole name words, name prefixes, then address matches; distance and stable ID break ties. Same local OSM extract adds 231 malls, including AFI Brașov/Cotroceni/Ploiești. All original 25,834 rows unchanged; total 26,065. No remote search. |
| 3. Around this spot | Fixed: Material 3 draggable standard bottom sheet, compact summary, explicit accessible expand/collapse alternative, scrollable expanded options. Primary action stays outside the sheet. Map remains interactive when exposed. |
| 4. Coordinates / download explanation | Fixed: coordinates require an additional Show coordinates action. Download copy explains roads/labels, theme/detail/area limits and that search already works offline; downloads add no searchable shops or geofences. |
| 5. Editor hierarchy | Fixed: smaller heading, normal body-sized input, tighter cards/groups; sp scaling remains enabled, large text still stacks choices and scrolls, Save stays fixed. |
| 6. Chain map / distant stores | Fixed: dedicated full-screen map of persisted store areas with a list alternative and address/distance summaries. New/explicitly refreshed selections use at most 40 listed stores within 25 km of the pin. Old snapshots remain intact; distant selections are flagged for explicit refresh. Missing stores, empty coverage and the shared 100-area limit are explained. |
| 7. Earlier Place / Chain choice | Fixed: Single place / Supermarket chain choice on Create before searching or confirming. Search moves the anchor; chain areas preview around it. Editor still allows changing the choice. |
| 8. Cards / Done / selection / intensity | Fixed: Done on active cards, including before any crossing and while snoozed. Transaction retains the reminder, clears alert/snooze state and rotates generation; the existing reconciliation removes registrations and notifications. Occurrence check and conditional worker writes reject stale work. Status labels align, alert type has its own line, selected list has a checkmark/color/text and stays visible while scrolling. |
| 9. Urgent dismissal | Limited by Android. Existing high-importance/ongoing request retained; exact delete intent is exercised through the real receiver/worker and must mean Next time. Help explicitly explains Android 14+ swiping. No repost loop or misuse of call/media exemptions. |
| 10. Settings | Fixed: consistent section surfaces/insets, aligned status icons, contained accessible disclosures. Retry explains registration repair without replaying missed visits. Offline maps explains basemap versus catalogue. Generic notification setup now opens app-wide settings; per-intensity links still open channels. |
| 11. Fox icon | Fixed: original native vector fox with graphite, slate-blue ears/face and pale muzzle; separate monochrome silhouette, adaptive safe bounds and small-size rendering review. No generated bitmap, dependency or external asset licence. |

All implementation items await the owner's phone retest. No item is silently deferred. Older Android runtime verification is limited to available emulator images; see evidence below.

## Preserved boundaries

No Room schema/migration/dependency/permission changes. Application ID and debug signing key retained. Existing installed data and selected-store snapshots are preserved. Notification tags, occurrence guards, event queues, registration ledger, outbox, both geofence transitions, disabled initial triggers and 100-area allocation remain. Completion uses a new generation to reject an interrupted worker's older write; platform cleanup still requires successful asynchronous reconciliation. No guarantee of immediate/absolute exactly-once delivery.

The 25 km snapshot bound is a disclosed local-selection default, not a delivery radius, nationwide coverage or automatic roaming. It affects new selections and explicit refresh only. Mall points approximate building geometry, not entrances. OSM source date remains 15 Oct 2026; regeneration on 17 Oct does not make source data newer.

## Platform evidence

Official documentation checked 2026-10-17: [Android 14 ongoing-notification changes](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications) and [Notification.Builder ongoing contract](https://developer.android.com/reference/android/app/Notification.Builder#setOngoing(boolean)). Android 14+ allows individual dismissal while unlocked; documented ongoing protections remain while locked and for Clear all. Reporting phone: **Samsung S26 Ultra, One UI 8.5, Android 16**, supplied by the owner. Manufacturer behavior still needs a retest on that phone. A location reminder is not eligible to pretend to be an exempt call/media/DPC notification.

## Rendering investigation

The initial nested map dialog and experimental TextureView approach exposed a native renderer hang/crash during emulator testing. The final design uses a dedicated full-screen Map/List screen, the established SurfaceView renderer and one active map at a time. Returning preserves the pin/draft. No renderer dependency was changed. Network tiles are not mocked or asserted by UI tests; missing map detail has a loading/failure explanation and List alternative.

## Verification

Executed **2026-10-17**, final source unchanged afterward:

`./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug`

- **68 unit tests and 23 connected tests passed**, zero failures/errors/skips. Android 17/API 37.2 Medium_Phone Play-services emulator, 1080×2400, Swiftshader software GPU. Only that runtime image was installed; Android 8–16 were not runtime-tested in this batch.
- Lint: **0 errors, 39 warnings**. Debug assembly/install and APK signature verification passed. Room schemas/migrations remain unchanged.
- Coverage includes generation-safe card completion and stale worker writes, cleanup of all chain registrations, real Room/receiver/WorkManager notification cancellation and stale actions, Urgent delete-intent Next time, catalogue AFI/mall separation, bounded chain selection, UI card completion and existing migrations/lifecycle flows.
- Actual native emulator screenshots in `docs/design/screenshots/review-1/`: both themes, Create collapsed/expanded, AFI results, secondary coordinates, editor, store map/list, Settings disclosures, 2× Compose font density and original adaptive/monochrome fox at 108/48dp. Store-map screenshot renders real roads/labels and selected area circles; network tiles are visually inspected, not asserted by tests. List/Back and fixed primary actions remain reachable at larger text. These are emulator images, never physical-phone evidence.
- Manual emulator gestures expanded/collapsed the sheet and panned the exposed map. The actual MainActivity was visually checked with Android system font_scale=2.0, then restored to 1.0; see review-manual-drag/review-system-font captures. A retained-capture rerun of six release/location tests passed after the full gate. The standalone coordinates fixture does not apply MainActivity system insets; its status-bar overlap is test-host presentation, not the application window.
- Intermediate iterations exposed substring ranking failures, dynamic sheet-anchor/restored-state test assumptions, asynchronous notification fixture races and native nested/TextureView map failures. They were corrected before the green final gate; the final preview uses one SurfaceView map at a time. A targeted release-flow rerun also passed before that gate.

Physical-device geofencing, actual sound/vibration, One UI behavior, TalkBack traversal and older-Android runtime behavior remain **unverified**. No physical phone was connected or modified. Tests use disposable emulator fixtures; no owner data was cleared. The earlier rc1 gate (64/19) is historical evidence only.


## Documentation synchronization

The existing vault and portable export were synchronized on 17 October: 26 focused notes/27 emulator images; previous-export comparison found no conflicts. Original planning notes, private review and eight phone screenshots were hash-checked unchanged. Repository handoff remains usable independently.

## Phone retest

1. Install `Ariciu-1.1-rc2-debug.apk` over the existing app with the same signing key. Keep all reminders; do not uninstall or clear storage. Confirm version and the new fox icon.
2. Create: search `afi` near Brașov; AFI Brașov should lead relevant results. Test My location, map pan, sheet drag and labeled expand/collapse. Confirm the primary action stays reachable. Repeat Light/Dark, larger font/display size and keyboard open.
3. Choose Supermarket chain before selecting the place. Review Map and List; check stores within 25 km. Try an area with no listed stores. Open an existing 40-store reminder: it must keep its old selection until you explicitly refresh. Refresh should remove distant stores from the draft, then Save changes monitoring identity.
4. Mark a waiting reminder Done on its card, then a triggered and a snoozed Alarm reminder. Each should move to Completed, stop alerting and lose monitoring after synchronization. Other reminders must remain. Editing a completed reminder must keep it completed; stale notification buttons must not revive it.
5. Test Urgent on the Samsung phone: unlocked swipe should mean Next time, without immediate repost. Leave/return for the next visit. Compare locked-screen and Clear all behavior; Android controls dismissibility. Check normal/urgent/alarm sound settings and DND without assuming louder or full-screen behavior.
6. Settings: expand/collapse Sound and alert settings and How reminders work. Check app-wide notification setup and individual channel links. Retry monitoring after restoring permissions; it must not invent a visit. Download one small map area and verify saved roads/labels in that theme with networking off; search coverage must remain the same.
7. Continue background ENTER/EXIT, duplicate/next-visit, reboot/update, permission/Location-off and OEM battery checks from repository `docs/PHONE_VERIFICATION.md`. Record actual outcomes; automated checks are not a substitute.

## Actual emulator captures

![[Attachments/review-1-rc-dark-create.png]]

![[Attachments/review-1-rc-dark-settings.png]]

![[Attachments/review-1-rc-large-alerts.png]]

![[Attachments/review-1-rc-large-reminders.png]]

![[Attachments/review-1-rc-light-create.png]]

![[Attachments/review-1-rc-light-editor.png]]

![[Attachments/review-1-rc-light-settings.png]]

![[Attachments/review-1-review-afi-search.png]]

![[Attachments/review-1-review-coordinates.png]]

![[Attachments/review-1-review-create-expanded.png]]

![[Attachments/review-1-review-fox.png]]

![[Attachments/review-1-review-large-create.png]]

![[Attachments/review-1-review-large-store-list.png]]

![[Attachments/review-1-review-manual-drag.png]]

![[Attachments/review-1-review-settings-expanded.png]]

![[Attachments/review-1-review-settings-help.png]]

![[Attachments/review-1-review-store-list.png]]

![[Attachments/review-1-review-store-map.png]]

![[Attachments/review-1-review-system-font.png]]
