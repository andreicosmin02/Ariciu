# Optional Ariciu knowledge base

This portable collection explains Ariciu’s architecture, reminder lifecycle, Room migrations, geofence recovery, notifications, maps/search, design and testing. It contains **29 notes and 54 real emulator screenshots**, including dated release/review evidence. Obsidian is optional: it is not needed to build, test or contribute.

## Read on GitHub or in a checkout

Start at [Ariciu.md](Ariciu/Ariciu.md). Its standard Markdown links work on GitHub and in Obsidian. Notes retain internal `[[wikilinks]]` and have ordinary related-reading links; images use relative `Attachments/` links. Historical reports identify their version/date rather than describing old behavior as current.

The [ordinary documentation index](../README.md), [developer setup](../DEVELOPMENT.md), [current status](../PROJECT_STATUS.md) and [source code](../../app/src/main/java/ro/andrei/ariciu) live in the same repository. Source code is authoritative for implementation; dated tests and device observations establish only the cases actually checked. The knowledge base complements these documents rather than replacing them.

## Open with Obsidian

1. Clone or download this repository.
2. In Obsidian, choose **Open folder as vault** and select **`docs/obsidian-export/Ariciu`** inside your checkout. Select the folder containing `Ariciu.md` and `Attachments/`, not `docs/obsidian-export` or the repository root.
3. Open **Ariciu.md** and follow the topic links. Diagrams use built-in Mermaid support; no community plugin, account or synchronization tool is required.

Obsidian may create a local `.obsidian/` settings folder. Keep personal settings outside commits; no settings folder is needed to share the notes.

## Incorporate into another vault

Copy the entire **`Ariciu/`** directory into a location of your choice in an existing vault, retaining note filenames and the `Attachments/` directory together. All note/attachment targets are included. Review same-named files before copying to avoid overwriting your own notes; no pre-existing Ariciu notes are assumed.

Internal navigation and screenshots remain available offline after copying. Source/document references inside the notes use GitHub links with repository-relative path labels, so moving the collection does not break them; those online links need internet and repository access where applicable. The outer README’s relative links to the checkout assume the original repository layout. The note index also links to online repository documentation for standalone copies.

## Organization and maintenance

- Current implementation: architecture, data/migrations, lifecycle, recovery, notifications, maps, catalogue and UI.
- Engineering: code map, setup, maintenance, tests, bug-report template and known limitations.
- History: included review/release records, with older behavior explicitly dated and linked to current policy.

Update the relevant focused note when verified behavior changes, and retain the date/scope of historical evidence. See [Documentation Maintenance](Ariciu/Ariciu%20-%20Documentation%20Maintenance.md). Never copy private phone data, credentials or an unrelated personal vault into this repository.
