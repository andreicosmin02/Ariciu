# Urgent re-alert screenshot evidence

Android 17/API37.2 emulator, rc4, 17 October 2026. Eight actual emulator captures, dummy content only; no personal-phone image or private reminder/log data.

- urgent-pending-dark/light: scheduled time, selected connected tabs, Mark done/Next time and unchanged Normal card.
- urgent-pending-large: 2× Compose text fixture, scrolling/wrapping actions and fixed tab selection.
- legacy-alarm-light: existing Alarm editor, retirement explanation and explicit conversion path.
- three-levels-light/dark: only Silent/Normal/Urgent with explanation before selection. The dark root behind the native dialog uses a Compose font-scale fixture; native dialog density follows the system.
- urgent-editor-large: selected Urgent explanation and reachable Save at enlarged Compose text.
- three-levels-system-large-dark: separately run native-window test with actual Android system font scale 2.0. Content scrolls; selection/Close/Save passed. System font scale restored after the test.

These exercise actual composables in an emulator test harness, not previews or golden-image tests. Lifecycle/migration/real delayed-work assertions are separate. See [the current audit and phone checklist](../../../URGENT_RE_ALERTS.md). Samsung rendering/audio/GPS/TalkBack remain unverified.
