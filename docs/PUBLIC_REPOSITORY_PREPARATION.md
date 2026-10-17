# Public-use repository preparation

Documentation and non-runtime ignore housekeeping from clean master **77ee6c9896ec3f4dffede22d0e01ae2369e704da**, on `docs/public-repository-preparation`. This branch is for review: **do not merge or change visibility without approval**. The existing repository, author history, Samsung evidence, v1.1-rc4 tag/release/APK, application, dependencies, permissions, Room schemas and signing remain unchanged.

## Improvements

- Root README retains the fox and three actual reviewed emulator captures, accurate features, release/checksum/signing/upgrade instructions and privacy/platform limits. It links ordinary contributor setup rather than requiring Codex.
- DEVELOPMENT documents actual SDK 37, Build Tools 36.0.0, Gradle 9.5.0, JDK 25 (distinct from Java source compatibility 11), local SDK configuration, sync/build/test/report paths and explicit disposable test-device selection.
- CONTRIBUTING covers focused branches/PRs, regression testing, data/migrations, Android restrictions, offline-first/zero-cost architecture and redacted bug reports.
- Personal `.obsidian/` settings are ignored so opening the tracked notes as a vault does not invite accidental settings commits.
- The normal documentation index separates current implementation, tests/known limits, historical evidence, future plans and optional automation/knowledge-base material.
- Publication/cleanup records now distinguish visibility at publication from a permanent restriction and record the completed approved eight-branch deletion. Historical measurements and commit/version context are retained.

## Portable knowledge base

All **29 notes and 54 PNG attachments** remain; no technical note, diagram or screenshot was deleted. Screenshot bytes and Mermaid diagrams are unchanged. Open `docs/obsidian-export/Ariciu` directly as an optional vault, or copy the whole Ariciu folder with its Attachments. No plugin, account, personal path or synchronization is required.

Four missing private-note links were replaced by included review/technical chronology. Internal wikilinks remain; a standard Markdown index, related-reading links and ordinary image embeds support GitHub readers. Online source/document links use repository-relative path labels and survive copying notes; internal notes/images remain local. No actual personal vault was accessed or modified.

Removed/generalized: the personal Desktop-vault destination; assumptions about pre-existing planning/review notes; personal synchronization/hash-copy outcomes; session filesystem/escalation permissions; old “never start from master” guidance; stale schema/file counts; current four-intensity wording; obsolete cleanup/private-only instructions. Exact emulator serials in dated executed reports remain historical evidence; current runnable examples select the contributor’s own test-device serial.

## Verification

- Link validation scanned 58 Markdown files and 748 Markdown/HTML link references: local/repository file targets, 33 internal anchors, 157 wikilinks and all 58 embedded image references resolved. External website availability was not exhaustively rechecked. All 54 knowledge-base attachments are referenced; none missing/orphaned.
- All 29 note filenames remain; 54 attachment byte comparisons and all three Mermaid-diagram comparisons against master passed.
- Seventeen shell-code blocks passed bash syntax checks. Build commands were compared with the actual Gradle/task configuration; placeholders and historical transcripts are not claimed executed.
- `./gradlew :app:testDebugUnitTest --rerun :app:assembleDebug` passed in 2s: **97 unit tests, zero failures/errors/skips**, debug assembly passed. Existing source/dependency outputs were up to date; unit execution was forced fresh.
- Application/Gradle/tools diff against master is empty. The built debug APK still has the recorded rc4 SHA256. GitHub API confirms unchanged release/asset IDs, size/digest, prerelease state and current visibility; v1.1-rc4 points to its original commit.
- Focused scan of 175 current UTF-8 tracked/proposed files and tracked filenames found no configured common credential/private-key markers, actual home-directory/personal-vault paths, or tracked local.properties/keystore/APK files. Whitespace checks and independent knowledge-base/contributor reviews passed.

The build smoke check uses the existing configured development environment and dependency caches; it is not a fresh-machine provisioning test. No connected instrumentation or new physical-phone test is run for this docs-only change. Historical rc4 instrumentation/lint/device evidence stays explicitly dated in the release records.

## Focused privacy/licensing review and decisions

Current tracked documentation, source/config text and filenames were checked for machine-specific paths, unavailable file dependencies, common credential/private-key markers and accidentally tracked local configuration/signing files. README image provenance and unchanged screenshot bytes were checked. No credential values are reproduced in this record.

This is **not a complete security audit** of every historical Git blob, binary dependency or APK. Existing author identity/email and Samsung references are intentionally retained. Historical commits are not rewritten; older documentation in Git history naturally retains its original workflow context.

No project-wide licence has been applied. [LICENSING.md](LICENSING.md) recommends **Apache-2.0**, requiring separate approval. Catalogue ODbL/provenance, style licences and upstream notices remain unchanged; a full transitive binary/distribution review is not claimed. Before changing visibility, review this PR, decide licensing and retain the early-testing/debug-signing limitations of the existing release. Visibility remains the owner’s separate decision.
