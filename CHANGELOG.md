# Changelog

All notable changes to Folio. Versions follow [Semantic Versioning](https://semver.org) (MAJOR.MINOR.PATCH; 0.x while
Folio is in development), and this file follows [Keep a Changelog](https://keepachangelog.com). The app's
`versionCode` is derived from the version name ((MAJOR × 10000 + MINOR × 100 + PATCH) × 10 + HOTFIX), so every release sorts correctly and a fix on top of one has nine numbers of its own.
Folio shows the newest section on the phone after an update, and every version under Settings › What's New › Version History.

## [1.0.3] - Unreleased

### Fixed
- **Widgets stay on Home:** if you opened Twofold from its app icon or from Google Play before making it your Home app, widgets you added to Home no longer turn into "Widget unavailable" a few minutes later.

## [1.0.2] - 2026-10-31

### Removed
- **One wallpaper, the phone's own:** Home always shows the wallpaper you set in Android, live wallpapers included, and Settings › Home › Wallpaper is just Change Android Wallpaper and Wallpaper Moves with Pages. Twofold's own backgrounds are gone: the photo picker, the two Hiroshige prints and the Twofold background live wallpaper. If you had chosen a Twofold background, Home switches to your Android wallpaper, and the photo Twofold kept for it is deleted from the phone. If you had set the Twofold background as your phone's wallpaper, Android goes back to its default one.

## [1.0.1] - 2026-10-31

### Added
- **A new widget gallery:** tap + while editing Home and the gallery slides up over Home with every app that has widgets. Choose one to swipe through its widgets at the size they'll be on Home, pick Small, Medium or Large, and tap Add Widget to put it in the first free spot. Unfolded, the apps and their widgets sit side by side, and Twofold's own widgets show what they really look like.

- **A hello on first launch:** the first time Twofold opens, the dock and your apps settle onto Home one after another, starting next to the Side Bar.
- **Search holds a second search:** touch and hold the round Search button for the other search, Twofold's own Search when the button opens Google and Google when it opens Twofold's.

### Changed
- **The Ring notices things:** plug in and the battery Ring fills up in green, and the bell rings for a moment when you turn on Silent mode.
- **Home stretches at the ends:** swiping past the first or last page pulls Home a little way and lets it spring back, instead of stopping dead.
- **Badges come and go:** a new notification badge pops onto its icon, and one you've read shrinks away instead of vanishing.
- **The App Library catches the light:** the App Library has its own light, up in its top corner, and the edges of each category's glass catch it from wherever they sit. Tilt the phone and the light moves with it. Reduce Motion and Battery Saver keep it still.
- **The Search button presses like an icon:** it dips under your finger like the apps around it, instead of showing a ripple.
- **A new app icon:** Twofold's icon is now two glass panels opening on a sunset gradient, the same one you see on Google Play and the website.
- **Brighter glass edges:** glass in Twofold now catches the light on two opposite corners so the App Library, folders, the dock and menus stand out more clearly from the wallpaper.
- **App Library search:** the search field is taller and clearer, and its magnifier and "App Library" sit in the middle until you tap it.
- **Search matches the App Library:** the search field at the top of Search is the same light glass capsule as the App Library's, instead of a dark one.
- **The privacy policy on Twofold's website:** Settings › General › About › Privacy Policy opens it at twofoldlauncher.app instead of on GitHub.
- **Settings sidebar:** unfolded, the Settings list sits right on the background, and the open page is marked with a soft grey capsule instead of a teal highlight. On the cover screen, the lines between rows start at the row's name.

### Fixed
- **Labels while editing Home:** on the cover screen, the bottom row of apps keeps its names while you edit Home.
- **Smoother return to Home:** swiping up from an app, Home settles into place without stalling and jumping, whichever app you opened.
- **Widgets that came out short:** a widget drawn as a card a little smaller than its spot on Home, like Tesla's, now fills the spot, so it lines up with the widgets beside it and catches the light on its glass edge like they do.
- **Dropping a widget where it doesn't fit:** a widget held out of the gallery and let go where there's no room goes to the next free spot instead of disappearing.
- **A full dock on any phone:** the first time Twofold starts, the dock and Home get your phone, browser, messages and music apps, and the usual everyday apps, whoever made the phone. Before, Twofold only knew Samsung's and Google's apps.
- **Fold settings only on foldables:** Settings › Fold, with Page Landing, shows only on a phone that folds. Stay Awake on the Cover Screen shows only on Samsung foldables, the phones that need it.
- **Cloned apps:** a second copy of an app is labelled Clone under its name, Android's own word for it.
- **Notes that fit every phone:** the wallpaper and navigation notes in Settings no longer point to Samsung's menus.

## [1.0.0] - 2026-10-26

### Added
- **Twofold on Google Play:** a two-panel Home screen with a Side Bar, a widgets page, the App Library, search and folders, on the cover screen and unfolded, and Home landing like a page when the phone opens or shuts.
- **Clear permission requests:** before Twofold opens Android's settings for its gestures service, notification access or usage access, it says what that access is for and what it never does, and you choose Agree or Not Now.
- **Privacy policy and licenses:** Settings › General › About links to the privacy policy, and Credits & Licenses names McCal, who created Folio, and shows the licenses Twofold ships with.

### Changed
- **Settings in light:** Settings, What's New, alerts and every sheet now use the same light look as the rest of Twofold, with notes under each group instead of inside it, one accent color for actions, and row names written the same way everywhere.
- **Alerts with capsule buttons:** Twofold's alerts, including the permission requests, show their choices as rounded buttons side by side, with the one you probably want in bold.
- **Diagnostics in one place:** crash reports, the performance log and recent activity moved from General to General › About › Diagnostics.
- **Search, by name:** the search panel is called Search everywhere in Twofold, including its Quick Settings tile and the Swipe Down on Home menu.

### Fixed
- **Wide tablets:** on a screen wider than about 1200dp, the edge of the App Library no longer shows beside the dock while you're on Home.
- **Widgets with large text:** with Android's font size turned up past 130%, Twofold's Clock, Date and other widgets keep their text at 130% so the time fits instead of being cut off.
- **Permission requests on every phone:** where a phone has no settings page for Twofold's own notification or usage access, Agree opens the general list instead of doing nothing.
- **Less in problem reports:** a crash report keeps where Twofold failed but no longer the error's own text, which could name a file or a contact, and the Up Next calendar is only read when a stack holds that widget.

## Before Google Play

Folio's history before it was published on Google Play: McCal's releases, and this fork's own builds that were never
released. The app shows only the sections above this one.

## [0.6.9] - Not released

### Added
- **A widgets page left of Home:** on the cover, swipe right from Home 1 for a page that holds only widgets. Open the phone and it is on the left half beside Home 1, like iPhone Duo. An empty one shows an Add Widgets button; widgets move, resize and stack there like on Home, but apps and folders stay on the Home pages. The page dots and the bottom dock step aside while it is showing.
- **The page landing:** the fold moves like a book. Close the phone and you feel a click and a thud the moment the hinge shuts; the cover lights partway through its fall and drops shut faster and faster, stopping dead with no bounce, the crease darkening as it shuts. Open it flat and the moving half springs up off the spine with a crisp tick, then settles slowly with no second bounce, the shade in the crease clearing last. It plays when the hinge gets there, not on a timer. Settings › Fold › Page Landing turns it off, and Reduce Motion keeps only the haptics.
- **Home comes back from the app:** leaving an app, Home settles in from a little closer around the icon you opened it from, instead of just appearing. Off with Reduce Motion.
- **Settings pages push and pop:** opening a page in Settings slides it in from the side and Back slides it away, like iOS, instead of swapping the content in place. With Reduce Motion they fade.
- **Side Bar status without the capsule:** the time, signal and battery on the Side Bar now sit straight on the wallpaper in the same text color as the rest of Home, like iPhone's status bar, instead of in their own frosted pill with text that could come out dark when everything else was white. Tap them to open Android's Quick Settings, the same as pulling down from the top right. The dock's corners now follow the icons inside it, the icon's corner plus the gap around it, like iPhone's dock, instead of a full capsule.
- **Side Bar Ring like iPhone's:** with Wi-Fi and a cellular signal, the Ring's cellular dots are now part of the ring, curving along its bottom where the ring opens, instead of a row inside it under the Wi-Fi symbol, which now sits in the middle.
- **Spotlight in the App Library's glass:** Spotlight now sits on the same light or dark wash as the App Library, with its cards in Home's tinted glass and its text following Home's text color, instead of dark gray cards on a dark scrim. The search field is the App Library's, and Top Hit and app results use the same larger icons with iPhone's corners, spaced like Home's grid.
- **App icons with depth:** icons now have iPhone's smooth continuous corners instead of plain rounded ones, everywhere they appear. Each icon also gets a bright edge catching Home's light, a soft light from above, a little shade at the bottom and a faint tint from your wallpaper, so it sits on Home like iOS 26 icons instead of looking flat. The live Calendar and Clock icons are now the same size as the icons around them.
- **Liquid glass:** Home's glass is Liquid glass. In the App Library the wallpaper blurs behind everything, like iPadOS, and the category folders and search field are the only glass on it (where Android can't blur, a soft tint stands in). Widgets, folders on Home, the Side Bar and the dock take a soft tint from your wallpaper's colors, a light from above, a fine grain and a bright edge instead of one flat color, and open folders, menus and Spotlight match. An open folder blurs Home behind it like other menus do.
- **One light for all of Home:** glass and icons are lit from one point above the screen, a little in from the left, instead of each having its highlight in the same corner. Two widgets side by side catch it in different places, and as a page swipes the bright spot slides along each widget's edge. App icons keep the light they have at rest while a page moves, so they never flicker mid-swipe. Other apps' widgets that fill their card, like Calendar's, get the same lit edge as app icons; round or shaped ones, like the Clock's, are left as they are.
- **Smoother widget resize:** resizing a widget over apps shows them sliding out of the way as you drag the handle, and sliding back if you cancel.
- **Home settles when the layout changes:** after a widget move or resize that pushes apps along, or an Undo, apps and icons slide to their new places instead of jumping. Folding the phone still places them at once, and Reduce Motion applies as everywhere.
- **Clear Suggestions History:** Settings › Search › Suggestions has a Clear Suggestions History row. After you confirm, Folio forgets what it learned from your app use (Spotlight suggestions, the App Library tile, the Suggestions widget, Smart Stack relevance) and empties your recent apps. Android's own usage data is not changed; Folio just ignores anything from before you cleared.
- **Reduce Motion:** Settings › Gestures › Reduce Motion is a step between all motion and Android's Remove animations, like iOS. Pages, sheets, menus and alerts fade in where they land instead of sliding or growing, icons stop wiggling while you edit Home, and switches stop without bouncing. Remove animations turns it on too.
- **Press and lift on icons:** apps, dock icons and folders all dip the same way under your finger, an icon lifts with a soft shadow while its menu is open, and after a tap it stays dipped until the app is up (half a second at most), so a slow app no longer looks like nothing happened. With Reduce Motion on the icon only dims.
- **Performance log by what Folio was doing:** besides the whole run, the report gives the frame timing (p50, p95, p99 and janky frames) of the fold animation, Home swipes, folders opening, widget resizing, pages, sheets, menus and alerts opening, and moving an icon on Home, each on their own, so a slow one shows up by name.
- **Copy Diagnostics beside Share:** Settings › General has Copy Diagnostics next to Share Diagnostics, so the report can be pasted into a form or a chat without going through a share sheet. It is the same text a shared report holds, and nothing leaves your phone.
- **Capabilities and Recent Activity:** Settings › General shows what Folio can use on this phone (Standard, Notification access and Accessibility, each On or Off, and what it is for), with a button to Android's settings for the ones that are off, and Folio's own recent activity, newest first, with a Failed filter for problems it carried on from. Both are read-only and stay on the phone.
- **Suggestions, any size:** the Suggestions widget now goes from one cell to the whole grid. Its size menu adds 1 × 1, Row (four across) and Column (four down), and the icons keep to the size of your Home icons instead of stretching to fill the card.
- **Your own picture for an app's icon:** long-press an app, More › Edit Icon › Your Picture › Choose Picture, and one icon shows a photo you pick, cropped to a square. Remove Picture puts the normal icon back. The picture is copied into Folio, so it keeps working if the photo is deleted; it stays on this phone and is not part of a backup.
- **Performance log:** Settings › General › Performance log can record Folio's own CPU, memory and battery use, so slowdowns and drain can be measured instead of guessed. It stays on the phone.
- **Stronger rings:** dragging along the page dots still moves between pages. The status ring gets a thicker stroke and a clearer track with Android's Bold text.
- **Drop an app on another to make a folder:** drag one app's icon onto another's, on Home or in the dock, and the two become a new folder together, the way iOS and Android do. Dropping on a folder that already exists still just adds to it.
- **Drag to reorder inside a folder:** hold and drag an app over another one in an open folder to put it there instead, the way reordering works everywhere else in Folio.
- **Drag an app out of a folder, like iPhone:** hold an app in an open folder and drag it past the card's edge, and the folder steps aside while you carry the icon to any spot: any cell, another page (hold at the screen's edge), the dock, or onto another app or folder. The other icons make room as you go, and the folder closes when you let go.
- **Sort a folder A to Z:** a folder's options popup has a Sort A to Z row that puts its apps in alphabetical order in one tap.
- **Place the Big Clock anywhere:** long-press a Big Clock, then Place Freely, and drag it where you want it, a part of a cell off the grid. A dashed outline shows the cells it keeps for itself, so apps never end up underneath, and it turns red where those cells are taken. Arrows nudge it a quarter of a cell, Snap to Grid puts it back, and moving or resizing it on the grid starts it fresh. On the two-column and half-folded layouts it stays on its cells.
- **Badge counts that roll:** when an app's badge number changes it rolls up or down instead of swapping.
- **Smoother sheets and menus:** a page sliding in, a menu or alert opening, and the Library's category popup now settle on the same springs, a touch softer for pages and with a hint of life for menus, and the icon fan lands a little more gently.
- **Page dots that stretch:** the active page dot stretches toward the next one as you swipe and the tail follows, so a swipe has a visible companion. With Reduce Motion on it is a plain dot.
- **Icons that land:** an app you move on Home or the dock lifts where it is and keeps the spot you grabbed, and when you let go it flies from your finger to its new place and settles, instead of appearing there. With Reduce Motion on it fades into place.
- **Folder or make room:** hold an app over the middle of another for a moment and a folder plate grows behind it; hold it over the edge of an app or a gap and the others make room, so you can put an app between two others again. Passing over an app no longer makes a folder by accident, and a folder that has to move along now slides with the apps instead of jumping.
- **Folders and menus from their icon:** a folder's icon turns into the open folder and Home steps back a little while its apps settle in one after another, and an app's menu goes back into the icon when you close it instead of disappearing. With Reduce Motion on, both only fade.
- **See where back goes:** Spotlight eases back as you swipe back from the edge, so you can see what's behind; let go and it closes from there, or change your mind and it springs back. With Reduce Motion on it closes as before.
- **Delete apps from Folio:** press and hold an app on Home, then Remove App, to delete it or only take it off Home; in the App Library the menu says Delete App. Android asks you to confirm every delete, as it does for any app. Apps that came with the phone can be hidden or turned off in App Info instead, and work apps are deleted from App Info. Folio now asks Android for permission to request deletes (`REQUEST_DELETE_PACKAGES`), granted with the app; see PERMISSIONS.md.
- **Widgets make room (#329):** moving or resizing a widget over apps no longer turns the spot red. The apps in the way move along, in order, only as far as they must, an empty cell further on takes up the slack, and when a page is full they carry on to the next page (a new one is made if there isn't one). Widgets never move, apps never go under a widget or into a row Home isn't showing, and Undo Last Layout Change puts everything back. The unfolded-only page works as before.
- **Add to Home from Spotlight:** search for an app and its Top Hit has a + button that puts it on Home, the same way Add to Home in an app's menu does, and tells you with a notice. Spotlight stays open, so you can add more, and the row then says On Home. An app that is already on Home says so, and an app inside a folder says which folder holds it, instead of showing a button that would do nothing.

### Changed
- **Heavier blur behind folders, menus and sheets:** Home now blurs more than twice as much behind anything opened over it (72 dp instead of 32), like iOS, so the widgets and icons behind an open folder become soft color instead of shapes showing through the folder's glass. An open App Library category blurs the library behind it just as much.
- **Adding a widget, clearer:** the widget gallery is light and opaque like iOS's, with white cards, and runs behind the navigation bar instead of stopping at a dark strip. While you place a widget the toolbar offers Small, Medium and Large (and the widget's own size when it is something else), finding room for the new size, on another page if it has to; swipe to change page. Place is in bold beside Cancel.
- **Softer glint on icons and search fields:** the round glow near the top of app icons and the bright streak along the App Library and Spotlight search fields are gentler, so they read as light on glass rather than a shiny spot. The dock, the Search button and the rest of Home's glass keep theirs.
- **Carrying an icon to another page is easier to find:** while you drag, a glass tab with an arrow sits at each edge that has a page beyond it and grows when you reach it; hold there for half a second (was 0.65) and Home turns the page with a tick. The edge reaches further in, and holding an icon over a page dot for a moment jumps straight to that page.
- **Page dots spaced like iPhone's:** the dots sit 16 dp apart instead of 28 and are all the same size, so several pages no longer stretch across the bottom.
- **iOS 27 glass:** every glass surface has iOS 27's thin darker edge under a brighter rim, and a small highlight at its top corner, so layers separate more clearly. App icons get a brighter edge, more light from above, a deeper shade at the bottom and a soft resting shadow.
- **Light glass menus, folders and App Library cards:** app menus, the Home menu and pop-up menus are light glass with dark text, like iOS 27, instead of dark grey. An open folder and an opened App Library category are clear glass with the name left-aligned above in a large title, like iPadOS 27. Edit-mode bars, drag previews, empty widget slots and drop spots are Liquid glass instead of flat white.
- **Corners like iOS 26 and 27:** search fields, buttons, chips, pills, segmented controls and toasts are capsules; menus and grouped cards use 26 dp corners, widgets 28 dp, alerts 34 dp and sheets 38 dp. App icons keep iPhone's continuous corners everywhere they appear, App Library tiles have corners that follow the icons inside them, and large titles are 34 sp.
- **Search like iOS 27:** Spotlight's field is a dark glass bubble with silver edges, the one dark surface in Folio, and Spotlight drops in from the top. Home always shows the page dots under the grid and a round Search button at the foot of the Side Bar, like iPhone Duo.
- **One motion language:** paging to the App Library, tapping a dot, Back and Add Page move on the same spring as a swipe; the Edit bar, toggles and page dots settle on Folio's springs; the dock shakes its head with a spring instead of fixed steps. Jiggling eases in and each icon wiggles at its own rate, icons dip a little less under your finger, and a folder or Spotlight opening gives a light tick.
- **Home button on Home goes to the first page:** press Home while Folio is already in front and it returns to your first Home page. Pressed from another app it still returns to the page you left, and with a sheet, menu, folder or Spotlight open it only closes that.
- **App Library opens without a flash:** while the category tiles are worked out, the library no longer shows the whole A to Z list for a moment first, and it looks each app's category up once instead of on every change. Scrolling also does less work per icon.
- **Swipe an incoming call away:** swipe a ringing call's card up on Home and it shrinks to the pill, like a message or notice already does. The call keeps ringing, and a tap on the pill opens the card again.
- **A simpler folder header:** opening a folder now shows just its name and one options button. Color and Add Apps used to sit under the title every time a folder opened; they're in that button's popup now instead.
- **Quiet confirmations:** a short confirmation such as a saved layout backup is a short notice instead of a dialog you have to dismiss.

### Removed
- **Duet's frost and the timed fold:** the frosted, timed fade on the open screen, the fold dimming as you close, the cover's blur while it opens, fold motion (the icon ripple, depth and hinge light) and Your Fold Timing are gone, replaced by the page landing. The halfway tick while folding is gone too; the landing has its own.
- **Bottom of Home:** the Search bar and Nothing choices are gone. Home shows the page dots, and Search is the round button in the Side Bar.
- **The Market, supporter codes and update checks:** the Market and its packages, supporter and beta codes, feature gates, the Developer page, Software Update, the Roadmap, What to Test and Report a Bug are gone, and every feature that waited behind a gate is now simply on. Folio no longer goes online or posts notifications, and a backup that carries packages still restores, without them. Widget Size and the Motion pass switch went with their gates.
- **Dark appearance, glass choices and Themes:** Folio is always light and always uses Liquid glass. Appearance (Dark, Automatic and Sunrise to Sunset, with its location), Accent, Dark appearance dims wallpaper, the Glass card (Style, Widgets, Side Rail, Outline and Wallpaper Tint), Folder background, Reduce Transparency and Themes (saving, importing and sharing theme files) are gone. Liquid glass now draws even with Android's high contrast on, and Folio no longer asks for your location. A backup or saved layout with the old settings still restores.
- **The Dynamic Island, Control Center and Notification Center:** Folio no longer draws an island on Home, over other apps or on the Side Bar, shows no live activities, and has no Control Center or Notification Center of its own. Pulling down from the top corners of Home, tapping the Side Bar status, the Notifications choice for a swipe down on Home and the Notifications and Quick Settings actions open Android's own shade and Quick Settings, and a plain swipe down on Home still opens Spotlight. Folio's short notices are plain toasts. The Notification shade and Quick Settings shortcuts, the Island tile, the headphones card and the Roll Call, Palette and Afterglow tweaks are gone, and Folio no longer asks to modify system settings. The dock handle switch is on the Gestures page.
- **Setup, Screenshot Mode, the small-cover Home and System Bridge:** a new install opens straight on Home with the default layout, with no welcome pages, Finish Setting Up card, setup reminder, first-use hints or Arrange Like iPhone. Settings › General › Privacy & Permissions is where Folio asks for Home app, Notification access and the gestures service. Screenshot Mode and its Swap Apps page, the separate Home for covers 320 dp wide or less, and System Bridge with its root hinge helper and settings permission grant are gone, and Folio no longer asks for WRITE_SECURE_SETTINGS. The fold animation reads only the public hinge sensor. Diagnostics still lists what Folio can use.
- **StandBy, Lock Cover, Focus, Today View, Discover and the side key:** StandBy (half open, charging on its side, the tent, its screen saver and Quick Settings tile), the Lock Cover after unlocking, Focus modes (schedules, triggers, pages, the status glyph and the Focus actions), the Today View and Google Discover to the left of Home, and the side key assistant picker are gone. On the cover screen Home 1 is the first page; unfolded, the leading workspace and Home 1 still open together. Do Not Disturb On and Off stay as actions. Folio no longer offers itself as a digital assistant or screen saver. A backup or saved layout with the old settings still restores.
- **Size, spacing and look settings:** Home always uses its own icon size, spacing between rows and columns, dock width, spacing and position, status position and Automatic rows, so Separate Upright Layout, Reset This Layout, the per-page icon sizes and the edit-mode dock handle are gone. So are Name Size, folder Columns and folder resizing, Screen Corners, Background Blur, Animation Speed, Touch and Hold, Flipbook, Icon Style and Shape (also per app: Edit Icon now only sets your own picture), the Live Icons look, Badge Color, Look and Size, the status icon styles with their Time, Date, Battery Percentage, Silent, Color Battery, Spacing, Background and Stronger Rings switches, the Big Clock's Customize bar, Big Buttons' Size, Width, Order, Look and Fade, Harborline's Amount and Tick, Duet's styles, sliders, Plays When, Fold Motion switches and Handover, and the App Icon choice. Each works the way its default did.
- **Settings search, Tweaks and the old Settings pages:** Settings is now five flat pages, Home, Search, Gestures, Fold and General, and has no search field. The Tweaks pages and their per-screen Use On choices are gone: Cabinet and Harborline are plain switches on Home and Duet is a switch on Fold, each for both screens. Help, Language and the idea-only credits are gone too; General keeps What's New, Backup, Layout History, Privacy & Permissions, Diagnostics and the credits the licences ask for. The Side Bar status shows only the time and the Ring, without the date and battery percentage, and the Home long-press menu's Wallpaper & Appearance is now Wallpaper, which opens Home's settings.

### Fixed
- **App Library categories blur on Samsung phones:** One UI has no window blur, so an open category left the App Library and the dock sharp under it. It now blurs them the way a folder on Home does.
- **Place and Cancel could be covered:** when a new widget went into the top rows, its preview drew over the placement toolbar and Place could not be tapped. The toolbar now stays on top and moves to the bottom when the widget is near the top.
- **Widget menu text was unreadable:** Resize on Home, Custom Size, Replace Widget and Make a Stack were dark gray on the dark menu and looked turned off. They are white again.
- **Search over Done:** unfolded, the round Search button sat on top of the Done button while editing Home. It now hides while you edit.
- **Smoother swipes:** swiping between Home, the widgets page and the App Library no longer hitches at the start or end of a swipe, and scrolling the App Library is smoother. On the cover, pages beside the one you're on stay ready instead of being rebuilt, so widgets no longer reload as you swipe back to them.
- **App Library's top edge:** the App Library opens with its first row crisp, and the soft fade at the top comes in gradually as you scroll down, instead of showing at the top from the start and vanishing when you pull down.
- **Home holds still for the keyboard:** typing in the App Library's search field no longer nudges the dock and Side Bar a few pixels, and they no longer jump back when the keyboard closes.
- **Search fields lit like the rest of the glass:** a long field's light now runs evenly along its top edge, with a soft streak where it catches the light, instead of a round burst and a bright line along its bottom left.
- **Holding an app in the App Library:** the lifted icon now sits exactly over the icon you're holding, not at the screen's edge, and the menu lines up with it the way iPhone's does. Home blurs in behind the menu instead of snapping, without a dark flash first, and no larger copy of the icon pops up under your finger. Menus on Home line up with their icon's edge too.
- **App Library search results:** the highlight under a held app leaves room around the icon and follows its corners, and the "Search with" label, the Google, DuckDuckGo and ChatGPT buttons and the letter headings now use Home's text color instead of dark gray.
- **Pages slide under the dock:** swiping between Home pages or into the App Library no longer shows them appearing from behind an invisible line beside the Side Bar, with a strip of wallpaper next to the dock. Pages now slide across the whole screen and pass under the dock, the time and the signal, and nothing peeks out beside the dock when Home is still.
- **The Search button grows from its middle:** when the Search button comes back at the bottom of Home, it now grows out from its center with round ends the whole way, instead of looking cut off with straight sides.
- **Folio's app icon comes back:** a phone that had picked the Soft or Olive app icon, both gone now, had no Folio icon in the App Library or other launchers. Folio turns its own icon back on when it starts.
- **Find Folio's own widgets by search:** the widget gallery's search now finds Clock, Date, Big Clock and the rest by name (or "Folio"), and "No Widgets Found" no longer shows under Folio's own widgets.
- **A missing widget no longer blocks a layout backup:** if a widget's app was uninstalled or disabled, the backup used to stop with "Widget 7 is unavailable". It now leaves that widget out, saves everything else, and says how many widgets were left out.
- **Every layout backup can be restored:** a backup could save and then refuse to restore if an app, shortcut or widget had a blank name. Backups now name those by their package, so the file always reads back.
- **Layout backup errors speak plainly:** a failed backup showed the program's own text ("Failed requirement"). It now says a short translated sentence, with the file-too-large and made-by-a-newer-Folio cases named, and keeps the technical text in Diagnostics.
- **Button Bar labels are translated:** the Recent apps, Home and Back buttons of the on-screen Button Bar were always read out in English. They now follow your language, for TalkBack and for the button's long-press label.
- **Arrows face the right way in right-to-left languages:** the back and disclosure chevrons in Settings rows now flip in Arabic, Hebrew and other right-to-left languages, instead of pointing the wrong way. Home's page arrows keep their direction, because Home's pages stay in a fixed order.
- **Icon actions that open a missing app say so:** an Open App or Open Shortcut action whose app or shortcut is gone now shows "App is unavailable" instead of doing nothing.
- **Unavailable apps in a folder explain themselves:** tapping one now shows the same "is unavailable" notice as on Home, instead of doing nothing.
- **Spotlight launches count once:** opening an app from Spotlight added it to Recents twice, which tilted the order of Suggestions and Recents. It counts once now.
- **Cancelled folder reorder:** if a drag inside a folder is interrupted, the apps go back to their saved order instead of showing an order that was never saved.
- **Home no longer stays blurred after a sheet:** each Home screen now counts only its own open sheets, menus and pages, so one that was stopped behind another can no longer leave Home blurred with nothing in front until Folio was force-stopped.
- **Home no longer stays blurred behind nothing:** a sheet or menu that was still open on a Home screen Android had stopped could keep the current Home blurred with nothing in front, and only force-stopping Folio cleared it. Each Home screen now counts only its own sheets and menus, and drops them when it closes.
- **Smart Rotate turns a stack again:** its 15 minute wait started over every time you came back to Home, so it almost never moved. Time away from Home now counts toward the wait, and coming back after it has passed checks right away.
- **Show First keeps a widget stack's Undo:** Show First on a widget stack right after removing a widget no longer throws away that removal's Undo or the removed widget.
- **Turning the phone keeps its rows (#13):** Automatic rows measure the upright and sideways open screens separately, so turning the phone no longer overwrites one with the other.
- **Safe Mode stays on until you leave it, and a newer layout says so:** Safe Mode no longer forgets its crashes after half a minute while you stay in it, and its dialog does not come back after a theme or language change. A Home layout saved by a newer Folio, after a downgrade, now says to update Folio and offers only Not Now and Restore instead of Start Fresh.
- **Notification badges show on apps inside an open folder (#362):** a folder showed its total on Home, but once open its apps had no badges. Each app now has its own badge on the top-left corner of its icon, clear of its options button.
- **The dock says why it won't take an app:** choosing an app for the dock greyed out any app already in a folder without saying why, so it looked broken. Those apps now say In a folder · Move it out first under their name.
- **More of Folio follows your language:** the check list for an Icon Stack and a few other labels were English in every language. They are now in Korean and Chinese too.
- **Create Folder always makes the folder:** picking the second app did nothing when the app came from the dock or the App Library and the page had no free spot. The folder now goes on the first page with room, or on a new page at the end, and Home turns to it. Its list also stops offering apps that are already in a folder, which did nothing when picked. Its title, its "New folder with" heading (which was nearly invisible on the dark dialog) and a new folder's name now follow your language.
- **No outlines through widgets while moving an app:** the faint outlines that show where a carried app could go no longer appear on the cells under a widget, which showed through the widget.
- **Back closes a folder again:** since folders got their own window in beta 1, the Back button and the back swipe did nothing while a folder was open; only a tap outside closed it. Back closes it again, and the back swipe shrinks it toward its icon as before.
- **Full-Width Home is centered:** with the dock at the bottom and no Side Bar, the apps sat a little to the right, narrower columns hugged the left edge, and the App Library ran to the right edge of the screen. All three are now centered, and the same goes for the upright inner screen with the dock at the bottom.
- **The Search button no longer squeezes the dock (#256):** with Search button on Home turned off, the round Search button in the Side Bar set aside a fixed strip under the dock, much taller than the button, so in a short window the space between your dock apps shrank. Folio now keeps only the room the button really takes.
- **One prompt at a time, and a truthful preview:** the Turn On Folio Gestures dialog no longer opens on top of What's New (or Folio Dev's build page); it waits until that sheet is closed. The Home preview in Settings shows your phone's real battery and signal instead of a fixed 80%.
- **Missing spaces between sentences:** the Turn On Folio Gestures note read "over other apps.It can't read" and the Backup note "layout settings.Restore shows". Both have their space back, in English and Korean.
- **Media controls and Notification access:** Folio no longer risks a crash or a stuck media callback when notification access is turned off while a media session is changing.
- **Less work while apps install and when offline:** while any app installs or updates, only that app's icon redraws instead of the whole of Home, the status rail no longer redraws its whole self every frame when there is no connection, and a clock icon on Home redraws its hands without rebuilding the icon each second.
- **Dropping an app on a folder adds it:** holding an app over a folder on Home used to slide the folder out of the way, so letting go swapped the two places and the app never went in. The folder now stays where it is, lifts a little, and takes the app, whether it comes from Home, the dock or the App Library.
- **Even rows under a widget (#13):** a widget at the top of Home now fills exactly two rows, so every row has the same spacing and the dock lines up with them, where the rows below a widget used to sit a little off. Widget Size is gone, because the widget's height is now set by the rows.
- **Safe Mode counts only real crashes:** Android closing Folio for memory no longer counts toward it.
- **Native crashes and freezes count toward Safe Mode:** a crash in Folio's own native code, or a freeze right at the start, now counts the same as any other quick crash, so Folio offers Safe Mode instead of starting the same way again.
- **Dual Messenger and other cloned apps show in the App Library:** a second copy of an app, made with Samsung's Dual Messenger or Android's app cloning, is listed right beside the original with Dual under its name, and in a folder too. Only the apps you cloned show, not the system apps Android keeps in the clone's space. Cloned apps stay out of the first dock and Suggestions, and leave Home when you turn cloning off. Work apps stay behind the Work switch as before.
- **A cancelled drag puts the app back:** a drag that Android cancels (the power button, a call, unfolding) puts the app back where it was instead of dropping it where your finger was, so nothing is removed by accident.
- **A Layout Backup lists only your Home apps:** a backup now names only the apps your layout uses (on Home, in the dock or a folder, or with a name or icon action you set), not every app on the phone in every profile, hidden ones included. Older Folio still reads it.
- **Restore on the damaged-layout notice works:** when Folio can't read your saved Home, Restore on its notice now really restores the backup or earlier layout you pick, instead of saying it is already active. Folio keeps a copy of the damaged layout first, as Start Fresh does, and won't export an empty Home as a Layout Backup while the layout is damaged.
- **Restored widgets keep their own Smart Stacks:** restoring a Layout Backup no longer puts an old Smart Stack on top of a different widget. Restored widgets get slot numbers nothing on your Home is using, so the stacks Folio keeps for Undo stay with their own widgets, and a Big Clock that was in the same place keeps its look.

## [0.6.8] - 2026-10-06

### Added
- **Edit an app's icon on its own:** long-press an app, More › Edit Icon, and give that one icon its own style (Default, Dark, Tinted or Clear) and shape, or leave either like the other icons. It shows everywhere Folio draws the icon, Reset Icon puts it back, backups carry the choices, and the icon pictures Folio saves are untouched, so nothing can be left stale. Custom pictures and per-app icon-pack icons are not part of this yet.
- **More search engines, and your own:** Settings › Search & App Library › Search with Enter now offers Bing, Brave Search, Ecosia, Startpage, Qwant and Kagi beside Google and DuckDuckGo, and Custom…, where you type your own search address with %s where the search goes. Folio checks it as you type and falls back to Google if it can't be used, and only http and https addresses are accepted. The one you pick also gets a chip under a search.
- **Add several apps to a folder at once:** an open folder has an Add Apps button. It lists your apps with a check each, shows how many you have picked, and Done puts them all in as one change, so Undo takes them all back. Apps already in the folder are shown ticked, and hidden apps stay out of the list.
- **Home beside an app in split screen:** when Folio is one half of a split screen, it shows a Home page, the one you were on or your last one, instead of the App Library or Today View it was left on. It is one cover-sized page next to your app. Android and your phone decide which half Folio gets.
- **Full-Width Home for ordinary phones:** Settings › Home Screen & Dock › Layout › Full-Width Home puts the dock along the bottom and turns off the status Side Bar, and Home now uses the whole width instead of leaving the Side Bar's empty strip beside the dock. It is two settings you already had, in one switch, and nothing changes until you turn it on.
- **Full-Width Home puts things back:** turning Full-Width Home off now restores the dock placement and status Side Bar you had before, instead of going to Automatic, so a dock you set to Side Bar comes back as Side Bar. With both screens in Full-Width Home, the Side Bar returns when the last one is turned off.
- **A dock that grows as you fill it:** the dock is four places, as it has always been, until you drag an app onto it with every place full: an open place appears at the end, drop there and the dock is a place bigger, up to six. Taking an app out closes the empty place again, never below four. There is nothing to set, and a dock of four is exactly what it was. On a bottom dock bar with more apps than the window has room for, the bar stops at the room it has and scrolls. The bar and the icons ease into their new spacing as a place opens or closes (at once with Reduce Motion), and dragging an app onto a dock that already holds six gives a refusal tick and a short shake. A backup of a four-place dock is unchanged, but a Folio older than this one can't restore a bigger one.
- **A layout for the upright inner screen:** Settings › Home Screen & Dock › Layout › Inner has Separate layout when upright. Turned on, the unfolded screen held upright gets its own spacing, dock and status settings (starting as a copy of the sideways ones) and the sideways layout keeps its own; turned off, they share one again as before. Nothing changes until you turn it on.
- **Choose how Folio feels at setup:** a new setup page offers Folio (Dynamic Island, Folio's own Notification Center and Control Center, Spotlight on a swipe down) or Android style (Android's notification shade and Quick Settings, no Dynamic Island). Skipping keeps Folio, and if you already use Folio nothing changes. Each is only a set of existing switches, so everything stays changeable in Settings.
- **The notification shade over other apps:** Folio Notification Shade and Folio Quick Settings are launchable entries, so a button or gesture action on your phone that opens an app (Samsung's One-hand operation, Routines, a Home shortcut) can pull Android's shade down over whatever is open. They use Folio's gestures service, so nothing is asked for that wasn't already, and without it they take you to how to turn it on.
- **Touch and Hold, shorter or longer:** Settings › Gestures & Actions › Touch and Hold sets how long you hold an app or widget on Home before its menu opens or it lifts. Standard follows Android, including Samsung's Touch and hold delay in Accessibility, so nothing changes unless you pick Shorter or Longer.
- **Add Folio Settings to Home:** Settings › Icons & Side Bar › App Icon has an Add Folio Settings to Home button that puts the Settings icon on Home in the first free place, so getting to Folio's Settings is one tap. It reads "on Home" once it is there.
- **A switch for Today View's Suggestions:** Settings › Today View › Suggestions turns off the row of apps you may want next at the top of Today View, and with it the usage check that picks them. The Suggestions widget and Spotlight's suggestions have their own switches and are not affected.
- **Duet, the fold animation as a tweak:** Settings › Tweaks › Duet has the fold effect's own page, with a Style (iPhone Duo, Duo, Classic Glass, Deep, Subtle or Minimal), Plays When (opening, closing or both), and Frost, Shade and Tilt sliders beside Intensity and the preview. Fold & Displays links to it instead of carrying the old controls. If you updated from an earlier Folio, you keep the fold you had, which is Duo; a new install starts on iPhone Duo. A package can set Duet's look. Classic Glass, Deep and the style list use code from Duo Fold Live, hingewave and iphone-duo (MIT), credited under Settings › Credits.
- **StandBy comes on in more ways:** besides half-open like a laptop, it comes on while the phone is charging, turned on its side and set down, in any pose, as on iPhone, and optionally when it stands as a tent on the cover screen. Each is a switch in Settings › Fold & Displays › StandBy. Ideas from nightstand and StandBy-Android, re-created for Folio.
- **StandBy on the lock screen:** choose Folio StandBy as Android's screen saver from Settings › Fold & Displays › StandBy › Screen Saver, and when the screen turns off on a charger, StandBy carries on over the lock screen while the phone locks as usual. Pick the phone up or swipe up from the bottom to leave. It dims at night and moves the clock a few pixels each minute so it can't burn in. Without it, StandBy on a charger now stays up only until the screen turns off, so the phone always locks.
- **Two more Flipbook effects:** Inside Cube folds Home pages toward you as you swipe, like the walls of a room, and Stack steps them back without turning. They re-create Cylinder's Cube (inside) by Reed Weichler and Page Squeeze by Beta382, and Settings › General › Credits names both.
- **Updates for apps in the Market:** an app a source lists, like Keyd, shows Update with the version it goes from and to, instead of Open, when the source has a newer one. Folio checks the download is the same app signed by the same developer before it installs it over the old copy.
- **Widgets, one app at a time:** the widget picker has a row of app icons across the top; tap one to see only that app's widgets. While browsing, each app shows a single row with Show All for the rest, so an app with hundreds of widgets no longer stands in front of everything else.
- **An Accessibility page in Settings:** it sits under General, as in iOS Settings, and holds Big Buttons (the larger Back, Home and Recents bar), which used to be on the Dynamic Island page where few people would look. Its options are unchanged.
- **Settings search finds the setting itself:** searching "badge size", "touch and hold", "dock height", "save backup", "check for updates" and well over a hundred more names now lists the setting and opens its page. Searching a page by its full name, such as "Fold & Displays", works too; the "&" used to make it find nothing. Before, only page names and a fixed set of keywords matched. Each result names the page it opens, and tapping it scrolls to that row and lights it for a moment. A test now fails when a switch, menu or slider has no way to be found.
- **Screenshots that move:** a package page can show a short looping clip (an animated WebP) in place of a still, and Cabinet, Harborline and Flipbook now do: the panel opening, the dock swelling under a finger, a page turning. With Reduce Motion on, a clip stays on its first frame. A clip over 1.5 MB isn't played.
- **Harborline and Afterglow options:** each has an Options group on its page in Settings › Tweaks. Harborline: Amount (Subtle, Standard, Strong) for how far the icon under your finger grows, and Tick as you slide. Afterglow (the new name for Colored Albums): Music card and Island sound bars, to choose where the album art's color goes. Everything starts as it was, they are kept when a tweak is turned off, and Reset puts them back.
- **Market shows if a package will work:** a package's page has a Compatibility card (works with your Folio, needs a later Folio, which screens it covers, what it needs or replaces), and a row in a list says "Needs Folio 0.8.0" or "Inner screen only" under its name. A package that can't be installed on this phone no longer offers Get, instead of failing after you tap it.

### Changed
- **A cleaner Roadmap:** Settings › General › Roadmap shows what is in progress now, what comes next, and what is later, with the releases you already have folded under Shipped. Each item has one status, and one in the current beta says In beta rather than Done. It says when it was last updated, and when it can't reach GitHub it says so and shows the copy saved on your phone.
- **StandBy fits phones without a fold:** Settings › Fold & Displays › StandBy offers only the ways a phone can use. Half-open, like a laptop and Standing as a tent need a hinge, so a phone without one sees While charging on its side and the screen saver, which work on any phone. Folio asks Android whether the phone has a hinge rather than keeping a list of models.
- **Home is ready sooner after Folio starts:** Folio loaded every app's icon, one after another, before Home showed any, which took most of a second on a Galaxy Z Fold8. It now loads the apps on Home, in the dock and in Home's folders first, so Home is complete in about a sixth of the time. The App Library and search fill in as the rest load.
- **The App Library is ready sooner after a start:** Folio saves app icons and names between starts and uses them while an app is unchanged, so after a start the App Library and search are ready in well under half the time (about 0.3 s instead of 0.8 s on a Galaxy Z Fold8). An app that updates is loaded again, and Refresh Icons forgets them all.
- **Edit Home Screen gives a tap:** choosing Edit Home Screen from an app's menu now vibrates lightly as the icons start to jiggle, like pressing and holding an empty spot on Home does.
- **Effects say what they work with:** a page effect's page in the Market and its install sheet say it works with Flipbook. Without Flipbook on your phone they say so before Get, with a way to get Flipbook, instead of an error afterward. Try Again and restoring a backup leave an effect off until Flipbook is back.
- **Flipbook's effects are on Flipbook's page:** the built-in effects and any you add from the Market are listed on Flipbook's own page in Settings › Tweaks, instead of a menu in Gestures & Actions. Choosing one turns Flipbook on, and Settings search finds Flipbook when you type Page Effects.
- **A shorter Settings list:** the first screen goes from 26 rows to 16. Software Update, What's New, Help, the Roadmap, Credits and Advanced are under a new General page, which also has Language (Android's own per-app language screen). Ko-fi, your supporter code and the Supporters list are one Support Folio row. The Market opens from Tweaks, and Themes from Wallpaper & Appearance.
- **Easier to tap:** the Discover and All Apps buttons beside Home's page dots and the round buttons in Spotlight look the same, but a finger now has a 48 dp target to land on.
- **Settings is ordered like iOS Settings:** General and Accessibility come first, Backup is now inside General, the Dynamic Island page opens with the island and its pop-ups before the options for other apps, and the App Icons card starts with Style (Default, Dark, Tinted) and puts Notification Badges last, where its options open. Nothing is renamed and every setting keeps its value. The Brief pop-ups card no longer also shows on the Icons & Side Bar page.
- **Colored Albums is now Afterglow:** the tweak that tints the music card and the island's sound bars with the album art's color has a new name, in Settings › Tweaks and the Market. Nothing else changes, and searching "colored albums" still finds it.
- **One compatibility check for the Market and the installer:** whether a package works on your phone (it needs a later Folio, a tweak you don't have, a package that conflicts or one it depends on) is now answered in one place, in the order the installer always refused in, so a package's page can say it before you tap Get.
- **Layout History is out of beta:** the Beta label is gone from Settings › General › Backup › Layout History. It works as it did, and the snapshots you already have are still there.

### Fixed
- **Arrange Like iPhone no longer crashes Folio:** Settings › Home Screen & Dock › Arrange Like iPhone closed Folio as soon as it found an app for any of the iPhone spots. It now lays out Home's first page and dock as it says.
- **Beta Updates says it's for supporters:** Settings › Software Update › Beta Updates now says that betas are for supporters and that without a supporter code Folio only finds public releases, instead of saying you're up to date while a beta is out (#239).
- **More of Folio in your language:** in Korean and Chinese, the setup card, What's New's headings, Settings' Location fields and its Top choice, the Home for tiny cover screens, Lock Cover's buttons, the headphones card and the folder panel are translated, and TalkBack reads the Side Bar, the island rail's call and music controls and the search field's clear button in your language. In English, What's New now says "1 more new feature".
- **Dates and switches in your language:** in Korean and Chinese, StandBy, the Big Clock, Lock Cover, the Side Bar and Today View show the date in that language's order (10월 2일 금요일, 10月2日 星期五) instead of English order, and Control Center, the island, Settings and the dock chooser say On and Off, Airplane Mode, Cellular Data, Your Apps and Dock Position in your language instead of English.
- **Back in Settings goes up a page from the Folio icon too:** opened from the Folio icon or Android's settings gear, Settings is a tab beside the Market, and there Back closed everything from any page. It now goes up a page, as it does everywhere else, and closes Settings only from the top.
- **TalkBack in Settings:** the search field is read as "Search" instead of a nameless edit box, rows on the phone no longer each say "Not selected", and the preview of Home at the top is one picture, "Preview of Home", instead of about 25 of its parts and a made-up battery level read out before General.
- **Nothing covers StandBy:** while StandBy is up on Home, the island, the headphones card and the setup reminder wait instead of drawing over it.
- **Gestures say what they need:** a gesture or Control Center button set to Lock or Screenshot, with Folio's accessibility service off, did nothing. A Do Not Disturb action without Do Not Disturb access did nothing too. Each now says what to turn on.
- **Safe Mode pauses the Market refresh:** while Folio is in Safe Mode, the daily check of your Market sources waits, so a refresh that finds packages is not added to a crash loop.
- **StandBy's night text is readable:** at night the date, the battery and alarm, and the cards used a red too dim to read (2.2:1 against black). They use brighter reds now, at least 4.5:1, while the big clock keeps its dim red.
- **The charging island follows the cable:** plugging in at a charge limit, like Samsung's battery protection, shows the charging island and runs your Charging automations, and the battery topping back up at the limit no longer shows the island again with nobody touching the cable.
- **StandBy shows more of a song's title:** on the cover screen, Now Playing in StandBy puts its controls under the title, so a long title has the card's width and two lines instead of a few letters, and breaks between words.
- **Plugged in shows as charging:** at a charge limit, like Samsung's battery protection holding at 80%, Android reports the battery as not charging while the phone is still on the cable, so the Side Bar and StandBy showed it as running on battery. They follow the cable now, as iOS does.
- **The Market's buttons are easier to tap:** Get, Remove, Try Again and the Undo in a banner are full 48 dp targets now. A tap just above Undo used to dismiss the banner instead.
- **Packages Folio can't use yet are refused:** a package of a kind Folio doesn't support yet, such as a script, offered Get and then installed as nothing, labeled as running a script. The Market now says it needs a newer Folio and doesn't install it; Folio doesn't run scripts.
- **Opening a package file shows it again:** opening a `.foliopkg` from Files or a download opened the Market and nothing else, so the package couldn't be installed. Its install sheet comes up now, opening one while the Market is already open no longer lands on Settings, and a package installed from a file is listed under Installed (Opened from Files), where it can be removed. If Safe Mode turned one off after a crash, its row says so and offers Try Again, as a listed package's page does.
- **Flipbook turns the way it was designed to:** Cube and Carousel drew with their camera about six times too close, so a page shrank to a sliver partway through a swipe instead of turning like the side of a box. The camera sits where it was meant to now, and a page effect looks the same on every phone, whatever its screen density.
- **Hidden apps stay hidden:** the dock's app chooser and the list of apps for a new folder offered every app, including the ones hidden in Settings. They leave hidden apps out now, as the App Library and search do. Searching the dock's chooser or Stack Apps also finds an app you renamed by its original name, and a Chinese name by its pinyin.
- **An icon pack Folio can't read doesn't slow the App Library:** a pack with no usable list of icons was read again for every icon. It is left alone for half a minute after it fails, then tried again, so a pack that was updating still comes back.
- **Apply and Undo for a theme go in order:** choosing a theme and then Undo within a moment of each other could end with the theme applied instead of undone.
- **TalkBack reads the Focus switch once:** in Control Center it read "Turn off Focus, On, Switch". It reads the Focus's name and whether it is on.
- **Settings keeps your place on Back:** going back to a list, like Tweaks from one of its tweaks, always showed it from the top. It opens where you left it now. Back names the page it returns to, and next to the Settings list the pages under General, Support Folio and Wallpaper & Appearance have a Back, as the pages under Tweaks do.
- **Tapping a clock or a date opens its app:** tapping Folio's Clock, Big Clock, Date or Up Next widget opened the widget's options, which holding it already does, and in the Today View it started editing. A tap opens Clock or your calendar now, and in the Today View holding a widget starts editing. While you're editing Home, a tap still opens a widget's options.
- **Problem messages in your language:** in Korean and Chinese, the alert for apps that couldn't be loaded told you to tap to retry, beside its own Try Again button, and a saved Home layout that couldn't be read was reported in English. Both are in your language now. Tapping that layout message on Home brings back the choices to restore it or start fresh; it used to reload your apps, which can't repair a layout.
- **The island waits as long as you've asked:** what the island shows for a moment, like Folio's notices and new messages, went away after a fixed few seconds. It follows Time to take action in Android's accessibility settings now.
- **A quick action that won't start says so:** choosing one of an app's quick actions, from its menu or its panel, that the app had removed or turned off closed the menu and did nothing. Folio says it's unavailable now, as it does for an app that won't open.
- **TalkBack says what controls do:** the middle music button was read as "Play or pause"; it's Play or Pause now, whichever a tap will do. The widget gallery's close button is called Close instead of Back, a widget waiting to be set up is read by its name instead of a name from the app's code, and the Today View's Remove, Move Up and Move Down buttons are read in your language.
- **TalkBack follows your language:** in Korean and Chinese, TalkBack read a Focus's switch, the reply field and Send button on a message, "Show all apps" in the App Library, an app's Open, Moving and Choose, and an install's progress in English. They are in your language now. The reply Send button, a message's action buttons and the Clear button in search have a 48 dp tap area, drawn at the size they were.
- **Market clips only play on screen:** a clip scrolled out of view stops playing, and a still screenshot is no longer decoded twice.
- **Remove stays on a package you have:** when its newer version is one Folio can't read, the row and the page still offer Remove, and neither offers an Update that can't work.
- **A change right before you leave Home is kept:** a change to Home made in the last third of a second before you left Folio, or folded the phone, could be lost. Folio now saves it as Home stops.
- **Big theme files are refused:** choosing a file that isn't a Folio theme, but is very large, no longer reads it all into memory. It says it isn't a Folio theme, as before.
- **Less waiting on the main thread:** looking up your icon packs for the Icons page and for a theme, and saving the trail Folio keeps for Diagnostics, now happen off the thread that draws Home. A pack that was updating when Folio read it is tried again instead of being treated as empty.
- **An update that can't be saved says so:** if Folio can't put a checked update in place after downloading it, it says the update couldn't be downloaded instead of showing it as ready.

## [0.6.7.3] - 2026-09-28

### Fixed
- **Settings search finds tweaks:** searching Settings for a tweak by name, like Flipbook or Cabinet, found nothing. Tweaks now show up by their name, by the tweak they're based on and by what they do, and open their own page.
- **Wallpaper & Appearance lines up again:** the Dark Appearance Dims Wallpaper switch and the note under Also Set as Phone Wallpaper were drawn against the edge of their card. They have the same margins as the rows around them now.

## [0.6.7.2] - 2026-09-28

### Fixed
- **Flipbook remembers your style:** turning Flipbook off and on in Tweaks always brought back Cube, so a Carousel choice was lost. It now comes back as whichever you chose last.
- **Your photo comes back in one tap:** after choosing an artwork in Wallpaper & Appearance, tapping Your Photo opened the photo picker, so the photo you had was gone unless you found it again. It now puts your photo back; Choose a Different Photo still picks a new one.
- **Restored backups tidy up wallpapers:** restoring a Market backup that didn't include a wallpaper you had installed left that wallpaper in the Installed grid and on the phone. It's cleared now, the same as removing it.

## [0.6.7.1] - 2026-09-28

### Fixed
- **Searching works again** (#166): typing in the App Library's search field showed nothing, because the field grew to fill the panel and left the results no room. The same happened in Settings search, Choose Home Apps, the Market's sources and icon stacks.

## [0.6.7] - 2026-09-27

### Added
- **Folio speaks Korean:** the whole interface, from a community translation by qlife1146, with the newest strings drafted alongside it. A beta translation until a native speaker reviews it; Settings › General › Language lists it, and it follows the phone's language on its own.
- **White text reads on a pale wallpaper:** Home darkens a little at the top and bottom, the way iPhone does, so the clock, app names, page dots and the buttons beside them stay legible over a dawn beach or a cream painting. The middle of your wallpaper is left as it is, and it steps aside where Folio is already using dark text on Home or already dimming the wallpaper for dark appearance. Settings > Wallpaper & Appearance > Text on Home > Darken behind text turns it off. It is drawn once with the background rather than over it, so a swipe costs the same as before.
- **Flipbook, for supporters first:** Home pages turn in 3D as you swipe instead of sliding flat, after Barrel by Aaron Ash. Supporters get it from Folio's own source in the Market, and everyone gets it in 0.6.8. Cube or Carousel is picked in Settings › Gestures.
- **Folio's backgrounds are real art:** Folio ships two woodblock prints by Utagawa Hiroshige, Night View of Saruwaka-machi from the Library of Congress and Naruto Whirlpools from the National Library of New Zealand, and Settings › Wallpaper & Appearance is now one grid of everything you can put behind Home: your photo, the art Folio ships, and the art you install. Every one names its artist and its license. The drawn dunes are gone.
- **Installed wallpapers keep their credit:** Folio shows who made a picture and what it is licensed under wherever that picture is offered, and refuses one that cannot say. Removing it puts back whatever was behind Home before.
- **Folio has its own color:** buttons, switches, selection and links use Folio Teal, the teal the app icon ships in, instead of Apple's blue. Settings › Wallpaper & Appearance › Accent keeps Classic Blue for anyone who prefers it. Status colors stay as they are, so Wi-Fi is still blue and a warning still amber.
- **Folio Beta in the Market:** supporters see Folio Beta under Sources, with what you're on, the newest beta and an Update button. It's the same download Settings › Software Update makes: through your supporter code, checked against Folio's own signing key before anything installs, with Update Now or Tonight.
- **Made with AI, said plainly:** a package whose author says AI helped make it shows an AI-assisted tag beside its developer, which tools helped under Information, and the same on the Get sheet before anything installs.
- **A lasting Supporter badge:** back Folio with $15 or more and Settings › Supporter carries a Folio Supporter badge with the month you started. It stays after the code runs out, and after you remove the code. Nothing about it leaves your phone: the code says so, and Folio reads it offline.
- **Refresh Icons:** Settings › Icons & Side Bar loads your icons again, for one that a theme app changed and Folio still shows the old way.
- **Supporters in Settings:** a page beside Credits listing the people who backed Folio and said their name could be there, newest first. The list is a public file Folio fetches like the Roadmap, so a name can go on or come off without an update.

### Changed
- **Large text and right-to-left languages:** rows that held text at a fixed height now grow with the text instead of cutting it off, and back and disclosure chevrons point the right way in Arabic and Hebrew.
- **A proper welcome to the Market:** the first time it opens, the Market says what it is in three rows (packages, the sources you choose, and what is checked before anything changes), then lets you pick how Featured looks. Two steps instead of three, and it no longer says adding a source is coming later.
- **TalkBack keeps up:** it says the new Home page when a swipe lands, and reads a Dynamic Island notice when it appears.
- **Release notes you can read:** What's New draws a release's notes properly now, so a line can stress the word that matters, put a name in italics, or link to [the page that explains it](https://github.com/McCal-Codes/folio/blob/main/docs/user-guide.md), instead of showing the markers around them.
- **Folio's own buttons, menus and alerts:** the work-profile, update, retry and Add widget buttons, the folder's move menu and Rename app are Folio's controls now rather than Android's. Labels wrap instead of ending in "...", and the blues and reds they use were picked so the words stay readable on every background.
- **Panes fit the screen:** the Market and Settings show as many panes as the window has room for, the way iPad's App Store and Settings do. On a Fold's inner screen that's two: a package, a source or a nested setting opens over the list with Back, and the Market's list uses the whole width, in two columns. Three panes are for windows 1200 dp and wider.

### Fixed
- **Apps can't sneak widgets onto Home anymore:** a widget or shortcut only gets added once Android has actually approved it, so an app faking an Add to Home Screen request gets turned away.
- **Your background is decoded once, not three times:** on a cold start Home, the wallpaper preview and Folio's live wallpaper each read your background picture from disk and each kept its own copy in memory, because the shared copy was only ever filled in by the photo picker. They share one now, which is roughly twice the memory back on every start.
- **Android's wallpaper drifts the right distance:** with Android's own wallpaper behind Home, Folio asked it to slide the full width of your pages. A still picture has no spare width to slide, so it moved further than it could and the system squashed the difference. Folio now spreads the drift the way Android's own launcher does, and uses the full travel only for a live wallpaper, which can draw itself as wide as it likes.
- **The island lets go of a card it was told to drop:** swiping a message or notice away from the Everywhere overlay, or an event arriving for a kind you had turned off, used to leave the previous card on the island until the next event happened along. And an island dragged to the bottom edge is kept where you can still reach it.
- **A menu opens on its row, and a tap anywhere closes it** (#117): every pop-up menu of choices was drawn in the middle of the screen instead of under the row you tapped, and nothing but Back would close it, because the menu's window covered the whole screen and took the taps meant for what was behind it. Menus now hang from the control that opened them, flip above it near the bottom of the screen, and close on a tap outside.
- **The Settings search field takes the keyboard unfolded** (#117): tapping it opened the keyboard and closed it again, and nothing could be typed. Settings was reading the room the keyboard left as a smaller screen, folding its two panes back into one, and the list holding the field you had just tapped went with them. The keyboard covers Settings now instead of resizing it.
- **The Market holds its shape while you type** (#117): opening the keyboard, to add a source or to search the Settings tab, moved the tabs from the sidebar to the edge and took the pane beside the list away until the keyboard went again. The Market was reading the room the keyboard left as a smaller screen, the same mistake Settings made. The keyboard covers the Market now instead of resizing it.
- A beta from the supporters' source can be downloaded. Folio asked for the list of betas with your code but downloaded the file without it, and the supporter service turns those downloads away.
- **Double tap on an empty spot works again** (#61): every empty grid cell was taking its own taps, and 0.6.5's extra rows made those cells cover most of a page, so the page's Double Tap action never saw the second tap.
- **The dock steps aside for Today View** (#25): unfolded and upright it sat over Today's widgets and its Edit button. Like iPhone, it now fades away as you swipe there and comes back on Home, so it can't take Today's taps.
- **Icons follow a theme that changes them** (#19): Folio kept an app's icon until its package, density, language or dark mode changed, and a theme engine changes none of those. Icons now also follow Android's theme and overlay changes, and folding or rotating still costs nothing.
- **No more smeared, doubled pages** (#12, #35): choosing Android's wallpaper as your background left every swipe painted over the last. The screen starts again with a window that really shows the wallpaper, and Folio draws its own background whenever it isn't.
- **The Preview bar no longer covers the App Library**: before Folio is your Home app, the space kept for the bar was a guess. It's measured now. Once Folio is the Home app nothing moves, so no one's rows shrink.
- **Every Settings page has a way back**: unfolded and upright, a top-level page showed only the sidebar button, which doesn't read as a way back. It now shows the sidebar button and a way back to Folio together.

## [0.6.6] - 2026-09-21

### Added
- **The Folio Market:** the app icon opens a store with Featured, Sources, Packages, Installed and Settings. For now it's for supporters: a code redeemed in Settings › Supporter opens it, and switches on Beta Updates so the builds come too. Everyone gets it in 0.7.0. Every theme and tweak it hands out is still in Settings for everyone else, so nothing is waiting behind it.
- **Themes and tweaks are packages:** Folio's own now have a page each, with what they do, what you see, screenshots, what's changed, and a privacy label built from what the package asks for rather than from anything its author wrote.
- **Add a source:** any HTTPS address, or a `folio://source/` link. Folio shows the source's key fingerprint before you trust it, remembers it, and says plainly what a changed key looks like.
- **A source is a place you go into:** tapping one opens its page, the way tapping a repo does in Cydia and Sileo, with its address, when it was signed, Refresh and Remove, and every package it offers. A package's page names the source that lists it, and that leads back there.
- **Nothing is applied unchecked:** size, checksum and signature first, then the whole package in one go, with Undo beside it. A package that fails halfway is put back the way it was.
- **Signed by its developer:** not just by the source handing it out, so a mirror can carry a package but can't change it or publish under someone else's name. A `.foliopkg` sent to you carries its own signature too.
- **Progress in the Get button:** a ring that fills, App Store style, and roughly how much longer on the package's page.
- **Open a `.foliopkg`:** a package file installs like a shared theme, and `folio://package/<id>` opens its page.
- **Updates in Installed:** a banner and an optional badge when one is ready. Background refresh is off by default and waits for Wi-Fi when it's on.
- **Safe Mode per package:** if Folio stops twice just after a package changed something, only that package is turned off, and its settings are kept.
- **A source can pull a package:** a withdrawn one can be removed but never installed again, including as an update.
- **Share and Report:** a package's page links to its source's issue form with the id, version and checksum filled in.
- **Your packages travel with your layout:** a layout backup now carries what you got from the Market, and restoring one puts those packages back on the new phone, applied over the layout it just restored, not over the old phone's. Anything Safe Mode had turned off comes back turned off, and one this Folio can't apply waits in Installed instead of being half applied.
- **Folio can install an app from a source** (Settings › Market › Installing apps, off until you turn it on). Some listings are apps of their own rather than something Folio applies, because Android says a keyboard has to be its own app. With this on, Folio downloads one, checks it against the checksum its source signed, and hands it to Android, which asks before installing and names the app itself. With it off, Folio opens the store the author named. What Folio can't check is the app itself: the source vouches for that, and the setting says so.
- **Keyd, for supporters:** redeeming a code adds Keyd's own source to the Market, so the keyboard is there to get. Keyd splits around the crease, corrects and suggests in six languages, and asks for no permissions at all. It's also on [GitHub](https://github.com/McCal-Codes/folio-keyd/releases).
- **Simplified Chinese (beta):** Folio speaks 简体中文 when your phone does, on nearly every screen, the Market included. On Android 13 and later you can also set Folio's language on its own in Android Settings › Apps › Folio › Language. Spotlight and the App Library find Chinese app names by pinyin or initials ("weixin" or "wx" for 微信), and the A–Z list files them under their pinyin letter, as on iPhone. It's a beta translation until a native speaker has reviewed it; corrections are welcome through the Translation form on GitHub.
- **Report a bug by email:** Settings › Help › Report a Bug opens your mail app with Folio's details attached as a file, so it needs no GitHub account, and you can read everything before you send it. GitHub is still there if you have an account.
- **A crash offers to send a report:** after Folio closes unexpectedly or freezes, the next launch asks once whether to send one. It never asks twice about the same report.

### Changed
- Settings shows two columns from 700 dp in either orientation, not only in landscape, and three from 920 dp: the list, the page, and whatever you opened from it, so tapping a tweak no longer replaces the list you tapped it in. Half folded, the divider stays on the crease.
- A settings row title wraps to a second line in a narrow window or at a large text size instead of being cut short, and its value moves underneath when there isn't room beside it.

### Fixed
- Switching Background to Android wallpaper shows it straight away. Turned on from Settings it did nothing to Home's window, so the wallpaper flashed up and vanished and Home was left a flat colour. Left of Home and the setup step's wallpaper button were quietly doing nothing in the same way.
- Coming from Folio 0.6.0 or earlier, Home keeps the four rows you arranged instead of growing on a tall screen. Settings › Home Screen & Dock › Rows turns Automatic on.
- With TalkBack, every saved layout under Layout History offered a button that read as just "Restore", with no way to hear which layout it would put back. Each row is now one thing to land on, and it names the layout and when it was saved.

## [0.6.5] - 2026-09-20

### Added
- **Gauge status glyph:** a sixth Icon style for the Side Bar: the battery as an arc, your connection inside it, and the percentage above (Icons & Side Bar › Icon style).
- **More rows:** Home fills taller screens with up to 3 more rows, the same on both screens of a foldable.
- **Layout sliders:** adjust row, column and dock spacing, widget size and status spacing, and watch Home change as you drag.
- **Apps, dock and status position:** apps at the top, the dock on the side or bottom, and the status anywhere, for each screen.
- **Software Update:** updates install automatically overnight, with release notes, Update Now and Update Tonight. A new install starts there; updating from an earlier Folio does not change what you chose, and if you never chose, Folio stays on Manual until you say otherwise.
- **Big Clock:** a large Lock Screen-style clock with the date and what's next.
- **Try Folio first:** open Folio as a preview before making it your Home app.
- **Clear icons:** frosted glass icons with white symbols, like iOS.
- **Small cover screens:** a focused Home for tiny flip-phone covers, with the time, your dock apps and what's playing.
- **App Library pull-down:** pull down from the top of the App Library for Notification Center and Control Center.
- **Folder badges:** folders show the total of their apps' notification badges.
- **Better bug reports:** Folio notes freezes and restarts, and Report a Bug can include them (kept on your phone).
- **A live Roadmap:** see what's coming, updated from Folio's GitHub page.
- **Supporter codes with months:** a code can carry a number of months that starts the day you redeem it, so one handed out from a batch still gives its full time. Settings › Supporter shows the day it runs out.
- **Folio Keys, for supporters:** a code that carries the keyboard scope shows where Folio's own keyboard has got to, in Settings › Supporter. It's in design (a separate app, because Android needs a keyboard to be its own input method), so there is nothing to install yet.
- **Beta updates for supporters:** with a code that carries beta access, Beta Updates installs the beta builds the same way as any other update. They live in a private repository, and Folio reaches them through the supporter worker, which checks your code and never puts a key on your phone. No GitHub account, no downloading APKs by hand.

- **Move apps without dragging:** TalkBack actions and Alt+arrow keys move apps and folders around Home and between pages.
- **Wallpaper Tint:** one slider from Clear to Tinted glass (Wallpaper & Appearance › Glass).
- **Reduce Transparency:** nearly solid widgets, Side Bar and dock; it also turns on with Android's high contrast.
- **Big Buttons:** optional large Back, Home and Recents buttons over other apps, for when the system's are too small (Dynamic Island › In Every App). They sit above Android's own navigation, hide in full-screen apps and fade when idle.
- **Swipe Down on Home:** pick what a swipe down the middle of Home does: Spotlight, Notification Center or nothing (Gestures & Actions). Set to Notification Center it works like Android's usual one-finger pull-down, and follows your choice of Folio's panels or Android's own shade.
- **Move the buttons:** long-press and drag Big Buttons up the screen, away from the keyboard or an app's own bottom bar; Settings puts them back.
- **Rename apps:** long-press an app, More › Rename… and give it any name; the new name shows on Home, in the dock, in folders, in the App Library and in search. Searching the app's original name still finds it, and clearing the field puts that name back.
- **The island steps aside in full screen:** the island in every app now leaves full-screen video and games alone, with switches for full screen and landscape (Dynamic Island › In Every App).
- **Predictive back:** folders, the App Library and Settings follow your back swipe before closing.

### Changed
- Cleaner Settings, like iOS: every group is one card with thin dividers between rows, explanations sit under their card, and actions line up with the other rows. Island pop-ups have their own group.

### Fixed
- **Google Discover works again:** Folio was asking the Google app for an old version of its feed connection, and newer Google app builds answered with nothing at all. It now asks for the same version Android's own launcher does.
- **No button to a page that isn't there:** with Today View and Discover both off, Home stops offering the button that led nowhere.
- **The island lets go of forgotten music:** a player that was paused and closed used to sit in the island for good, with buttons that did nothing.
- With a keyboard, Tab and the arrow keys now move between apps on Home instead of stopping on empty spaces behind them.
- If your saved Home layout can't be read, Folio now says so and offers to restore a backup or start fresh (keeping a copy), instead of quietly showing an empty Home.
- Half folded, Home keeps off the hinge: unfolded pages stay on their side of a book fold, the bottom dock moves to one half, and on a table-style fold the status stays above the hinge and the dock goes below it.
- On tall phones, Home sits centered in the screen instead of high up with empty space below.
- On narrower phones, apps keep more space between them: icons never take more than 80% of their column.
- Android's status bar no longer reappears over the Side Bar status after changing Display size, Smallest width or window size.
- On folds with more than one hinge (tri-folds) and dual-screen phones, sheets, menus and alerts stay on one panel instead of crossing a hinge.
- With a larger Smallest width (like 600 dp for the cover screen), the unfolded screen no longer gets bigger icons and the cover's two-column layout: Folio's tablet scaling now judges the screen at the phone's own density.
- The Home Screen & Dock preview shows the unfolded layout on the Inner tab.
- An app that re-posts the same notification (like a repeating warning) no longer pops up in the island each time; it stays in Notification Center.
- In short landscape windows, the widget row on the left no longer runs under the page controls.
- The Dynamic Island now always covers a punch-hole camera, instead of sitting below it on screens where the camera is close to the top (like the Galaxy Z Fold7's inner screen).
- A panel or Spotlight that was closed while Folio wasn't drawing can no longer stay on screen (or leave Home blurred) with no way out: it finishes closing on its own.
- Turning on Folio gestures now names Samsung's "App was denied access" message and walks through Allow restricted settings step by step.
- Google Discover beside Home is only used on Android 17 and newer; on Android 16 (reported on the Galaxy Z Fold7, issue #12) it could leave smeared copies of Home on screen, so Discover opens as its own page there.
- Creating a folder no longer shows a second copy of it on the unfolded screen's extra left page.
- Folders can be moved again: dragging one no longer drops it onto itself (which buzzed and put it back).
- Folding or rotating no longer throws away a search: the pull-down panel closes, Spotlight keeps what you typed, and the panel now closes on the first fold after a restart too.
- In the Gauge, the mark shown when there's nothing to connect to sits in the middle of the ring, level with the Wi-Fi one.
- Music that stops to load no longer drops out of the island and back in on every skip, and a player with no track title no longer hides one that has it.
- On a phone set to Turkish, supporter codes typed in lower case can be redeemed, the Roadmap shows every item, and What's New keeps its symbols: Folio was reading its own data through the phone's language.
- The time on each notification ("now", "5m ago") follows your language and its plural rules, and a phone whose clock has just been corrected no longer shows a notification as arriving in the future.
- The last of Folio's own words follow your language: brief messages (a file that isn't a theme, an app that won't open, a redeemed code) and the text left in What's New, Choose Home Apps, widget editing, Up Next, restore and the alerts.
- A backup now carries the names you gave your apps, and restoring puts them back: renames lived in one file on the phone and went no further.
- A clock that comes back years ahead after a flat battery no longer expires your supporter code for good; winding the date back still can't hand time back.
- Layout History keeps the snapshots it can read instead of dropping all ten when one of them can't be, and a second damaged layout no longer writes over the first rescue copy.
- A message with the same words as the last one shows in the island again: repeats are judged by when the app posted them, and dismissing one ends its quiet window.
- An automatic update tidies up after itself again, instead of leaving the installed APK in Folio's storage.
- Opening Software Update no longer uses up the day's check, so an automatic update still installs; a check that Android stops mid-way no longer stops the daily check for good.
- A folio://redeem link can add a supporter code but never replace one you already have. Swapping is done in Settings › Supporter, which the link now opens.
- Unfolded, the row of page dots, Search and App Library sits under Home instead of across the middle of the screen, where it ran into the widgets on the page beside it.

## [0.6.0] - 2026-09-16

### Added
- Badge options: an iOS, Classic or Glass look, three sizes, and blue, green, orange and purple colors, with a live preview (Icons & Side Bar).
- Folder options (columns and a glass, solid or clear background), app name size, and Animation Speed (Relaxed, Standard or Snappy).
- Side key: choose what holding it does: Folio's picker, a Google search without AI Overviews, or talking straight to Google, Claude or Perplexity (Side Key page).
- Software Update: check GitHub for a new Folio, download and install it after verifying its checksum and signing key; optional daily checks, update notifications and automatic installs.
- Roadmap in Settings (replaces Coming Soon): what's in this update, what's next, later and being explored.
- Tweak Library: tweaks are packages you Get (Sileo-style) and only the ones you get show in Settings › Tweaks. New installs start with none; updating keeps the tweaks you already use.
- Hidden apps stay out of the App Library, like iOS: they're listed in Settings › Search & App Library after you unlock with your fingerprint, face or PIN. A Work Apps switch hides the Personal and Work toggle.
- Left of Home can be None (Settings › Today View), alongside Today View and Google Discover.
- Support Folio in Settings, for buying me a coffee on Ko-fi.
- Save Backup and Save Theme go straight to Download/Folio; backups can be named (the default is dated).
- Fold transition: a light tick as the hinge passes halfway, a soft light sweep and a slight settle in size as the open screen clears, and a Preview slider in Fold & Displays to see the effect without folding. Phones whose hinge sensor reports in-between angles follow the real angle. Ideas from FoldFX.
- Beta Updates in Software Update: choose Folio Beta to get GitHub pre-releases too. Turning it off keeps your beta until a newer public release.
- Other notifications in the Dynamic Island (off by default): new notifications from apps you choose pop up in the island like messages, following each app's alert settings, Do Not Disturb and "Don't double up with Android pop-ups". Choose apps in Dynamic Island › Other Notifications.
- Swipe an island pop-up up to hide it early, on Home and over other apps; it stays in Notification Center.
- Finish Setting Up: if required setup is left, a card on Home brings you back (not on the first day; Not Now waits three days; never again once setup is finished), and Settings shows a progress ring.
- Folio's own short messages (an app that won't open, a panel Android couldn't open) show in the Dynamic Island instead of a toast when the island is on screen.
- Status styles for the Side Bar: Rings (battery, Wi-Fi and cellular as Activity-style rings) and Ring with Percentage, alongside Ring, Icons and Battery only.
- A red bell in the Side Bar while the phone is on silent or vibrate, like iPhone (Icons & Side Bar › Silent mode icon). "Color battery when charging or low" is now "Status colors" and covers it.
- Rounded screen corners (Wallpaper & Appearance › Screen Corners, off by default): black iPhone-style corners over Home with a size slider, for the iPhone Duo look (issue #8).
- Settings opens where you left it, on the same page and scrolled the same, like iPhone Settings.
- Report a Bug and Show Welcome Again moved into Settings › Help, so the main list is shorter; searching Settings for "bug" or "welcome" finds them.
- Screenshot Mode (Advanced): Folio shows 9:41 with full battery and signal and hides notifications, music, messages, device names, calendar events and alarms, for sharing your setup. Turns off by itself after 30 minutes.
- Headphones & speakers: when Bluetooth headphones or a speaker connects, an iPhone-style card shows its name on Home, and the Dynamic Island shows it over other apps.

### Changed
- New muted teal app icon. Olive and Soft are alternate icons in Wallpaper & Appearance › App Icon.
- Development builds install as a separate "Folio Dev" app with an amber icon, next to the release.
- Folio no longer asks for the Nearby devices (Bluetooth) permission: headphone and speaker names now come from Android's audio device list.

### Fixed
- Home can no longer stay blurred behind a Lock Cover that was turned off while it was about to show.
- Folio's short notices fall back to a regular message while Settings or another sheet covers Home, so they're never hidden behind it, and they don't follow you into other apps.
- The dock no longer overlaps the Side Bar's status when it grows (Focus or Silent icons, the Rings styles); it always starts just below it, including in Discover.
- In jiggle mode, the dock's remove buttons stay inside the Side Bar instead of hanging off its edge.
- Spotlight's Suggestions show whole rows only, so the unfolded screen shows one clean row above the keyboard instead of a cut-off second row.
- Unfolded in portrait, Home's four columns spread across the screen at every Screen zoom instead of sitting in a narrow block with a wide gap (issue #10).
- On phones without Samsung's "Continue apps on cover screen" setting, setup no longer lists it as a required step that can never be done.
- Scrolling lists and grids fade softly at their edges instead of being cut off.
- The App Library on the unfolded screen shows more, phone-sized category tiles (five across in landscape) instead of two giant columns (issue #9), and no longer covers the Side Bar's status.
- Beta labels no longer wrap in narrow Settings layouts.
- Turning on Folio gestures explains Android's "Allow restricted settings" step for apps installed from a file, with a button to App Info.
- Friendlier setup: Folio's own icon on the welcome page, Skip on every step, setup moves on by itself after you allow something, clearer tips at the end, and choosing a wallpaper no longer restarts the screen.
- Phone-sized screens keep the phone layout when Developer options' Smallest width or Display size is changed (a Fold8 cover set to 600dp got the unfolded layout, with the dock at the bottom and wide margins).
- Folders take more than two apps from the app menu: Create Folder becomes Add to Folder once you have one, listing your folders first.
- Scrolling lists fade at their edges more softly, and the fade grows in as you scroll instead of popping in.
- App Library folders open without building every app at once, and holding an app there opens its menu.
- Setting Folio up no longer leaves the screen blurred and unresponsive. A sheet opened while Folio was behind a system permission screen could stay invisible and still take every tap, so Home sat blurred with no way out but a restart. Sheets now always appear, and the Home button closes anything that's open.
- Setup and full-screen Settings pages are smoother: Home no longer blurs behind a page that covers it, which was work nobody could see.
- Message pop-ups can be handed back to Android: every messaging app in Dynamic Island settings has a button to its own notification settings, both ways, and that list stays reachable while the island is off. Apps switched to the island used to end up with no pop-up at all and no way back.
- With Dock Magnification on, sliding along the side dock no longer opens Spotlight.
- The fold preview in Settings shows one Side Bar, like the open Fold, instead of one on each page.
- Folio's own screens always show its real icon; only the Folio Dev launcher icon is amber.
- Back in Settings returns to the page you came from, such as Tweaks or Focus, instead of the top of Settings.
- Unfolded in portrait, going back to the top of Settings opens the settings list again instead of a mostly empty page.

## [0.5.1] - 2026-09-16

### Added
- Software Update (Settings › Software Update): check GitHub for a new Folio, download and install it after verifying its checksum and signing key, with optional daily checks, update notifications and automatic installs. Beta Updates lets you try pre-releases.
- Support Folio in Settings, for buying me a coffee on Ko-fi.
- Hidden Apps and Work Apps in Settings › Search & App Library.

### Changed
- Friendlier setup: Folio's icon on the welcome page, Skip on every step, setup moves on by itself after you allow something, and clearer tips at the end.
- Turning on Folio gestures explains Android's "Allow restricted settings" step for apps installed from a file, with a button to App Info.
- Hidden apps no longer appear in the App Library; they're listed in Settings after you unlock with your fingerprint, face or PIN.
- Choosing a wallpaper no longer restarts the screen.

### Fixed
- Setup no longer leaves the screen blurry and frozen until you restart the phone. The Home button now also closes anything Folio has open.
- Setup and full-screen Settings pages are smoother.
- Apps you switched to the Dynamic Island can go back to Android's own pop-ups: Settings › Dynamic Island lists each messaging app with a button to change it, even while the island is off.
- The App Library on the unfolded screen shows phone-sized category tiles instead of two giant columns (issue #9).
- Phone-sized screens keep the phone layout when Developer options' Smallest width or Display size is changed.
- Folders take more than two apps: Create Folder becomes Add to Folder once you have one.
- App Library folders open without building every app at once, and holding an app there opens its menu.
- With Dock Magnification on, sliding along the side dock no longer opens Spotlight.

## [0.5.0] - 2026-09-15

### Added
- Alternate app icons: choose Olive or Soft in Settings › Wallpaper & Appearance › App Icon.
- Folio shows up as an app: its icon (in the App Library or another launcher) opens Settings, like iOS Settings.
- Clear Badge in an app's long-press menu hides its badge until a new notification arrives.
- Clock & Calendar setting: the apps' own icons, or live icons that are Automatic, Light or Dark.
- Coming Soon in Settings: what's planned next (including Page Effects, inspired by Barrel), with a Suggest a Feature link.
- Version History in What's New, with every earlier version.
- Beta: Layout History saves Home before big changes (restoring a backup, Arrange Like iPhone, restoring an older layout) so you can go back. Settings › Backup.
- Beta: Recent App Dots mark dock apps you used in the last hour, using Usage Access. Settings › Home Screen & Dock.
- Credits for SnowBoard, Apex, Icon Restore, Lynx 2, ColorBadges, Barrel and Contributor Covenant.
- Email the Developer and Buy Me a Coffee (Ko-fi) in Settings › Help.

### Changed
- New olive green app icon.
- Glass settings in Wallpaper & Appearance: a Clear, Light, Frosted or Solid style, plus sliders for widget frost, Side Bar frost and the outline.
- Shorter, iPhone-style setup: Home app, notifications, pull-down gestures and a look. Optional permissions are asked where they're used.
- Settings has one list of permissions (Privacy & Permissions) instead of a separate Setup Checklist, and no repeated Home app rows.
- Settings tidied: the Notification Center and Control Center switch sits with their options, page dots and haptics moved to Gestures & Actions, and the Side Bar frost moved into the new Glass settings.
- Live Clock and Calendar icons match the icons around them: light or dark to fit the app icons, the tint color for Tinted, and an icon pack's own Clock and Calendar when it has them.
- Notification Center slides down like iOS instead of zooming, stacks slide apart when you expand them, swipe buttons grow in as you swipe, and cards press down when tapped.
- Built with Android Gradle Plugin 9.4, Gradle 9.7.1 and Kotlin 2.4.20.
- Layouts are checked against real Android phone, foldable, tablet and desktop screen sizes from Android Studio's device list.

### Fixed
- Back always closes Spotlight first, instead of sometimes changing the Home page behind it.
- Spotlight's keyboard comes back if it didn't appear when Spotlight opened.
- Unfolded, Settings › Icons & Side Bar no longer shows its Home preview twice.
- Restoring a Layout History snapshot handles removed apps and folders the same way as a normal refresh, so the saved layout always loads again.
- Switching the app icon keeps Folio's place on Home, in the dock and in folders.
- Sharing a theme file to Folio reads it in the background, so a slow or broken file can't freeze Home.
- Spotlight no longer closes when the keyboard drops for a moment while folding, rotating or switching to voice typing.
- Clear Badge stays cleared while an app updates the same notification, and is hidden when badges are off.
- Setup resumes on the right screen after an update.
- Discover's Side Bar follows the Glass outline setting.
- Holding the side key opens Folio's assistant picker on phones that start the assistant through a voice interaction service (like One UI 9). Folio asks Android not to share the current app's screen with it, and the Side Key page warns when Good Lock's RegiStar can override the key.
- The live Clock icon no longer occasionally stays a normal icon.
- An app's long-press menu no longer cuts off its last row when Clear Badge is showing.
- Spotlight's Cancel hides the keyboard and has a bigger touch target.

### Known issues
- During setup, the screen can go blurry and stop responding, and only a restart clears it: a sheet opened while Folio was behind a system permission screen stays invisible while still taking every tap. If it happens, restart the phone; setup works afterwards. Fixed in 0.6.0.
- Lists and grids are cut off hard at their edges instead of fading out (App Library, Settings, Notification Center, Spotlight and folders). Fixed in 0.6.0.
- On the unfolded screen in portrait, the App Library shows two oversized columns and can cover the Side Bar's status. Fixed in 0.6.0.

## [0.4.0] - 2026-09-14

### Added
- Focus: Do Not Disturb, Sleep, Personal and Work, with schedules, silencing, Home pages to show or open, Android 15 look changes, a Control Center module, and Focus actions.
- iOS-style app downloads: progress rings on updating icons, a Downloading row in the App Library, a blue dot on new apps, and an option to add new apps to Home.
- Add to Home Screen for widgets and shortcuts that apps offer, including websites from Chrome.
- Suggestions for this time of day in Spotlight, the Today View and a new Suggestions widget; Up Next widget, and Up Next on StandBy and the Lock Cover.
- Icon Stacks: swipe down on a Home icon to fan out the apps stacked behind it.
- Per-page icon size and labels.
- Themes: Classic, Dark, Tinted and Clear, plus saving and importing theme files.
- Half folded with the phone upright, Home rows that would sit in the fold move below it.
- What's New after an update.
- Report a Bug in Settings opens GitHub's bug form with your Folio version and phone filled in.
- Icon packs made for Lawnchair or Apex show up in Icon Pack, along with ADW and Nova packs.
- Share a theme file to Folio from Files or Chrome to apply it, and community themes in the repo's themes/ folder.
- Big screens: on tablets, Chromebooks and desktop windows Folio scales up like iPad instead of looking like a phone layout in a big window. Phones and foldables are unchanged.

### Changed
- Everything says Folio now: the README, user guide, troubleshooting, privacy notes, backup file name (folio-layout.json) and release files.
- Settings choices use iOS controls: a menu row with the current value that opens a checkmark menu, and segmented controls for two or three options.
- The Status Bar picks its text color from its frosted background, and uses stronger colors on light wallpapers.
- The side column is now called the Side Bar (status bar, Dynamic Island and dock), as on iPhone Duo.
- Edit while icons wiggle opens a short iOS 18-style menu under the button.
- Smart Rotate moves a stack to the widget that matters now.
- Settings previews draw Home with its real layout, widgets, status bar and dock.
- The status bar shows cellular bars, an airplane, or a searching fan when there's no Wi-Fi, instead of a line.

### Fixed
- Spotlight's and Settings' search fields no longer grow and jump when you start typing.
- Edit mode no longer pushes Home down or cuts off the bottom row: unfolded, + / Edit / Done sit beside the page dots and the Edit menu opens upward; folded, the bar clears the Dynamic Island.
- Apps and shortcuts added while a Focus hides pages go to a page that's showing.
- Selected rows in the Settings sidebar use a rounded, inset highlight; the widget resize hint is rounded.
- Long app menus no longer push Edit Home Screen and More out of view.
- Settings pages that could miss updates while a Focus hides Home pages.

## [0.3.0] - 2026-09-14

### Added
- Layouts that follow size classes: the unfolded screen in either rotation, the cover in portrait and landscape, and short windows.
- iPhone Duo-style Home: two columns on the cover in landscape, a centered page with a bottom dock bar in unfolded portrait.
- Settings split view with a sidebar on the unfolded screen; centered form sheets and iOS alerts.
- Hinge awareness: sheets, alerts and panels move off the fold when the phone is partly folded.
- Dynamic Island that wraps a side-edge camera, expands along the edge, and handles calls; live activities under the status bar as an option.
- Arrange Like iPhone; Lock Cover layout for wide windows; fold effect that follows the hinge and rotation.

## [0.2.0] - 2026-09-14

### Added
- Full-screen iOS-style Settings with search, Privacy & Permissions, Side Key and Lock Cover pages, and credits.
- Setup Assistant-style onboarding that walks through every permission Folio uses.
- Tweaks with per-screen overrides, Safe Mode after repeated crashes, local crash reports, and the Folio app icon.

### Changed
- Everything says Folio now: the README, user guide, troubleshooting, privacy notes, backup file name (folio-layout.json) and release files.
- Accessibility labels, text moved to string resources, and iOS styling across settings and pickers.

## [0.1.0] - 2026-09-13

### Added
- Folio, forked from DuoLauncher by jakesgoodapps: Notification Center and Control Center panels, Spotlight, Dynamic Island, jiggle mode, quick replies, Smart Stacks, Today View, the iOS widget gallery, Quick Settings tiles, page scrubbing, automatic text color over the wallpaper, tinted glass, and tweak-inspired features.

## DuoLauncher history (before Folio)

### 0.15.0-beta01

First public-beta preparation release. Tested scope and APK checksums accompany the release package.

- Add a skippable introduction for fresh installations and help through customization; existing layouts open directly.
- Improve recovery choices when Google Discover is unavailable.
- Show distinct Wi-Fi levels across the dot and three arcs.
- Preserve the current wallpaper when photo selection is canceled or fails, and improve interrupted preview recovery and temporary permission cleanup.
- Prepare optimized release builds, external signing, public-source export, and automated build checks.
- Add installation, update, permission, contribution, and compatibility documentation.

### 0.14.7

- Restore long-press pickup in scrollable Android widgets while preserving native vertical scrolling.

### 0.14.6

- Preserve the selected Home page or unfolded pair when returning from an app.

### 0.14.5

- Allow vertical scrolling inside native Android widgets.

### Earlier development

Home/All apps paging; right-side dock; overlapping unfolded pages and an unfolded-only workspace; native widgets and visual selection; cross-page dragging and temporary pages; work/personal profiles; Home folders; local wallpapers and daylight appearance; layout backup; Google search/Discover; long-press customization; and motion/recovery refinements.
