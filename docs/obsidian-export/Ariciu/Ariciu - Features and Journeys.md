---
project: Ariciu
reviewed: 2026-10-17
implementation_baseline: fix/phone-review-1
version: 1.1-rc2
---

# Features and user journeys

Related: [[Ariciu - Reminder Lifecycle]], [[Ariciu - Notifications and Alarm]], [[Ariciu - Design and Accessibility]].

## Create a single-place reminder

1. Create opens the map around Brașov by default (45.6579, 25.6012, zoom 14), or restores the selection.
2. Search Romanian towns/shops/store addresses, use My location, or move the map. Search results centre the map; they do not save a named address into the reminder.
3. Expand the draggable place sheet and choose 100/200/500m, or More distances for 100–1000m in 100m increments. The centre pin is the selected point; the boundary is approximate visual geometry.
4. Use this place opens the editor. It is disabled during native camera movement.
5. Enter text, choose Arrive/Leave and Just once/Every visit; Alert opens explicit Silent/Normal/Urgent/Alarm choices. Save requires valid nonblank text/location and a valid selected chain if applicable.
6. Save persists locally, clears the draft and opens Reminders. Monitoring is synchronized separately; read the card status and Settings.

My location accepts approximate foreground access for centring. Actual reminders require precise/background access. Declining does not disable manual creation or local search.

## Use a chain

Choose Single place or Supermarket chain on Create before search/confirmation, or change it in the editor. New selections record up to 40 listed stores within 25 km. View selected stores shows a real map and an accessible name/address/distance list. Change place and refresh when travelling. This is a stored selection, not live detection of all national branches.

## Manage saved work

Reminders separates Active and Completed. Cards show task, crossing, radius/repeat/intensity, optional chain count and honest status. Edit opens the existing draft; Delete asks before removing the local record. An existing draft is protected before replacing it to edit another reminder. Editing preserves completion and occurrence state.

Create resumes a named draft after tab changes. Back/Cancel may require discard confirmation. A notification tap opens/highlights the relevant list item without deliberately discarding the existing draft. Internal routes remain MAP/LIST/DELIVERY/EDITOR for compatibility.

## Respond to a reminder

| Action | Behavior |
| --- | --- |
| Done | Completes the whole reminder, including every selected store; synchronization removes monitoring. |
| Next time | Hides this occurrence and waits for a distinct visit; it is not a timed snooze. |
| Swipe | Uses the same Next time action, including dismissible ongoing alerts. |
| Alarm Stop | Requests immediate platform silence, then persists quiet state; Done/Snooze remain. |
| Alarm Snooze | Hides the occurrence and stores a ten-minute deadline; the later alert can be delayed by Android. |
| Tap | Opens the matching reminder in the list; does not itself complete the task. |

## Settings and offline viewing

Settings combines appearance, staged delivery setup, collapsed per-intensity channel settings and offline map management. No initial permission prompt is automatic. Foreground, background and notifications have separate explanations/requests. Check reminders again retries synchronization; a capability summary is not proof of a particular geofence being registered.

Download this area lives under Create → Location details. Confirm a small pack for the current theme. Keep Create open, or Resume from Settings after tab changes. Settings shows progress/resource MB and deletion. Other themes/areas/zoom levels may still need internet.

## Failure and empty states

The app presents initial empty lists, loading reminders, save/load/delete errors, unavailable catalogue/map/positioning, denied setup and registration failure/capacity statuses. Save failures preserve draft identity. Error handling is implemented; not every provider/OS/device failure path has automated or physical coverage. Known copy weaknesses are listed in [[Ariciu - Risks and Technical Debt]].


Phone review changes and current evidence: [[Ariciu - Phone Review 1]].
