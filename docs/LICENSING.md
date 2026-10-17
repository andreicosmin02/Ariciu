# Apache-2.0 scope and third-party licensing

The owner explicitly approved **Apache License 2.0** for Ariciu-owned project material. The complete, unmodified official text is in [LICENSE](../LICENSE), with an informational [NOTICE](../NOTICE). SPDX identifier: **Apache-2.0**. No repetitive Kotlin headers, invented copyright holder or contributor agreement is added.

## Scope

The declaration covers original Ariciu application source, tests, scripts/build configuration, documentation and original project assets, including the original fox artwork, **where the owner has the necessary rights**. It does not assert ownership of upstream code/data or override notices already attached to third-party material. Screenshot map content and derivative map styles are separately licensed even where the surrounding UI/documentation is original.

Apache-2.0 permits modification and commercial/proprietary redistribution, includes an express contributor patent grant with patent-litigation termination, and requires retaining applicable licence/notices and identifying modifications. It does not grant trademark rights or warranties. The [official text](https://www.apache.org/licenses/LICENSE-2.0.txt) controls; NOTICE is informational and does not amend it.

## Third-party exclusions and attribution

| Material | Terms and retained evidence |
| --- | --- |
| Romanian catalogue | OpenStreetMap-derived database under **ODbL 1.0**, not Apache-2.0. [NOTICE](../app/src/main/assets/places/NOTICE.md), [complete TSV](../app/src/main/assets/places/romania.tsv) and [generator](../tools/build_place_catalog.py) retain OSM attribution, Geofabrik provenance, source timestamp/hash, transformation and coverage boundaries. [ODbL text](https://opendatacommons.org/licenses/odbl/1-0/) governs the database. |
| OpenFreeMap upstream material | The [bundled notice](../app/src/main/assets/map/LICENSE-openfreemap.md) retains its MIT and related notices. Its “project/repo MIT” wording refers to upstream OpenFreeMap, not Ariciu. |
| Map styles, OpenMapTiles and depicted map content | [Light](../app/src/main/assets/map/LICENSE-light-style.md) / [dark](../app/src/main/assets/map/LICENSE-dark-style.md) notices retain BSD code, CC-BY design and CartoDB/Stamen/Paul Norman provenance. Visible OSM/OpenMapTiles attribution remains in maps/screenshots. These portions are excluded from the project-wide Apache declaration. |
| Fonts/icons/background data | Upstream notice records Noto Sans (SIL OFL), Maki (CC0), Natural Earth (public domain) and related credits. They retain those terms. Remote map resources are not newly owned by Ariciu. |
| MapLibre Compose / Native | Version-pinned complete **BSD-3-Clause / BSD-2-Clause** notices are supplied in [companion distribution documents](third-party/README.md); no upstream copyright is removed. |
| AndroidX, Kotlin, Gradle, OkHttp and other libraries/tools | Retain their respective upstream terms, commonly Apache-2.0; no third-party ownership is transferred. OkHttp’s Public Suffix List separately retains **MPL-2.0**. Optional PyOsmium tooling is upstream BSD-2-Clause and not an Android runtime dependency. |
| Google Play services | Proprietary SDK terms remain separate from extracted OSS component licences. Version-matched notice bundles/indexes and provenance are provided with the [companion notices](third-party/README.md). No SDK is relicensed. |

All four pre-existing bundled map/catalogue notice files and the derived TSV are preserved byte-for-byte. New full upstream companion texts are also copied without reformatting; their source/hashes are recorded. The root Apache text is reused for OkHttp because it is byte-identical to OkHttp 4.12.0’s upstream licence text, not because Ariciu claims ownership of it.

## Contributions

Contributors should have the rights necessary to submit their work and identify third-party material with its original terms. Apache-2.0 section 5 addresses intentional submissions unless explicitly stated otherwise; this documentation does not introduce a separate CLA or establish ownership of unreviewed external material.

## Historical tag, APK and visibility

The owner’s licence decision is implemented in the current licensing branch and will reach master only after the new PR is approved/merged. **`v1.1-rc4` remains its historical pre-licensing source snapshot**, with no new root LICENSE/NOTICE files inserted. The existing debug APK, checksum, certificate, package/version and original release page remain unchanged. The current source declaration does not rewrite historical Git objects or certify the old binary’s complete licensing inventory.

Companion notices linked beside installation instructions supply the identified missing full BSD texts and Google OSS bundle documentation separately from the APK. Full transitive/native distribution certification is not claimed. Future production packaging should provide a complete version-specific notice inventory and user-accessible presentation.

Repository visibility remains a separate owner decision. No licence approval authorizes a merge, a Private→Public change, a new signing key or release publication. See the [final readiness audit/checklist](PUBLIC_READINESS_AUDIT.md).
