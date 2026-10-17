---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: master
version: 1.1-rc4
---

# Keeping the knowledge base accurate

[Knowledge-base index](Ariciu.md) · [Repository docs](https://github.com/andreicosmin02/Ariciu/blob/master/docs/README.md) · [Developer setup](https://github.com/andreicosmin02/Ariciu/blob/master/README.md#development)

Related: [[Ariciu]], [[Ariciu - Current Status]], [[Ariciu - Maintenance and Extension]].

## Authority and scope

Source shows implemented behavior; dated executed reports establish test evidence; device observations establish only the scenario actually tried. Owner decisions define product scope. Planning prose alone proves none of these. Notes changed for the first phone review are marked 1.1-rc2; unchanged mechanism notes retain their historical rc1 metadata. Future editors should update metadata only after checking relevant source.

Repository essentials: [AGENTS.md](https://github.com/andreicosmin02/Ariciu/blob/master/AGENTS.md) gives working rules/current priority; [docs/PROJECT_STATUS.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/PROJECT_STATUS.md) gives evidence and state; [docs/CODEX_HANDOFF.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/CODEX_HANDOFF.md) gives next-session instructions; [docs/STABILIZATION.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/STABILIZATION.md) and [STABILIZATION_AUDIT.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/STABILIZATION_AUDIT.md) preserve debugging methods/limits without requiring the vault.

## Organization

[Ariciu.md](https://github.com/andreicosmin02/Ariciu/blob/master/docs/obsidian-export/Ariciu/Ariciu.md) is the entry point. Focused notes link by Obsidian wikilinks and refer to repository-relative code paths. The Code Map locates implementation; Tests and Evidence owns the detailed test inventory; Sources and Licensing owns external references; Current Status owns the dated baseline. Avoid copying every detail into every note.

`Attachments/` contains project emulator screenshots. Historical decisions are summarized in [[Ariciu - Historical Notes]] using included records; there are no external private-note dependencies. Opening/copying instructions are in the export README.

## Update protocol

1. Inspect Git/source and relevant tests before rewriting a claim.
2. Update the mechanism's focused note, then Current Status/repository handoff for material changes.
3. Distinguish implemented, automatically tested, emulator observed, physical verified, unverified, planned, limitations and audit hypotheses.
4. Record exact commands/date/results. Old green reports do not validate new code; screenshots need source/version/context.
5. Check wikilinks, local file links, headings and screenshot references; retain required attribution/licensing.
6. Review the documentation diff and commit only project-owned material. Never commit the personal vault itself.

When incorporating a newer copy into another vault, compare same-named notes first and preserve your own edits. This is an optional manual import; no synchronization service, filesystem permission arrangement or personal directory is assumed. Work on repository files when contributing, without accessing anyone else’s vault.

## Related reading

[Ariciu](Ariciu.md) · [Current Status](Ariciu%20-%20Current%20Status.md) · [Maintenance and Extension](Ariciu%20-%20Maintenance%20and%20Extension.md) · [Historical Notes](Ariciu%20-%20Historical%20Notes.md)
