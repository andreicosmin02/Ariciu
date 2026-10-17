---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/urgent-realerts
version: 1.1-rc4
---

# Code map

## Current rc4 update

UrgentRetryWork.kt owns unique delayed-job synchronization/cancellation. DeliveryState.onUrgentDismiss/onUrgentRetryDue and DeliveryEngine.wakeUrgentRetries preserve occurrence and check attempt; Room 5 stores deadlines/attempts. ReminderDao.nextTime/Repository/ViewModel own safe card resolution. See [[Ariciu - Urgent Re-alerts]].


Related: [[Ariciu - Architecture]], [[Ariciu - Tests and Evidence]], [[Ariciu - Debugging Playbook]].

One Gradle module: `:app`. Main package root: `app/src/main/java/ro/andrei/ariciu/`. Paths below are relative to that root. There are 33 production Kotlin files, 14 unit-test classes and 9 instrumentation-test classes at this baseline.

| Package / files | Responsibility and first place to investigate |
| --- | --- |
| `MainActivity.kt` | Activity wiring, ViewModel factory, channels/startup reconciliation, theme collection and notification intents. |
| `feature/reminders/model/Reminder.kt` | Reminder, draft, intensity/transition/repeat choices and validation. |
| `feature/reminders/model/ReminderRepository.kt` | Application-owned persistence interface. |
| `feature/reminders/data/ReminderDatabase.kt` | Room entities/DAO/database/migrations, repository, delivery store and chain encoding. |
| `feature/reminders/ui/RemindersViewModel.kt` | StateFlow, SavedStateHandle, draft/edit/open identity, save/delete jobs and errors. |
| `feature/reminders/ui/AriciuApp.kt` | Routes, scaffold, tabs, draft/deletion confirmations and screen composition. |
| `feature/reminders/ui/RemindersScreen.kt`, `ReminderPresentation.kt` | Active/Completed list, contextual status/copy and notification destination highlighting. |
| `feature/remindereditor/ui/ReminderEditorScreen.kt` | Text/transition/repeat/intensity/chain controls, catalogue selection and fixed Save. |
| `feature/delivery/model/ReminderLifecycle.kt` | Plain lifecycle and occurrence/action guards. |
| `feature/delivery/model/ChainLifecycle.kt` | Independent store state and parent union crossings. |
| `feature/delivery/model/DeliveryEngine.kt` | Event/action processing, allocation/reconciliation, outbox and platform contracts. |
| `feature/delivery/platform/AndroidDelivery.kt` | Play services backend, notification channels/post/cancel/quiet, capabilities and runtime construction. |
| `feature/delivery/platform/DeliveryWork.kt` | WorkManager queues/worker, event/action/recovery receivers and retries. |
| `feature/delivery/ui/DeliverySetupScreen.kt` | Settings, staged permission help, channel links, appearance and offline packs. |
| `feature/locationpicker/model/SelectedCoordinates.kt`, `SelectedLocation.kt` | Plain geographic values. |
| `feature/locationpicker/geometry/RadiusGeometry.kt` | Spherical radius boundary generation. |
| `feature/locationpicker/map/RadiusMapGeometry.kt` | SpatialK/MapLibre geometry adapter. |
| `feature/locationpicker/ui/LocationPickerScreen.kt` | Map camera/pin, search, radius controls and map/offline presentation. |
| `feature/locationpicker/ui/CurrentPosition.kt` | Bounded one-shot Fused Location request and errors. |
| `feature/locationpicker/ui/RadiusSelection.kt`, `SelectedCoordinatesFormatter.kt` | Radius choices/validation and locale-stable coordinate copy. |
| `feature/places/PlaceCatalog.kt`, `LocalPlaces.kt` | TSV parsing/normalization/ranking/chains and cached IO loading. |
| `feature/offlinemap/DownloadBudget.kt`, `OfflineMaps.kt` | Download policy, HTTP configuration and Native offline lifecycle/status. |
| `ui/components/AriciuComponents.kt` | Shared labeled controls, selection cards, headings and visual conventions. |
| `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` | Graphite/light tokens, typography, shapes and theme selection. |
| `ui/theme/ThemePreference.kt`, `ThemePreferences.kt` | Pure preference choice and SharedPreferences/StateFlow adapter. |

## Non-Kotlin assets and configuration

- `app/src/main/AndroidManifest.xml`: permissions, single activity, internal receivers and backup policy.
- `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`: cloud/transfer exclusions.
- `app/src/main/res/drawable/` and `mipmap-*`: launcher/notification icons. Legacy XML colors are not the Compose brand tokens.
- `app/src/main/assets/map/`: graphite/light style JSON, style licences and attribution.
- `app/src/main/assets/places/`: Romania TSV and provenance/licensing NOTICE.
- `app/schemas/ro.andrei.ariciu.feature.reminders.data.ReminderDatabase/`: schemas 1–4.
- `tools/build_place_catalog.py`: development-only Romania dataset generator.
- `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/gradle-daemon-jvm.properties`: build/dependency/toolchain truth.
- `.gitignore`: generated outputs, local settings, signing/sensitive files and requested IDE file exclusions. Do not add local SDK configuration or signing keys to documentation.

Tests mirror these packages in `app/src/test/java/` and `app/src/androidTest/java/`; release-flow instrumentation also lives in `feature/release/`. Generated KSP classes, build outputs and native caches are not source to edit. See [[Ariciu - Maintenance and Extension]] for extension boundaries.


## Historical first phone review (rc2)

`feature/places/StorePreviewScreen.kt` renders persisted chain areas on MapLibre with a List alternative. `ReminderRepository.complete`, `ReminderDao.complete` and `RemindersViewModel.complete` own card completion; shared `ExpandableSection` contains expanded help. See [[Ariciu - Phone Review 1]].

## Historical second phone review — rc3

PlaceSearchController.kt and PlaceSearchDialog.kt own current-query states. ForegroundArrivalCheck.kt bounds/cancels one foreground fix; ImmediateArrival.kt owns pure evidence/rules/results; DeliveryEngine.immediateArrival and DeliveryWork.runtimeMutex/dispatch persist the normal occurrence/outbox. ReminderRepository.save returns the committed generation. Draft notifyOnSave/newSave marker are SavedState-only. [[Ariciu - Phone Review 2]] has mechanism/tests/limits.
