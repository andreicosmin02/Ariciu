# Project status

## Current direction — 2026-10-17

**Ariciu 1.1-rc4 / version code 5 / Room 5**, authoritative branch **master**. Owner-authorized normal merge [PR #1](https://github.com/andreicosmin02/Ariciu/pull/1), commit `c8c47ce4642ba7f70ee0d61355bb5175e2334cc0`, has exactly the verified rc4 tree from `525b609`. Tag **v1.1-rc4** identifies that commit; the [GitHub pre-release](https://github.com/andreicosmin02/Ariciu/releases/tag/v1.1-rc4) includes the verified, existing debug-signed APK. Repository visibility is managed separately; this is an early testing build, not production-stable or Google Play distribution. [Publication audit, verification and completed branch cleanup](GITHUB_RELEASE_RC4.md). Current behavior remains in [URGENT_RE_ALERTS.md](URGENT_RE_ALERTS.md); [PHONE_REVIEW_2.md](PHONE_REVIEW_2.md) is historical rc3 evidence.

## Current owner-directed stabilization

Silent, Normal and Urgent are the three available levels. An Urgent swipe retains its unresolved occurrence and schedules one delayed re-alert after about five minutes. Explicit Done/Next time resolves it, including from the card while waiting. Separate persisted deadline/attempt fields, generation/occurrence guards and unique one-time WorkManager jobs protect against duplicates and stale retries. Unresolved Urgent holds across Every visit/chain crossings. No periodic job, exact alarm, foreground service, continuous sound or GPS polling.

Alarm is hidden from new choices; legacy ALARM data/actions/snooze remain supported. Existing Alarm edits keep their intensity until explicit conversion; a current snooze retains its deadline when converted to Urgent. Room **5**, additive 4→5 migration/export, keeps every reminder and earlier migration history. No permission/dependency/channel identity or recurring-cost change.

## Preserved rc3 implementation

- Search now has explicit catalogue-loading, idle, searching, current-results and failure states; inline progress, retry, cancellation/revision guards and recreation-safe query. Local ranking/catalogue remain unchanged.
- Active/Completed are horizontal accessible tabs joined directly to the scrolling bordered panel, with counts and fixed selection. Redundant headings/empty spacing removed; notification targeting uses corrected offsets. Mark done is an outlined action without a checkmark; completion lifecycle is unchanged.
- Create has an 8dp selector/map gap; short screens at large text scroll the header above a bounded map, keeping options and the primary action reachable. Settings places its status check immediately before Review and stacks the action group at larger text.
- Dedicated monochrome 24dp fox notification resource; launcher/adaptive icons/channel identities unchanged.
- New single-place ENTER editor drafts optionally check Notify me now if I’m already here, initially off. Save commits first and returns the exact generation. One bounded foreground precise fix may create only occurrence 0’s real engine transition/outbox; failure keeps the reminder saved and explains the result. Freshness/accuracy/lifecycle guards and no automatic replay. Normal registration remains initialTrigger0.

## Physical evidence versus pending retest

The owner reports positive initial rc4 physical-phone feedback. No detailed scenario/results were supplied, so the targeted Urgent/legacy Alarm/GPS/OEM/TalkBack checklist remains open. Consolidation verification used only the deliberately selected emulator; no daily-phone settings/data/process changes occurred.

The Samsung S26 Ultra / Android 16 / One UI 8.5 was connected for read-only rc2 diagnostics. Its actual Urgent alert had HIGH importance, ongoing flag and a delete intent. After the owner's unlocked swipe, the matching row was WAITING_NEXT_VISIT/INSIDE, occurrence 1, no visible/pending alert, still REGISTERED; no active Ariciu notification or delivery warning remained at the snapshot. This confirms that swipe’s Next time handling, not background/geofence timing or all later visits.

Connection dropped, and the owner explicitly requested emulator-only continuation. In that agent-led session, the phone was not upgraded, force-stopped, cleared, rebooted, reconfigured or used for instrumentation. New rc3 UI/fox/immediate location behavior, locked/Clear all, extended same-visit observations and actual crossings remain for physical retest. Android 14+ permits individual unlocked ongoing dismissal; Ariciu retains the strongest applicable compliant request.

## Preserved architecture and boundaries

Kotlin/Compose/Material 3, constructor injection, Room 5/additive migrations 1–5, Play-services geofencing, WorkManager and MapLibre remain. No dependency/permission/backend/account/paid API/recurring cost change; schema 5 is an additive migration. Existing reminders, generations/actions, registration journal/outbox, conservative rearming, saved chain snapshots and 100-area limit remain. No full-screen/FGS/exact-alarm workaround, continuous GPS or guaranteed immediate/exactly-once delivery.

Romania catalogue remains 26,065 rows from 15 October source (231 malls added in rc2). New/refreshed chain selections stay within 25km/max 40; old selections stay intact until explicit refresh. Theme-specific small offline basemap packs retain their existing limits and do not add search or guaranteed disconnected positioning. Graphite/slate identity, System/Light/Dark and Create/Reminders/Settings remain.

## Verification and next work

Current gate: **97 unit / 44 emulator tests**, lint **0 errors / 39 warnings**, debug assembly/install/signature passed. An extra native system-font 2.0 UI test passed; actual emulator reboot/update/process-death/permission-denial recovery was observed for the shipping five-minute retry. [Executed checks, screenshots and phone retest](URGENT_RE_ALERTS.md). Historical rc3 gate: 82 unit/35 connected tests; rc2: 68/23; rc1: 64/19. Those results are not reused to validate rc4.

Install the [current candidate](RELEASE_CANDIDATE.md) over the existing app with the same signing key, never uninstall/clear storage. Prioritize the new Urgent swipe/re-alert/resolve checklist and legacy Alarm conversion, then real crossings, idle/reboot/OEM behavior and TalkBack. No speculative expansion. The authorized rc4 merge/pre-release is complete; future merges/publications need session authorization. The eight fully merged development branches were safely deleted after explicit approval; all history, v1.1-rc4 and its APK remain. No force-push or history rewrite. Plan genuine Alarm support and production signing/data-preserving transition separately.

## Repository preparation

Merged [PR #3](https://github.com/andreicosmin02/Ariciu/pull/3), commit `cfed6a1a4ca9036c11074c8866753444f6d2515f`, provides [developer setup](DEVELOPMENT.md), [contributor guidance](../CONTRIBUTING.md), a portable optional [knowledge base](obsidian-export/README.md), and portable licensing documentation. The owner has now approved [Apache-2.0 for Ariciu-owned material](LICENSING.md); a new licensing PR adds LICENSE/NOTICE and version-matched third-party companion texts. Source, migrations, signing and published artifacts remain unchanged. Merge and visibility still need explicit authorization; see the [final readiness audit](PUBLIC_READINESS_AUDIT.md).
