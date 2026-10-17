# Third-party companion notices

These are distribution documents for the versions used by the existing **1.1-rc4 debug testing APK**. They supplement the preserved [bundled map notices](../../app/src/main/assets/map/LICENSE-openfreemap.md), [style licences](../../app/src/main/assets/map/LICENSE-light-style.md) and [catalogue notice](../../app/src/main/assets/places/NOTICE.md). They do not relicense third-party material under Ariciu’s project licence.

## Full texts

| Component | Companion material |
| --- | --- |
| MapLibre Compose 0.13.1 | [Complete BSD-3-Clause notice](LICENSE-MapLibre-Compose-0.13.1.txt) |
| MapLibre Native Android 13.0.2 | [Complete BSD-2-Clause notice](LICENSE-MapLibre-Native-13.0.2.txt) |
| OkHttp 4.12.0 | [Apache-2.0 text](../../LICENSE), identical to the version-pinned upstream text; [Public Suffix List notice](NOTICE-OkHttp-4.12.0-publicsuffix.txt) and [MPL-2.0 text](LICENSE-MPL-2.0.txt) for that separately licensed asset |
| Google Play services location 21.3.0 OSS components | Unmodified [notice bundle](play-services-location-21.3.0-third_party_licenses.txt) and [JSON index](play-services-location-21.3.0-third_party_licenses.json) |
| Google Play services base 18.5.0, basement 18.4.0, tasks 18.2.0 OSS components | Shared unmodified [notice bundle](google-play-services-common.txt) and [JSON index](google-play-services-common.json); these three input pairs are byte-identical |

[provenance.json](provenance.json) records version-pinned source/artifact URLs, archive entries, input AAR/JAR hashes, extracted-file hashes and storage mappings. Full texts are copied byte-for-byte, including upstream formatting and placeholders. The JSON indexes describe offsets/lengths in the corresponding raw text bundles; changing or reflowing those texts would invalidate them. Identical documents share a copy rather than being repeated.

The Google OSS bundles concern third-party components; they do **not** replace the proprietary Google Play services SDK terms. [Google’s notice guidance](https://developers.google.com/android/guides/opensource) explains the developer’s responsibility. AndroidX/Kotlin and other transitive libraries retain their upstream terms; this selection is not a complete native/transitive dependency licence inventory.

## Historical release boundary

The `v1.1-rc4` source tag and downloaded APK are unchanged and predate these companion files. The APK already carries the original map/catalogue notices and some library notices, but no obvious complete MapLibre BSD notice entries were found. These full BSD texts reproduce the copyright, conditions and disclaimer in the distribution documentation, as the upstream licences require; installation/release documentation links here beside the APK.

The Google bundles were extracted from the matching upstream AARs because their named notice files were not present as APK entries. No OSS-viewer dependency, Gradle plugin, runtime UI or rebuilt APK is introduced. The release page and asset are not republished or edited by this task. Keep these companion documents available with redistribution; do not imply they were inserted into the historical APK or tag.

This addresses the identified documentation gaps. It is not legal certification of every transitive/native binary obligation. A production release needs a complete version-specific notice inventory and an appropriate packaged/user-accessible notice presentation.
