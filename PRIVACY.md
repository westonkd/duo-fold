# Twofold privacy policy

Effective 26 October 2026. This policy covers the Twofold Home screen app published on Google Play as
`com.westonkd.folio` by Weston Dransfield, and builds of it made from this repository.

## The short version

Twofold collects nothing. It has no account, no ads, no analytics, no crash reporting service and no server, and it
does not ask Android for internet access, so it cannot send anything anywhere on its own. Everything Twofold knows about
you stays on your phone, in Twofold's private storage, and is deleted when you uninstall it.

## What Twofold reads on your phone, and why

Twofold is a Home screen, so it reads what a Home screen needs. None of it leaves the phone.

| What | Why | When |
|---|---|---|
| Installed apps, their icons and shortcuts | To show them on Home, in the App Library and in Search | Always; Android gives this to every Home screen |
| Widgets you add | To show them on Home | When you add one |
| Battery, Wi-Fi and cellular signal, ringer mode | For the status area and the battery ring. Twofold never reads the Wi-Fi network's name | While Home is on screen |
| Notifications (optional, "Notification access") | Badges on app icons, an app's recent notifications and media controls in its panel, what's playing | Only after you allow it. Notification content is kept in memory while it's shown and is never saved, logged or sent |
| Contacts (optional) | Finding people when you search | Only after you allow it, and only while you search |
| Calendar (optional) | The Up Next widget | Only after you allow it and add that widget |
| App usage (optional, "Usage access") | Suggesting the apps you use most, and dots beside dock apps you used in the last hour | Only after you allow it |
| Accessibility service (optional, "Twofold gestures & overlays") | Opening Android's notifications and Quick Settings from Home gestures, the system actions you give a gesture or button (Home, Back, Recents, close notifications, Android's app drawer, lock screen, screenshot, power menu, split screen, play/pause), and the dock handle, dock and Big Buttons over other apps if you turn them on | Only after you turn it on in Android's Accessibility settings. It cannot read what's on your screen or what you type, it doesn't observe what you do in other apps, and it collects no data |
| Do Not Disturb access (optional) | Gesture and button actions that change Do Not Disturb | Only after you allow it |
| Biometrics | Unlocking the list of hidden apps | When you open it. Twofold only receives "unlocked" or "not unlocked" from Android |

Twofold also remembers, in its private storage: your layout and settings, copies of any pictures you choose for an app
icon or a background, the apps you've launched recently (for Search suggestions), what your phone's hinge sensor can
report, and a few local problem reports (crashes, freezes, the app being closed by Android) with your phone model,
screen size and Twofold's last few internal events. An event can name an app you opened from Twofold or an app that asked
to add a widget to Home; problem reports never contain notification content, messages or contacts.

## When something does leave your phone

Only when you do it yourself:

- **Searching the web.** If you search the web from Twofold, your search text goes to the search engine you picked, in
  your browser or that engine's app. Their privacy policy applies.
- **Sharing a report.** Settings › General › About › Diagnostics can share a problem report or diagnostics (device details, Twofold's recent
  events and its own log lines) through Android's share sheet. You choose where it goes and can read it first.
- **Saving a layout backup.** Settings can save your layout to a file you choose, or to Download/Twofold. It can reveal
  which apps you have installed and your folder names. It goes nowhere unless you send it somewhere.

## Children

Twofold is a general-audience utility and is not directed at children.

## Your choices

Every optional permission above can be turned off in Android's settings at any time, and Twofold keeps working without
it. Settings › General › Privacy & Permissions in Twofold lists them. Clearing Twofold's storage or uninstalling it
deletes everything it kept.

## Changes and contact

If this policy changes, the new version is published here with a new date, and Twofold's release notes will say so.
Questions: open an issue at <https://github.com/westonkd/duo-fold/issues>, or use the developer email on Twofold's
Google Play page.
