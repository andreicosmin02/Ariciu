# Map search plan (replaces Phase 1 item 2; owner-authorized 2026-11-10)

Free only, no backend or remote search API, no new permissions, no Room schema change, catalogue stays offline.

## Picker state machine
- **MANUAL** (today's behaviour): centre crosshair is the selection. Confirm = "Use this place".
- **RESULTS**: a query or category chip produced up to 40 results (all within 25 km of the reference point, or a town anywhere for a name match). The selection is a result pin. Camera fits the results and the nearest result to the reference point is selected.
  - Reference point = user location if known, else the current pin or map centre.
  - Tap a pin <-> highlight its list row. "Search this area" appears after the map moves; it reruns with the new centre.
  - Confirm = "Remind me at this place" (one reminder, title and point prefilled) or "Remind me at all N places" (chainAreas, 1-40, search label in chainName).
- RESULTS -> MANUAL: long-press the map (drops a manual pin) or clear the search. The selected point becomes the crosshair position.
- MANUAL -> RESULTS: submit a query or tap a category chip.

## Data size plan
- Today: 26,065 rows, 1.4 MB TSV, load 474 ms on the emulator (parse-bound), already at the 500 ms budget. Adding 11 categories makes it roughly 3-4x larger, so a single in-memory TSV will not meet the budget.
- Plan: grid index. `places/towns.tsv` (all towns/villages, loaded once for name search) plus `places/grid/<ix>_<iy>.tsv`, one file per 0.25 degree cell for everything else, loaded lazily for the reference point +/- 25 km (<= 12 cells) and cached. Search scans only those candidates. Row format is the existing 7 columns plus a category column, so old rows still parse.
- Budgets: added APK size <= 5 MB, cold catalogue load <= 500 ms, search <= 100 ms (emulator). Measurements go in docs/MAP_USABILITY.md.

## Legacy chain reminders
`Reminder.chainName`/`chainAreas`, `ChainLifecycle`, `DeliveryEngine`, encode/decode and Room stay unchanged. Area ids stay `n<id>`/`w<id>`. The brand is a catalogue field, so existing chain names ("Lidl", "Kaufland") still match. The picker chain UI goes away; the editor keeps showing and editing legacy and new multi-area reminders, and never replaces areas with an empty refresh.

## Test plan (JVM unit tests, test first)
search ranking (name, brand, category, address, accents), 25 km radius and cap 40, nearest/tie-break, snap bounds, grid cell selection, TSV parsing incl. old 7-column rows, area budget with the 100 limit, legacy chain reminder encode/decode and display. No fake tests of native map rendering; screens checked on the emulator in light/dark, default/2x, 320dp.
