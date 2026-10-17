---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Original notes and decision history

Related: [[Ariciu - Product and Decisions]], [[Ariciu - Current Status]].

The existing vault's Ariciu folder contains three original planning notes. This handoff preserves them in place, without renaming, editing or copying their full contents into Git. Links below resolve after importing this export into that same folder; they are intentional vault-only references in the repository export.

- [[Ariciu - General Idea]] — original context/place task concept, examples and principles.
- [[Ariciu - Notification System]] — early desired Silent/Normal/Urgent/Alarm behavior.
- [[Ariciu - Tech Stack]] — early zero-recurring-cost stack and future ideas.

## Reading old plans against the candidate

| Historical proposal | Actual 1.1-rc1 / current interpretation |
| --- | --- |
| Contextual place tasks and offline reminders | Single-place and selected supermarket-chain reminders; local persistence/lifecycle, with Android/GMS delivery limits. |
| Repeated alerts until done | Once stays triggered until handled; Every visit needs opposite crossings. Only Alarm requests repeating system notification sound, not a reminder polling loop. |
| Timed Snooze for Normal | Normal retains Done/Next time/swipe. Timed ten-minute Snooze is Alarm-only. |
| Urgent impossible to swipe, stronger audio/vibration | Requests ongoing high-importance presentation; modern Android can allow dismissal. No custom stronger tone/pattern configured. |
| Alarm always full-screen/continuous playback | Supported system notification fallback, with requested repeat sound/strong vibration and Stop/Done/Snooze. No full-screen/FGS/exact-alarm workaround. |
| Possible DataStore preference storage | SharedPreferences + StateFlow currently suffice for appearance; no DataStore dependency. |
| Chains as TODO | Romania-first fixed nearest-store snapshots implemented, with catalogue/capacity limitations. |

Original intentions remain valuable product history, but should not override platform restrictions or be read as shipped behavior. Current source and dated owner decisions/evidence take precedence. The next session's priority is real-phone debugging/stabilization, not completing every old brainstorming idea.
