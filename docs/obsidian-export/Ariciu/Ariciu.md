---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: master
version: 1.1-rc4
---

# Ariciu

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Ariciu is a local Android app that reminds you when a place becomes relevant. This knowledge base describes **1.1-rc4, version code 5**, checked against source on 17 October 2026. The current phase is **debugging and stabilization from real-phone use**.

## Start here

- [Releases and Signing](Ariciu%20-%20Releases%20and%20Signing.md) — authoritative master, first GitHub pre-release, verified APK and safe branch cleanup.

- [Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md) — current rc4 behavior, Alarm retirement, migration, verification and retest.

- [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md) — rc3 dispositions, actual rc2 Samsung swipe evidence and retest.
- [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) — dispositions, verification and phone retest checklist.

- [Current Status](Ariciu%20-%20Current%20Status.md) — branch, commits, evidence and immediate priorities.
- [Product and Decisions](Ariciu%20-%20Product%20and%20Decisions.md) — purpose, constraints and owner decisions.
- [Features and Journeys](Ariciu%20-%20Features%20and%20Journeys.md) — what users can actually do.
- [Debugging Playbook](Ariciu%20-%20Debugging%20Playbook.md) — reproduce, explain, repair and prevent regressions.
- [Phone Issue Template](Ariciu%20-%20Phone%20Issue%20Template.md) — capture a useful field report without exposing private data.

## How the app works

- [Architecture](Ariciu%20-%20Architecture.md) and [Code Map](Ariciu%20-%20Code%20Map.md)
- [Data and Migrations](Ariciu%20-%20Data%20and%20Migrations.md) and [Reminder Lifecycle](Ariciu%20-%20Reminder%20Lifecycle.md)
- [Geofencing and Permissions](Ariciu%20-%20Geofencing%20and%20Permissions.md) and [Background Work and Recovery](Ariciu%20-%20Background%20Work%20and%20Recovery.md)
- [Notifications and Alarm](Ariciu%20-%20Notifications%20and%20Alarm.md)
- [Maps and Current Location](Ariciu%20-%20Maps%20and%20Current%20Location.md), [Search Catalogue](Ariciu%20-%20Search%20Catalogue.md), [Chain Reminders](Ariciu%20-%20Chain%20Reminders.md), [Offline Maps](Ariciu%20-%20Offline%20Maps.md)

## Design and engineering

- [Design and Accessibility](Ariciu%20-%20Design%20and%20Accessibility.md)
- [Development and Builds](Ariciu%20-%20Development%20and%20Builds.md) and [Tests and Evidence](Ariciu%20-%20Tests%20and%20Evidence.md)
- [Risks and Technical Debt](Ariciu%20-%20Risks%20and%20Technical%20Debt.md) and [Maintenance and Extension](Ariciu%20-%20Maintenance%20and%20Extension.md)
- [Sources and Licensing](Ariciu%20-%20Sources%20and%20Licensing.md)
- [Historical Notes](Ariciu%20-%20Historical%20Notes.md) and [Documentation Maintenance](Ariciu%20-%20Documentation%20Maintenance.md)

## Evidence vocabulary

| Label | Meaning |
| --- | --- |
| Implemented | Present in inspected source; not a claim that every device scenario works. |
| Automatically tested | A named test and a recorded passing result cover the stated behavior. |
| Emulator verified | An interaction/rendering observation was recorded on an emulator. |
| Physical-phone verified | A dated device observation establishes that specific case. The rc2 Samsung unlocked swipe was inspected through read-only ADB; rc4 initial owner feedback is positive without detailed scenarios; targeted checks remain pending. |
| Not verified | No supporting executed check or device observation is recorded. |
| Planned | A possible future change, not current behavior or automatically authorized scope. |
| Known limitation | A documented implementation/platform boundary, not necessarily a defect. |
| Audit hypothesis | A source-derived risk needing reproduction; not a confirmed field bug. |

The repository's [AGENTS.md](https://github.com/andreicosmin02/Ariciu/blob/master/AGENTS.md), [docs/CODEX_HANDOFF.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/CODEX_HANDOFF.md), [docs/PROJECT_STATUS.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/PROJECT_STATUS.md) and [docs/STABILIZATION.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/STABILIZATION.md) remain the essential handoff if this vault is unavailable. This collection contains project documentation and emulator evidence only.
