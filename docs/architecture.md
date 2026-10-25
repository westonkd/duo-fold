# Contributor code map

This page says how Folio works. The [Folio Standards](standards/README.md) say how changes to it should be built.

Folio is a Kotlin/Jetpack Compose Android Home application with one normal app module. It owns its Home content, dock, editing UI and widget hosts. Android owns the secure lock screen, recents, notification panels and system app transitions.

## Where to start

All paths below are relative to `app/src/main/java/com/mccal/folio/`.

| Area | Entry points | Responsibility |
| --- | --- | --- |
| Activity | `MainActivity.kt` | Android intents/results and Home selection |
| App catalog and saved layout | `LauncherModel.kt`, `ProfileSupport.kt` | Observable launcher state, profile-aware identities, package changes and persistence |
| Home and navigation | `LauncherScreen.kt`, `PageGestures.kt`, `WorkspacePageMotion.kt` | Page composition, shared gestures, unfolded pairs and page motion |
| Editing and folders | `HomeEditing.kt`, `HomeDrag.kt`, `FolderEditing.kt`, `FolderPanel.kt` | Placement rules, drag previews, insertion, folders and cancellation |
| Native widgets | `WidgetController.kt`, `WidgetPicker.kt`, `WidgetSizing.kt`, `ZeroPaddingWidgetHost.kt`, `WidgetVerticalGestures.kt` | Provider catalog, binding/configuration, geometry and native touch arbitration |
| Google search | `GoogleSearch.kt` | The Google app's search intent |
| Customization | `CustomizationSheet.kt`, `LauncherActionSheet.kt` | Long-press actions, settings subpages and sheet navigation |
| Photos and status | `LauncherBackground.kt`, `DeviceStatus.kt`, `StatusRail.kt` | Private photo staging, live status and its presentation |
| Backup and shade access | `LayoutBackup.kt`, `BackupController.kt`, `SystemShadeController.kt` | Portable layout import/export and optional system-panel actions |

## Layout and identity

`LauncherModel` exposes a `StateFlow<LauncherState>`. App discovery happens off the main thread; installed-app identities include their Android profile. A temporarily unavailable package or paused profile must not silently erase its placements.

Each ordinary Home page has a four-column, six-row grid. Apps occupy cells; widgets occupy explicit rectangles with durable slot identities. The dock has four positions and rejects incoming apps when full. Moving a shortcut between Home and the dock moves that placement; All apps remains the installed-app catalog.

Unfolded navigation uses overlapping pairs: leading workspace + Home 1, Home 1 + Home 2, and so on. The leading workspace has separate `leadingSlots` and durable widget page `-1`; it disappears from the cover view without deleting its contents. The pager has no page to the left of Home: pager page 0 is Home 1 (on the inner screen, the leading workspace + Home 1 pair). Use the address helpers in `HomeEditing.kt` rather than treating negative cell indices as missing values.

Saved layouts use explicit JSON fields with migration backups. Layout export is a portable description, not a copy of Android's widget capabilities: an import must reconnect widgets, and cross-installation work profiles may require manual correction. Photo files are outside the layout backup; Market packages are inside it, as records and the changes they made, never as the snapshots of what they replaced - those describe the phone the backup came from, so a restore applies each package's recorded changes again over the layout it has just put back.

## Native widgets and gestures

Android widgets render through native `AppWidgetHostView` children inside Compose. Binding and configuration are asynchronous: preserve the pending operation and real binding until it finishes or is explicitly canceled. Restoring a saved numeric widget ID cannot recreate a deleted system binding.

The shared ancestor recognizes horizontal one-page gestures across the page, dock and rail. Native vertical widget content must retain vertical input. A bound widget's scoped long-press recognizer tolerates consumed sub-slop jitter, but scrolling, cancellation, release or another pointer cancels pickup. Do not solve one path by consuming every event: that breaks the others.

Widget size publication uses measured content with `updateAppWidgetOptions`, posted/coalesced after layout. Framework padding behavior is relevant when changing this. Nearby pages remain composed to avoid expensive RemoteViews reinflation during a swipe.

## Persistence and recovery

A selected photo is decoded into private staging before **Apply** replaces the committed background. URI permission belongs to that selection operation and is released when no longer needed. Failed or canceled selection preserves the existing background. Never explicitly recycle a bitmap already published to Compose; rendering may still reference it. Activity and ready-file recovery have tests; provider process death during early decode remains a separate validation gap.

## Testing changes

Pure placement, gesture-decision, sizing, profile and status rules have JVM tests under `app/src/test/`. Native widget/provider behavior, real sheet Back handling and Android results require emulator integration checks under `app/src/androidTest/`.

Start with a focused regression that reproduces the behavior, then run the relevant build/unit/lint checks described in [Contributing](../CONTRIBUTING.md). Integration fixtures need a disposable emulator with their expected apps/providers. Record real binding tuples before testing and preserve them during cleanup; preferences alone are not a complete widget backup. Cover/inner dimension overrides test layout behavior, not physical hinge timing or real-device performance.

Debug probes and instrumentation fixtures are separate source sets. The release build uses R8/resource shrinking and excludes debug activities. Signed releases are built with `scripts/release-signed.sh`; see the README's tested devices before expanding compatibility claims.
