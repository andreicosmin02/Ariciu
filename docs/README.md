# Ariciu documentation

Current implementation: **1.1-rc4 / code 5 / Room 5**, authoritative branch `master`. Ariciu is in physical-phone testing and stabilization; the [debug-signed pre-release](RELEASE_CANDIDATE.md) is not production-stable. Start with the [project introduction](../README.md).

## Getting started

- [Developer setup, SDK/JDK and build/test commands](DEVELOPMENT.md)
- [Contributing and reporting bugs](../CONTRIBUTING.md)
- [Current implementation/status and pending device checks](PROJECT_STATUS.md)

## Current implementation

| Topic | Start here | Focused explanation |
| --- | --- | --- |
| Architecture and reliability boundaries | [Delivery architecture](DELIVERY_ARCHITECTURE.md) | [Architecture](obsidian-export/Ariciu/Ariciu%20-%20Architecture.md), [code map](obsidian-export/Ariciu/Ariciu%20-%20Code%20Map.md) |
| Reminder data and persistence | [Architecture](DELIVERY_ARCHITECTURE.md) | [Data model and Room migrations](obsidian-export/Ariciu/Ariciu%20-%20Data%20and%20Migrations.md) |
| Geofence registration/recovery | [Architecture](DELIVERY_ARCHITECTURE.md) | [Permissions/geofencing](obsidian-export/Ariciu/Ariciu%20-%20Geofencing%20and%20Permissions.md), [background recovery](obsidian-export/Ariciu/Ariciu%20-%20Background%20Work%20and%20Recovery.md) |
| Notifications and Urgent retries | [Current Urgent policy and rc4 evidence](URGENT_RE_ALERTS.md) | [Lifecycle](obsidian-export/Ariciu/Ariciu%20-%20Reminder%20Lifecycle.md), [notifications](obsidian-export/Ariciu/Ariciu%20-%20Notifications%20and%20Alarm.md) |
| Maps and local search | [Implemented scope/coverage](FEATURE_COMPLETION.md) | [Maps](obsidian-export/Ariciu/Ariciu%20-%20Maps%20and%20Current%20Location.md), [catalogue](obsidian-export/Ariciu/Ariciu%20-%20Search%20Catalogue.md), [chains](obsidian-export/Ariciu/Ariciu%20-%20Chain%20Reminders.md), [offline packs](obsidian-export/Ariciu/Ariciu%20-%20Offline%20Maps.md) |
| UI and accessibility | [Design system and screenshot evidence](design/DESIGN.md) | [Design/accessibility](obsidian-export/Ariciu/Ariciu%20-%20Design%20and%20Accessibility.md) |

## Verification, limitations and releases

- [Build/test commands](DEVELOPMENT.md), [dated test inventories/evidence](obsidian-export/Ariciu/Ariciu%20-%20Tests%20and%20Evidence.md) and [physical-phone checklist](PHONE_VERIFICATION.md)
- [Debugging workflow](STABILIZATION.md) and [known boundaries versus unreproduced hypotheses](STABILIZATION_AUDIT.md)
- [APK installation/signing](RELEASE_CANDIDATE.md), [rc4 publication and completed branch cleanup](GITHUB_RELEASE_RC4.md)
- [Licensing review and recommendation awaiting approval](LICENSING.md)
- [Public-use preparation audit and verification scope](PUBLIC_REPOSITORY_PREPARATION.md)

Future work includes proper Alarm support and production signing with a data-preserving transition. These are plans, not implemented capabilities or automatic authorization for expansion. Current phone feedback is positive; detailed OEM/background/accessibility checks remain open.

## Historical evidence

[First phone review (rc2 fixes)](PHONE_REVIEW_1.md), [second review (rc3 fixes)](PHONE_REVIEW_2.md), and [Urgent implementation batch (rc4)](URGENT_RE_ALERTS.md) retain dated root causes, tests and limitations. Old branch names, exact device commands and version-specific behavior in those reports are historical, not current setup instructions. Use current source/status when they differ.

## Optional knowledge base and automation

The [portable knowledge base](obsidian-export/README.md) contains **29 notes and 54 emulator screenshots**. Its [index](obsidian-export/Ariciu/Ariciu.md), standard links and images are browsable without Obsidian; internal wikilinks and Mermaid diagrams also work when opened as a vault. No personal directory, account, plugin or synchronization is required.

[AGENTS.md](../AGENTS.md) and [Codex handoff](CODEX_HANDOFF.md) support optional agent-assisted work. Ordinary contributors can use CONTRIBUTING and developer setup directly.
