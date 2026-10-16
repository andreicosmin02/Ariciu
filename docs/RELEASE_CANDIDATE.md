# Ariciu 1.1-rc1 — phone testing

Install `app/build/outputs/apk/debug/app-debug.apk` over your existing Ariciu installation. Do not uninstall or clear storage: reminders stay only on this phone. The APK must use the same debug signing key as your previous install. No account, key or payment is required.

You can create arrival/departure reminders, choose Silent/Normal/Urgent/Alarm, use Done/Next time or Alarm Stop/ten-minute Snooze, search Romanian towns and selected shops privately, select your current location, download small map areas, and remind yourself near selected branches of seven supermarket chains. Light/dark/System appearance, editing, saved drafts, recovery and local storage remain.

| What | Connectivity and supported limits |
| --- | --- |
| Saved reminders, search, lifecycle and actions | Stored/processed locally. Search is an incomplete Romania town/shop-address catalogue dated 16 Oct 2026, not every street address. |
| Chain reminders | Up to 40 nearest listed stores around the selected pin. Review/refresh when travelling. Android monitors at most 100 areas across the app; partial monitoring is shown. No continuous tracking or all-Romania coverage promise. |
| Maps | New areas need internet. Explicit downloads keep approximately 4×4 km at neighbourhood detail for the chosen theme, up to three packs. Resume/delete in Settings. |
| Detection and alerts | Google Play services, precise/background location, device Location and notification access are needed. Android, positioning and battery restrictions can delay or prevent delivery; offline maps do not guarantee offline detection. |
| Urgent/Alarm | Ongoing presentation and audio are requests controlled by Android/channels/DND. Alarm uses repeating system notification sound, Stop/Done/Snooze; opening notifications may stop the sound. Supported fallback is an actionable notification, with no automatic full-screen launch or exact alarm workaround. Snooze may be delayed. |

64 unit tests, 19 Android 17 emulator tests, lint and debug assembly passed. An actual map pack rendered with emulator Wi-Fi/mobile data disabled. Both themes and enlarged text were visually reviewed. These results do not establish real-phone geofencing, sound/vibration, reboot/OEM behavior or accessibility usability.

Follow [the non-technical phone checklist](PHONE_VERIFICATION.md) over the next few weeks. No additional product approval is pending for this documented candidate; broader search/store coverage, full-screen special access or a public release would require a separate decision. Technical evidence and licences are in [FEATURE_COMPLETION.md](FEATURE_COMPLETION.md).

APK SHA256: `e67b4867033b33c7462af33435a58ebe045fee1dd3c11430806c17e76cb4afe9` (2026-10-16 verified build).
