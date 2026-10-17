# Final licensing and public-readiness audit

18 October 2026. PR #3 was already merged at **cfed6a1a4ca9036c11074c8866753444f6d2515f**. Work continues on the fresh **docs/apache-2-public-readiness** branch, for a new PR. No merge or visibility change is authorized. Application baseline remains **1.1-rc4 / code 5 / Room 5**.

## Approved licence and distribution notices

- Root LICENSE is the complete, unmodified official Apache-2.0 text: **11,358 bytes**, SHA256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`. It was downloaded from the owner-specified Apache URL and compared byte-for-byte.
- NOTICE is an informational scope/attribution guide, without an invented copyright holder or new CLA. Apache-2.0 covers Ariciu-owned original material where the necessary rights are held; third-party code/data/assets and depicted map content retain their own terms. See [LICENSING.md](LICENSING.md).
- All pre-existing app map/catalogue notices, the TSV, source/build configuration and Room history are unchanged. All 29 portable knowledge notes, 54 screenshots and three Mermaid diagrams remain; screenshot/diagram comparisons pass. The actual personal vault is not accessed.
- [Companion full notices](third-party/README.md) add exact version-pinned MapLibre BSD texts, Google OSS bundles/indexes and the separately MPL-licensed OkHttp Public Suffix List notice. Provenance records URLs/input artifact hashes/output hashes. Three identical Google input pairs share a copy. Google JSON byte ranges were validated (16 common and 21 location entries).
- Git attributes preserve exact licence/bundle bytes across platforms. Whitespace checking is disabled only for the two raw Google text bundles, which contain 76 upstream trailing-whitespace lines and byte-indexed content; hashes still verify their exact contents. No source/docs whitespace rules are weakened elsewhere.

## Repository security/privacy review

Independent review used checksum-verified standalone **Gitleaks 8.30.1**, downloaded from its official release into temporary tools storage, with full finding redaction. No project dependency or scanner configuration was added.

At baseline cfed6a1, the reachable-history scan covered **43 nonmerge commits / 2.62 MB**, zero findings. A tracked-HEAD archive scan covered 2.37 MB, zero findings. Complementary Python patterns/path checks examined **46 reachable commits / 583 unique blobs (511 text, 72 binary), 24,739,545 bytes**, with no common credential/private-key markers or accidentally tracked local.properties, environment secrets, signing keystores, databases, APK/AAB or build directories. Current text had no machine-specific home/vault dependencies; author identity/email and historical Samsung references were intentionally retained.

The exact staged licensing tree was separately archived and scanned with Gitleaks in redacted mode: **zero findings**. The current tracked/proposed UTF-8 and filename pattern check also passed (189 files). Scanner reports stay outside Git; final committed-tree/history checks are repeated before handoff.

This is not an absolute safety guarantee: unreachable Git objects, image OCR/steganography, deep native/DEX reverse engineering, dynamic vendor behavior and malicious dependency analysis are not fully audited. Automated pattern scanners can miss unknown secret formats. No secret values are printed, committed or included in this record; raw reports remain outside Git.

## Existing historical APK and release

The APK was actually downloaded from the existing GitHub Release, without rebuilding/replacing it. **289 ZIP entries / 98,367,874 uncompressed bytes** passed CRC checks and byte-pattern/filename scans, with zero configured credential/private-key or unexpectedly bundled local config/keystore/database findings. This includes raw DEX/native/resource bytes, not full decompilation or OCR. Eleven licence/notice-named entries were identified; bundled map notices, catalogue notice and full TSV match repository bytes.

The identified missing obvious full MapLibre notice entries and Google bundle names are addressed with separate companion distribution documentation. These texts are **not** claimed embedded in the old binary; the existing release body is not edited. Provide the companion material with redistribution. Full native/transitive binary licence certification and a future packaged/user-accessible notice inventory remain outside this task.

Reverified unchanged:

- Tag **v1.1-rc4** still resolves to **c8c47ce4642ba7f70ee0d61355bb5175e2334cc0**; its historical tree has no new root LICENSE/NOTICE. No tag/history rewrite.
- [Existing pre-release](https://github.com/andreicosmin02/Ariciu/releases/tag/v1.1-rc4), release ID **416031806**, asset ID **651470045**, uploaded/non-draft/pre-release state.
- [Ariciu-1.1-rc4-debug.apk](https://github.com/andreicosmin02/Ariciu/releases/download/v1.1-rc4/Ariciu-1.1-rc4-debug.apk), **74,677,975 bytes**, SHA256 **`f6bfa834034599addd680768d39e1e86caac96c32a9cd79052723be1f5863d78`**.
- Valid APK signature and original Android Debug certificate SHA256 **`3064d0124e266f0d7cf26c742b7ab50a01afd118bf03e40cf731a3b2d765ad2c`**; package **ro.andrei.ariciu**, version **1.1-rc4/code 5**, min SDK 26/target 37.

Licence approval does not turn the existing debug testing APK into a production-certified build or retroactively insert files into a historical snapshot. Existing app data/signing compatibility and install-over-existing guidance remain unchanged.

## Executed verification

Fresh `./gradlew :app:testDebugUnitTest --rerun :app:compileDebugKotlin :app:lintDebug` passed in 2s: **97 unit tests, zero failures/errors/skips; compile check passed; lint 0 errors/39 warnings**. Source/compiled dependencies were already up to date; unit execution was forced fresh. No assemble/package APK task, connected instrumentation or phone operation was run. This is not a clean-machine provisioning test.

The runtime dependency report confirmed the companion versions: MapLibre Compose 0.13.1/Native 13.0.2, GMS location 21.3.0/base 18.5.0/basement 18.4.0/tasks 18.2.0. No dependency version was changed.

Link validation scanned **60 Markdown files / 782 link references**: repository/local targets, **157 wikilinks, 58 image references and 33 internal anchors** resolved; **17 shell blocks** passed syntax checks. All 29 notes, 54 screenshot bytes and three Mermaid blocks match the preservation checks. External web-link availability was not exhaustively tested. Canonical Apache bytes, all 14 companion provenance entries and upstream index ranges passed; staged whitespace checks pass with the narrowly documented upstream-text attributes.

The review confirms the documented SDK/JDK/wrapper, selected-device testing, truthful offline/network boundaries and Android/background limits. Runtime application behavior, package, permissions, geofences, notifications, migrations, branding and signing are untouched. Existing release links/default master were checked through authenticated GitHub access; current repository visibility remains Private.

## Final publication checklist

- [x] New-developer README, setup and contribution documents are navigable; Obsidian is optional and portable.
- [x] Owner-approved Apache-2.0 applied in this PR with explicit third-party exclusions and complete official text.
- [x] Existing notices/provenance preserved; known companion notice gaps addressed separately from the historical APK.
- [x] No identified blocking secret exposure in the stated scans; limitations are documented above.
- [x] Build/test instructions match configuration; executed smoke/link checks are recorded.
- [x] Existing testing APK remains clearly debug-signed/early testing, with checksum, companion notices and update safety instructions.
- [x] Default branch/master and release URLs work for authorized access; no visibility or release changes made.
- [ ] Owner reviews and explicitly approves/merges the new licensing PR.
- [ ] Owner separately authorizes Private→Public visibility. **Do not change visibility as part of this PR.**

Production signing, complete transitive/native notice packaging and broader device certification are future release work, not claims of this readiness audit.
