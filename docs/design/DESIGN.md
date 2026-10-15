# Ariciu design direction

Feature expansion is paused while the product owner evaluates this redesign. The aim is a calm, readable app that tells people what to do next, without looking like a component catalogue.

## Design principles

- Aim for a premium, quiet identity through hierarchy, typography, spacing, and consistent surfaces. Avoid gradients, playful decoration, visual clutter, and generic demo styling.
- Make the next action explicit. Prefer labeled navigation and visible choices over hidden gestures; keep coordinates and platform diagnostics secondary.
- Design for older or less technical users: plain language, scalable text, large controls, and status conveyed in words as well as color. Usability with these users is a goal still awaiting observation.
- Preserve honest delivery status and existing reminder behavior. Visual polish must not imply that a saved reminder is registered or that Android guarantees immediate alerts.

## Visual system

- **Graphite first:** near-black `#111214`, raised surfaces `#191B1E` / `#1D1F23`, warm white `#F2F0EC`. Dark surfaces are separated by tone and spacing rather than heavy shadows.
- **Slate blue:** `#B4C9EC` in dark mode, `#355B8D` in light mode. Used for the primary action, selected choices, and the area boundary. Amber is reserved for an actionable setup warning; green is not part of the identity.
- **Light mode:** warm paper `#F5F4F0`, near-white surfaces `#FCFBF8`, charcoal text `#202329`. It has its own contrast relationships rather than an inverted dark palette.
- **Typography:** Android’s local sans-serif, with custom sizes, weights, line heights, and restrained tracking. Main body text is 15–17sp, secondary small text is 13sp, and headings are 24–32sp. No downloaded font, font service, decorative lettering, or new dependency.
- **Spacing:** 24dp page edges, 20dp card interiors, 12–24dp group separation, and smaller 4–8dp spacing within a related group. Reading order comes before decoration.
- **App icon:** a native vector place/check mark on graphite replaces the green Android starter artwork. The adaptive/monochrome resource routes are retained.
- **Shape:** 12dp small controls, 18dp choice cards and primary actions, 24dp larger cards, 28dp sheet corners. Elevation is limited to the map guidance/sheet; forms and lists use tonal surfaces.
- **Status:** an explicit text label and short explanation. Color reinforces meaning, never carries it alone. Selected place distances and appearance choices have checkmarks; editor choices have radio indicators and a stronger selected outline.

## Navigation and screen roles

The three primary destinations are **Create**, **Reminders**, and **Settings**, always labeled. Existing internal route names remain MAP, LIST, and DELIVERY to preserve state compatibility. EDITOR is a focused secondary screen with Cancel and a fixed Save action. Switching tabs preserves a named draft; Create resumes it. Map position/radius are retained across tab switches through saveable screen state.

**Create:** a large map, a clear centre marker, and a persistent place-selection sheet. The common 100m/200m/500m distances are directly selectable. More distances reveals the existing 100–1000m slider; Location details reveals coordinates and the map-network note. The primary action is Use this place. There is no place search, address lookup, or current-location shortcut yet. Short screens and larger text use a scrolling layout with a fixed-height map so the controls remain reachable.

**Editor:** a readable reminder field, a human place summary, explicit Arrive/Leave choice cards, and explicit Just once/Every visit choices. Save stays available at the bottom while the content scrolls or the keyboard is open. Larger text switches the side-by-side trigger cards into a vertical group.

**Reminders:** separate Active and Completed views; task title leads each card, followed by arrival/departure, area, repeat choice, and plain-language status. Setup help appears only where relevant. Empty states explain the next action. The notification destination still scrolls to and highlights the correct reminder.

**Settings:** locally persisted System / Light / Dark appearance, followed by the staged delivery checklist. System is the default and follows Android. Wallpaper-based dynamic color is removed. Delivery permission requests, Settings shortcuts, and retries are preserved. Detailed Android/network/battery limitations live under How reminders work. Map credits remain available.

## Dark map, licensing, and cost

The dark map is a bundled adaptation of the existing provider’s Dark style. It uses graphite land/buildings, clearer roads, and lighter labels. Its JSON references the same OpenFreeMap tiles, fonts, and sprites. The light map retains Positron. No provider switch, API key, billing account, credit card, subscription, paid font, or hosted infrastructure was introduced.

The official OpenFreeMap site and terms were checked on 2026-10-15: the public instance is free without registration/API keys or view/request billing. Offline-download limits are 3,000 tiles or 50MB, four parallel requests, and a unique User-Agent; this redesign does not add offline downloads. Availability and future terms are not guaranteed.

The original style is derived from CartoDB Basemaps / Dark Matter by Stamen, Paul Norman, CartoDB, MapTiler and OpenMapTiles contributors. Code is BSD 3-Clause; design is CC BY 4.0 with upstream CC BY 3.0 attribution. OpenFreeMap’s MIT notice and the style’s complete license are bundled beside the JSON in `app/src/main/assets/map/`. Ariciu changes the palette/label contrast and credits the sources. Visible OpenMapTiles/OpenStreetMap map credits and the native attribution control are retained. The decorative SDK logo/imperial scale bar are replaced by the simpler map presentation; the radius is expressed in metres.

## Research translated into decisions

- [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps) and [Compose defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): visibly large targets, meaningful semantics, readable text contrast, and explicit selection. Buttons have at least 48dp targets; the primary action starts at 56dp. Radio-card parents own the action so TalkBack gets one choice rather than duplicate controls.
- [NN/G: usability for older adults](https://www.nngroup.com/articles/usability-for-senior-citizens/): predictable navigation, legible text, and clear affordances. The redesign uses persistent labeled destinations and explicit choices rather than hidden gestures or unexplained switches. This is research-informed design, not a claim of usability testing with older adults.
- [Android navigation bars](https://developer.android.com/develop/ui/compose/components/navigation-bar) and [bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets): use stable primary destinations and keep contextual map controls together. Ariciu’s place sheet is a persistent surface, not a modal that blocks map interaction or requires discovering a drag gesture.
- [Android dark theme](https://developer.android.com/develop/ui/views/theming/darktheme): designed semantic colors, legible foregrounds, correct system-bar icons, and a matching map. Appearance respects the user’s explicit choice.
- [OpenFreeMap quick start](https://openfreemap.org/quick_start/), [terms](https://openfreemap.org/tos/), [style sources](https://github.com/hyperknot/openfreemap-styles/tree/main/styles/dark), and [licenses](https://github.com/hyperknot/openfreemap-styles/blob/main/styles/dark/LICENSE.md): reuse the existing free provider with the required credits and notices.

## Review and accessibility limits

Automated checks cover palette text contrast (at least 4.5:1 for the tested role pairs), theme preference resolution/persistence, honest status presentation, navigation, radius/result behavior, editor choices, draft retention, and a 2× text editor interaction. The delivery/migration/notification regression suite remains in place. Native map rendering is visually checked, not “proved” by a unit test.

Screenshots below are actual Android emulator captures. Delivery permissions were granted on the emulator to review populated/ready states; the sample reminder is demonstration data. They are review artifacts, not screenshot-golden tests. Physical-phone TalkBack, Switch Access, different screen widths, display-size changes, and observed usability with older or less technical people remain necessary. Background-delivery validation remains in [the phone checklist](../PHONE_VERIFICATION.md).

## Screenshots

- [Dark Create](screenshots/dark-create.png)
- [Dark editor](screenshots/dark-editor.png)
- [Dark reminders](screenshots/dark-reminders.png)
- [Dark Settings](screenshots/dark-settings.png)
- [Light Create](screenshots/light-create.png)
- [Light Settings](screenshots/light-settings.png)
- [Larger-text editor](screenshots/large-text-editor.png)

The most useful next design feedback is about the slate-blue accent, map-to-sheet balance, and whether the path from choosing a place to saving a reminder feels clear. Feature expansion stays paused while that feedback is incorporated.

Implementation baseline: `3a2f5a7`. See [the session handoff](../CODEX_HANDOFF.md) for branch, verification, and continuation guidance.
