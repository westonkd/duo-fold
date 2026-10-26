# Permissions

Every permission and special access Folio declares, what uses it, and what happens without it. Home works with all
of the optional ones refused ([PRV-1](docs/standards/privacy-permissions.md)). Folio declares no `INTERNET`
permission. What Folio does with the data is in [PRIVACY.md](PRIVACY.md).

## Granted with the app

| Permission | What uses it | Without it |
|---|---|---|
| `VIBRATE` | Haptics, including the fold landing (`FoldTransition.kt`) | Nothing to grant |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Wi-Fi and cellular signal in the status area (`DeviceStatus.kt`). The network's name is never read | Nothing to grant |
| `REQUEST_DELETE_PACKAGES` | Delete App from an icon's menu (`AppRemoval.kt`). Android shows its own confirmation every time | Nothing to grant |
| `USE_BIOMETRIC` | Unlocking the hidden apps list (`CustomizationSheet.kt`) | Nothing to grant |
| `BIND_APPWIDGET` | Adding widgets (`WidgetController.kt`). A Play install never holds it, so Android asks per widget | Android's bind dialog |

## Asked for in context

| Access | What uses it | Asked from | Without it |
|---|---|---|---|
| Home app role | Being the Home screen | The Preview bar's Use as Home, Settings | Folio opens as a preview |
| `READ_CONTACTS` | Contact results in Search (`Spotlight.kt`) | The Search your contacts row in Search | No contact results |
| `READ_CALENDAR` | The Up Next widget (`UpNext.kt`) | The Up Next card | The card asks for access |
| Notification access (`NotificationListener`) | Badges, an app's notifications and media controls in its panel, what's playing | A disclosure, then Android's settings (`PermissionDisclosure.kt`) | No badges or media |
| Accessibility service (`SystemShadeAccessibilityService`) | Pull-downs to Android's notifications and Quick Settings, gesture and button actions, the dock and Big Buttons over other apps | The Turn On Folio Gestures disclosure (`MainActivity.showShadeSetup`) | Those gestures and overlays are off |
| Usage access (`PACKAGE_USAGE_STATS`) | Better suggestions, recent-app dots on the dock (`Suggestions.kt`, `LayoutHistory.kt`) | A disclosure, then Android's settings | Suggestions count only launches from Folio |
| Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`) | Gesture and button actions that change Do Not Disturb (`FolioActions.kt`) | Privacy & Permissions | Those actions do nothing |

## Components other apps can reach

| Component | Why it's exported |
|---|---|
| `MainActivity` | The Home intent, and Android's Home app gear (`APPLICATION_PREFERENCES`) |
| `PinWidgetActivity` | Android delivers widget and shortcut pin requests to it |
| `FolioSettingsActivity` | Opens Folio's settings from Android's Accessibility, Quick Settings and wallpaper screens |
| Quick Settings tiles, the wallpaper service, the notification listener, the accessibility service | Each is guarded by the system permission Android binds it with |
| `ButtonsRoomProvider` | Answers one number, how much room Big Buttons take at the bottom, so a keyboard can stay clear of them. Read-only, no personal data |
