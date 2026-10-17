---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Design and accessibility

Related: [[Ariciu - Features and Journeys]], [[Ariciu - Risks and Technical Debt]]. Canonical repository design record: `docs/design/DESIGN.md`. Sources: `ui/theme/`, `ui/components/`, and feature UI files in [[Ariciu - Code Map]].

## Identity and tokens

Dark-first graphite, warm restrained light mode and slate-blue accent. No green brand, wallpaper-derived dynamic colors, decorative gradients or unnecessary illustration. System appearance is the default; explicit Light/Dark overrides persist locally.

| Role | Dark | Light |
| --- | --- | --- |
| Background | `#111214` | `#F5F4F0` |
| Surface | `#191B1E` | `#FCFBF8` |
| Primary text | `#F2F0EC` | `#202329` |
| Primary slate blue | `#B4C9EC` | `#355B8D` |

Dark container is `#1D1F23`. Amber communicates warnings, not identity. Use semantic theme roles rather than copying raw colors into new screens. Legacy resource purple/teal values are unused by Compose; they do not define the current palette.

Sans-serif typography: headings approximately 24–32sp, shared screen heading 24sp/31sp line height, body 15–17sp, secondary 13sp, labels 12–16sp depending on role. Shapes use 8/12/18/24/28dp radii. Typical page/card/editor spacing is 24/20/12dp; primary controls are at least 56dp high and ordinary touch controls at least 48dp. Verify actual screen context rather than forcing every component to one measurement.

## Screen responsibilities

Create keeps the map central, with clear search/current-location/radius and a deliberate move to the editor. Reminders separates Active and Completed with readable task-first cards. Settings contains delivery readiness, staged setup, appearance, offline areas and expandable alert-channel details. Editor is focused and has no bottom navigation; Save remains fixed with keyboard insets. Advanced chain/intensity choices are explicit dialogs, with contextual limitations rather than a dashboard of technical controls.

User copy uses familiar verbs (Arrive, Leave, Done, Next time), named choices and actionable errors. A saved task, a monitored area and a deliverable alert are different states. Never replace this with a reassuring but false “everything ready” message.

## Scaling and interaction

Implemented: labeled controls, selected semantics and grouped radio choices; shared heading semantics; error/status text beyond color alone; keyboard dismissal before selection dialogs; scrollable forms and fixed Save with imePadding. At font scale >1.3, relevant rows stack and navigation height increases from 80 to at least 104dp. Map layouts at short available height or enlarged text use a 280dp map plus scrollable controls.

Automated evidence includes palette contrast checks, selected Compose semantics and a complete flow at 2× Compose text. This does not verify every color pairing, system display scaling, keyboard traversal, TalkBack, Switch Access, landscape or older-user comprehension. Those remain physical/manual checks.

## Reviewed baseline screenshots

These are copies of actual emulator captures collected on 16 October, not new screenshots from this documentation audit. Original files remain in `docs/design/screenshots/rc/`.

Dark Create:
![[Attachments/rc-dark-create.png]]

Light Create and editor:
![[Attachments/rc-light-create.png]]
![[Attachments/rc-light-editor.png]]

Settings in both themes:
![[Attachments/rc-dark-settings.png]]
![[Attachments/rc-light-settings.png]]

Enlarged alerts and reminders:
![[Attachments/rc-large-alerts.png]]
![[Attachments/rc-large-reminders.png]]

Offline native rendering is in [[Ariciu - Offline Maps]]. Earlier redesign images remain historical in the repository. Compare future fixes against the appropriate RC baseline and capture new evidence with the issue/device/font settings recorded.

## Next UX work

Prioritize reported confusion, clipped/unreachable controls and accessibility barriers. Source review suggests checking Alarm wording that still mentions Next time, snooze status accuracy, partial-chain explanations and app-wide notification-settings navigation. These are documented in [[Ariciu - Risks and Technical Debt]]; do not treat speculative redesign as a reported user problem.


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
