---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Sources, licensing, cost and privacy

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Maintenance and Extension]], [[Ariciu - Maps and Current Location]], [[Ariciu - Search Catalogue]], [[Ariciu - Notifications and Alarm]].

These references preserve the official-source review recorded on **2026-10-15/16** in repository [docs/DELIVERY_ARCHITECTURE.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/DELIVERY_ARCHITECTURE.md) and [docs/FEATURE_COMPLETION.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/FEATURE_COMPLETION.md). This documentation audit checked local implementation/provenance. OpenFreeMap terms and MapLibre licence files were also rechecked live on 2026-10-17; the other references retain their earlier review dates, rather than implying a fresh verification of every external term. Recheck current official sources before changing services, permissions or provider usage.

## Cost boundary

No backend, Firebase, subscriptions, billing account, paid key, mandatory account or cloud storage. Room/lifecycle/actions/search run locally. Maps contact OpenFreeMap only as needed or during explicit download; positioning uses the existing Play services client. Free public hosting has availability/terms risk, not a financial commitment or guaranteed perpetual service. Do not mistake a service's paid free tier for compliance.

| Component/data | Existing licence/cost evidence and obligation |
| --- | --- |
| AndroidX Room/WorkManager/Compose | Apache 2.0 families; local libraries, not hosted accounts/services. Retain required notices for distribution. |
| Play services location 21.3.0 | Proprietary Android SDK licence, royalty-free SDK use; no Maps Platform key/billing setup introduced. GMS device dependency remains. |
| MapLibre Compose / Native | Open-source BSD-3-Clause / BSD-2-Clause upstream projects; existing renderer. Check bundled/transitive notices for a public distribution. |
| OkHttp 4.12.0 | Apache 2.0; existing transitive runtime made direct for HTTP configuration. No new service. |
| OpenFreeMap project/styles | Project MIT; style code/design and source data have separate licences. No key/account/billing in recorded public service terms. |
| OpenMapTiles/style derivatives | Bundled style notices: BSD-3-Clause code, CC-BY-4.0 visual design with CartoDB/CC-BY-3.0 provenance. Visible OpenMapTiles/OSM attribution. |
| Romania catalogue | OSM-derived database under ODbL 1.0. Attribute contributors, distribute derivative and provenance; TSV is shipped and in Git. |
| PyOsmium | BSD-2-Clause development-only parser; no app runtime dependency. |
| Fonts/icons/background data | OpenFreeMap notice records Noto Sans SIL OFL 1.1, Maki CC0, Natural Earth public domain. Preserve licence files/credits. |

There is no project-wide application licence declaration established by this audit; do not infer that third-party licences grant ownership/licensing of Ariciu itself. Future public distribution should review all required notices, signing and store policy. This does not block the existing private debug candidate.

MapLibre licence evidence rechecked on 17 October: [Compose 0.13.1 licence](https://raw.githubusercontent.com/maplibre/maplibre-compose/v0.13.1/LICENSE), [Native licence](https://raw.githubusercontent.com/maplibre/maplibre-native/main/LICENSE.md). OpenFreeMap terms dated 14 October still state the recorded explicit download allowance.

## Map/data official references

- [OpenFreeMap terms](https://openfreemap.org/tos/) — recorded explicit download allowance up to 3,000 tiles or 50 MB, four concurrent requests and identifying User-Agent; no unrestricted bulk downloading.
- [MapLibre Native offline API](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html)
- [MapLibre Compose source](https://github.com/maplibre/maplibre-compose) and [Native source](https://github.com/maplibre/maplibre-native)
- [Geofabrik free data](https://www.geofabrik.de/en/data/download.html), [Romania extract](https://download.geofabrik.de/europe/romania.html)
- [OSM attribution/copyright](https://www.openstreetmap.org/copyright), [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/)
- [PyOsmium](https://osmcode.org/pyosmium/), [OkHttp licence at 4.12](https://raw.githubusercontent.com/square/okhttp/parent-4.12.0/LICENSE.txt)

Local evidence: [app/src/main/assets/map/LICENSE-openfreemap.md](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/map/LICENSE-openfreemap.md), [LICENSE-dark-style.md](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/map/LICENSE-dark-style.md), [LICENSE-light-style.md](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/map/LICENSE-light-style.md); [app/src/main/assets/places/NOTICE.md](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/places/NOTICE.md); full TSV and generator. Do not replace required legal notices with this summary.

## Android/Play official references

- [Geofencing guide and recovery](https://developer.android.com/develop/sensors-and-location/location/geofencing)
- [GeofencingClient](https://developers.google.com/android/reference/com/google/android/gms/location/GeofencingClient), [CurrentLocationRequest](https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest)
- [Background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [Notification API/flags](https://developer.android.com/reference/android/app/Notification), [VIBRATE permission](https://developer.android.com/reference/android/Manifest.permission#VIBRATE)
- [Android 14 behavior/ongoing dismissal](https://developer.android.com/about/versions/14/behavior-changes-all)
- [Android 16 behavior](https://developer.android.com/about/versions/16/behavior-changes-all), [Android 17 targeted changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Play full-screen intent/FGS policy](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Android SDK licence](https://developer.android.com/studio/terms), [Play services setup](https://developers.google.com/android/guides/setup)
- [Play location POM](https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-location/21.3.0/play-services-location-21.3.0.pom), [WorkManager POM](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/2.10.1/work-runtime-ktx-2.10.1.pom)

## Privacy behavior

Catalogue queries, reminders, lifecycle and theme are local; no analytics/backend/cloud sync was added. Normal map browsing/downloads reveal requested tile/style/font areas and IP to the provider/CDN. Play services positioning is a vendor/system boundary and may use network assistance. No claim of wholly disconnected positioning or absence of vendor telemetry is made. Avoid storing precise user locations, log dumps or personal vault contents in documentation/Git.

## rc4 consolidation and pre-release

Authoritative baseline is now master, tag v1.1-rc4, with an actual verified attached debug APK. [[Ariciu - Releases and Signing]] records the authorized merge, asset download/checksum/certificate, fresh 97-unit/44-emulator gate, signing/licensing limits and pending phone checks. Repository visibility is unchanged by documentation work; no production key or app-wide licence was introduced. Earlier dated implementation/source evidence above retains its own scope.

## Related reading

[Maintenance and Extension](Ariciu%20-%20Maintenance%20and%20Extension.md) · [Maps and Current Location](Ariciu%20-%20Maps%20and%20Current%20Location.md) · [Search Catalogue](Ariciu%20-%20Search%20Catalogue.md) · [Notifications and Alarm](Ariciu%20-%20Notifications%20and%20Alarm.md) · [Releases and Signing](Ariciu%20-%20Releases%20and%20Signing.md)
