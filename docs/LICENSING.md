# Licensing review — approval pending

Ariciu has **no project-wide licence declaration**. Making a repository visible does not itself grant an open-source licence. No licence is applied by this documentation PR; a maintainer decision is required.

## Recommendation

Recommend **Apache License 2.0** for Ariciu-owned source, documentation and original assets, subject to confirming ownership and explicit maintainer approval. It permits modification and commercial/proprietary redistribution, includes an express contributor patent grant with patent-litigation termination, and requires retaining the licence/notices and identifying modifications. It does not grant trademark rights or warranties. These consequences are defined by the [authoritative licence text](https://www.apache.org/licenses/LICENSE-2.0).

MIT is a shorter permissive alternative, but lacks Apache-2.0’s explicit patent terms. Apache-2.0 is the recommended developer-facing choice; this review is not a grant or a claim that all existing contributors’ rights have been independently established.

Third-party code, data and styles retain their own terms under either choice. In particular, an application licence cannot replace the OSM-derived database’s ODbL or map-design attribution obligations. A future LICENSE/NOTICE change should make these boundaries explicit.

## Existing evidence and attribution

| Material | Existing evidence / obligations |
| --- | --- |
| Romanian catalogue | [NOTICE](../app/src/main/assets/places/NOTICE.md), complete [derived TSV](../app/src/main/assets/places/romania.tsv), [generator](../tools/build_place_catalog.py). OSM contributors, Geofabrik source, timestamp/hash, transformations and coverage exclusions are documented. Derived database is distributed under [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/), separately from application code. |
| OpenFreeMap upstream material | [Bundled upstream notice](../app/src/main/assets/map/LICENSE-openfreemap.md). Its “project/repo MIT” wording refers to **OpenFreeMap**, not an Ariciu-wide MIT licence. |
| Map styles / OpenMapTiles | [Light](../app/src/main/assets/map/LICENSE-light-style.md) and [dark](../app/src/main/assets/map/LICENSE-dark-style.md) notices retain BSD code, CC-BY design and CartoDB/Stamen/Paul Norman provenance. Visible OSM/OpenMapTiles map and screenshot attribution is retained. |
| Fonts, icons, background data | Upstream notice records Noto Sans (SIL OFL), Maki (CC0) and Natural Earth (public domain). Keep their notices with distributed material. |
| MapLibre Compose / Native | [Compose BSD-3-Clause](https://raw.githubusercontent.com/maplibre/maplibre-compose/v0.13.1/LICENSE) and [Native BSD-2-Clause](https://raw.githubusercontent.com/maplibre/maplibre-native/main/LICENSE.md); preserve upstream copyright/licence conditions. |
| Android/Kotlin libraries | AndroidX, Kotlin and OkHttp retain their licences; Google Play services has SDK terms. Declared dependency versions remain unchanged. See the [source/licence evidence note](obsidian-export/Ariciu/Ariciu%20-%20Sources%20and%20Licensing.md). |

The repository already includes catalogue provenance/distribution material, bundled style notices, in-app attribution and README screenshot credits. This focused review preserves them byte-for-byte; it does not certify every transitive binary notice or all distribution obligations. Broader binary/licensing review and production signing are separate work before a production release.

## Before public visibility

Approve a project-wide licence deliberately; keep author history and third-party notices intact. Decide visibility separately. The current debug-signed rc4 pre-release remains an early testing build, with no replacement APK/key or production-signing transition implied.
