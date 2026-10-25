# iOS 27 alignment

Where Folio stands against iOS 27 and iPhone Duo as of 24 Oct 2026, and what to work on next. The goal is in the
[README](../README.md): look and move like the iPhone, and get there through consistency and motion rather than
features.

## Sources

- Photos of iPadOS 27 on a real iPad (an open folder and the App Library), Apple's iPhone Duo marketing and HIG art,
  and two hands-on photos of iPhone Duo. Kept outside the repo (DES-2: no Apple artwork in Folio).
- [Six Colors, iOS 27 review](https://sixcolors.com/post/2026/09/ios-27-review-little-things-mean-a-lot/) and
  [iPhone Duo, 24 hours](https://sixcolors.com/post/2026/10/iphone-duo-24-hours-in-the-splash-zone/)
- [MacStories, iOS and iPadOS 27 review](https://www.macstories.net/stories/ios-and-ipados-27-review/2/) (pages 2, 3, 5)
- [MacRumors, how Liquid Glass is changing in iOS 27](https://www.macrumors.com/2026/06/10/how-liquid-glass-is-changing-in-ios-27/)
- [Daring Fireball, the iPhone Duo](https://daringfireball.net/2026/10/the_iphone_duo),
  [Gizmodo review](https://gizmodo.com/iphone-duo-review-2000827502),
  [TechRadar review](https://www.techradar.com/phones/iphone/apple-iphone-duo-review)
- Apple: [iPhone Duo newsroom](https://www.apple.com/newsroom/2026/09/apple-unveils-iphone-duo/),
  [HIG: Designing for iPhone Duo](https://developer.apple.com/design/human-interface-guidelines/designing-for-iphone-duo),
  [HIG: Materials](https://developer.apple.com/design/human-interface-guidelines/materials),
  [HIG: Widgets](https://developer.apple.com/design/human-interface-guidelines/widgets),
  [WWDC25 Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/),
  [WWDC25 Get to know the new design system](https://developer.apple.com/videos/play/wwdc2025/356/),
  [WWDC23 Animate with springs](https://developer.apple.com/videos/play/wwdc2023/10158/)

The HIG's Materials and Motion pages have not been updated for iOS 27, so the iOS 27 glass changes below come from the
reviews. Nothing found documents iOS 27's folder, App Library, jiggle or haptic values. Those items use iOS 26 and the
photos as the reference.

## What iOS 27 changed that matters to a Home screen

- **Calmer glass.** The default glass is more tinted than iOS 26's and diffuses busy content better. Each glass shape
  gets a **darkened outer edge** and a **brighter specular highlight**, so layers separate more clearly. Context menus
  are now glass, not dark.
- **Sharper, more vibrant icons**, with layered glass inside the icon.
- **Extra-large widgets** that fill a whole Home page.
- **Search and Siri in one field.** Swipe down on Home and a **dark glass bubble with silver overtones** drops in.
- **Faster everything.** Page swipes and the App Library scroll were a named performance target.

iPhone Duo adds the side column: the time, then a round status button (Wi-Fi and cellular around the edge, battery),
then a vertical dock whose **icons are smaller than the grid's**, then a round Search button at the bottom. Controls sit
on the outer edge, the column has a fixed width and flexible item heights, and on rotation the dock slides off and
comes back in from the new edge. During a fold the content stays still while the moving half frosts over.

## Priorities

Ordered by how much each makes Folio feel like one designed thing. Each item names the files involved.

### 1. One glass, everywhere

DES-14 says Liquid glass is the only glass. The code disagrees.

- **Flat `Glass.copy(alpha = …)` fills** with alphas from .08 to .97 stand in for glass in `LauncherScreen.kt`
  (1233, 1364, 1374, 1385, 1397, 1451, 1689), `HomeWidgets.kt` (174, 274), `HomeWorkspace.kt` (341, 463, 546) and
  `WidgetPlaceOverlay.kt:112`. Move each onto `liquidGlass` with a `GlassRole`, or onto a named scrim if it isn't glass.
- **The jiggle pills and the Search pill share a strip but not a material.** `JigglePill` is flat white
  (`Jiggle.kt:116`) and `HomeSearchPill` is Liquid glass (`Jiggle.kt:142`). Make both glass.
- **Three bases for one kind of card.** Folders use `SurfaceCard`, the App Library's category card uses `SheetSurface`,
  menus use `PopoverSurface`. iOS 27 uses the same glass for all three. Pick one base.
- **Frost values written in place** (`HomeTiles.kt:283,341`, `Jiggle.kt:132`) should become `GlassRole`s.
- **Add iOS 27's darkened edge.** `drawLiquidGlass` (`FolioGlass.kt:284`) draws only a white rim. Add a thin dark outer
  stroke under it and raise `RIM_BRIGHT` slightly. This one change touches every glass surface at once and is the
  most visible difference between iOS 26 and 27 glass.
- **Context menus as glass.** `AppContextMenu.kt:189` and `LauncherActionSheet.kt:105` sit on `PopoverSurface`. Make
  them light glass with the same rim as folders.

### 2. One icon shape and a short list of icon sizes

- `IconContinuousShape` (`IconDepth.kt:63`) is used on Home, but other surfaces clip icons with plain
  `RoundedCornerShape` at other ratios: `EverywhereOverlay.kt:279`, `LauncherSheetParts.kt:124`, `WhatsNew.kt:230`,
  `AppPanel.kt:135`, `AppContextMenu.kt:251`, `PinWidgetActivity.kt:90`, `LibraryCategories.kt:105,111`. Route all of
  them through `AppIcon` and the continuous shape (DES-18).
- The empty-slot drop ghost is a fixed 18dp corner at any size (`HomeWorkspace.kt:469`). Derive it from the icon size.
- Icons come in 66 (Home), 60 (Spotlight), 58 (folder), 54 (Library card), 50 (overlay), 47 (dock) and 44 (sheets).
  iOS uses about three: grid, dock (smaller, which Folio already gets right), and small list icons. Collapse to
  `HOME_ICON_SIZE`, the dock size and one small size in `LayoutModel.kt`. In the iPad photo, icons in an open folder
  are the same size as on Home.

### 3. One type scale for titles

- Large titles are 32sp Bold (`CustomizationSheet.kt:453`, `WidgetPicker.kt:277`), 30sp Bold (`WhatsNew.kt:143`,
  `LibraryCategories.kt:148`) and 28sp SemiBold (`FolderPanel.kt:205`). Use `FolioType.TITLE` and one weight. In the
  iPad photo the folder title is left-aligned above the card in Bold.
- Clock widget digits are 34, 44 and 22sp (`HomeWidgets.kt:129-151`) against 30sp in `SmartWidgets.kt:81`. Give
  widgets their own small type scale.
- Secondary text uses `Color.White.copy` with 20 different alphas across 84 places. `SecondaryLabel` (.62) exists. Use
  it, plus at most a tertiary token.
- `letterSpacing = .6.sp` appears three times as a literal while `GROUP_LABEL` uses .4.

### 4. Corners from the scale and concentric

- Folder card 38dp (`FolderPanel.kt:278`) and Library category card 36dp (`LibraryCategories.kt:152`) are the same
  object in iOS. Make them one token.
- `FolioSheet.kt:137,190` writes 28dp where `FolioRadius.SHEET_TOP` exists. The 22dp notification-card radius in
  `design.md` has no token (`LauncherScreen.kt:1233`, `WidgetPicker.kt:345,379`). 8, 6 and 5dp in
  `CustomizationSheet.kt:807,930,993` are off the scale.
- Apple's model is three shape kinds: fixed, capsule and concentric (parent radius minus padding, with a minimum).
  A small `concentric(parent, padding, min)` helper would let the dock, widget content and folder contents compute
  their corners instead of choosing them.

### 5. Motion: one spring language, and nothing on a default spec

**Re-express `FolioMotion` the way Apple does.** Apple's springs have a perceived duration and a bounce, and `.smooth`,
`.snappy` and `.bouncy` default to 0.5 s. With mass 1, duration = 2π/√stiffness and bounce ≈ 1 − damping ratio.
Folio's springs, in those terms:

| Spring | Damping, stiffness | Duration | Bounce |
|---|---|---|---|
| Control | .75, 1500 | 0.16 s | .25 |
| Quick | .80, 700 | 0.24 s | .20 |
| Menu | .78, 650 | 0.25 s | .22 |
| Settle | .86, 420 | 0.31 s | .14 |
| Appear | .78, 400 | 0.31 s | .22 |
| Sheet | .90, 380 | 0.32 s | .10 |
| Bounce | .70, 480 | 0.29 s | .30 |

Every Folio spring is shorter than Apple's defaults. Small controls should be quick, but big surfaces (a folder, the
App Library, Spotlight, a sheet) probably want 0.4 to 0.5 s, which may be why some transitions feel abrupt next to the
iPhone. Try it on the phone before changing the tokens: add a `FolioMotion.spring(duration, bounce)` constructor, then
tune by eye against a recording of an iPhone.

**Default and linear specs to replace** (these are the abrupt spots):

- Programmatic page changes use the pager's default spec: going to the App Library (`LauncherScreen.kt:304,320,328`),
  tapping the dots (`:934`), adding a page (`:1541`) and the edge drag (`:1244`). Use the same spring as a swipe.
- Settings swaps page content with no transition (`CustomizationSheet.kt:378-404`), and its sidebar uses default
  fade and slide (`:413-419`). Push and pop like iOS navigation.
- The jiggle bar's enter and exit (`LauncherScreen.kt:953`), the jiggle room inset (`HomeWorkspace.kt:322`), insertion
  opacity (`HomeWorkspace.kt:492`, `HomeTiles.kt:213`), Smart Stack dots (`HomeWidgets.kt:348`), control colors
  (`IosControls.kt:48,63`) and the overlay (`EverywhereOverlay.kt:272,381`) all use defaults.
- The Search pill and dots strip crossfades on literal `tween(180)` and `tween(120)` (`LauncherScreen.kt:899`).
- The dock's refusal shake is five `tween(55)` steps (`LauncherScreen.kt:365`). A short `Bounce` spring with an
  initial velocity reads more like iOS's shake.
- Unnamed spring pairs at `MainActivity.kt:117`, `LauncherScreen.kt:244`, `FolioSheet.kt:185` and
  `HomeWorkspace.kt:432` should become named springs (DYN-6).

**Jiggle.** It's a linear 280 ms loop at ±1.5° (`Jiggle.kt:75`). iOS's wiggle is a little slower and eased at the
turns, and each icon's rate varies slightly so the grid never syncs up. Use a sine-eased loop, vary the period per
icon by a few percent, and ease into the wiggle when edit mode starts instead of starting mid-swing.

**Returning to Home.** App launch has the platform's scale-up and an icon dip (`MainActivity.kt:387`,
`PressFeedback.kt`), but there's no return animation: Home just appears. iOS shrinks the app back into its icon while
Home settles from a slight zoom. A launcher can do the Home half: when Folio resumes from an app it launched, scale the
pages from about 1.06 to 1 and fade the wallpaper dim out on `Settle`, starting from the icon that was tapped. This is
the biggest missing piece of motion.

**Press scale.** `PressFeedback.DIP` is .9 and `motion-references.md` says about .96. iOS icons dip gently. Try .94
and decide one number.

### 6. Surface by surface

**Folders** (`FolderPanel.kt`). Compared with the iPad photo:
- The card should be clearer glass with the bright rim along the top and bottom edges. Home behind should be
  blurred, not just stepped back. The photo shows the wallpaper and icons heavily blurred.
- The title sits left-aligned just above the card, not centered.
- Icons inside are full Home size (see item 2).
- Add a light haptic when a folder opens. Nothing fires today.

**App Library** (`AppLibrary.kt`, `LibraryCategories.kt`). Close already. From the photo:
- The search field is a wide, very clear capsule with a thin bright rim. Match its frost to the category boxes.
- Category boxes: two large icons on top, then two large and a 2×2 of small icons. Labels sit under the box, small
  and SemiBold. Check the small-icon gap is concentric with the box's corner.
- The empty **Hidden** box is drawn as four ghost squares. Folio could use that same placeholder for an empty
  category.
- `AssistChip` is still used directly (DES-10).

**Spotlight and Search** (`Spotlight.kt`, `Jiggle.kt:128`).
- iOS 27's search field is a dark glass bubble that drops in from the top. Folio is light-only, so this needs a
  decision: keep the light field, or make the search field the one dark-glass exception because that's what iOS 27
  does in light mode too. The second is truer to the reference.
- Open on a spring that starts above the screen and lands with a little bounce, not a scale from .96.
- The round Search button should sit in the Side Bar column, centered on it, at the same width as the status button.

**Side Bar** (`StatusRail.kt`, dock at `LauncherScreen.kt:806-860`).
- Order to match: time, round status button, dock, round Search button. The column has a fixed width; items share
  its center line.
- The dock's corner should be concentric with its icons (the changelog says this is done; check it on the phone
  against the marketing art, where the inset looks a little larger than Folio's).
- On rotation iOS slides the dock out and back in from the new edge. Folio could do the same on fold and unfold
  instead of placing it at once.
- Battery and silent colors are hex literals (`StatusRail.kt:166-173`). Move them to `FolioColors`.

**Widgets** (`HomeWidgets.kt`, `SmartWidgets.kt`).
- Content corners should follow the widget's corner (concentric).
- An extra-large widget that fills a page is now iOS. Folio's Suggestions widget already goes to the whole grid.
  Decide whether that is the one XL widget or whether Clock and Calendar get one too. Cut, don't add, if unsure.
- Text no smaller than 11sp.

**Fold** (`FoldTransition.kt`). The reference behavior is content held still while the moving half frosts. Folio's
fold runs on fixed ms timings with `easeInOutSine` (`:500-545`). That's right for a hinge-driven effect, but check the
reported iOS glitch (wallpaper jumping on slow hinge moves) doesn't happen here.

**Haptics.** `FolioHaptic` is the vocabulary, but the switch (`IosControls.kt:52`), the overlay
(`EverywhereOverlay.kt:394,409`) and the fold (`FoldTransition.kt:84`) bypass it. Route them through `perform()`,
and add one for folder, App Library and Spotlight opening.

### 7. Hex colors still outside the tokens

The remove state in `LauncherScreen.kt:1282-1317,1397` uses three hand-mixed reds although `FolioColors` has `Red`,
`RedLight`, `RedSoft` and `InvalidDrop`. Also `Jiggle.kt:104` (minus badge) and `AppLibrary.kt:178` (letter chip).
Lower the counts in `DesignTokensTest` as each goes.

## Stale docs to fix along the way

- `design.md` DES-19 says sheets and overlays are dark by design. The fork is light only.
- `design.md` and `motion-references.md` cite `MotionSpeed` and `LabelSize`, which no longer exist, and
  `docs/research-motion-0-6-9.md`, which was never committed. `design.md`'s `FolioSheet.kt` line numbers are stale.
- `docs/in-flight.md` still lists `dev/streamline`, which has merged.

## Suggested order

1. Darkened edge on `drawLiquidGlass`, and flat fills onto Liquid glass (item 1). One change, visible everywhere.
2. Default specs onto named springs, and the Home return animation (item 5).
3. Icon shape and sizes, titles, corners (items 2 to 4), as one consistency pass with `DesignTokensTest` counts
   going down.
4. Folder and Spotlight passes against the photos (item 6).
5. Spring durations, tuned on the phone against an iPhone recording.
