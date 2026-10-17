# Project status

## Current direction

Current phase (2026-10-17): **debugging and stabilization from real physical-phone use of Ariciu 1.1-rc1**. Feature completion was authorized on 2026-10-16 and produced this candidate; do not resume broad feature expansion by default. Work continues on `feat/phone-release-candidate`, created from `feat/considered-ui-redesign` at `de9644e`. Nothing has been merged into master. The graphite/slate-blue identity, warm light theme, System appearance default and Create / Reminders / Settings navigation remain.

Romania-first private/offline town and supermarket catalogue coverage was explicitly approved. No backend, accounts, paid API, billing key, cloud storage or recurring charge has been added.

## Release-candidate functionality

- Local creation/editing/deletion, named drafts and saved-state identity, validation, Active/Completed views and notification destinations remain.
- Four explicit intensities: Silent, Normal, Urgent and Alarm. Normal keeps its channel. Silent defaults to no audio/vibration/pop-up and supports Done/Next time/swipe. Urgent requests ongoing high-importance presentation; modern Android can allow dismissal. It does not configure a stronger custom tone or vibration than Normal; timed Snooze is Alarm-only. Alarm requests repeating **system notification** sound and strong vibration, with immediate silencing request and persisted Stop/Done/ten-minute Snooze actions. Full-screen launch is intentionally replaced with the documented notification fallback; no exact alarm or background audio service workaround is added. Channel/DND/OS settings control alerting.
- Current-location button: user-initiated foreground permission, approximate-location support for map centring, disabled-location/error handling, bounded fix age and timeout. Background precise access remains separate for reminders.
- Search runs entirely locally against 25,834 OSM-derived Romanian town/shop rows, including known store addresses. It is accent-insensitive and ranks nearby results. Not every street/address or store is included; no worldwide search claim. Dataset source timestamp 2026-10-15T20:21:00Z, built 2026-10-16. Attribution, derivative TSV and regeneration script are provided.
- Chain reminders: seven supermarket chains, up to 40 nearest listed stores around the chosen pin, store review and explicit refresh after moving the pin/travelling. Completion and Once apply to the whole reminder; Every visit rearms after leaving the union of store areas. Per-store duplicate guards persist. No continuous GPS or automatic nationwide roaming coverage is claimed.
- Android’s 100-area capacity is shared in deterministic rounds across reminders. Partial chain monitoring and overflow are explicit. Generation-conditional writes, registration ledger, persistent occurrence/outbox identities and recovery remain intact.
- Offline maps: explicit approximately 4×4 km, zoom 10–14 packs for the selected theme; up to three packs, one at a time; progress, resource MB, pause/resume/delete in Settings. Downloads pause on screen exit/background and after 90 seconds. Four concurrent requests, app User-Agent and conservative byte/tile budgets preserve OpenFreeMap limits. Partial packs survive process death and resume explicitly. Display styles/licences are bundled; native download definitions use matching hosted styles. Other areas/themes/detail levels can need internet. Offline maps do not guarantee offline Play services positioning.
- Settings rows are denser at ordinary text sizes and stack at larger text. Alert settings are collapsed until requested. Editor spacing is tighter; advanced chain explanations are contextual, selection dialogs dismiss the keyboard, shared headings have accessibility semantics, and Save remains fixed/reachable.

## Architecture and upgrades

Kotlin/Compose/Material 3, single activity, constructor injection. Reminder/lifecycle/chain/allocation/search/budget logic is plain Kotlin. MapLibre and Play services remain at platform boundaries.

Room is now version 4 with exported schemas 1–4 and additive 1→2→3→4 migrations. Existing reminders default to Normal; occurrence/completion/registration identity is retained. Schema 3 adds quiet/snooze state; schema 4 adds chain identity/store positions. No destructive migration or cloud backup. WorkManager snooze jobs are inexact and recover persisted deadlines. Old actions cannot act on newer occurrences/generations.

OkHttp 4.12.0, already present through MapLibre at runtime, is now a direct compile dependency to enforce HTTP download limits; no unrelated versions were upgraded. It is Apache-licensed and adds no remote service. Detailed pricing/licensing/privacy/platform evidence: [FEATURE_COMPLETION.md](FEATURE_COMPLETION.md), [delivery architecture](DELIVERY_ARCHITECTURE.md).

## Verification (2026-10-16)

- **64 unit tests passed**, zero failures/errors/skips.
- **19 Android 17 emulator instrumentation tests passed**, zero failures/errors/skips. The final creation-flow screenshot review was also rerun separately against the installed candidate.
- `:app:lintDebug` passed with zero errors and 39 advisory warnings; `:app:assembleDebug` passed. No unrelated dependency upgrades were made.
- Installable debug APK: `app/build/outputs/apk/debug/app-debug.apk`, version `1.1-rc1` / code 2, min Android 8/API 26. It was installed successfully on the emulator. The existing debug signing key is retained; no release was published.
- Executed command: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug`. The final full run included chain validation, the vibration permission, normalized catalogue assets and honest channel/offline-theme statuses. Settled review captures were also collected from an installed candidate.
- Added coverage includes Alarm Stop/Snooze/stale tokens, chain union/overlap/Once/completion/capacity, per-intensity blocking, accent/address search, download budgets, draft restoration, version-1 and version-2 migration to schema 4, store recreation and controlled stale-write/edit protection, and the complete private-search → chain → Alarm flow in both themes and at 2× Compose text.
- Real emulator map check: a light pack completed with 31 protected vector tiles and 396 protected style/font/sprite resources. Roads/labels and pin selection rendered with Wi-Fi/mobile data disabled; connectivity was restored. This observed region does not prove all regions or provider responses.
- Review screenshots: [design documentation](design/DESIGN.md). They are actual emulator captures, not golden tests or proof of physical accessibility.

## Remaining boundaries

No physical-phone geofence delivery, reboot/process recovery, audible/vibration behavior, OEM battery behavior, TalkBack/Switch Access or observed older-user usability is claimed verified. Follow the [phone checklist](PHONE_VERIFICATION.md) for several weeks. Android may delay/suppress geofencing and WorkManager; force-stopped apps need reopening; missed crossings cannot be reconstructed. Exactly-once/immediate/full-screen delivery is not guaranteed.

Catalogue data can be missing/outdated; chain monitoring covers selected stores around a pin, with explicit refresh rather than continuous tracking. Full address/worldwide coverage, automatic roaming store selection, full-screen alarm special access and release publication are not claimed. No further product approval is pending for the documented Romania-first candidate and supported fallbacks. Future expansion beyond these boundaries should be agreed with the owner.

Next priority: reproduce issues reported during daily phone use, identify root causes, fix them with focused regressions, and improve confusing UX. Preserve the installed data and working features; use the checklist for targeted retesting. No concrete field bug report or dated physical-phone result has yet been supplied. Do not merge or publish a release without authorization.

Verified application commits: `4e959b9` (delivery/storage) and `011746c` (map/search/offline/UI). Current debug version: `1.1-rc1`, version code 2.

## Documentation audit (2026-10-17)

Read source, configuration/manifest, exported Room schemas, test sources/reports, resources/assets and Git history against the project documentation. At audit start HEAD was `42f77a3`, latest app implementation was `011746c`, and the worktree was clean. This task changed **documentation only**; application checks were not rerun. The 64/19 counts above were confirmed from stored XML reports (Oct 16), lint from stored XML. Controlled persistence tests do not establish live concurrent/process-death/reboot behavior on a phone.

The debug APK was rebuilt/signature-verified after the original release-note snapshot, without source changes. Current local files `app-debug.apk` and `Ariciu-1.1-rc1-debug.apk` match at 74,463,275 bytes; SHA256 `cc8c447b52ea8a9ab5afb10d01a24d0dfeecf97217b0bab1154dcb10a5d0567e`. See [release candidate](RELEASE_CANDIDATE.md) for installation limits. Generated outputs are not committed and may be absent elsewhere.

[Stabilization workflow](STABILIZATION.md) and [source audit/known boundaries](STABILIZATION_AUDIT.md) distinguish established limits/copy debt from unreproduced risks. No new critical bug is claimed merely from source review. [Documentation index](README.md) and [Obsidian export](obsidian-export/README.md) provide a complete linked knowledge base. The existing vault's Ariciu folder was found but is read-only in this environment; its three original notes were preserved. Essential handoff remains in the repository.
