---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Product and decisions

Related: [[Ariciu - Features and Journeys]], [[Ariciu - Historical Notes]], [[Ariciu - Sources and Licensing]].

## Problem and vision

Tasks such as buying groceries or collecting a prescription are often tied to a place rather than a time. A reminder combines text, a geographic point or selected chain stores, a radius, arrival/departure, repetition and notification intensity. Android observes the areas; Ariciu keeps the reminder and its lifecycle locally.

The product should feel calm, polished and predictable, including for older and less technical people. Reliability, understandable status and accessible controls matter more than adding capabilities without a demonstrated need. Offline-first describes local ownership and processing; it does **not** promise entirely disconnected positioning or maps everywhere.

## Decisions and chronology

| Decision | Current interpretation |
| --- | --- |
| Zero recurring financial cost | Permanent hard constraint, including dependency/provider review before adoption. |
| Local-first Android | No backend, Firebase, subscription, mandatory account or cloud storage. No cloud backup/device-transfer of app data. |
| Google Play services initially | Supported positioning/geofencing boundary; no alternative backend for devices without it. |
| Graphite identity | Near-black/charcoal dark theme, slate-blue accent, warm light theme; no green brand, wallpaper palette or gradients. |
| Primary navigation | Create / Reminders / Settings; focused editor is secondary. |
| Defaults | ENTER, ONCE, Normal, 200m, System appearance. |
| Initial visual pause | Superseded by feature-completion authorization on 2026-10-16. |
| Romania-first catalogue | Explicitly approved on 2026-10-16, with incomplete town/shop-address coverage and selected supermarkets. |
| Alarm fallback | System notification audio/actions; no automatic full-screen activity, exact-alarm permission or background playback workaround. |
| 2026-10-17 priority | Reproduce phone-reported bugs, find root causes, fix UX/reliability and add meaningful regression coverage. |

## Scope and compromises

Implemented: all four intensity choices, current map positioning, private local search, limited theme-specific offline map packs and selected-store chain reminders. See the feature note for their boundaries rather than reading this list as a guarantee.

Not implemented: complete/worldwide address search, automatic roaming chain coverage, user-importable store catalogues, editable snooze duration, full-screen alarm UI, custom continuous playback, completed-reminder restart, in-app Snooze controls, data export/backup and a non-Play-services positioning engine. These are not assumed next tasks.

Creating a separate reminder is currently the restart path for a completed task. Catalogue updates ship with app updates. The phone is the sole copy of user reminders; uninstall/clear-data is destructive. External maps disclose requests to their provider/CDN even though text/search queries stay local.

## Owner/engineer responsibilities

The owner decides material UX/product trade-offs, coverage and privacy/cost changes. The engineer owns routine implementation, diagnosis, verification and reviewable commits. Preserve working semantics and data; no merge into master, force-push, branch deletion or release publication without authorization. New functionality should support a reported problem or a separately agreed objective during stabilization.


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
