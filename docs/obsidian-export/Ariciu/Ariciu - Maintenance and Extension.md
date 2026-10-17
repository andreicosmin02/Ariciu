---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Maintenance and safe extension

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu - Architecture]], [[Ariciu - Data and Migrations]], [[Ariciu - Sources and Licensing]], [[Ariciu - Documentation Maintenance]].

## Stabilization workflow

Inspect current Git/source before edits; preserve unrelated changes. Create a focused branch from the latest clean master; do not revive obsolete release branches. Reproduce a reported issue, use TDD for application-owned behavior, keep changes coherent and inspect the exact diff. Ordinary contributors follow CONTRIBUTING.md and docs/DEVELOPMENT.md. Codex users can optionally use the repository android-milestone/android-verification skills; no agent or skill installation is needed to contribute. Documentation-only work does not require a new feature implementation cycle.

Run focused tests first, then relevant full gates and assembly; record unexecuted device checks. Commit/push authorized verified changes without routine approval pauses. No force push, merge, branch deletion, data discard or release publication. Keep credentials, phone databases, APKs and unrelated personal notes out of Git.

## Preserve architectural invariants

Keep plain models/lifecycle/allocation/search/budgets independent of Android/Compose/vendor types. Extend the existing ViewModel/repository/Room pipeline instead of rewriting it. Preserve SavedStateHandle draft/save identity and notification destination behavior. Avoid adding Hilt, DataStore, navigation frameworks, services or SDKs unless complexity demonstrates a need.

Generation changes isolate edits; occurrence changes isolate alerts. Keep conditional engine writes and stale-action guards. Journal IDs before registration and retain them until removal succeeds. Keep outbox-before-post semantics, process mutex and transaction boundaries. Explicitly consider races because Room, WorkManager and platform APIs cannot share one atomic transaction.

When editing chain behavior, test parent union and per-store timestamps, overlap, partial allocation, capacity recovery, selection refresh, Once/Every visit, Done and stale events. Count actual areas, not reminder rows, against 100. Do not add continuous GPS to solve snapshot limitations without a significant product/architecture decision.

## Room/schema changes

1. Define old-row/default behavior and migration plan first.
2. Increment schema version, add explicit non-destructive migrations and register them in the builder.
3. Export/check in schemas; do not edit historical schemas to match new code.
4. Test upgrade paths with actual older fixtures, completion/occurrence/quiet/snooze/chain/ledger preservation and reopening.
5. Check strict chain serialization compatibility separately; a Kotlin type change alone does not migrate TEXT contents.

Do not clear user storage or add destructive fallback to make migration tests pass. This candidate has no cloud backup/data export, so updates must preserve local data.

## Catalogue maintenance

[tools/build_place_catalog.py](https://github.com/andreicosmin02/Ariciu/blob/master/tools/build_place_catalog.py) accepts input PBF and output TSV paths; uses development-only PyOsmium. Run in an isolated development environment, not Android. Download a free Geofabrik Romania extract deliberately, retain source replication timestamp and SHA, regenerate the ODbL derivative, review counts/bounds/dedup/chain recognition and run catalogue tests.

Update together: [assets/places/romania.tsv](https://github.com/andreicosmin02/Ariciu/blob/master/app/src/main/assets/places/romania.tsv), NOTICE/source hash/date/counts, hardcoded UI catalogue wording, relevant tests and docs. The script writes a generation date but does not update all provenance/UI. Store coordinates approximate ways and can change. Existing reminders persist snapshots; an app data update is not automatic chain refresh. Test same store ID/new coordinates and selection/generation behavior before shipping data refreshes.

Never substitute paid Places/geocoder or unrestricted public Nominatim/Overpass endpoints. Broader countries, completeness promises or a new provider are meaningful product decisions.

## Map/provider maintenance

Bundled display styles and hosted offline definitions must share tile/font/sprite resources. Preserve attribution/licences and both themes when editing style layers. Native `asset://` download definitions previously stalled; real offline verification is required after URL/style changes. MapLibre initialization must precede HttpRequestUtil client setup.

Recheck current OpenFreeMap terms before changing pack dimensions/zoom/concurrency/budgets/provider. Count HTTP traffic and native resource status honestly; shared cache and resume counts differ. Use small explicit test downloads, never bulk-fetch to test limits. Keep user-triggered download, visible progress/errors, pause/resume/delete and bounded execution. Do not describe resource bytes as exact total disk usage.

## Dependencies and platform policy

Use the existing version catalogue; avoid unrelated upgrades. Verify official pricing/licensing/usage limits/billing/privacy before introducing any service/library. A metered free tier is insufficient for the permanent zero-recurring-cost constraint. Android permission/background/alarm changes need fresh official platform/Play policy review and documented supported fallback. Respect user/channel/DND controls; do not add full-screen/exact-alarm/FGS workarounds to conceal delivery uncertainty.

## Release maintenance

Increment version deliberately for a new candidate, rebuild/test, preserve signing identity and provide a checksum plus date. Update phone checklist and evidence separately from claims of phone success. Keep screenshots dated and tied to the source version. Public release/signing/policy preparation is separate authorized work, not implied by a debug APK.

## Related reading

[Architecture](Ariciu%20-%20Architecture.md) · [Data and Migrations](Ariciu%20-%20Data%20and%20Migrations.md) · [Sources and Licensing](Ariciu%20-%20Sources%20and%20Licensing.md) · [Documentation Maintenance](Ariciu%20-%20Documentation%20Maintenance.md)
