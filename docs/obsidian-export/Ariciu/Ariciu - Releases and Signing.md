---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: master
version: 1.1-rc4
---

# Releases, signing and branch consolidation

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu]], [[Ariciu - Current Status]], [[Ariciu - Development and Builds]], [[Ariciu - Sources and Licensing]], [[Ariciu - Tests and Evidence]], [[Ariciu - Urgent Re-alerts]].

## First GitHub pre-release

`master` is authoritative. Owner-authorized normal [PR #1](https://github.com/andreicosmin02/Ariciu/pull/1) merged all completed source/history at `c8c47ce4642ba7f70ee0d61355bb5175e2334cc0`, with exactly rc4's tree from `525b609`. Tag **v1.1-rc4** identifies that merge. The later README/docs-only PR leaves app source, permissions, migrations and APK unchanged; it does not move the release tag.

[Ariciu 1.1-rc4 — Phone Testing](https://github.com/andreicosmin02/Ariciu/releases/tag/v1.1-rc4) / [attached APK](https://github.com/andreicosmin02/Ariciu/releases/download/v1.1-rc4/Ariciu-1.1-rc4-debug.apk). Published, prerelease=true, draft=false. **Early testing, not production-stable or Google Play distribution.** The repository was private at publication; access follows its visibility, which this documentation change does not alter. The APK is an actual release asset, not committed into Git.

## Artifact and upgrades

Package `ro.andrei.ariciu`, version **1.1-rc4/code 5**, Room **5**, minimum SDK **26**, target **37**, Google Play services/compatible rendering device required. Minimum SDK is not proof every older runtime was tested. APK **74,677,975 bytes**.

SHA256 `f6bfa834034599addd680768d39e1e86caac96c32a9cd79052723be1f5863d78`.

Existing Android Debug certificate SHA256 `3064d0124e266f0d7cf26c742b7ab50a01afd118bf03e40cf731a3b2d765ad2c`. apksigner/aapt2 checked validity, package/version/permissions; the uploaded asset was actually downloaded and independently matched bytes/checksum/certificate. Debug assembly reproduced the same candidate. No new production key, permission or migration change.

Install over the same-signed existing debug app; never uninstall/clear storage to update. A different developer's local debug key can be incompatible. Stop and investigate signature/version mismatch while retaining data. Plan production signing and a safe data-preserving transition separately. The owner subsequently approved Apache-2.0 for current Ariciu-owned material; the historical release/tag retains its original pre-licensing snapshot. Third-party notices remain separate.

## Verification and branch cleanup

Fresh consolidation gate passed 97 unit tests (explicit rerun), 44 Android 17 emulator tests, zero failures/skips; lint 0 errors/39 warnings and debug assembly. Connected tests deliberately targeted emulator-5554, never the daily Samsung. The source merge equals the tested rc4 tree; subsequent public README/docs changes preserve it. User-reported initial phone feedback is positive, without detailed scenario evidence. Remaining GPS/OEM/idle/audio/TalkBack checks remain open.

All eight original development branch tips were ancestors of rc4, zero independent commits: chore/autonomous-codex-workflow; feat/map-first-reminder-creation; feat/reliable-reminder-delivery; feat/considered-ui-redesign; feat/phone-release-candidate; fix/phone-review-1; fix/phone-review-2; fix/urgent-realerts. After PR #2 merged, the owner approved cleanup. All eight were rechecked against master and safely deleted locally and from origin; fetch/prune confirmed only master remained before this docs branch was created. All history remains reachable from master; v1.1-rc4 and its release/APK were preserved. No force deletion/push or history rewrite.

Full audit, commands, merge/tag metadata, branch table and limits: repository [docs/GITHUB_RELEASE_RC4.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/GITHUB_RELEASE_RC4.md). Public introduction: root [README.md](https://github.com/andreicosmin02/Ariciu/blob/master/README.md), using only existing emulator captures and an SVG export of the original fox. Private reviews, screenshots, logs and coordinates remain outside Git. This optional knowledge base is self-contained; no personal vault is part of the contribution workflow.

## Related reading

[Ariciu](Ariciu.md) · [Current Status](Ariciu%20-%20Current%20Status.md) · [Development and Builds](Ariciu%20-%20Development%20and%20Builds.md) · [Sources and Licensing](Ariciu%20-%20Sources%20and%20Licensing.md) · [Tests and Evidence](Ariciu%20-%20Tests%20and%20Evidence.md) · [Urgent Re-alerts](Ariciu%20-%20Urgent%20Re-alerts.md)
