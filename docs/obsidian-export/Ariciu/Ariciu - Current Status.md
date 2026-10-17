---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Current status — first phone review

Current candidate: **1.1-rc2 / code 3**, branch **fix/phone-review-1**, based on feat/phone-release-candidate at 62557c5. No merge or publication. Source is authoritative; related: [[Ariciu - Phone Review 1]], [[Ariciu - Tests and Evidence]], [[Ariciu - Risks and Technical Debt]].

The owner's full rc1 review and all eight accompanying screenshots were inspected before implementation. Reporting phone: Samsung S26 Ultra, One UI 8.5, Android 16. **Post-fix physical-phone retesting is pending.**

Implemented: safe Done on cards; clearer Active/Completed and intensity; token-ranked local search with 231 malls added from the original source; draggable Create sheet and early Place/Chain choice; dedicated full-screen store map/list; new/refreshed snapshots bounded to 25 km; compact scalable editor; clearer Settings/offline explanations; original adaptive/monochrome vector fox. Urgent individual unlocked swiping is permitted by Android 14+ and means Next time.

Room remains version 4, migrations unchanged. Existing data, notification identities and geofence recovery remain; all original 25,834 catalogue rows remain unchanged. No dependency/service/permission/backend/cost added. Older saved chain snapshots stay intact until explicit refresh. Android monitors at most 100 areas across reminders.

## Evidence and next action

See [[Ariciu - Phone Review 1]] for the executed checks, screenshots and focused phone checklist. Repository docs/RELEASE_CANDIDATE.md owns the current APK checksum; rebuilding can change bytes. Install with the same signing key over the existing app; never uninstall or clear the owner's data to test an upgrade.

Prioritize the owner's phone retest, background lifecycle/notification recovery, actual sound and OEM restrictions. No speculative feature expansion, master merge, force-push or release publication is authorized.

Repository handoff remains independent of the vault: AGENTS.md, docs/CODEX_HANDOFF.md, PROJECT_STATUS.md, STABILIZATION.md and PHONE_REVIEW_1.md. The export mirrors focused project notes; original planning notes and the private rc1 review are preserved.

Synchronization completed 17 October with filesystem approval: 26 notes/27 emulator images match the portable export. Original planning/review notes and phone screenshots remain unchanged.
