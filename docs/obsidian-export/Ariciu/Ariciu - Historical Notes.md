---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Original notes and decision history

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Product and Decisions]], [[Ariciu - Current Status]].

Early product proposals are summarized below against the implementation they informed. No unpublished planning document is needed to navigate this collection. The included review records and Git commits preserve the technical chronology.

| Milestone | Included evidence |
| --- | --- |
| Map-first local reminders | [9e91f09](https://github.com/andreicosmin02/Ariciu/commit/9e91f09), [[Ariciu - Maps and Current Location]] |
| Durable geofence/notification pipeline | [e334adf](https://github.com/andreicosmin02/Ariciu/commit/e334adf), [[Ariciu - Architecture]] |
| rc1 catalogue, chains and offline maps | [62557c5](https://github.com/andreicosmin02/Ariciu/commit/62557c5), [[Ariciu - Features and Journeys]] |
| First and second phone reviews | [[Ariciu - Phone Review 1]], [[Ariciu - Phone Review 2]] |
| rc4 Urgent retries and legacy Alarm policy | [[Ariciu - Urgent Re-alerts]] |
| Master consolidation and first pre-release | [[Ariciu - Releases and Signing]] |

## Reading old plans against the candidate

| Historical proposal | Actual 1.1-rc1 interpretation (historical) |
| --- | --- |
| Contextual place tasks and offline reminders | Single-place and selected supermarket-chain reminders; local persistence/lifecycle, with Android/GMS delivery limits. |
| Repeated alerts until done | Once stays triggered until handled; Every visit needs opposite crossings. Only Alarm requests repeating system notification sound, not a reminder polling loop. |
| Timed Snooze for Normal | Normal retains Done/Next time/swipe. Timed ten-minute Snooze is Alarm-only. |
| Urgent impossible to swipe, stronger audio/vibration | Requests ongoing high-importance presentation; modern Android can allow dismissal. No custom stronger tone/pattern configured. |
| Alarm always full-screen/continuous playback | Supported system notification fallback, with requested repeat sound/strong vibration and Stop/Done/Snooze. No full-screen/FGS/exact-alarm workaround. |
| Possible DataStore preference storage | SharedPreferences + StateFlow currently suffice for appearance; no DataStore dependency. |
| Chains as TODO | Romania-first fixed nearest-store snapshots implemented, with catalogue/capacity limitations. |

Original intentions remain valuable product history, but should not override platform restrictions or be read as shipped behavior. Current source and dated owner decisions/evidence take precedence. Current rc4 differs: Urgent swipes schedule delayed re-alerts until explicit resolution, while Alarm is available only for legacy records. See [[Ariciu - Urgent Re-alerts]]. Development prioritizes real-phone stabilization over old brainstorming ideas.

## Related reading

[Product and Decisions](Ariciu%20-%20Product%20and%20Decisions.md) · [Current Status](Ariciu%20-%20Current%20Status.md) · [Maps and Current Location](Ariciu%20-%20Maps%20and%20Current%20Location.md) · [Architecture](Ariciu%20-%20Architecture.md) · [Features and Journeys](Ariciu%20-%20Features%20and%20Journeys.md) · [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) · [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md) · [Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md) · [Releases and Signing](Ariciu%20-%20Releases%20and%20Signing.md)
