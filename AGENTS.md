# Arichu

Offline-first Android app (Kotlin, Compose, Material 3, Room, Play Services geofencing, MapLibre) for location-based reminders. Baseline: **1.2-rc1 / code 6 / Room 5** on `master`. Priority: stabilization from physical-phone feedback, no broad feature expansion. **Owner authorized a map-usability batch (Phase 1) on 2026-11-10**: POI markers, map controls, reminders map view, nearest-place label, area budget; branch `feat/map-usability`. Same constraints; no Room change; favourites not authorized. Same date: owner replaced always-on POI markers with map search (categories, results pins, auto-snap, multi-place reminders via chainAreas); see docs/MAP_SEARCH_PLAN.md. Same constraints. **2026-11-11**: owner authorized Europe-wide search via downloadable per-country packs hosted as GitHub release files (docs/SEARCH_DATA_PACKS.md); branch `feat/europe-search-packs`. Branch from `master`; never merge, force-push, delete branches or publish without session authorization.

## Hard constraints
- Zero recurring cost: no backend, Firebase, account, subscription, paid API/key or billing. Verify pricing/licence/limits of any new dependency and record the evidence first.
- Core behavior is local and offline-first; map tiles may need internet.
- Max 100 active geofences. Never claim delivery is immediate, exact or guaranteed; saving a reminder does not mean its geofence is registered.
- Before changing permissions, background work, geofencing, alarms or notification behavior, check current official Android docs and state the platform limits.
- Room schema change needs an explicit migration plus migration test. Never erase user data.
- Keep existing names/packages/routes. Reuse existing code and dependencies before adding any. Do not touch unrelated Gradle versions.
- Tests first for app-owned logic. Handle failures explicitly. Never claim a check passed unless it was run.

## Verify (keep output small)
```bash
./gradlew --console=plain -q :app:testDebugUnitTest --tests '<one.Class>' 2>&1 | tail -n 40
./gradlew --console=plain -q :app:testDebugUnitTest 2>&1 | tail -n 40
./gradlew --console=plain -q :app:assembleDebug 2>&1 | tail -n 40   # finish production changes with this
./gradlew --console=plain -q :app:connectedDebugAndroidTest 2>&1 | tail -n 40   # only if a device/emulator is attached
```
Run one class first, then the module. Show full logs only on failure (or `grep -E 'FAILED|error:|e: '`). Final geofence validation needs a physical device.

## Read only when needed
- Status / pending device checks: docs/PROJECT_STATUS.md (verify against source; may be stale)
- Urgent re-alert policy: docs/URGENT_RE_ALERTS.md · Delivery/geofence architecture: docs/DELIVERY_ARCHITECTURE.md
- Build setup: docs/DEVELOPMENT.md · UI/design rules: docs/design/DESIGN.md · Licensing: docs/LICENSING.md
- Product direction, workflow, communication style: docs/AGENT_GUIDE.md
- Doc index: docs/README.md · Historical reports: docs/archive/ (not current instructions)
- Skills: `$android-milestone` (feature batches), `$android-verification` (before reporting done)

## Do not read casually (denied in .claude/settings.json)
docs/obsidian-export/ (duplicate notes), screenshots/*.png/*.apk, build/, .idea/, app/schemas/ and app/src/main/assets/places/romania.tsv (26k rows). When really needed: `head -n 5 romania.tsv`, `grep -m 20 'term' romania.tsv`, `jq` on one schema file via Bash.

## Style
Short replies, small diffs, report only what changed, what ran and what needs manual device checks.
