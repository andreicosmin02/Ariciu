# Obsidian knowledge-base export

Updated 2026-10-17 for **Ariciu 1.1-rc4 / code 5**, authoritative master and the first verified GitHub pre-release, retaining the Urgent stabilization policy. Start at [Ariciu.md](Ariciu/Ariciu.md). **29 focused notes and 54 emulator screenshots** (8 historical rc1, 19 review1, 19 review2, 8 Urgent). Only project-owned documentation and dummy emulator captures are exported, never private owner reviews, phone images, logs or database snapshots.

## Existing vault synchronization

Destination: existing `Desktop/obsidian/obsidian_vault/Personal Projects/Ariciu`. Managed files were compared with the preceding export at `525b609`; conflicts are checked again before copying. Authorized direct filesystem synchronization maintains the same existing folder; no nested duplicate or background/bidirectional plugin.

Synchronization completed on 17 October: all 29 notes/54 images match, and all 12 unmanaged original planning/review/phone files remain byte-for-byte unchanged; vault settings and unrelated folders are outside this copy. Historical private wiki targets intentionally resolve only in the existing vault, not the export.

## Repository handoff

[Codex handoff](../CODEX_HANDOFF.md), [status](../PROJECT_STATUS.md) and [current Urgent policy/evidence/retest](../URGENT_RE_ALERTS.md) remain independent of vault access. Current gate: 97 unit/44 connected tests passed, lint 0 errors/39 warnings, debug assembly/install/signature; an additional native font-scale 2.0 test passed 1/1. Actual emulator default-five-minute recovery, real permission denial/restoration, channel blocking and card resolution were observed. Samsung checks remain pending; no phone operation occurred in this batch.

Publication/signing/cleanup evidence: [Releases and Signing](Ariciu/Ariciu%20-%20Releases%20and%20Signing.md). The public root README references only inspected emulator screenshots, not private vault files. The current source tag is v1.1-rc4 on the verified source merge; subsequent README/docs commits do not alter the binary.
