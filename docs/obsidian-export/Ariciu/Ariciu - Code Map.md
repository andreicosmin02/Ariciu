---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/urgent-realerts
version: 1.1-rc4
---

# Code map

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

## Current rc4 update

UrgentRetryWork.kt owns unique delayed-job synchronization/cancellation. DeliveryState.onUrgentDismiss/onUrgentRetryDue and DeliveryEngine.wakeUrgentRetries preserve occurrence and check attempt; Room 5 stores deadlines/attempts. ReminderDao.nextTime/Repository/ViewModel own safe card resolution. See [[Ariciu - Urgent Re-alerts]].


Related: [[Ariciu - Architecture]], [[Ariciu - Tests and Evidence]], [[Ariciu - Debugging Playbook]].

One Gradle module: `:app`. Main package root: `app/src/main/java/ro/andrei/ariciu/`. Paths below are relative to that root. Use the current source and test directories for the file inventory; historical test counts are dated in the evidence note.

| Package / files | Responsibility and first place to investigate |
| --- | --- |
| [MainActivity.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/MainActivity.kt) | Activity wiring, ViewModel factory, channels/startup reconciliation, theme collection and notification intents. |
| [feature/reminders/model/Reminder.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/model/Reminder.kt) | Reminder, draft, intensity/transition/repeat choices and validation. |
| [feature/reminders/model/ReminderRepository.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/model/ReminderRepository.kt) | Application-owned persistence interface. |
| [feature/reminders/data/ReminderDatabase.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/data/ReminderDatabase.kt) | Room entities/DAO/database/migrations, repository, delivery store and chain encoding. |
| [feature/reminders/ui/RemindersViewModel.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/RemindersViewModel.kt) | StateFlow, SavedStateHandle, draft/edit/open identity, save/delete jobs and errors. |
| [feature/reminders/ui/AriciuApp.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/AriciuApp.kt) | Routes, scaffold, tabs, draft/deletion confirmations and screen composition. |
| [feature/reminders/ui/RemindersScreen.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/RemindersScreen.kt), [ReminderPresentation.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/reminders/ui/ReminderPresentation.kt) | Active/Completed list, contextual status/copy and notification destination highlighting. |
| [feature/remindereditor/ui/ReminderEditorScreen.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/remindereditor/ui/ReminderEditorScreen.kt) | Text/transition/repeat/intensity/chain controls, catalogue selection and fixed Save. |
| [feature/delivery/model/ReminderLifecycle.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/model/ReminderLifecycle.kt) | Plain lifecycle and occurrence/action guards. |
| [feature/delivery/model/ChainLifecycle.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/model/ChainLifecycle.kt) | Independent store state and parent union crossings. |
| [feature/delivery/model/DeliveryEngine.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/model/DeliveryEngine.kt) | Event/action processing, allocation/reconciliation, outbox and platform contracts. |
| [feature/delivery/platform/AndroidDelivery.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/platform/AndroidDelivery.kt) | Play services backend, notification channels/post/cancel/quiet, capabilities and runtime construction. |
| [feature/delivery/platform/DeliveryWork.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/platform/DeliveryWork.kt) | WorkManager queues/worker, event/action/recovery receivers and retries. |
| [feature/delivery/ui/DeliverySetupScreen.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/delivery/ui/DeliverySetupScreen.kt) | Settings, staged permission help, channel links, appearance and offline packs. |
| [feature/locationpicker/model/SelectedCoordinates.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/model/SelectedCoordinates.kt), [SelectedLocation.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/model/SelectedLocation.kt) | Plain geographic values. |
| [feature/locationpicker/geometry/RadiusGeometry.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/geometry/RadiusGeometry.kt) | Spherical radius boundary generation. |
| [feature/locationpicker/map/RadiusMapGeometry.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/map/RadiusMapGeometry.kt) | SpatialK/MapLibre geometry adapter. |
| [feature/locationpicker/ui/LocationPickerScreen.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/LocationPickerScreen.kt) | Map camera/pin, search, radius controls and map/offline presentation. |
| [feature/locationpicker/ui/CurrentPosition.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/CurrentPosition.kt) | Bounded one-shot Fused Location request and errors. |
| [feature/locationpicker/ui/RadiusSelection.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/RadiusSelection.kt), [SelectedCoordinatesFormatter.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/locationpicker/ui/SelectedCoordinatesFormatter.kt) | Radius choices/validation and locale-stable coordinate copy. |
| [feature/places/PlaceCatalog.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/places/PlaceCatalog.kt), [LocalPlaces.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/places/LocalPlaces.kt) | TSV parsing/normalization/ranking/chains and cached IO loading. |
| [feature/offlinemap/DownloadBudget.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/offlinemap/DownloadBudget.kt), [OfflineMaps.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/offlinemap/OfflineMaps.kt) | Download policy, HTTP configuration and Native offline lifecycle/status. |
| [ui/components/AriciuComponents.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/components/AriciuComponents.kt) | Shared labeled controls, selection cards, headings and visual conventions. |
| [ui/theme/Color.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/theme/Color.kt), [Type.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/theme/Type.kt), [Theme.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/theme/Theme.kt) | Graphite/light tokens, typography, shapes and theme selection. |
| [ui/theme/ThemePreference.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/theme/ThemePreference.kt), [ThemePreferences.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/ui/theme/ThemePreferences.kt) | Pure preference choice and SharedPreferences/StateFlow adapter. |

## Non-Kotlin assets and configuration

- [app/src/main/AndroidManifest.xml](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/AndroidManifest.xml): permissions, single activity, internal receivers and backup policy.
- [app/src/main/res/xml/backup_rules.xml](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/res/xml/backup_rules.xml), `data_extraction_rules.xml`: cloud/transfer exclusions.
- `app/src/main/res/drawable/` and `mipmap-*`: launcher/notification icons. Legacy XML colors are not the Compose brand tokens.
- `app/src/main/assets/map/`: graphite/light style JSON, style licences and attribution.
- `app/src/main/assets/places/`: Romania TSV and provenance/licensing NOTICE.
- `app/schemas/ro.andrei.ariciu.feature.reminders.data.ReminderDatabase/`: schemas 1–5.
- [tools/build_place_catalog.py](https://github.com/andreicosmin02/Ariciu/blob/master/tools/build_place_catalog.py): development-only Romania dataset generator.
- [app/build.gradle.kts](https://github.com/andreicosmin02/Ariciu/blob/master/app/build.gradle.kts), [gradle/libs.versions.toml](https://github.com/andreicosmin02/Ariciu/blob/master/gradle/libs.versions.toml), [gradle/wrapper/gradle-wrapper.properties](https://github.com/andreicosmin02/Ariciu/blob/master/gradle/wrapper/gradle-wrapper.properties), [gradle/gradle-daemon-jvm.properties](https://github.com/andreicosmin02/Ariciu/blob/master/gradle/gradle-daemon-jvm.properties): build/dependency/toolchain truth.
- [.gitignore](https://github.com/andreicosmin02/Ariciu/blob/master/.gitignore): generated outputs, local settings, signing/sensitive files and requested IDE file exclusions. Do not add local SDK configuration or signing keys to documentation.

Tests mirror these packages in `app/src/test/java/` and `app/src/androidTest/java/`; release-flow instrumentation also lives in `feature/release/`. Generated KSP classes, build outputs and native caches are not source to edit. See [[Ariciu - Maintenance and Extension]] for extension boundaries.


## Historical first phone review (rc2)

[feature/places/StorePreviewScreen.kt](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/java/ro/andrei/ariciu/feature/places/StorePreviewScreen.kt) renders persisted chain areas on MapLibre with a List alternative. `ReminderRepository.complete`, `ReminderDao.complete` and `RemindersViewModel.complete` own card completion; shared `ExpandableSection` contains expanded help. See [[Ariciu - Phone Review 1]].

## Historical second phone review — rc3

PlaceSearchController.kt and PlaceSearchDialog.kt own current-query states. ForegroundArrivalCheck.kt bounds/cancels one foreground fix; ImmediateArrival.kt owns pure evidence/rules/results; DeliveryEngine.immediateArrival and DeliveryWork.runtimeMutex/dispatch persist the normal occurrence/outbox. ReminderRepository.save returns the committed generation. Draft notifyOnSave/newSave marker are SavedState-only. [[Ariciu - Phone Review 2]] has mechanism/tests/limits.

## Related reading

[Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md) · [Architecture](Ariciu%20-%20Architecture.md) · [Tests and Evidence](Ariciu%20-%20Tests%20and%20Evidence.md) · [Debugging Playbook](Ariciu%20-%20Debugging%20Playbook.md) · [Maintenance and Extension](Ariciu%20-%20Maintenance%20and%20Extension.md) · [Phone Review 1](Ariciu%20-%20Phone%20Review%201.md) · [Phone Review 2](Ariciu%20-%20Phone%20Review%202.md)
