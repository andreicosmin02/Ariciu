# Obsidian knowledge-base export

Updated 2026-10-17 for **Ariciu 1.1-rc3 / code 4**, second phone-review stabilization. Start at [Ariciu.md](Ariciu/Ariciu.md). **27 focused linked notes and 46 emulator screenshots** (8 historical rc1, 19 review-1, 19 review-2). Only project-owned documentation and emulator images are exported, never the owner's private review/phone screenshots.

## Existing vault synchronization

Destination: the existing `Desktop/obsidian/obsidian_vault/Personal Projects/Ariciu` folder under the owner's home. Existing managed files were compared with the preceding repository export at `1b2eb13`; no user-edit conflicts were found. Synchronization completed on 17 October: all 27 notes/46 images match. A before/after preservation manifest confirmed all 12 unmanaged original planning/review/phone files remained byte-for-byte unchanged. Vault settings and unrelated folders were not touched.

The owner authorized this synchronization; the current environment permits direct filesystem access. There is no background/bidirectional synchronization or new plugin. Future copies must compare same-named notes before overwriting user edits. Copy the contents into the existing Ariciu folder, including Attachments; do not create a nested duplicate folder. Historical/private wiki targets resolve only in the existing vault and intentionally are not exported.

## Repository handoff

[Codex handoff](../CODEX_HANDOFF.md), [project status](../PROJECT_STATUS.md), [second-review evidence/retest](../PHONE_REVIEW_2.md) and [stabilization](../STABILIZATION.md) remain independent of vault access. Current verification: **82 unit / 35 Android 17 connected tests passed**, lint **0 errors/39 warnings**, assembly/install/signature verified. Actual foreground provider-to-outbox behavior was manually checked with simulated emulator positions. Read-only rc2 Samsung diagnostics confirmed the owner's unlocked swipe-to-Next-time; rc3 phone retest remains pending. Unchanged mechanism notes retain dated historical metadata; updated notes identify rc3.
