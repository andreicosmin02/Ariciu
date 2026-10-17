---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/urgent-realerts
version: 1.1-rc4
---

# Ariciu

Ariciu is a local Android app that reminds you when a place becomes relevant. This knowledge base describes **1.1-rc4, version code 5**, checked against source on 17 October 2026. The current phase is **debugging and stabilization from real-phone use**.

## Start here

- [[Ariciu - Urgent Re-alerts]] — current rc4 behavior, Alarm retirement, migration, verification and retest.

- [[Ariciu - Phone Review 2]] — rc3 dispositions, actual rc2 Samsung swipe evidence and retest.
- [[Ariciu - Phone Review 1]] — dispositions, verification and phone retest checklist.

- [[Ariciu - Current Status]] — branch, commits, evidence and immediate priorities.
- [[Ariciu - Product and Decisions]] — purpose, constraints and owner decisions.
- [[Ariciu - Features and Journeys]] — what users can actually do.
- [[Ariciu - Debugging Playbook]] — reproduce, explain, repair and prevent regressions.
- [[Ariciu - Phone Issue Template]] — capture a useful field report without exposing private data.

## How the app works

- [[Ariciu - Architecture]] and [[Ariciu - Code Map]]
- [[Ariciu - Data and Migrations]] and [[Ariciu - Reminder Lifecycle]]
- [[Ariciu - Geofencing and Permissions]] and [[Ariciu - Background Work and Recovery]]
- [[Ariciu - Notifications and Alarm]]
- [[Ariciu - Maps and Current Location]], [[Ariciu - Search Catalogue]], [[Ariciu - Chain Reminders]], [[Ariciu - Offline Maps]]

## Design and engineering

- [[Ariciu - Design and Accessibility]]
- [[Ariciu - Development and Builds]] and [[Ariciu - Tests and Evidence]]
- [[Ariciu - Risks and Technical Debt]] and [[Ariciu - Maintenance and Extension]]
- [[Ariciu - Sources and Licensing]]
- [[Ariciu - Historical Notes]] and [[Ariciu - Documentation Maintenance]]

## Evidence vocabulary

| Label | Meaning |
| --- | --- |
| Implemented | Present in inspected source; not a claim that every device scenario works. |
| Automatically tested | A named test and a recorded passing result cover the stated behavior. |
| Emulator verified | An interaction/rendering observation was recorded on an emulator. |
| Physical-phone verified | A dated device observation establishes that specific case. The rc2 Samsung unlocked swipe was inspected through read-only ADB; rc3 post-fix phone retest remains pending. |
| Not verified | No supporting executed check or device observation is recorded. |
| Planned | A possible future change, not current behavior or automatically authorized scope. |
| Known limitation | A documented implementation/platform boundary, not necessarily a defect. |
| Audit hypothesis | A source-derived risk needing reproduction; not a confirmed field bug. |

The repository's `AGENTS.md`, `docs/CODEX_HANDOFF.md`, `docs/PROJECT_STATUS.md` and `docs/STABILIZATION.md` remain the essential handoff if this vault is unavailable. This export contains project documentation only; the personal vault and its original notes are not part of Git.
