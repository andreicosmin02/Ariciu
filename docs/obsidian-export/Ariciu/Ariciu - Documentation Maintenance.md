---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Keeping the knowledge base accurate

Related: [[Ariciu]], [[Ariciu - Current Status]], [[Ariciu - Maintenance and Extension]].

## Authority and scope

Source shows implemented behavior; dated executed reports establish test evidence; device observations establish only the scenario actually tried. Owner decisions define product scope. Planning prose alone proves none of these. Every note is reviewed against the 1.1-rc1 source baseline, with dated YAML metadata; future editors should update metadata only after checking relevant source.

Repository essentials: `AGENTS.md` gives working rules/current priority; `docs/PROJECT_STATUS.md` gives evidence and state; `docs/CODEX_HANDOFF.md` gives next-session instructions; `docs/STABILIZATION.md` and `STABILIZATION_AUDIT.md` preserve debugging methods/limits without requiring the vault.

## Organization

`Ariciu.md` is the entry point. Focused notes link by Obsidian wikilinks and refer to repository-relative code paths. The Code Map locates implementation; Tests and Evidence owns the detailed test inventory; Sources and Licensing owns external references; Current Status owns the dated baseline. Avoid copying every detail into every note.

`Attachments/` contains only copied project emulator screenshots. The three pre-existing vault planning notes remain historical and are linked by [[Ariciu - Historical Notes]]. No plugins/settings, new vault or unrelated personal files are required.

## Update protocol

1. Inspect Git/source and relevant tests before rewriting a claim.
2. Update the mechanism's focused note, then Current Status/repository handoff for material changes.
3. Distinguish implemented, automatically tested, emulator observed, physical verified, unverified, planned, limitations and audit hypotheses.
4. Record exact commands/date/results. Old green reports do not validate new code; screenshots need source/version/context.
5. Check wikilinks, local file links, headings and screenshot references; retain required attribution/licensing.
6. Review the documentation diff and commit only project-owned material. Never commit the personal vault itself.

When copying newer exports into an existing vault, compare/merge same-named notes first; do not overwrite user edits blindly. Keep the original planning notes. There is no automatic bidirectional sync or new maintenance automation in this handoff.

## Access note

The registered existing vault and project folder were found unambiguously, but are outside the sandbox's writable roots. The safe deliverable is `docs/obsidian-export/Ariciu/`; direct vault installation remains blocked by filesystem permissions. The README beside the export explains the destination/import. Original note hashes were checked locally before/after; hashes and unrelated vault information are not committed.
