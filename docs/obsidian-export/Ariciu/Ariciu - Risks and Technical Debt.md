---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Risks, limitations and technical debt

Related: [[Ariciu - Debugging Playbook]], [[Ariciu - Current Status]]. Essential repository record: `docs/STABILIZATION_AUDIT.md`. Reviewed source on 2026-10-17; **no field bug report has yet been supplied**. The recorded automated suite is green. Items below distinguish established boundaries from hypotheses.

## Established limitations

- Android/GMS/background positioning and WorkManager can be delayed/suppressed. Force-stop recovery requires reopening. Missed crossings cannot be reconstructed; no exact/immediate/exactly-once guarantee.
- Silent/Normal/Urgent/Alarm respect user channels/DND/OS. Urgent requests ongoing presentation but does not configure a stronger custom sound/vibration than Normal. Modern Android may allow dismissing it.
- Alarm is a system notification with requested repeating sound/vibration; opening the panel can end sound. No full-screen activity, continuous app playback, exact alarm or FGS. Snooze is Alarm-only and inexact.
- Search is an incomplete dated Romania towns/shops catalogue. Chain selection is a fixed snapshot of up to 40 listed stores within 25 km of the pin (older snapshots remain until refreshed), not worldwide coverage or automatic roaming. The app-wide limit is 100 areas.
- Offline packs cover limited bounds/zoom/theme, at most three, with foreground/user-driven bounded downloads. Resource MB is not total disk footprint; delete may leave shared/ambient cache. Map downloads do not guarantee offline positioning.
- Completed reminders cannot currently be restarted by editing and have no reopen control. Active cards now have Done. No backup, import/export, scheduled refresh, cloud sync, mandatory account or non-GMS implementation.

## Confirmed copy/maintenance debt in inspected source

- Triggered status now offers card Done and refers to the notification’s available actions. Snooze status can describe ten minutes without calculating remaining time; partial-chain status can take precedence over snooze.
- Generic notification setup opens the Normal channel even when app-wide notification access is blocked. Per-intensity links exist in expanded alert settings. Reproduction should establish the confusing route on the reported Android version before changing it.
- Catalogue date/attribution wording appears in multiple UI/NOTICE/generator locations and must be updated together. Strict chain encoding can fail decoding rather than tolerate malformed stored data; no such corrupt user row is reported.
- There are 39 lint warnings, unused legacy resources and large combined platform/database files. Refactor only when it materially supports a fix; do not redesign working layers or bulk-upgrade dependencies.

## Source-derived hypotheses requiring reproduction

| Area | Why to investigate | Useful reproduction |
| --- | --- | --- |
| Offline creation/lifecycle | A pending Native create callback could start downloading after screen-exit pause; a failed singleton region-list load may not actually reload on reopen. | Delay region creation/listing, leave/background immediately, reopen and inspect status/network. |
| Download caps across resumes | Byte counter initializes from stored resource bytes; tile response count resets on resume and `.pbf` also includes glyphs. In-flight work and shared cache differ from status bytes. | Bounded failure/pause/resume fixture; measure actual requests/bytes, do not bulk-download the provider. |
| Chain union recovery | An INSIDE store later unmonitored under partial allocation can conservatively keep union INSIDE and delay rearm. | Reach capacity after an entry, miss that store's exit, inspect union state and later crossings. |
| Dataset-coordinate refresh | Parent movement detection compares store IDs; a refreshed coordinate for the same ID may interact with incoming per-store states. | Controlled catalogue fixture with same ID/new coordinates; preserve completion and new generation. |
| Quiet versus posting race | Receiver reads/clones a matching platform notification before posting a quiet update; a concurrent newer post could race with that read-notify sequence. | Controlled interleaving of immediate silence/new occurrence; verify newer token remains intact. |
| Separate persistence/platform commits | Stop silences before command persistence; post/cancel/add/remove can finish around a newer edit/delete/crash. Generation guards protect Room but cannot make OS/WorkManager/Room atomic. | Fault-inject at the exact boundary on disposable data; check recovery rather than assume exactly once. |
| Snooze/time/queue recovery | Wall-clock changes, delayed jobs and old-generation wake jobs need device scenarios beyond model tests. | Forward/backward clock, reboot, edit/delete while snoozed and stale wake execution. |

These are **audit hypotheses, not reproduced critical bugs**. The documentation task deliberately leaves production behavior untouched. Prioritize an actual user report over speculative hardening; reproduce before fixing, then add focused regressions.


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
