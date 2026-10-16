# Codex handoff

Updated 2026-10-16 for the phone release candidate. Read AGENTS.md, PROJECT_STATUS.md, design/DESIGN.md, FEATURE_COMPLETION.md and PHONE_VERIFICATION.md before changing behavior.

## Branch and authorization

Continue on `feat/phone-release-candidate`, created from the complete redesign branch at `de9644e`, not master. Use `git status --short --branch` and `git log -5 --oneline` for current commits. User authorized autonomous feature completion, tests, focused commits and pushes. No merge, force push, branch deletion or release publication is authorized.

Feature expansion is authorized. Preserve graphite/slate blue, warm light theme, locally persisted System/Light/Dark and Create/Reminders/Settings. Preserve local-only core behavior and zero recurring cost. Romania-first offline town/shop catalogue scope was explicitly approved. Ask only for meaningful product/coverage/privacy/architecture decisions or genuine blockers.

The former three staged .idea files were explicitly requested ignored by the owner: .idea/codeStyles/Project.xml, codeStyleConfig.xml and deviceManager.xml remain on disk and are ignored. Their ignore rules and the new priority are committed in `92ba3e9`. Recheck for subsequent user changes before staging anything.

## Implemented boundaries

- Four intensities with preserved Normal channel; channel-specific blocking; generation/occurrence-safe actions; persisted ten-minute inexact Snooze; quiet restoration of already-delivered alarms. Alarm uses Android notification `FLAG_INSISTENT`, not an app playback service. Receiver requests immediate matching-alert silence before scheduling the persisted action.
- No full-screen, exact-alarm, FGS or DND-bypass workaround. The supported fallback is an actionable prominent notification; opening the panel can stop its repeat audio. Ongoing notifications may be dismissible on modern Android. VIBRATE is a normal manifest permission, with no new runtime prompt.
- Foreground current-location button with bounded age/timeout/cancellation and denial/errors. Search stays local. No Nominatim, Overpass, Geocoder, Google Maps Platform or paid API is called.
- ODbL Romania catalogue in assets/places, source timestamp 2026-10-15T20:21:00Z, 25,834 rows; seven chains. NOTICE.md provides source/hash/licence/limitations. tools/build_place_catalog.py regenerates it from a free Geofabrik extract using development-only PyOsmium. Keep dates, attribution and UI catalogue wording synchronized when updating it.
- Chains persist up to 40 nearest selected stores around the pin, union-of-areas visit state and independent duplicate guards. Explicit store review/refresh handles travel/data updates without continuous GPS. One parent reminder/notification identity; Done completes all stores. Fair allocation caps actual registered areas at 100 and exposes partial monitoring. Missed exits remain conservative.
- MapLibre native offline packs are explicitly requested, small, theme-specific and bounded. Native download definitions must use the matching hosted OpenFreeMap style; `asset://` definitions stalled at zero resources in real verification. Display styles stay bundled/custom graphite and share the downloaded tile/font/sprite URLs. Four HTTP requests maximum, unique User-Agent, byte/tile budgets. Resume/pause/delete/resource size and a 90-second pause are explicit. Do not use the native Mapbox-only tile limiter as protection for OpenFreeMap.
- Room schemas 1–4 and additive migrations remain. Do not weaken generation conditions, occurrence tokens, outbox flags, registration journal or transaction/mutex boundaries. Title edits preserve delivery/store/snooze state; area/chain changes reset positioning while preserving completion.

## Important paths

App wiring and existing routes: MainActivity.kt, feature/reminders/ui/AriciuApp.kt and RemindersViewModel.kt. New fields flow through existing draft/SavedStateHandle boundaries; internal MAP/LIST/DELIVERY/EDITOR names are preserved.

Domain/engine/chain logic: feature/reminders/model and feature/delivery/model. Room adapter: feature/reminders/data/ReminderDatabase.kt. Android notifications/work: feature/delivery/platform. Search: feature/places. Native download policy/controller: feature/offlinemap. Picker/editor/Settings remain in their existing feature packages.

## Verification and next work

See PROJECT_STATUS.md for final executed counts and artifact; FEATURE_COMPLETION.md for official provider/Android sources. No failing reproducible critical bug is knowingly left. Emulator tests do not establish physical positioning, sound, vibration, reboot delivery, OEM restrictions or accessibility usability.

Next: install the debug APK over the existing physical-phone build (same signing key), use PHONE_VERIFICATION.md for several weeks, and repair observed issues. Nothing has been merged or published. Build outputs are disposable; regenerate them if missing. After the direct OkHttp catalogue addition, Android Studio may need a normal Gradle sync.

Verified application commits: `4e959b9` (delivery/storage) and `011746c` (map/search/offline/UI). Current debug version: `1.1-rc1`, version code 2.
