# Codex handoff — second phone review

Updated 17 October 2026. Current candidate **1.1-rc3/code 4**, branch **fix/phone-review-2**, based on clean fix/phone-review-1 at `1b2eb13`. Inspect actual Git/history/worktree before continuing; nothing merged/published.

Read AGENTS.md, PROJECT_STATUS.md, PHONE_REVIEW_2.md, PHONE_REVIEW_1.md, STABILIZATION.md, STABILIZATION_AUDIT.md, DELIVERY_ARCHITECTURE.md and design/DESIGN.md. Owner authorizes focused autonomous debugging, verification, commits and fix-branch push. Preserve data/unrelated edits; no master merge, force-push or publication.

## Second-review changes

Search moved from ambiguous produceState to dialog-owned PlaceSearchController, explicit current-query states, progress/error/retry and revision/cancellation guards. Query stays saveable; IO/catalogue and Default CPU ranking remain private/offline. Tests cover late noncooperative results, clearing, failure/retry, cancellation and recreation. No artificial loading delay.

Reminders use fixed horizontal tabs connected to a bordered scrolling panel, no redundant headings/empty placeholder. Corrected notification scroll offsets; selected/count/Tab semantics and large-text wrapping. Mark done has no checkmark and uses the existing safe completion transaction. Settings status is beside Review, with row-specific accessible action/state and stacked larger text. Create selector/map gap: 8dp; short screens with larger text scroll the header above a bounded map and use a compact Options summary, retaining the fixed primary action. Dedicated white/transparent fox small-icon resource; launcher and channels remain.

New draft notifyOnSave flag is SavedState only. Eligible only for new single-place ENTER; off by default, cleared for chain/EXIT/edit. New-save retries keep ID and eligibility via newSave saved-state marker. ReminderRepository.save now returns its exact committed generation; all fakes/callers are updated. Room schema remains 4.

ForegroundArrivalCheck is created per current Compose lifecycle, passed to save rather than retained inside the ViewModel across Activity recreation. Save commits first, then a 20s bounded foreground precise fix (provider age 5s/duration 18s). ON_STOP/DESTROY cancels the provider child/token. Result explains a saved reminder even when unavailable/uncertain; saving failure and location-check failure are distinct. Engine requires same boot, age 0–10s, accuracy≤50m and distance+2×accuracy≤radius, exact request generation, WAITING/occurrence 0/single ENTER. DeliveryWork.immediateArrival shares the worker runtimeMutex, persists normal lifecycle/outbox and enqueues ordinary dispatch. It is not called by reconciliation/restart. No new Room fields, continuous GPS or global initial trigger.

## Phone evidence and restrictions

Read-only ADB on Samsung S26 Ultra/Android 16/One UI 8.5 confirmed rc2/code 3 permissions, actual HIGH/ongoing Urgent/deleteIntent and old pin small icon. Owner swiped; DB WAITING_NEXT_VISIT/INSIDE/occurrence 1, no visible/pending alert, registration REGISTERED, no active alert/warnings at snapshot. This is physical evidence for that swipe only. Private read-only database/WAL/log/screenshot artifacts stay outside Git; documentation contains no reminder title/ID/coordinates/ADB credentials.

Phone disconnected; owner explicitly said to continue on emulator. Do not attempt further phone operations without renewed context. Never uninstall/clear data, run daily-use-phone instrumentation, change permissions/settings/clock/location, force-stop or reboot it without explicit permission. Commands that install/run tests must target emulator-5554 explicitly; do not rely on an unqualified adb command or all-device Gradle test task. No rc3 phone upgrade/retest occurred.

Android 14+ unlocked individual dismissal is permitted. Ongoing/HIGH and safe Next time remain; locked/Clear all and new Samsung fox/immediate behavior still need phone retest. No call/media/DPC exemption or repost loop. Official Android/Google sources and exact evidence are in PHONE_REVIEW_2.md.

## Preserved delivery invariants

Room 4/migrations unchanged; exact-generation conditional writes, occurrence/visible action guards, journal-before-add/removal retry, stable notification tags/outbox, both crossings and initialTrigger0, 100-area allocation, conservative union rearming. Card Done rotates generation and cleanup remains asynchronous. Completed edits stay completed. Search: 26,065, chains new/refresh max 40 within 25km, older snapshots explicit refresh; small theme-specific offline packs unchanged. No dependency/permission/backend/cost added.

## Verification and continuation

Final executed commands/counts/screenshot review and intermediate fixture corrections are in PHONE_REVIEW_2.md. Use android-milestone/android-verification skills; unit+lint+assemble gate, emulator-only instrumentation. Installed MainActivity + actual foreground GMS adapter was exercised with emulator positions: outside skips; inside creates occurrence 1 and real Urgent via outbox. This does not validate real GPS/crossings/audio/OEM/TalkBack/older Android. Inspect current artifact signature/hash in RELEASE_CANDIDATE.md; install over existing app with same key.

Repository and existing Obsidian focused notes are synchronized after this batch; compare same-named notes before future overwrite and preserve private originals. The essential repository handoff must work independently of vault access. Next work is rc3 phone retest and actual reported failures, not speculative refactoring/features.
