# Second phone review — rc2 observations and rc3 retest

Received 2026-10-17 from Samsung Galaxy S26 Ultra (SM-S948B), Android 16/API 36, One UI 8.5. Development is on `fix/phone-review-2`, based on clean `fix/phone-review-1` at `1b2eb13`. Candidate: **1.1-rc3 / code 4**. No master merge or publication. Source remains authoritative; the written tab structure guided implementation. No private image, log, location or notification text is committed.

## Every review item

| Item | Outcome and cause |
| --- | --- |
| 1. Search feedback | Implemented. The old produceState retained the previous list and used an empty list for both pending and empty. A dialog-owned controller now distinguishes catalogue loading, idle, searching, results and failures; query changes synchronously clear old matches, cancel old work and invalidate its revision. Material 3 inline progress and polite announcements cover both loading phases. Only completed current searches show No places found. Retry/close work; query restores across recreation. Local IO/CPU dispatchers and ranking unchanged; no artificial delay. |
| 2. Active/Completed | Implemented. Removed redundant headings, empty placeholder item and chip spacing. Horizontal accessible tabs share a border with the scrolling content panel; selected top accent, bold label, state semantics and count make selection explicit. Tabs remain fixed while content scrolls; notification targeting selects/scrolls to the matching list with corrected item offsets. Large text wraps inside horizontal tabs. |
| 3. Done action | Implemented. Outlined 48dp Mark done action, no checkmark. Completed remains a status indicator; existing repository transaction, occurrence guard, generation rotation and cleanup remain unchanged. |
| 4. Create spacing | Implemented. 8dp gap between Place/Chain selector and map/sheet region. Fixed primary action and draggable/scrollable sheet retained. Actual 320dp-wide/short-screen testing at 2× text exposed a clipped map/options region; compact layouts now scroll the header independently above a bounded map, use a shorter sheet summary and retain a fixed primary action. A 320×360dp regression fixture checks reachable options/radius and action. |
| 5. Settings status | Implemented. Status check moves from title to immediately before Review, centered with the action. Large text stacks description above a right-aligned action group. Review semantics include the row title and Allowed/Needs setup. |
| 6. Notification fox | Implemented. Existing small-icon resource replaced with original 24dp transparent white fox silhouette/cutouts, separately sized for status rendering. Launcher resources, app ID and channel IDs unchanged. Actual emulator shades captured; new Samsung icon rendering needs updated-build retest. |
| 7. Urgent | Android-limited, actual rc2 phone swipe verified. High importance, ongoing flag and delete intent were present on the real posted alert. Owner swiped it unlocked; read-only DB/notification inspection confirmed Next time state, alert cleared and monitoring retained. See exact evidence/limits below. Locked/Clear all and extended same-visit return observations remain pending. |
| 8. Notify now | Implemented opt-in for new single-place ENTER only, initially off. Save commits first; one bounded foreground precise fix may create a legitimate first occurrence in the existing engine/outbox. Failure does not fail saving. Details below. No chain/EXIT/edit/reconcile opt-in. |
| 9. Wireless debugging | Used read-only Samsung ADB for version, installed package, permissions, notification flags/channel/delete intent, relevant delivery warnings and swipe lifecycle. Connection later dropped; the owner explicitly requested emulator-only continuation. No phone settings, permissions, storage, clock, process state or location were changed. No phone instrumentation was run. |

## Actual Samsung evidence — rc2

ADB confirmed installed version 1.1-rc2/code 3 and Android 16/API 36/One UI property 80500. Fine/coarse/background location and notifications were granted. The live Urgent notification had channel `reminders_urgent_v1`, importance HIGH/4, `FLAG_ONGOING_EVENT`, a broadcast deleteIntent, category reminder and private visibility. Existing user channel settings were read, never reset. The old small icon resolved to the original pin resource, confirming the separate launcher/notification-resource cause.

Owner reported “swiped the old notification”. After that unlocked swipe: zero posted Ariciu notifications; the matching persisted reminder was `WAITING_NEXT_VISIT`, `INSIDE`, occurrence 1, notificationVisible=false, notificationPending=false, registration=REGISTERED. No AriciuDelivery warning/error entries were present. The notification remained absent at the follow-up snapshot; this is a point-in-time observation, not an extended no-repost guarantee or proof of a new real crossing. Database/WAL copies were read only after matching before/after checksums, queried read-only locally and never committed. No title, coordinates or reminder identity are retained in this record.

Android 14+ explicitly permits individual unlocked dismissal of ongoing notifications. Ariciu can request ongoing/high-importance presentation and implement safe Next time; it cannot force an unlocked Samsung user to use the notification buttons. Official ongoing protections apply while locked and to Clear all; actual Samsung checks of those two conditions are pending. Ariciu is not a call/media/DPC app and uses no exemption, repost loop, foreground service or full-screen workaround. Channel/DND/OS choices still control alerting.

## Immediate-arrival implementation and boundaries

The editor option appears only for a new single-place arrival and defaults off. A failed-save retry preserves the opt-in and stable reminder ID; switching to chain/EXIT or editing clears it. The choice is draft/SavedState only, not a Room column.

`ReminderRepository.save` returns the exact committed generation. After saving, the foreground adapter checks precise permission, device Location, Play services and selected notification channel, then requests one high-accuracy/fine fix. Provider cache age is at most 5s, request duration 18s, total attempt bound 20s. Stopping/destroying the visible Activity cancels its child request and provider token; ordinary cancellation is not presented as a save failure. No continuous updates/background tracking or new permissions.

A fix must have finite/valid coordinates and accuracy, same boot, monotonic age 0–10s and accuracy at most 50m. Distance + twice reported accuracy must fit inside the selected radius. This is a conservative heuristic, not a guaranteed physical boundary: Android accuracy is an estimated uncertainty. Stale, uncertain, outside, missing/null, timed-out and unavailable fixes do not request an alert. All outcomes explain that the reminder was saved; setup remains available in Settings.

`DeliveryWork.immediateArrival` shares the worker runtime mutex. It rechecks foreground/capabilities, the exact ID/generation and fresh evidence. `DeliveryEngine.immediateArrival` uses a Room transaction, only occurrence 0/WAITING/single ENTER, and the normal lifecycle transition. Position becomes INSIDE; occurrence increments once and visible/pending outbox state persists before ordinary WorkManager dispatch. Exact-generation conditional writes reject concurrent completion/edit/delete. No standalone fake notification is posted. The selected intensity and original real notification actions apply.

Duplicate initial attempts, same-position ENTER and later reconciliation do not create another occurrence. Next time and Every visit require an actual opposite crossing before a subsequent visit. The explicit initial attempt is never replayed on restart/reconcile; only an already-persisted pending occurrence can recover through the existing outbox. Geofence registration remains `setInitialTrigger(0)`; 100-area allocation and saved chain snapshots remain unchanged. An interrupted pre-mutation check can be skipped; Room/outbox/WorkManager/Android cannot share one atomic commit and platform dispatch may still be delayed. Leaving the UI or losing permission during the attempt may skip it while the reminder remains saved.

## Official sources checked 17 October

- [Android 14 ongoing-notification changes](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)
- [GeofencingRequest initial triggers](https://developers.google.com/android/reference/com/google/android/gms/location/GeofencingRequest)
- [FusedLocationProviderClient single-fix and cancellation contract](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [CurrentLocationRequest age/duration/granularity](https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest)
- [Android Location accuracy](https://developer.android.com/reference/android/location/Location#getAccuracy())

No dependency, service, permission, Room schema/migration, signing identity, notification identity/channel or recurring-cost change. No continuous GPS, exact alarm, full-screen or background-service addition.

## Executed verification

On 17 October, `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` passed: **82 unit tests, zero failures/skips; lint zero errors/39 warnings; debug and test assembly passed**. The complete installed instrumentation suite passed **35/35** on `emulator-5554`, Android 17/API37.2, via `adb -s emulator-5554 shell am instrument -w ro.andrei.ariciu.test/androidx.test.runner.AndroidJUnitRunner`. `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest` also passed all **35 tests**, zero skipped/failed (1m27s). Only the emulator was connected during connected testing; no daily-use phone instrumentation.

Focused coverage includes seven search-controller unit cases and three dialog cases (catalogue/query progress, completed empty state, failure/retry, rapid typing/clearing, late noncooperative results, cancellation and state restoration); large-font tab selection/count/target navigation and completion; Settings action geometry/semantics; a 320×360dp Create fixture with 2× text; notification delete/Next time/Done occurrence identities and stale actions; transparent small-icon raster bounds and real notification posting; arrival freshness/accuracy/boot/generation/eligibility, duplicate dispatch, opposite-crossing rearm; save-first error/retry/default-off/edit/chain/EXIT behavior; bounded provider cancellation/foreground checks; actual Room/outbox/WorkManager notification and swipe lifecycle with injected location evidence. Existing migration tests also passed; no schema change was made.

Intermediate test failures were corrected before the final gate: a test still tried scrolling the now-fixed Completed tab; an async receiver assertion checked its queue before the lifecycle mutation finished; the system-theme screenshot fixture reused a dismissed alert and captured a transition. Assertions now await observable lifecycle state, and each shade capture posts a fresh fixture. No production delays were added. A final compact-screen regression checks the clipping found during visual review.

Actual captures in [design/screenshots/review-2](design/screenshots/review-2) show both app themes, selected Active/Completed panels, Mark done, Settings/action alignment, 2× text, immediate option off/on and native light/dark system notification shades. They were visually inspected, alongside search progress and expanded/collapsed Create. The system shade may choose the app's full-color badge; resource assertions independently verify the dedicated transparent small icon. Samsung small-icon tinting remains pending. These are review artifacts, not golden rendering tests or proof of real positioning.

Installed-MainActivity manual check also exercised the actual FusedLocationProviderClient adapter with emulator-injected positions (not injected directly into the engine). A panned pin correctly produced the saved/outside message and persisted WAITING/UNKNOWN, occurrence 0, no visible/pending alert. A new reminder centered with My location produced the saved/inside message, TRIGGERED/INSIDE, occurrence 1, visible=true/pending=false, and an actual HIGH/ongoing Urgent notification on the unchanged channel. This validates the foreground provider-to-outbox wiring with simulated evidence, not real GPS accuracy or real crossings. The short-screen check used system font scale 2.0 and 840×1470 display at density 420 (320dp wide); map/Options/radius and fixed primary action were reached by scrolling/expanding. Emulator font/display settings were restored.

Manual Android 17 notification-shade check: Clear all retained the actual ongoing Urgent alert; an individual unlocked swipe then cleared it and persisted WAITING_NEXT_VISIT/INSIDE, occurrence 1, no visible/pending alert. No Ariciu alert returned during a follow-up exceeding one minute at the same simulated position. This is a bounded emulator observation, separate from the earlier rc2 Samsung snapshot; it does not guarantee future OEM behavior or real crossing delivery.

Older Android runtime behavior, TalkBack use, real GPS/crossings, OEM delivery timing, sound/vibration and rc3 physical-phone verification remain unexecuted. Earlier rc2 68/23 results are historical, not proof for this source.

## Focused phone checklist

1. Install rc3 over rc2 with the same signing key. Never uninstall/clear storage. Check existing reminders remain.
2. Search: type/change/clear rapidly, try an impossible query and rotate while typing. Loading and empty results must be distinct; old results must not become final. Check offline search and both themes.
3. Reminders: Active/Completed stay horizontal, connected and visibly selected while scrolling; counts update. Mark done completes the chosen reminder and removes monitoring/alert state asynchronously. Tap a notification to verify correct list/scroll target. Repeat larger font/display sizes.
4. Create: inspect the small map gap and drag/expand/collapse on a short screen. Settings: check status immediately before Review and its stacked large-text layout.
5. At a safely stationary location, use My location and a 200–500m radius, choose Arrive, a harmless test title and Urgent, then explicitly enable Notify me now. Save while Ariciu remains open. Note the result message. If a fix confidently confirms inside, expect a real actionable alert and new fox status/shade icon. Default off, Leave, chains and edits must not offer an immediate alert. Poor positioning should explain a skipped attempt while retaining the reminder.
6. For a delivered Urgent alert: check locked-screen swipe first, then unlock; Clear all should retain ongoing alerts under documented Android behavior (do this only when willing to dismiss other notifications). Swipe the individual alert unlocked: expect Next time and no same-visit re-alert. Leave well beyond the area, return and allow Android time; Every visit should trigger once. Check card Mark done and notification actions, then ordinary background/reboot/OEM scenarios from PHONE_VERIFICATION.md with the owner's explicit cooperation.

## Commits

- `607abcd`: search loading/stale-result guards, connected tabs/Mark done, Create gap and Settings alignment with focused unit/UI coverage.
- `e071f78`: dedicated small notification fox and occurrence/dismissal identity coverage.
- `6e8a1fc`: recreation-safe search query and rejection of late cancelled search completion.
- `cb3c0bd`: reachable compact Create map/options and large-text short-screen regression.
- `058ec81`: save-first foreground immediate-arrival lifecycle, bounds/cancellation and integration tests; rc3/code 4.
- The documentation/evidence commit follows these implementation commits on the same branch; no merge/publication.
