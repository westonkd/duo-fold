# Design standard (`DES`)

Folio looks and behaves like iOS, running on Android. This standard says what that means in pixels, and where
Folio's own identity takes over.

## Rules

### Identity

- **DES-1 MUST** be iOS-inspired. A new surface starts from its iOS equivalent (grouped lists, sheets, pills, context
  menus, springs). Where iOS has none (smart collections, home modes, a command palette), model it on a well-known iOS
  jailbreak tweak, not an Android launcher convention.
- **DES-2 MUST NOT** copy Apple assets: no SF Symbols, Apple fonts, wallpapers or screenshots. Folio borrows patterns
  and measurements, not artwork.
- **DES-2a MUST NOT** ship a background that Folio draws in code. A wallpaper is real art: a photograph or a painting
  a person made. A generated scene is what every launcher falls back on, so it cannot be Folio's identity, however
  carefully it is drawn. This covers anything offered to the user as a wallpaper, bundled or through the Market. It
  does not cover the materials a surface is made of: a sheet's glass, a card's fill, a gradient inside an icon and a
  scrim behind text are not backgrounds, and DES-14 and DES-16 still govern those.
- **DES-2b MUST** be able to name the artist, the work and the license for every image Folio ships, and MUST NOT ship
  an image missing any of the three. Public domain qualifies (a museum's open access scan, a work old enough that its
  author's rights have lapsed), and so does a license whose own text permits redistribution inside an application
  (CC0, Unsplash, Pexels). "Found on a wallpaper board", "no watermark on it" and "we credit the artist" do not:
  credit is not a license, and an image with no author has no license to give. Record the artist, title, date, holding
  collection, source URL and license beside the file, show the credit in the app, and take the image from the original
  at full resolution rather than from the copy that was passed around.
- **DES-3 SHOULD** use Android's own behaviour where users expect it (system share sheet, permission dialogs,
  predictive back, the system notification shade when chosen) and describe it in iOS terms in the UI.

### Tokens

- **DES-4 MUST** take colors from `FolioColors` (or `FolioColors.Value`, where a color is held as a number), glass fills from `FolioGlass`, springs from `FolioMotion`, and app
  label sizes from `HOME_LABEL_SP` (new code). A hex literal is allowed only inside a token file, a theme file, or a
  deliberately fixed artwork such as `DuneWallpaper`.
- **DES-5 MUST NOT** re-type a token's value. `Color(0xFF0A84FF)` outside `FolioTokens.kt` is a bug waiting to drift.
- **DES-6 SHOULD** use the spacing, radius and type scales below. A value off the scale needs a reason in a comment.
- **DES-7 MUST** add a token, not a literal, when a new value is used in more than one file.

#### Scales (`FolioSpace`, `FolioRadius`, `FolioType`, `FolioRow` in `FolioTokens.kt`)

These are the values Folio already used most, so adopting them changed nothing on screen.

| Scale | Values |
|---|---|
| Spacing (dp) | 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 32 |
| Corner radius (dp) | iOS 26 and 27's values. Capsules for search fields, buttons, chips, pills, segmented controls and toasts. 10 (a pressed highlight), 12 (list rows), 14 (small cards, thumbnails), 16 (sheet groups' highlights), 24 (panels), 26 (menus, grouped cards, Spotlight's platters), 28 (widgets), 30 (dock and rail), 34 (alerts), 38 (open folders, sheets). Icon and thumbnail corners follow their size (DES-8a), and a container around icons is concentric with them (DES-8) |
| Type (sp) | 34 Bold large title (pages, sheets, open folders), 28 title, 22 title 2, 20 title 3, 17 body, 16 callout, 15 subheadline, 14 compact (dense rows, chips), 13 footnote, 12 SemiBold caps group label (+.4 tracking), 11 caption, 40 a widget's big number; Home labels via `HOME_LABEL_SP` |
| Row height (dp) | 48 action and menu rows, 52 navigation rows |

- **DES-8 SHOULD** make corners concentric: an inner radius is the outer radius minus the padding between them.
- **DES-8a** An app icon's or thumbnail's corner radius follows its size (roughly a quarter of it), so it keeps the
  squircle's proportions. Those radii are written beside the size they belong to, not taken from the scale.

### Components

- **DES-9 MUST** build from Folio's components before writing a new one:

  | Need | Use |
  |---|---|
  | Switch, chip, slider, search, rows, pop-up menu, segmented control | `IosControls.kt` |
  | Grouped settings card, note, action | `SettingsGroup.kt` |
  | Sheet group and label, app picker | `LauncherSheetParts.kt` |
  | Bottom sheet, form sheet, alert, full-screen page, peek capsule | `FolioSheet.kt` |
  | App context menu | `AppContextMenu.kt` |

- **DES-10 MUST NOT** use a Material 3 control directly in user-facing UI when a Folio component exists (new code).
  Material may sit underneath a Folio component, as `IosSlider` does.
- **DES-11 SHOULD** add a missing component to one of those files, with a short comment on its iOS reference, rather
  than building it inline in a screen.

### Color and materials

- **DES-12 MUST** reserve the accent color for what needs attention: selection, badges, unfinished
  setup. If everything is blue, nothing is.
- **DES-13 MUST** use `FolioColors.Red` (or `RedLight` on light surfaces) only for destructive actions and errors.
- **DES-14 MUST** draw Home's glass surfaces with Liquid glass (`liquidGlass`), the one glass Folio has.
- **DES-15 MUST NOT** run a per-frame Compose blur. Blur comes from the window (`FolioDialogWindow`) or a cached
  shader pass.
- **DES-16 MUST** keep text readable over any wallpaper: use `HomeInk` on Home, and a scrim or glass behind text that
  can't rely on it ([A11Y-6](accessibility.md)).

### Icons and imagery

- **DES-17 SHOULD** use `Icons.Rounded` (AutoMirrored where the glyph has a direction) for UI glyphs, 18 to 22dp.
- **DES-18 MUST** respect the user's `IconStyle`, `IconShape`, badge options and icon pack everywhere an app icon is
  drawn, by going through `AppIcon`.

### Theming

- **DES-19 MUST** draw Home surfaces in the light appearance through `LocalDuoPalette`. Menus, folders and the App
  Library's cards are light glass (`menuGlass`, `FOLDER_FROST`), as in iOS 27. Spotlight's and the App Library's search
  fields are the same light glass capsule (`IosSearchField` with `glass`), in Home's text color. Settings,
  sheets and alerts are iOS's light grouped list (`FolioSheetTheme`): white cards on `LightBackground`, iOS's label
  greys, and the accent's darker ink (`FolioAccents.TealOnLight`). Shared controls read `LocalLightSheet` to tell a
  sheet from Home's wallpaper and glass.
- **DES-19a MUST** put a group's explanation under the group, as an iOS footer: a `CardNote` after the last row of a
  `SettingsCard`, or `SettingsFootnote` under any other group. Notes never sit between rows or at the top of a card.

### Words

- **DES-21 MUST** put user-visible text in `strings.xml` (new code), with plurals as plurals.
- **DES-22 SHOULD** write the way iOS Settings does: row labels, buttons and page titles in title case ("Show App
  Names", "Clear Suggestions History"), notes and messages in sentence case. Product names are not translated, and
  Apple's own feature names (Spotlight, Dynamic Island, Control Center) stay out of what users see, because Folio is
  published on Google Play.

## Where Folio is today

Good:

- `FolioColors`, `FolioGlass`, `FolioMotion` exist (`FolioTokens.kt`, `FolioGlass.kt`).
- A real iOS component set: `IosSwitch`, `IosSegmented`, `IosMenuRow`, `GroupedCard`, `SheetGroup`, and
  `ModalBottomSheet` / `AlertDialog` replacements that shadow Material's by name (`FolioSheet.kt:116`, `:237`).
- Glass is Liquid glass everywhere on Home, blur is window-level, and there's no per-frame blur.
- Icon looks, shapes, badges, live icons and icon packs all flow through `AppIcon` and `LocalIconLook`.

Not yet:

- Every screen uses the scales: 1,018 values moved onto tokens across 48 files without a pixel changing, checked by
  expanding the tokens again and diffing against what was there.
- What stays a literal on purpose: icon and thumbnail radii, which follow their size (DES-8a); pill shapes; and the
  sizes of things (an icon, a sheet's width), which are dimensions rather than spacing.
- Outside the token files there are 30 `Color(0x…)`, 11 colors written as numbers, 96 `RoundedCornerShape(N.dp)`
  and 35 `fontSize = N.sp`. `DesignTokensTest` holds all four counts so they can only go down. Counting numbers as
  well as `Color(0x…)` is what found 78 retyped palette values hiding in tweak tints, Focus modes and saved layouts.
- Material still used directly: `AssistChip` in `AppLibrary.kt`, and Material's progress indicators. The buttons,
  the rename alert and the folder menu are Folio's own now.
- Material underneath a Folio component, the `IosSlider` pattern: `FolioMenuPopup` is Material's `DropdownMenu`
  wearing Folio's surface, for the anchoring and outside dismissal that the hand-placed `Popup` got wrong (#117).
- `FolioColors` now has status colors (`Warning`, `RedSoft`), the light-appearance variants (`GreenLight`,
  `RedLight`, `CyanLight`, `LightBackground`), the surfaces a sheet and a menu sit on, and `FolioColors.Value` for
  the places that hold a color as a number.

## Gaps

| # | Work | Size |
|---|---|---|
| 1 | ~~Add the scales and move every screen onto them~~ (done) | M |
| 2 | ~~Replace re-typed token hex values; one green~~ (done: 51 colors moved onto tokens, `IosGreen` is now `FolioColors.GreenLight`, iOS's light-appearance green) | S |
| 3 | ~~Move the rename dialog to Folio's `AlertDialog`, the direct Buttons to one `FolioButton`, the folder's menu to `FolioMenuPopup`~~ (done, #106) | S |
| 4 | ~~Light-surface variants and named status colors~~ (done: `Warning`, `RedSoft`, `CyanLight`, `Teal`, `SheetSurface`, `MenuSurface`, `LightBackground`, plus `FolioColors.Value` for colors held as numbers) | S |
| 5 | ~~A check that counts raw colors, radii and text sizes outside the token files~~ (done: `DesignTokensTest`) | S |
| 6 | Remove the drawn backgrounds (`DuneWallpaper`'s dunes, `MoonColumnWallpaper`) and replace them with licensed art under DES-2a and DES-2b, keeping the picker, the drift and the scrim | M |
| 7 | ~~Settings and sheets in the light appearance, with iOS 26 capsule alert buttons~~ (done) | L |
