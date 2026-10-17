# Second phone-review screenshot evidence

Captured and visually inspected on 17 October 2026, Android 17/API37.2 emulator (`emulator-5554`), rc3 source. Dummy test content only; no personal-phone images.

- `reminders-dark`, `reminders-light`, `completed-dark`: connected tab borders, selection/counts and outlined Mark done.
- `tabs-large`, `settings-large`, `arrival-large`: 2× Compose font-scale fixtures with reachable controls; not TalkBack verification.
- `settings-dark`, `settings-light`: status/check immediately before Review.
- `arrival-off-light`, `arrival-on-dark`: explicit optional arrival check, initially off.
- `search-query-loading`: a deliberately deferred local search demonstrates the real progress state; not a search performance measurement. Catalogue loading is also covered in dialog tests.
- `create-dark`, `create-light`, `create-expanded`, `create-large`: actual map/sheet layouts and 8dp gap; native tiles are not asserted by tests.
- `create-compact-map`, `create-compact-expanded`: installed MainActivity, emulator display 840×1470 at density420 (320dp wide) and system font scale2.0. Header scrolled to the map; radius/options and primary action remain reachable. System size/font settings were restored afterward. Narrow navigation labels wrap.
- `notification-dark`, `notification-light`: real native system shades with a freshly posted Urgent fixture per theme. Android chooses its app badge; independent tests verify the dedicated white/transparent 24dp small-icon resource and actual notification resource identity. Samsung small-icon tinting remains pending.

See [review dispositions, actual checks and limitations](../../../PHONE_REVIEW_2.md). These are review artifacts, not golden tests, physical-phone verification or evidence of real GPS/geofence delivery.
