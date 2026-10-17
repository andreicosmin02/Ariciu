# Project status

## Current direction — 2026-10-17

**Ariciu 1.1-rc3 / version code 4**, branch **fix/phone-review-2**, based on the completed first-review branch at `1b2eb13`. All second-review items and limits are in [PHONE_REVIEW_2.md](PHONE_REVIEW_2.md). No master merge or release publication. Source is authoritative; final evidence belongs to the review record.

## Second-review implementation

- Search now has explicit catalogue-loading, idle, searching, current-results and failure states; inline progress, retry, cancellation/revision guards and recreation-safe query. Local ranking/catalogue remain unchanged.
- Active/Completed are horizontal accessible tabs joined directly to the scrolling bordered panel, with counts and fixed selection. Redundant headings/empty spacing removed; notification targeting uses corrected offsets. Mark done is an outlined action without a checkmark; completion lifecycle is unchanged.
- Create has an 8dp selector/map gap; short screens at large text scroll the header above a bounded map, keeping options and the primary action reachable. Settings places its status check immediately before Review and stacks the action group at larger text.
- Dedicated monochrome 24dp fox notification resource; launcher/adaptive icons/channel identities unchanged.
- New single-place ENTER editor drafts optionally check Notify me now if I’m already here, initially off. Save commits first and returns the exact generation. One bounded foreground precise fix may create only occurrence 0’s real engine transition/outbox; failure keeps the reminder saved and explains the result. Freshness/accuracy/lifecycle guards and no automatic replay. Normal registration remains initialTrigger0.

## Physical evidence versus pending retest

The Samsung S26 Ultra / Android 16 / One UI 8.5 was connected for read-only rc2 diagnostics. Its actual Urgent alert had HIGH importance, ongoing flag and a delete intent. After the owner's unlocked swipe, the matching row was WAITING_NEXT_VISIT/INSIDE, occurrence 1, no visible/pending alert, still REGISTERED; no active Ariciu notification or delivery warning remained at the snapshot. This confirms that swipe’s Next time handling, not background/geofence timing or all later visits.

Connection dropped, and the owner explicitly requested emulator-only continuation. The phone was not upgraded, force-stopped, cleared, rebooted, reconfigured or used for instrumentation. New rc3 UI/fox/immediate location behavior, locked/Clear all, extended same-visit observations and actual crossings remain for physical retest. Android 14+ permits individual unlocked ongoing dismissal; Ariciu retains the strongest applicable compliant request.

## Preserved architecture and boundaries

Kotlin/Compose/Material 3, constructor injection, Room 4/additive migrations 1–4, Play-services geofencing, WorkManager and MapLibre remain. No dependency/permission/schema/backend/account/paid API/recurring cost change. Existing reminders, generations/actions, registration journal/outbox, conservative rearming, saved chain snapshots and 100-area limit remain. No full-screen/FGS/exact-alarm workaround, continuous GPS or guaranteed immediate/exactly-once delivery.

Romania catalogue remains 26,065 rows from 15 October source (231 malls added in rc2). New/refreshed chain selections stay within 25km/max 40; old selections stay intact until explicit refresh. Theme-specific small offline basemap packs retain their existing limits and do not add search or guaranteed disconnected positioning. Graphite/slate identity, System/Light/Dark and Create/Reminders/Settings remain.

## Verification and next work

Current executed gate: **82 unit / 35 emulator instrumentation tests passed**, lint **0 errors / 39 warnings**, debug assembly/signature verified. [Executed checks, screenshots, commits and targeted phone retest](PHONE_REVIEW_2.md). Historical rc2 gate: 68 unit/23 connected tests; rc1:64/19. Those results are not reused to validate rc3.

Install the [current candidate](RELEASE_CANDIDATE.md) over rc2 with the same signing key, never uninstall/clear storage. Continue the focused phone checklist, then background ENTER/EXIT, rearming, reboot/update/OEM battery and TalkBack checks. No speculative feature expansion, merge, force-push or release publication.
