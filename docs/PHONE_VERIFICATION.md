# Ariciu: first install and a few weeks of phone testing

Use a phone with Google Play services and Android 8 or newer. This is the 1.1-rc1 debug release candidate, not a Play release. No dated physical-phone results have been recorded in this repository handoff (2026-10-17). No account, key or payment is needed.

## Install without losing reminders

1. Copy `app/build/outputs/apk/debug/Ariciu-1.1-rc1-debug.apk` (or the identical `app-debug.apk`) to your phone and open it. Android may ask you to allow installation from the app opening the file. Install over the previous Ariciu build; **do not uninstall or clear storage**. Your reminders are kept only on this phone, without cloud backup. An upgrade must be signed with the same debug key as the previous install. If Android rejects it, keep the installed app and report the error.
2. Open Ariciu → Settings. Choose System, Light or Dark. Close/reopen: the choice should remain. Existing reminders should still be present.
3. Set up location while using the app, precise location, location all the time, notifications and device Location, one step at a time. Declining should still let you create/manage reminders and should show setup help. Saving a reminder alone does not prove monitoring is ready.

## Everyday creation and appearance

- In Create, try Search places: `Brașov`, a store name, or a known store street. Search covers towns and selected shops/store addresses in Romania, including seven supermarket chains. It is private/offline, incomplete and dated; it is not every street address. Check the pin against the map and move it if necessary.
- Try My location. Decline once, then allow it. Try with phone Location off and in a poor-signal place. It should explain unavailable positioning and allow search/map selection. Approximate positioning is accepted for map centring; reminders require precise/background access.
- Choose 100/200/500m and More distances. Add text, Arrive/Leave, Just once/Every visit and an alert intensity. Use a radius of 200–500m for initial real tests. Save and edit. Completed reminders stay completed on edit.
- Try both themes, the phone’s largest font and larger display size, keyboard open, and landscape. Controls/Save should remain reachable by scrolling. Test TalkBack reading order, headings and radio choices; test external keyboard Tab/Enter if available. Report clipped text, unreachable controls or confusing descriptions.

## Real arrivals, departures and actions

1. Create one arrival reminder and one departure reminder for a nearby safe walking destination. Close the UI and lock the phone. Cross well beyond the radius, wait a few minutes, then return. Never test while driving yourself. Registration while already inside should not alert immediately.
2. Stay inside after an arrival: there should be no duplicate alert. Just once remains triggered until handled; Every visit needs leaving and returning. Departure reminders need returning inside before the next departure.
3. Done completes the reminder. Next time and swiping wait for a different visit. Tapping a notification opens/highlights the matching reminder. Editing changes its monitoring identity; an old notification action must not complete a newer occurrence. Deleting stops its monitoring when synchronization succeeds.
4. Test all alert types with the phone locked and unlocked:
   - Silent: notification list only, no sound/vibration/pop-up with the default channel; Done/Next time and swipe work.
   - Normal: existing standard alert; sound/pop-ups follow Android settings.
   - Urgent: prominent ongoing request with Done/Next time; there is no separate custom stronger tone/vibration than Normal. Android 14+ may let you dismiss it; dismissal is Next time and should not immediately reappear.
   - Alarm: explicitly choose it in the editor. Check repeating system notification sound and strong vibration where allowed. Opening the notification panel may stop repeating sound. Stop should silence while keeping Done/Snooze; Done completes; Snooze should alert again after about ten minutes, possibly later if Android delays work. Close/kill the app after Snooze and test again. There is no automatic full-screen alarm or continuous app audio service. If Android suppresses audio, the notification/actions are the supported fallback.
5. In Settings → Sound and alert settings, mute/block one intensity channel. That type should show setup help without blocking other enabled types. Restore it. Test Do Not Disturb and lower volume: Ariciu respects the system and cannot guarantee a heads-up alert.

## Chains and offline maps

- Choose a pin near your usual shopping area, then “Use a supermarket chain instead”. Review the selected store list. This selects **up to 40 nearest listed stores around that pin**, not every Romanian store. Missing/outdated shops are possible. Change the pin and refresh stores when travelling.
- Visit two selected stores on different occasions. Done completes the whole chain reminder. Every visit rearms after leaving all selected store areas; overlapping stores count as one visit. Missed exits can delay rearming. If many reminders consume the 100-area limit, some stores may be on hold; the app should say so and activate more when capacity becomes available.
- In Create → Location details → Download this area, explicitly download a small area. Keep Create open until complete, or go to Settings → Offline maps and Resume. Check the theme, MB/resource usage, progress and pause/delete controls. Leaving the screen or backgrounding the app pauses unfinished work; a process restart retains the incomplete pack for Resume.
- Turn off Wi-Fi and mobile data. In the downloaded theme, reopen the area at neighbourhood detail: roads/labels and location selection should work. Unvisited areas/other themes or detail levels may be blank. Search still works. Offline maps **do not guarantee offline Google Play services positioning**.
- Delete a pack. Its protected offline resources should be released; shared resources and ordinary recently viewed map cache may remain until cache cleanup. Saved reminders must remain.

## Recovery and several weeks of use

- Reboot and unlock the phone. Allow Android time, then repeat a real crossing. Already-delivered alerts should restore quietly; a snoozed alarm should recover its deadline. Test an app update over the existing install.
- Close the app normally and remove it from recents. Continue using reminders. Android “Force stop” is different: reopen Ariciu afterwards before expecting delivery.
- Revoke a permission, turn Location off, disable a channel, or enable battery restrictions; return to Ariciu and check truthful status and working Settings shortcuts. Restore access and Check reminders again. Android/OEM battery restrictions, poor positioning and job quotas may delay or prevent delivery.
- Keep a short log for 2–3 weeks: device/Android version, task/place/radius/intensity, approximate crossing time, actual alert time, locked/unlocked, connectivity/battery mode, and what felt confusing. Include screenshots for UI problems. Log missed or duplicate alerts, and whether reopening Ariciu changed anything.

No physical-phone delivery, reboot recovery, OEM behavior, audible/vibration behavior, TalkBack/Switch Access or older-user usability is claimed verified by emulator tests. See PROJECT_STATUS.md for exactly which automated and emulator checks passed.

## When you find a problem

Keep Ariciu installed and preserve its storage. Record what you expected, what happened, steps to repeat it, date/time, phone/Android version and whether the phone was locked or the app was closed. For alert issues add intensity, Arrive/Leave, Just once/Every visit, radius and relevant Location/notification/DND/battery settings. For UI issues add theme/font/display size and a screenshot if comfortable; do not share private addresses unnecessarily.

The next development session will focus on these reports, reproduce the issue and fix its cause with regression checks. [Stabilization workflow](STABILIZATION.md) explains the developer process; a [copyable issue template](<obsidian-export/Ariciu/Ariciu - Phone Issue Template.md>) is available. A source hypothesis, emulator check and real-phone retest are recorded separately. The current [release note](RELEASE_CANDIDATE.md) identifies the rebuilt APK/hash.
