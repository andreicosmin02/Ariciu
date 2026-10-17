# Obsidian knowledge-base export

Updated 2026-10-17 for **Ariciu 1.1-rc2 / code 3**, first phone-review stabilization. Start at [Ariciu.md](Ariciu/Ariciu.md). **26 focused linked notes and 27 emulator screenshots** (8 historical rc1, 19 review-1). Only project-owned documentation and emulator images are exported, never the owner's private review/phone screenshots.

## Existing vault synchronization

Destination: the existing `Desktop/obsidian/obsidian_vault/Personal Projects/Ariciu` folder under the owner's home. Before synchronization, existing export filenames were compared with the previous repository export at 62557c5; no user-edit conflicts were found. Direct synchronization requires environment filesystem approval because the vault is outside writable roots. The three original planning notes, private rc1 review/eight screenshots, vault settings and unrelated notes must remain unchanged.

Synchronization completed on 17 October using filesystem approval: all 26 notes/27 images match; all original planning/review/phone captures remain unchanged. See docs/CODEX_HANDOFF.md and PHONE_REVIEW_1.md. There is no background/bidirectional synchronization or new plugin. Future copies must compare same-named notes before overwriting user edits. Copy the contents into the existing Ariciu folder, including Attachments; do not create a nested duplicate folder.

## Repository handoff

[Codex handoff](../CODEX_HANDOFF.md), [project status](../PROJECT_STATUS.md), [review evidence/retest](../PHONE_REVIEW_1.md) and [stabilization](../STABILIZATION.md) remain independent of vault access. Current verification: 68 unit/23 Android17 connected tests passed, lint 0 errors/39 warnings, assembly/install/signature verification passed. Phone retest on Samsung S26 Ultra / One UI8.5 / Android16 remains pending. Unchanged mechanism notes retain dated historical rc1 metadata; changed notes identify rc2.
