---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: 011746c
version: 1.1-rc1
---

# Current status — stabilization handoff

Source/documentation audit: **2026-10-17**. Related: [[Ariciu - Tests and Evidence]], [[Ariciu - Risks and Technical Debt]], [[Ariciu - Debugging Playbook]].

## Baseline and Git history

Current app: **Ariciu 1.1-rc1, version code 2**, Android 8+/Google Play services. Development branch: **feat/phone-release-candidate**, based on the completed redesign at `de9644e`; nothing merged into master. At audit start the worktree was clean and HEAD was `42f77a3`.

| Commit | Meaning |
| --- | --- |
| 42f77a3 | Release-candidate evidence and extended phone checklist; pre-audit documentation baseline. |
| 011746c | Latest application implementation: Romania search, current location, bounded offline maps and UI polish. |
| 4e959b9 | Alert intensities, snooze, Room 4 and multi-store lifecycle/allocation. |
| 92ba3e9 | Feature-completion authorization and owner-requested IDE ignore rules. |
| de9644e | Prior redesign handoff/base. |

The documentation-only handoff commit is discoverable with `git log -5 --oneline`; do not embed its own hash into files before it exists. Reinspect branch/worktree/history at the next session rather than assuming the repository never changes.

## What is implemented

Single-place and selected-chain reminders, ENTER/EXIT, Once/Every visit, Room persistence/additive upgrades, creation/edit/delete/drafts, Active/Completed, four intensities and Alarm Stop/ten-minute Snooze, generation/occurrence-safe actions, registration journal/outbox/recovery. Current-location selection, private Romania town/shop search, seven chains, fair 100-area allocation and small theme-specific offline packs. System/Light/Dark, graphite/slate identity, accessible labeled controls and enlarged-text adaptations.

See [[Ariciu - Features and Journeys]] for supported journeys and limitations rather than treating this inventory as an all-device guarantee.

## Evidence and artifact

Last full checks **2026-10-16**: 64 unit tests and 19 Android 17 emulator tests passed; lint 0 errors/39 warnings; debug assembly/install passed. Both themes/enlarged text reviewed and one small map area rendered with emulator networking disabled. Reports re-read on 17 October; no app checks rerun during documentation-only work. No dated physical-phone verification or field bug report has been supplied.

Current local rebuilt APK, inspected 2026-10-17: `app/build/outputs/apk/debug/Ariciu-1.1-rc1-debug.apk` (same bytes as `app-debug.apk`), **74,463,275 bytes**.

SHA256: `cc8c447b52ea8a9ab5afb10d01a24d0dfeecf97217b0bab1154dcb10a5d0567e`.

This is the 16 October rebuild/signature-verified debug artifact; the older `e67b…` hash recorded in the previous release note identified an earlier build snapshot. Same version/source does not imply byte-identical APKs after rebuilding. Generated APKs/reports are not committed or guaranteed available in another environment. Rebuild and verify an artifact if absent; update installation requires the same signing key. No release published.

## Next priorities

1. Reproduce the owner's actual phone issue, isolate the failing boundary and preserve their installed data.
2. Add focused regressions and fix root causes without weakening generation/occurrence/registration protections.
3. Validate notification/geofence recovery, real audio/vibration and confusing UX on the reported device; use emulator evidence honestly.
4. Keep both themes, accessibility, local operation and zero recurring cost intact. Update handoff/evidence after each meaningful fix.

Do not resume broad feature expansion by default. Current permitted work centers on stabilization; significant new features/services/coverage changes require product direction. The documented Romania scope and supported Alarm fallback need no repeated approval. No merge, force-push, branch deletion or publication.

## Knowledge transfer destination

An existing vault and Ariciu project folder were found; the sandbox makes them read-only. New focused notes/screenshots are provided under `docs/obsidian-export/Ariciu/`, ready to merge into the existing folder. The three original notes remain untouched; see [[Ariciu - Historical Notes]]. Essential next-session instructions remain in repository `AGENTS.md`, `docs/CODEX_HANDOFF.md`, `docs/PROJECT_STATUS.md` and `docs/STABILIZATION.md`, independent of Obsidian access.
