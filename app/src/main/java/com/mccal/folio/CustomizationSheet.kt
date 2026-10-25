package com.mccal.folio

import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Where Settings was when it closed, so opening it again picks up there, like iPhone Settings. The page itself is
 * kept by Home; this holds the rest. In memory only, so a restart opens at the top.
 */
/** A page to open Settings on, set by something outside Settings. */
internal object SettingsLink { var page: CustomizationPage? = null }

internal object SettingsMemory {
    var tweakId = ""
    var bodyScroll = 0
    var sidebarScroll = 0
    /** How far down each page was when Settings last left it, so Back lands in the same place. */
    val pageScroll = mutableMapOf<CustomizationPage, Int>()
}

/**
 * The scroll of the page Settings shows. Each page gets a new one: the page Back returns to starts where it was left,
 * as in iPhone Settings, and any other page at its top. A new state is at its offset before the page is laid out;
 * scrolling the old one back could only have gone as far down as the page it was showing reached.
 */
internal class SettingsScroll(private var page: CustomizationPage, offset: Int) {
    var state = ScrollState(offset)
        private set

    /** The scroll for [next], as it opens. */
    fun show(next: CustomizationPage): ScrollState {
        if (next == page) return state
        SettingsMemory.pageScroll[page] = state.value
        state = ScrollState(if (next == page.parent) SettingsMemory.pageScroll[next] ?: 0 else 0)
        page = next
        return state
    }
}

internal enum class CustomizationPage { OVERVIEW, WALLPAPER, HOME, STATUS, GESTURES, FOLD, BACKUP, HELP, CREDITS, TWEAKS, TWEAK, ADVANCED, SEARCH, PERMISSIONS, GENERAL, FOLD_TWEAK, ACCESSIBILITY;

    /** The page Back returns to: the nav bar button and the system Back gesture both use it. */
    val parent: CustomizationPage get() = when (this) {
        TWEAK -> TWEAKS
        FOLD_TWEAK -> FOLD // Duet opened from Fold & Displays goes back there
        // The pages you open once live under General, as iOS keeps them under General › About.
        ADVANCED, BACKUP, HELP, CREDITS -> GENERAL
        else -> OVERVIEW
    }

    /** The top-level page this one lives under, highlighted in the split view's sidebar. */
    val root: CustomizationPage get() = if (parent == OVERVIEW) this else parent.root

    /** The page's name, over the page and on the Back of the pages under it; a tweak shows its own. */
    @get:androidx.annotation.StringRes val title: Int get() = when (this) {
        OVERVIEW -> R.string.folio
        WALLPAPER -> R.string.wallpaper_appearance
        HOME -> R.string.home_screen_dock
        STATUS -> R.string.icons_side_bar
        GESTURES -> R.string.gestures_actions
        FOLD -> R.string.fold_displays
        BACKUP -> R.string.backup
        HELP -> R.string.help
        CREDITS -> R.string.credits
        TWEAKS -> R.string.tweaks
        TWEAK, FOLD_TWEAK -> R.string.tweak
        ADVANCED -> R.string.advanced
        SEARCH -> R.string.search_app_library
        PERMISSIONS -> R.string.privacy_permissions
        GENERAL -> R.string.general
        ACCESSIBILITY -> R.string.accessibility
    }
}

/**
 * What Back says in Settings' nav bar: the name of the page it returns to, as iOS does. Null where there's no Back,
 * because that page is already on screen: the list beside a top-level page in two columns, or the middle of three.
 * [columns] is 1 on the phone and while the list slides over the page.
 */
@androidx.annotation.StringRes
internal fun settingsBackLabel(page: CustomizationPage, columns: Int): Int? = when {
    page == CustomizationPage.OVERVIEW -> null
    columns >= 3 || (columns == 2 && page.parent == CustomizationPage.OVERVIEW) -> null
    else -> page.parent.title
}

/** Back goes up a page, as the nav bar's Back does. On the top page the host closes the sheet. */
@Composable
internal fun SettingsPageBack(page: CustomizationPage, onBack: () -> Unit) =
    ModalDialogBackHandler(enabled = page != CustomizationPage.OVERVIEW, onBack = onBack)

@Composable
internal fun CustomizationSheet(state: LauncherState, initiallyWide: Boolean, model: LauncherModel,
    isDefaultHome: Boolean, page: CustomizationPage, onPage: (CustomizationPage) -> Unit,
    onMakeDefault: () -> Unit, onClose: () -> Unit, onEditPins: () -> Unit, onWidget: (Int) -> Unit,
    onAddWidget: (Int) -> Unit, onRemoveWidget: (Int) -> Unit, onWallpaperPreview: () -> Unit,
    onExportLayout: () -> Unit, onImportLayout: () -> Unit, onSaveLayoutToFolder: (String) -> Unit = {},
    backgrounds: LauncherBackgroundController, homePage: Int = 0,
    onShadeSetup: () -> Unit = {},
    onShowWhatsNew: () -> Unit = {},
) {
    var wide by rememberSaveable { mutableStateOf(initiallyWide) }
    var tweakId by rememberSaveable { mutableStateOf(SettingsMemory.tweakId) }
    SideEffect { SettingsMemory.tweakId = tweakId }
    val openTweak: (TweakFeature) -> Unit = { tweak ->
        tweakId = tweak.id
        onPage(CustomizationPage.TWEAK)
    }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    var namingBackup by remember { mutableStateOf(false) }
    val title = when (page) {
        CustomizationPage.TWEAK, CustomizationPage.FOLD_TWEAK -> TweakFeatures.firstOrNull { it.id == tweakId }?.name
        else -> null
    } ?: stringResource(page.title)
    // Reopening Settings lands where it was, scrolled the same. Back lands where the page it returns to was left;
    // any other page opens at its top.
    val scroll = remember { SettingsScroll(page, SettingsMemory.bodyScroll) }
    val bodyScroll = remember(page) { scroll.show(page) }
    val sidebarScroll = rememberScrollState(SettingsMemory.sidebarScroll)
    DisposableEffect(Unit) { onDispose { SettingsMemory.bodyScroll = scroll.state.value; SettingsMemory.sidebarScroll = sidebarScroll.value } }
    val onBack = { onPage(page.parent) }
    SettingsPageBack(page, onBack)
    // A search target whose row never shows (its option is hidden until a switch is on) is dropped, not saved for later.
    LaunchedEffect(SettingsFocus.label) { if (SettingsFocus.label != null) { kotlinx.coroutines.delay(SettingsFocus.PATIENCE_MS); SettingsFocus.label = null } }

    // The settings list. On the phone it's the first page; in the split view it's the sidebar, with the open page highlighted.
    val overviewRows: @Composable ColumnScope.(selected: CustomizationPage?, sidebar: Boolean) -> Unit = { selected, sidebar ->
                    // Only the sidebar has an open page to mark; on the phone the rows are just places to go.
                    val pick = { row: CustomizationPage -> if (sidebar) selected == row else null }
                    SheetGroup {
                        // General holds what you open once: what's new, language, help, credits.
                        TweakRow(Icons.Rounded.Settings, FolioColors.Value.Gray, stringResource(R.string.general), "customization-general",
                            selected = pick(CustomizationPage.GENERAL), chevron = !sidebar) { onPage(CustomizationPage.GENERAL) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.Accessibility, FolioColors.Value.Blue, stringResource(R.string.accessibility), "customization-accessibility", selected = pick(CustomizationPage.ACCESSIBILITY), chevron = !sidebar) { onPage(CustomizationPage.ACCESSIBILITY) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.Wallpaper, FolioColors.Value.CyanLight, stringResource(R.string.wallpaper_appearance), "customization-wallpaper",
                            if (backgrounds.previewPending) stringResource(R.string.photo_ready) else null, selected = pick(CustomizationPage.WALLPAPER), chevron = !sidebar) { onPage(CustomizationPage.WALLPAPER) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.GridView, FolioColors.Value.Blue, stringResource(R.string.home_screen_dock), "customization-home", selected = pick(CustomizationPage.HOME), chevron = !sidebar) { onPage(CustomizationPage.HOME) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.Apps, FolioColors.Value.Indigo, stringResource(R.string.icons_side_bar), "customization-status", selected = pick(CustomizationPage.STATUS), chevron = !sidebar) { onPage(CustomizationPage.STATUS) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.Search, FolioColors.Value.Gray, stringResource(R.string.search_app_library), "customization-search", selected = pick(CustomizationPage.SEARCH), chevron = !sidebar) { onPage(CustomizationPage.SEARCH) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.Gesture, FolioColors.Value.Teal, stringResource(R.string.gestures_actions), "customization-gestures", selected = pick(CustomizationPage.GESTURES), chevron = !sidebar) { onPage(CustomizationPage.GESTURES) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.Devices, FolioColors.Value.Pink, stringResource(R.string.fold_displays), "customization-fold", selected = pick(CustomizationPage.FOLD), chevron = !sidebar) { onPage(CustomizationPage.FOLD) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.AutoAwesome, FolioColors.Value.Purple, stringResource(R.string.tweaks), "customization-tweaks",
                            selected = pick(CustomizationPage.TWEAKS), chevron = !sidebar) { onPage(CustomizationPage.TWEAKS) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.PanTool, FolioColors.Value.Blue, stringResource(R.string.privacy_permissions), "customization-permissions", selected = pick(CustomizationPage.PERMISSIONS), chevron = !sidebar) { onPage(CustomizationPage.PERMISSIONS) }
                    }
    }
    // Home-app actions: above the list on the phone, on Folio's own page in the split view.
    val overviewActions: @Composable ColumnScope.() -> Unit = {
                    if (!isDefaultHome || state.canUndoEdit) SheetGroup {
                        if (!isDefaultHome) IosActionRow(stringResource(R.string.set_as_home_app), "default-home-settings", onClick = onMakeDefault)
                        if (!isDefaultHome && state.canUndoEdit) MenuDivider()
                        if (state.canUndoEdit) IosActionRow(stringResource(R.string.undo_last_layout_change), onClick = { model.undoEdit(); onClose() })
                    }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    // iPad Settings / One UI on the unfolded screen: sidebar and page side by side, in either orientation.
    // Regular size class (both dimensions roomy), not a device check: the inner screen in either orientation.
    val fullWidth = maxWidth
    // The keyboard covers Settings; it doesn't make the window smaller (ADP-1, ADP-18). Measured out here, so that
    // tapping the search field can't turn the unfolded screen into a phone-sized one for as long as the keyboard is
    // up, take the sidebar away with it, and take the field you just tapped with the sidebar (#117).
    val keyboardDp = keyboardDpOverSheet()
    val split = settingsSplits(maxWidth.value, maxHeight.value, androidx.compose.ui.platform.LocalConfiguration.current.classScale, keyboardDp)
    // Takes the page to draw rather than reading the open one, so the split view can show a list and the thing you
    // picked from it side by side. Inside, `page` means "the page this column is drawing".
    val pageContent: @Composable ColumnScope.(CustomizationPage) -> Unit = { page ->
            when (page) {
                CustomizationPage.OVERVIEW -> if (split) {
                    TweakBanner()
                    overviewActions()
                    MiniHomePreview(backgrounds.previewBitmap, state, 260.dp)
                } else {
                    // Like iOS Settings: the header gets out of the way while searching.
                    if (settingsQuery.isBlank()) TweakBanner()
                    SettingsSearchField(settingsQuery) { settingsQuery = it }
                    if (settingsQuery.isNotBlank()) SettingsSearchResults(settingsQuery, onOpen = { page, row -> settingsQuery = ""; SettingsFocus.label = row; onPage(page) }, onOpenTweak = { settingsQuery = ""; openTweak(it) })
                    else {
                        overviewActions()
                        MiniHomePreview(backgrounds.previewBitmap, state, 176.dp)
                        overviewRows(null, false)
                        }
                }
                CustomizationPage.WALLPAPER -> {
                    val wallpaperContext = androidx.compose.ui.platform.LocalContext.current
                    // The order is the one every well-regarded product uses, sourced in docs/mockups/wallpaper-page.html:
                    // the preview first because it is what the rest edits, then the picture, then the things drawn over
                    // it. App Icon lives on the Icons page. That is the
                    // iOS Wallpaper flow's shape rather than a Folio invention (DES-1), built from Folio's own rows,
                    // groups and cards (DES-9), with the preview standing in for iOS's chrome-on-preview.
                    //
                    // The preview shows the candidate when there is one, under Home's real chrome, which is what AOSP's
                    // small preview and iOS's Lock Screen preview both do. Art and photos apply the same way: a tap
                    // stages, Set writes.
                    // "Once" means once: leaving the page ends the offer as well as taking it.
                    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { backgrounds.offerTaken() } }
                    val candidate = backgrounds.pendingArtBitmap ?: backgrounds.previewBitmap
                    val staging = backgrounds.pendingArt != null || backgrounds.previewPending
                    if (!state.systemWallpaper) {
                        MiniHomePreview(candidate, state, 228.dp)
                        if (staging) SheetGroup {
                            IosActionRow(stringResource(R.string.set), "background-set",
                                enabled = candidate != null || backgrounds.pendingArt != null,
                                onClick = { if (backgrounds.previewPending) backgrounds.applyPreview() else backgrounds.setPendingArt() })
                            MenuDivider()
                            IosActionRow(stringResource(R.string.cancel), "background-cancel",
                                onClick = { if (backgrounds.previewPending) backgrounds.cancelPreview() else backgrounds.cancelPendingArt() })
                        } else if (backgrounds.justSet) SheetGroup {
                            // Once, after a Set, and behind Android's own preview: the phone's wallpaper cannot be read
                            // back at this targetSdk, so setting it cannot be undone from here.
                            IosActionRow(stringResource(R.string.also_set_as_phone_wallpaper), "wallpaper-preview",
                                onClick = { backgrounds.offerTaken(); onWallpaperPreview() })
                            // A bare group doesn't pad its rows the way SettingsCard does, so the note brings its own.
                            CardNote(stringResource(R.string.set_as_phone_wallpaper_note),
                                Modifier.padding(start = FolioSpace.LARGE.dp, end = FolioSpace.LARGE.dp, bottom = FolioSpace.MEDIUM.dp))
                        }
                        if (backgrounds.loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("background-loading"))
                    }
                    SettingsCard(stringResource(R.string.wallpaper)) {
                        IosSegmented(listOf(true to stringResource(R.string.android_wallpaper), false to stringResource(R.string.folio_background)),
                            state.systemWallpaper, { system -> model.setSystemWallpaper(system); wallpaperContext.asActivity()?.applyWallpaperWindow(system) },
                            Modifier.padding(vertical = FolioSpace.SNUG.dp), tag = "background-choice")
                        if (state.systemWallpaper) CardAction(stringResource(R.string.change_android_wallpaper), onClick = {
                            runCatching { wallpaperContext.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER), wallpaperContext.getString(R.string.change_wallpaper))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }, modifier = Modifier.testTag("background-change-system"))
                        // Only Android's wallpaper drifts now, and the system moves it, so the switch lives with the
                        // wallpaper it moves. Folio's own picture has no spare width to slide (see DuneWallpaper).
                        if (state.systemWallpaper) SettingsSwitch(stringResource(R.string.wallpaper_moves_with_pages), state.wallpaperMotion, model::setWallpaperMotion, "wallpaper-motion-switch")
                        CardNote(if (state.systemWallpaper) stringResource(R.string.uses_the_same_wallpaper_as_your_phone_s)
                            else stringResource(R.string.wallpaper_group_note))
                    }
                    if (!state.systemWallpaper) {
                        BackgroundPicker(backgrounds)
                        (backgrounds.errorMessage ?: backgrounds.successMessage)?.let { message ->
                            TextButton(onClick = backgrounds::clearMessage, Modifier.fillMaxWidth().testTag("background-message")) { Text(message) }
                        }
                    }
                    SettingsCard(stringResource(R.string.text_on_home)) {
                        IosMenuRow(stringResource(R.string.text_color), listOf("AUTO" to stringResource(R.string.automatic), "LIGHT" to stringResource(R.string.light), "DARK" to stringResource(R.string.dark)), state.homeInk, model::setHomeInk, tag = "home-ink")
                        // Beside the text colour it exists for: the scrim is the other half of the same job, for a pale
                        // wallpaper that white text still has to read over.
                        SettingsSwitch(stringResource(R.string.darken_behind_text), state.homeScrim, model::setHomeScrim, "home-scrim-switch")
                        CardNote(stringResource(R.string.labels_status_page_dots_and_widget_text))
                    }
                }
                CustomizationPage.HOME -> {
                    HomeLayoutSettings(state, wide, { wide = it }, model, homePage, onEditPins, onWidget, onAddWidget, onRemoveWidget)
                    RecentDotsCard(state, model)
                }
                CustomizationPage.GESTURES, CustomizationPage.SEARCH -> {
                    if (page == CustomizationPage.GESTURES) SettingsCard(stringResource(R.string.gestures)) {
                        IosMenuRow(stringResource(R.string.swipe_down_on_home), listOf("SPOTLIGHT" to stringResource(R.string.spotlight), "NOTIFICATIONS" to stringResource(R.string.notifications_title), "OFF" to stringResource(R.string.nothing)),
                            state.swipeDownHome, model::setSwipeDownHome, tag = "swipe-down-home")
                        SettingsSwitch(stringResource(R.string.drag_page_dots_to_flip_pages), state.pageScrub, model::setPageScrub, "page-scrub-switch")
                        SettingsSwitch(stringResource(R.string.haptic_feedback), state.haptics, model::setHaptics, "haptics-switch")
                        CardNote(stringResource(R.string.pull_down_from_the_top_left_for_notifica))
                    }
                    if (page == CustomizationPage.SEARCH) SettingsCard(stringResource(R.string.spotlight)) {
                        IosMenuRow(stringResource(R.string.search_with_enter),
                            WebSearchTarget.enterEngines.map { it.name to when (it) { WebSearchTarget.GOOGLE -> stringResource(R.string.google_no_ai); WebSearchTarget.DUCKDUCKGO -> stringResource(R.string.duckduckgo); else -> it.label } } +
                                (CUSTOM_SEARCH to stringResource(R.string.custom_search_engine)),
                            state.searchEngine, model::setSearchEngine, tag = "search-engine")
                        if (state.searchEngine == CUSTOM_SEARCH) CustomSearchField(state.searchCustomUrl, model::setSearchCustomUrl)
                        SpotlightSection.entries.forEach { section ->
                            SettingsSwitch(stringResource(section.title), section.name !in state.spotlightHidden,
                                { model.setSpotlightSection(section.name, it) }, "spotlight-${section.name.lowercase()}")
                        }
                        val messageContext = androidx.compose.ui.platform.LocalContext.current
                        val iMessageApps = remember { Messaging.iMessageApps.filter { Messaging.installed(messageContext, it.first) } }
                        if (iMessageApps.isNotEmpty()) {
                            IosMenuRow(stringResource(R.string.message_contacts_with), listOf<Pair<String?, String>>(null to stringResource(R.string.texting_app)) + iMessageApps.map { it.first to it.second },
                                state.messagesApp, model::setMessagesApp, tag = "messages-app")
                        }
                    }
                    if (page == CustomizationPage.SEARCH) SettingsCard(stringResource(R.string.suggestions)) {
                        val historyContext = androidx.compose.ui.platform.LocalContext.current
                        var askClearHistory by remember { mutableStateOf(false) }
                        CardAction(stringResource(R.string.clear_suggestions_history), Modifier.fillMaxWidth().testTag("clear-suggestions-history"), destructive = true, onClick = { askClearHistory = true })
                        CardNote(stringResource(R.string.suggestions_start_again_from_now))
                        if (askClearHistory) AlertDialog(onDismissRequest = { askClearHistory = false },
                            title = { Text(stringResource(R.string.clear_suggestions_history_q)) },
                            text = { Text(stringResource(R.string.clear_suggestions_history_text)) },
                            confirmButton = { TextButton(onClick = { askClearHistory = false; SuggestionsHistory.clear(historyContext) },
                                modifier = Modifier.testTag("clear-suggestions-history-confirm")) { Text(stringResource(R.string.clear), color = androidx.compose.ui.graphics.Color(FolioColors.Value.Red)) } },
                            dismissButton = { TextButton(onClick = { askClearHistory = false }) { Text(stringResource(R.string.cancel)) } })
                    }
                    if (page == CustomizationPage.GESTURES) SettingsCard(stringResource(R.string.actions)) {
                        CardNote(stringResource(R.string.pick_what_gestures_and_events_do_activat))
                        FolioTrigger.entries.forEach { trigger ->
                            val current = FolioAction.entries.firstOrNull { it.name == state.triggerActions[trigger.name] } ?: FolioAction.NONE
                            IosMenuRow(stringResource(trigger.label), FolioAction.entries.map { it to stringResource(it.label) }, current, { model.setTriggerAction(trigger, it) }, tag = "trigger-${trigger.name.lowercase()}")
                        }
                    }
                    if (page == CustomizationPage.SEARCH) SettingsCard(stringResource(R.string.app_library)) {
                        SettingsSwitch(stringResource(R.string.group_apps_into_categories), state.libraryCategories, model::setLibraryCategories, "library-categories-switch")
                        if (state.profiles.any { it.isWork }) SettingsSwitch(stringResource(R.string.work_apps), state.libraryWork, model::setLibraryWork, "library-work-switch")
                        HiddenAppsRow(state, model)
                        SettingsSwitch(stringResource(R.string.add_new_apps_to_home_screen), state.addNewAppsToHome, model::setAddNewAppsToHome, "add-new-apps-switch")
                        CardNote(stringResource(R.string.off_new_downloads_go_to_the_app_library))
                    }
                    if (page == CustomizationPage.SEARCH) SettingsCard(stringResource(R.string.search)) {
                        // One choice instead of a switch: what sits above the dock at rest.
                        IosMenuRow(stringResource(R.string.home_strip), HomeStrip.entries.map { it to stringResource(it.label) }, state.homeStrip, model::setHomeStrip, tag = "home-strip")
                        CardNote(stringResource(R.string.home_strip_note))
                        SettingsSwitch(stringResource(R.string.search_button_opens_the_google_app), state.googleSearch, model::setGoogleSearch, "google-search-switch")
                        CardNote(stringResource(R.string.when_off_the_search_button_opens_spotlig))
                    }
                }
                CustomizationPage.STATUS -> {
                    if (!split) MiniHomePreview(backgrounds.previewBitmap, state, 210.dp)
                    FolioSettingsCard(state, model)
                    SettingsCard(stringResource(R.string.app_icons)) {
                        val iconContext = androidx.compose.ui.platform.LocalContext.current
                        // Reading the installed icon packs asks Android about every pack, so it happens off the main thread.
                        val packs by androidx.compose.runtime.produceState(emptyList<IconPacks.Pack>(), iconContext) {
                            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { IconPacks.installed(iconContext) }
                        }
                        IosMenuRow(stringResource(R.string.icon_pack), listOf<Pair<String?, String>>(null to stringResource(R.string.app_icons)) + packs.map { it.packageName to it.label },
                            state.iconPack, { IconPacks.clear(); model.setIconPack(it) }, tag = "icon-pack")
                        CardNote(stringResource(R.string.icon_packs_and_themes_are_made_by_indepe))
                        if (packs.isEmpty()) CardNote(stringResource(R.string.install_any_icon_pack_made_for_nova_styl))
                        SettingsSwitch(stringResource(R.string.settings_live_clock_and_calendar_icons), state.liveIcons, model::setLiveIcons, "live-icons-switch")
                        IosMenuRow(stringResource(R.string.notification_badges), BadgeStyle.entries.map { it to stringResource(it.label) }, state.badgeStyle, model::setBadgeStyle, tag = "badge-style")
                        if (state.badgeStyle != BadgeStyle.OFF) {
                            SettingsSwitch(stringResource(R.string.clear_badges_when_opened), state.badgesWhenOpened,
                                model::setBadgesWhenOpened, "badges-when-opened-switch")
                            CardNote(stringResource(R.string.a_badge_goes_away_when_you_open_the_app))
                        }
                        CardAction(stringResource(R.string.refresh_icons), onClick = model::reloadIcons)
                        CardNote(stringResource(R.string.refresh_icons_note))
                    }
                    SettingsCard(stringResource(R.string.side_rail)) {
                        SettingsSwitch(stringResource(R.string.left_handed_layout_rail_on_the_left), state.leftHanded, model::setLeftHanded, "left-handed-switch")
                        SettingsSwitch(stringResource(R.string.show_app_names), state.labels, model::setLabels, "label-switch")
                        CardNote(stringResource(R.string.frost_and_outline_are_in_wallpaper_appea))
                    }
                    SettingsCard(stringResource(R.string.status)) {
                        SettingsSwitch(stringResource(R.string.show_status_in_the_rail), state.verticalStatus, model::setVerticalStatus, "status-switch")
                    }
                }
                CustomizationPage.FOLD -> {
                    // What Folio has learned about how fast you fold (it adapts the animation to this).
                    val foldPrefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("folio", 0)
                    var learned by remember { mutableStateOf(foldPrefs.getFloat("fold_open_ms", 520f) to foldPrefs.getFloat("fold_close_ms", 650f)) }
                    SettingsCard(stringResource(R.string.your_fold_timing)) {
                        Text(stringResource(R.string.unfold_about_1_ms_fold_about_2_ms, learned.first.toInt(), learned.second.toInt()),
                            style = MaterialTheme.typography.bodyLarge)
                        CardAction(stringResource(R.string.reset_fold_timing), onClick = {
                            foldPrefs.edit().remove("fold_open_ms").remove("fold_close_ms").apply()
                            learned = 520f to 650f
                        })
                        CardNote(stringResource(R.string.learned_from_your_last_folds_and_used_to))
                    }
                    // The fold animation is the Duet tweak now; its switch lives on its own page.
                    SettingsCard(stringResource(R.string.fold_animation)) {
                        IosNavRow("Duet", stringResource(if (state.foldEffect) R.string.on else R.string.off), // english-only
                            { tweakId = DUET_ID; onPage(CustomizationPage.FOLD_TWEAK) }, "fold-duet")
                        CardNote(stringResource(R.string.duet_moved_note))
                    }
                    SettingsCard(stringResource(R.string.closing_from_home)) {
                        SettingsSwitch(stringResource(R.string.stay_awake_on_the_cover_screen), state.stayAwakeOnFold, model::setStayAwakeOnFold, "fold-awake-switch")
                        CardNote(stringResource(R.string.samsung_locks_the_phone_when_you_fold_on))
                    }
                }
                CustomizationPage.BACKUP -> {
                    if (namingBackup) BackupNameAlert(onCancel = { namingBackup = false }) { name -> namingBackup = false; onSaveLayoutToFolder(name) }
                    SheetGroup {
                        IosActionRow(stringResource(R.string.save_backup), "layout-save-folder", onClick = { namingBackup = true })
                        MenuDivider()
                        IosActionRow(stringResource(R.string.save_backup_to_files), "layout-export", onClick = onExportLayout)
                        MenuDivider()
                        IosActionRow(stringResource(R.string.restore_from_backup), "layout-import", onClick = onImportLayout)
                    }
                    CardNote(
                        stringResource(R.string.save_the_current_home_layout_folders_wid) + stringResource(R.string.restore_shows_a_review_before_changing_h),
                        Modifier.padding(horizontal = FolioSpace.TINY.dp),
                    )
                    LayoutHistoryCard(state, model, onClose)
                }
                CustomizationPage.ACCESSIBILITY -> {
                    SettingsCard(null) {
                        // Android's Remove animations already means no motion at all, so the switch shows on and waits.
                        val removed = reduceMotionEnabled(androidx.compose.ui.platform.LocalContext.current)
                        SettingsSwitch(stringResource(R.string.reduce_motion), state.reduceMotion || removed, model::setReduceMotion, "reduce-motion-switch", enabled = !removed)
                        CardNote(stringResource(if (removed) R.string.reduce_motion_note_removed else R.string.reduce_motion_note))
                    }
                    SettingsCard(null) {
                        // For people who find the system's buttons too small, especially on the inner screen.
                        SettingsSwitch(stringResource(R.string.big_buttons), state.buttonBar, { on ->
                            model.setButtonBar(on); if (on && !SystemShadeAccessibilityService.isConnected()) onShadeSetup()
                        }, "button-bar-switch")
                        // The footer says what the switch does, so it shows while the switch is off too.
                        CardNote(stringResource(R.string.big_back_home_and_recents_buttons_float))
                        if (state.buttonBar) {
                            val buttonContext = androidx.compose.ui.platform.LocalContext.current
                            CardAction(stringResource(R.string.put_the_buttons_back_at_the_bottom), onClick = { ButtonBarPosition.reset(buttonContext) })
                            CardNote(stringResource(R.string.long_press_and_drag_the_bar_to_move_it_u))
                            if (!gestureNavigation(androidx.compose.ui.platform.LocalContext.current))
                                CardNote(stringResource(R.string.android_s_three_buttons_are_on_so_you_ll))
                        }
                    }
                    SettingsCard(null) {
                        SettingsSwitch(stringResource(R.string.dock_handle_on_the_rail_edge), state.dockEverywhere, { on ->
                            model.setDockEverywhere(on); if (on && !SystemShadeAccessibilityService.isConnected()) onShadeSetup()
                        }, "dock-everywhere-switch")
                        CardNote(stringResource(R.string.uses_folios_accessibility_service_the_sa))
                    }
                }
                CustomizationPage.CREDITS -> CreditsPage()
                CustomizationPage.GENERAL -> {
                    val generalContext = androidx.compose.ui.platform.LocalContext.current
                    SheetGroup {
                        TweakRow(Icons.Rounded.NewReleases, FolioColors.Value.Green, stringResource(R.string.what_s_new), "customization-whats-new",
                            "v" + WhatsNew.currentVersion(generalContext)) { onClose(); onShowWhatsNew() }
                        if (DevBuild.isDevApp(generalContext) && DevBuild.load(generalContext) != null) {
                            MenuDivider()
                            TweakRow(Icons.Rounded.Build, FolioColors.Value.Orange, DevBuild.THIS_BUILD_LABEL, "customization-dev-build",
                                DevBuild.load(generalContext)?.sha) { onClose(); DevBuild.reopen.intValue++ }
                        }
                    }
                    SheetGroup {
                        // Android's own per-app language screen (13+), which lists every language Folio ships, as iOS does.
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            TweakRow(Icons.Rounded.Translate, FolioColors.Value.Blue, stringResource(R.string.language), "customization-language",
                                generalContext.resources.configuration.locales[0].let { it.getDisplayLanguage(it).replaceFirstChar { c -> c.titlecase(it) } }) {
                                runCatching { generalContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS,
                                    android.net.Uri.parse("package:${generalContext.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }
                            MenuDivider()
                        }
                        TweakRow(Icons.Rounded.Save, FolioColors.Value.Gray, stringResource(R.string.backup), "customization-backup") { onPage(CustomizationPage.BACKUP) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.Tune, FolioColors.Value.Gray, stringResource(R.string.advanced), "customization-advanced") { onPage(CustomizationPage.ADVANCED) }
                    }
                    SheetGroup {
                        TweakRow(Icons.Rounded.HelpOutline, FolioColors.Value.Blue, stringResource(R.string.help), "customization-help") { onPage(CustomizationPage.HELP) }
                        MenuDivider()
                        TweakRow(Icons.Rounded.Favorite, FolioColors.Value.Red, stringResource(R.string.credits), "customization-credits") { onPage(CustomizationPage.CREDITS) }
                    }
                }
                CustomizationPage.HELP -> {
                    // Getting help lives here rather than as more rows in the main list (fewer choices there).
                    LauncherHelp(
                        isDefaultHome = isDefaultHome,
                        onHomeSettings = onMakeDefault,
                        onAddWidget = { onAddWidget(homePage) },
                        onShadeSetup = onShadeSetup,
                    )
                }
                CustomizationPage.ADVANCED -> {
                    SettingsCard(stringResource(R.string.safe_mode)) {
                        CardNote(if (SafeMode.active) stringResource(R.string.folio_is_running_in_safe_mode_optional_f)
                            else stringResource(R.string.if_folio_closes_unexpectedly_twice_right))
                    }
                    CapabilitiesCard()
                    RecentActivityCard()
                    CrashReportsCard()
                    PerformanceCard()
                }
                CustomizationPage.TWEAKS -> {
                    CardNote(stringResource(R.string.features_inspired_by_ios_jailbreak_tweak), Modifier.padding(horizontal = FolioSpace.TINY.dp))
                    SheetGroup {
                        TweakFeatures.forEachIndexed { index, tweak ->
                            if (index > 0) MenuDivider()
                            TweakRow(tweak.icon, tweak.color, tweak.name, "tweak-${tweak.id}", if (tweak.get(state)) stringResource(R.string.on) else stringResource(R.string.off)) {
                                tweakId = tweak.id; onPage(CustomizationPage.TWEAK)
                            }
                        }
                    }
                }
                CustomizationPage.PERMISSIONS -> PermissionsPage(isDefaultHome, onMakeDefault, onShadeSetup)
                CustomizationPage.TWEAK, CustomizationPage.FOLD_TWEAK -> TweakFeatures.firstOrNull { it.id == tweakId }?.let { tweak -> TweakPage(tweak, state, model) }
                    ?: LaunchedEffect(Unit) { onPage(CustomizationPage.TWEAKS) }
            }
    }
    if (!split) Column(Modifier.fillMaxSize().padding(horizontal = FolioSpace.LARGE.dp)) {
        SettingsNavBar(settingsBackLabel(page, columns = 1)?.let { stringResource(it) }, onBack, onClose)
        if (page != CustomizationPage.OVERVIEW) SettingsLargeTitle(title)
        Column(Modifier.weight(1f).edgeFade(bodyScroll).verticalScroll(bodyScroll).padding(bottom = FolioSpace.XL.dp),
            verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) { pageContent(page) }
    } else {
        // Half folded with the fold running down the screen, the divider goes on the fold, so no row sits on the
        // crease (iPhone Duo: controls move away from the fold). Flat, it's a share of the width.
        val hinge = LocalHinge.current?.takeIf { it.active && it.vertical }
        val density = LocalDensity.current
        val onFold = hinge?.let { with(density) { it.startPx.toDp() } }?.takeIf { it >= 280.dp && it <= fullWidth * .7f }
        val columns = settingsColumns(
            maxWidth.value, maxHeight.value, androidx.compose.ui.platform.LocalConfiguration.current.classScale,
            nested = page.parent != CustomizationPage.OVERVIEW,
            onFold = onFold != null,
            keyboardDp = keyboardDp,
        )
        val tiled = columns >= 2
        val threeColumns = columns == 3
        // Tiled it shares the width; as an overlay it can be a little wider so rows don't wrap. With three, the list
        // of settings gives up some width so the other two stay readable.
        val sidebarWidth = onFold
            ?: when {
                threeColumns -> (fullWidth * .28f).coerceIn(260.dp, 320.dp)
                tiled -> (fullWidth * .4f).coerceIn(280.dp, 380.dp)
                else -> minOf(360.dp, fullWidth * .6f)
            }
        val middleWidth = ((fullWidth - sidebarWidth) * .44f).coerceIn(280.dp, 380.dp)
        val middleScroll = rememberScrollState()
        var sidebarOpen by rememberSaveable { mutableStateOf(tiled || page == CustomizationPage.OVERVIEW) }
        var shownPage by remember { mutableStateOf(page) }
        // Opening a page slides the list away; coming back to the top brings it back, since the list is all that page has.
        SideEffect { if (page != shownPage) { shownPage = page; if (!tiled) sidebarOpen = page == CustomizationPage.OVERVIEW } }
        val sidebar: @Composable () -> Unit = {
            Column(Modifier.width(sidebarWidth).fillMaxHeight().edgeFade(sidebarScroll).verticalScroll(sidebarScroll)
                .padding(horizontal = FolioSpace.LARGE.dp).padding(top = 44.dp, bottom = FolioSpace.XL.dp).testTag("settings-sidebar"), verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) {
                SettingsLargeTitle(stringResource(R.string.folio))
                SettingsSearchField(settingsQuery) { settingsQuery = it }
                if (settingsQuery.isNotBlank()) SettingsSearchResults(settingsQuery, onOpen = { page, row -> SettingsFocus.label = row; onPage(page) }, onOpenTweak = { openTweak(it) })
                else {
                    // Like the account card at the top of iPad Settings: Folio's own page.
                    SheetGroup { SidebarAppRow(selected = page == CustomizationPage.OVERVIEW) { onPage(CustomizationPage.OVERVIEW) } }
                    overviewRows(page.root, true)
                }
            }
        }
        val divider: @Composable () -> Unit = {
            Box(Modifier.fillMaxHeight().width(.5.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = .14f)))
        }
        Row(Modifier.fillMaxSize()) {
            if (tiled) {
                sidebar()
                divider()
            }
            // The list you picked from stays where it was, and what you picked opens to the right of it - the way
            // Mail and Notes use an iPad's width. Only for a page that came from a list, so a column is never empty.
            if (threeColumns) {
                Column(Modifier.width(middleWidth).fillMaxHeight().padding(horizontal = FolioSpace.XL.dp)) {
                    Spacer(Modifier.height(44.dp))
                    SettingsLargeTitle(stringResource(page.parent.title))
                    Column(Modifier.weight(1f).edgeFade(middleScroll).verticalScroll(middleScroll).padding(bottom = FolioSpace.XL.dp),
                        verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) { pageContent(page.parent) }
                }
                divider()
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = FolioSpace.XL.dp)) {
                // With the list Back would return to still on screen there's nothing for Back to reveal, so the bar keeps
                // only Done. Otherwise every page but the first gets a way back, named for where it goes, not only the
                // sidebar button, which doesn't read as one.
                SettingsNavBar(settingsBackLabel(page, columns)?.let { stringResource(it) }, onBack, onClose,
                    leading = if (tiled) null else ({ SidebarButton { sidebarOpen = !sidebarOpen } }))
                if (page != CustomizationPage.OVERVIEW) SettingsLargeTitle(title)
                Column(Modifier.weight(1f).edgeFade(bodyScroll).verticalScroll(bodyScroll).padding(bottom = FolioSpace.XL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) {
                        // The space beside the list shows what the page changes, drawn from your real Home.
                        if (page == CustomizationPage.HOME && wide) UnfoldedHomePreview(backgrounds.previewBitmap, state, 200.dp)
                        else if (page == CustomizationPage.HOME || page == CustomizationPage.STATUS)
                            MiniHomePreview(backgrounds.previewBitmap, state, 240.dp)
                        pageContent(page)
                    }
                }
            }
        }
        if (!tiled) {
            val reduceMotion = LocalReduceMotion.current
            androidx.compose.animation.AnimatedVisibility(sidebarOpen, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .4f))
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { sidebarOpen = false })
            }
            androidx.compose.animation.AnimatedVisibility(sidebarOpen,
                enter = if (reduceMotion) androidx.compose.animation.fadeIn() else androidx.compose.animation.slideInHorizontally { -it },
                exit = if (reduceMotion) androidx.compose.animation.fadeOut() else androidx.compose.animation.slideOutHorizontally { -it }) {
                Box(Modifier.fillMaxHeight().background(FolioColors.SecondaryBackground)) { sidebar() }
            }
        }
    }
    }
}

/** The iPad/iPhone Duo sidebar toggle: shows or hides the settings list over the page. */
@Composable private fun SidebarButton(onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = stringResource(R.string.show_or_hide_the_settings_list), onClick = onClick)
        .testTag("settings-sidebar-toggle"), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ViewSidebar, null, tint = LocalAccent.current.ink, modifier = Modifier.size(26.dp))
    }
}

@Composable private fun SettingsNavBar(backLabel: String?, onBack: () -> Unit, onClose: () -> Unit, leading: (@Composable () -> Unit)? = null) {
    // iOS navigation bar: the sidebar button and "‹ Back" on the left, Done on the right.
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            if (backLabel != null) Row(Modifier.clip(RoundedCornerShape(FolioRadius.CONTROL.dp))
                .clickable(onClick = onBack).padding(vertical = FolioSpace.SMALL.dp, horizontal = FolioSpace.HAIR.dp).testTag("customization-back"),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChevronLeft, null, tint = LocalAccent.current.ink, modifier = Modifier.mirroredForRtl().size(28.dp))
                Text(backLabel, color = LocalAccent.current.ink, fontSize = FolioType.BODY.sp)
            }
        }
        Text(stringResource(R.string.done), color = LocalAccent.current.ink, fontSize = FolioType.BODY.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(FolioRadius.CONTROL.dp)).clickable(onClick = onClose)
                .padding(horizontal = FolioSpace.SMALL.dp, vertical = FolioSpace.SMALL.dp).description(R.string.close_customization))
    }
}

@Composable private fun SettingsLargeTitle(title: String) = Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = 32.sp,
    fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = FolioSpace.TINY.dp, bottom = FolioSpace.SMALL.dp))

/** Sidebar header row: Folio's icon and name, like the account card in iPad Settings. */
@Composable private fun SidebarAppRow(selected: Boolean, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icon = remember { folioIconBitmap(context) }
    // iPad Settings: the selection is a rounded highlight inset from the group's edges, not a square band.
    Row(Modifier.fillMaxWidth().padding(FolioSpace.TINY.dp).clip(RoundedCornerShape(FolioRadius.ROW.dp)).background(if (selected) LocalAccent.current.fill else androidx.compose.ui.graphics.Color.Transparent).clickable(onClick = onClick)
        .padding(horizontal = FolioSpace.COMFY.dp, vertical = FolioSpace.COMPACT.dp).testTag("settings-sidebar-folio"), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Image(it, null, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.folio), color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.TITLE3.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.home_panels_and_tweaks),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = if (selected) .85f else .55f), fontSize = FolioType.COMPACT.sp)
        }
    }
}

@Composable
private fun LauncherHelp(
    isDefaultHome: Boolean,
    onHomeSettings: () -> Unit,
    onAddWidget: () -> Unit,
    onShadeSetup: () -> Unit,
) {
    SheetGroup {
        HelpTip(Icons.Rounded.Home, FolioColors.Value.Blue, stringResource(R.string.edit_home), stringResource(R.string.hold_an_app_for_its_menu_or_move_while_h))
        MenuDivider()
        HelpTip(Icons.Rounded.Widgets, FolioColors.Value.Indigo, stringResource(R.string.widgets_smart_stacks), stringResource(R.string.hold_a_widget_and_let_go_for_sizes_stack))
        MenuDivider()
        HelpTip(Icons.Rounded.SwipeDown, FolioColors.Value.RedLight, stringResource(R.string.notifications_quick_settings), stringResource(R.string.pull_down_from_the_top_left_or_top_right))
        MenuDivider()
        HelpTip(Icons.Rounded.Devices, FolioColors.Value.Pink, stringResource(R.string.folding), stringResource(R.string.folio_fades_between_screens_and_keeps_th))
    }
    SheetGroup {
        IosActionRow(stringResource(R.string.add_widget_to_this_page_2), "help-add-widget", onClick = onAddWidget)
    }
}

@Composable
private fun HelpTip(icon: ImageVector, color: Long, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = FolioSpace.COMFY.dp, vertical = FolioSpace.COMPACT.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(androidx.compose.ui.graphics.Color(color)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp)
            Text(detail, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), fontSize = FolioType.COMPACT.sp)
        }
    }
}

/** Tweak-style header: Folio's icon, name and version, like a jailbreak tweak's preference banner. */
@Composable private fun TweakBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    Column(Modifier.fillMaxWidth().padding(top = FolioSpace.TINY.dp, bottom = FolioSpace.SNUG.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Folio's own launcher icon, like a tweak's preference banner.
        val icon = remember { folioIconBitmap(context) }
        if (icon != null) androidx.compose.foundation.Image(icon, null, Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)))
        Text(stringResource(R.string.folio), color = androidx.compose.ui.graphics.Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = FolioSpace.SMALL.dp))
        Text(stringResource(R.string.iphone_duo_for_your_fold_v, version), color = androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), fontSize = FolioType.COMPACT.sp)
    }
}

/** iOS Settings row: colored rounded icon square, title, optional value, chevron. */
/**
 * A Settings row that opens a page. [selected] is null for a row that's only a place to go; true or false only in
 * the split view's sidebar, where the open page is marked. There TalkBack hears a tab, selected or not, as in
 * Material's navigation rows; elsewhere it heard "Not selected" on every row.
 */
@Composable internal fun TweakRow(icon: ImageVector, color: Long, title: String, tag: String, value: String? = null,
    selected: Boolean? = null, chevron: Boolean = true, onClick: () -> Unit) {
    val open = selected == true
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .then(if (open) Modifier.padding(horizontal = 5.dp, vertical = FolioSpace.HAIR.dp).clip(RoundedCornerShape(FolioRadius.CONTROL.dp)).background(LocalAccent.current.fill) else Modifier)
        // The inset is taken back from the content padding, so the icon and title don't shift when selected.
        .clickable(onClick = onClick).padding(horizontal = if (open) 9.dp else FolioSpace.COMFY.dp, vertical = if (open) FolioSpace.SNUG.dp else FolioSpace.SMALL.dp).testTag(tag)
        .then(if (selected != null) Modifier.semantics { this.selected = selected; role = androidx.compose.ui.semantics.Role.Tab } else Modifier),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(androidx.compose.ui.graphics.Color(color)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        // A long title wraps to two lines rather than being cut off — iOS does the same in a narrow window or at a
        // large text size — and the value drops under it when there isn't room beside it.
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val tight = config.screenWidthDp < 360 || config.fontScale >= 1.3f
        val valueColor = androidx.compose.ui.graphics.Color.White.copy(alpha = if (open) .85f else .5f)
        Column(Modifier.weight(1f)) {
            Text(
                title, color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (tight) value?.let { Text(it, color = valueColor, fontSize = FolioType.SUBHEAD.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        }
        if (!tight) value?.let {
            Text(it, color = valueColor, fontSize = FolioType.BODY.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = FolioSpace.SMALL.dp))
        }
        if (chevron) Icon(Icons.Rounded.ChevronRight, null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = .3f), modifier = Modifier.mirroredForRtl())
    }
}

/** iOS Settings search field. */
/** A row that just says something: a title and a line under it, with no control. */
@Composable private fun SwitchlessRow(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.MEDIUM.dp)) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun SettingsSearchField(query: String, onQuery: (String) -> Unit) =
    IosSearchField(query, onQuery, stringResource(R.string.search), fieldModifier = Modifier.testTag("settings-search"))

/** Where each setting lives, for Settings search: its title as shown, and words people search for (settings_keywords_*). */
internal val SettingsIndex: List<Triple<Int, Int, CustomizationPage>> = listOf(
    Triple(R.string.settings_background_wallpaper, R.string.settings_keywords_background_wallpaper, CustomizationPage.WALLPAPER),
    Triple(R.string.text_on_home, R.string.settings_keywords_text_on_home, CustomizationPage.WALLPAPER),
    Triple(R.string.settings_big_buttons_in_every_app, R.string.settings_keywords_big_buttons_in_every_app, CustomizationPage.ACCESSIBILITY),
    Triple(R.string.settings_dock_placement, R.string.settings_keywords_dock_placement, CustomizationPage.HOME),
    Triple(R.string.reduce_motion, R.string.settings_keywords_reduce_motion, CustomizationPage.ACCESSIBILITY),
    Triple(R.string.darken_behind_text, R.string.settings_keywords_darken_behind_text, CustomizationPage.WALLPAPER),
    Triple(R.string.widgets, R.string.settings_keywords_widgets, CustomizationPage.HOME),
    Triple(R.string.settings_icon_pack, R.string.settings_keywords_icon_pack, CustomizationPage.STATUS),
    Triple(R.string.notification_badges, R.string.settings_keywords_notification_badges, CustomizationPage.STATUS),
    Triple(R.string.settings_live_clock_and_calendar_icons, R.string.settings_keywords_live_clock_and_calendar_icons, CustomizationPage.STATUS),
    Triple(R.string.settings_side_bar_status_bar, R.string.settings_keywords_side_bar_status_bar, CustomizationPage.STATUS),
    Triple(R.string.settings_dock_in_every_app, R.string.settings_keywords_dock_in_every_app, CustomizationPage.ACCESSIBILITY),
    Triple(R.string.spotlight, R.string.settings_keywords_spotlight, CustomizationPage.SEARCH),
    Triple(R.string.app_library, R.string.settings_keywords_app_library, CustomizationPage.SEARCH),
    Triple(R.string.settings_search_button_swipe_down, R.string.settings_keywords_search_button_swipe_down, CustomizationPage.SEARCH),
    Triple(R.string.settings_page_dots_haptics, R.string.settings_keywords_page_dots_haptics, CustomizationPage.GESTURES),
    Triple(R.string.settings_gestures_pull_downs, R.string.settings_keywords_gestures_pull_downs, CustomizationPage.GESTURES),
    Triple(R.string.settings_swipe_down_on_home, R.string.settings_keywords_swipe_down_on_home, CustomizationPage.GESTURES),
    Triple(R.string.actions, R.string.settings_keywords_actions, CustomizationPage.GESTURES),
    Triple(R.string.fold_animation, R.string.settings_keywords_fold_animation, CustomizationPage.FOLD),
    Triple(R.string.tweaks, R.string.settings_keywords_tweaks, CustomizationPage.TWEAKS),
    Triple(R.string.general, R.string.settings_keywords_general, CustomizationPage.GENERAL),
    Triple(R.string.language, R.string.settings_keywords_language, CustomizationPage.GENERAL),
    Triple(R.string.privacy_permissions, R.string.settings_keywords_privacy_permissions, CustomizationPage.PERMISSIONS),
    Triple(R.string.settings_safe_mode_crash_reports, R.string.settings_keywords_safe_mode_crash_reports, CustomizationPage.ADVANCED),
    Triple(R.string.settings_backup_restore, R.string.settings_keywords_backup_restore, CustomizationPage.BACKUP),
    Triple(R.string.help, R.string.settings_keywords_help, CustomizationPage.HELP),
    Triple(R.string.credits, R.string.settings_keywords_credits, CustomizationPage.CREDITS),
)

/**
 * Settings that Settings search finds by the name they have on screen, so they need no keyword string of their own.
 * SettingsSearchCoverageTest fails when a switch, menu or slider is on a page and neither list has it.
 */
internal val SettingsRows: List<Pair<Int, CustomizationPage>> = listOf(
    R.string.full_width_home to CustomizationPage.HOME,
    R.string.clear_badges_when_opened to CustomizationPage.STATUS,
    R.string.settings_live_clock_and_calendar_icons to CustomizationPage.STATUS,
    R.string.left_handed_layout_rail_on_the_left to CustomizationPage.STATUS,
    R.string.show_app_names to CustomizationPage.STATUS,
    R.string.show_status_in_the_rail to CustomizationPage.STATUS,
    R.string.dock_handle_on_the_rail_edge to CustomizationPage.ACCESSIBILITY,
    R.string.drag_page_dots_to_flip_pages to CustomizationPage.GESTURES,
    R.string.add_new_apps_to_home_screen to CustomizationPage.SEARCH,
    R.string.group_apps_into_categories to CustomizationPage.SEARCH,
    R.string.message_contacts_with to CustomizationPage.SEARCH,
    R.string.home_strip to CustomizationPage.SEARCH,
    R.string.search_button_opens_the_google_app to CustomizationPage.SEARCH,
    R.string.search_with_enter to CustomizationPage.SEARCH,
    R.string.work_apps to CustomizationPage.SEARCH,
    R.string.stay_awake_on_the_cover_screen to CustomizationPage.FOLD,
    R.string.text_color to CustomizationPage.WALLPAPER,
    R.string.wallpaper_moves_with_pages to CustomizationPage.WALLPAPER,
    R.string.placement to CustomizationPage.HOME,
    R.string.fold_displays to CustomizationPage.FOLD,
    R.string.gestures_actions to CustomizationPage.GESTURES,
    R.string.home_screen_dock to CustomizationPage.HOME,
    R.string.icons_side_bar to CustomizationPage.STATUS,
    R.string.search_app_library to CustomizationPage.SEARCH,
    R.string.clear_suggestions_history to CustomizationPage.SEARCH,
    R.string.haptic_feedback to CustomizationPage.GESTURES,
    R.string.swipe_down_on_home to CustomizationPage.GESTURES,
    R.string.dock to CustomizationPage.HOME,
    R.string.side_rail to CustomizationPage.STATUS,
    R.string.icon_pack to CustomizationPage.STATUS,
    R.string.big_buttons to CustomizationPage.ACCESSIBILITY,
    R.string.restore to CustomizationPage.BACKUP,
    R.string.add_widget_to_this_page to CustomizationPage.HOME,
    R.string.choose_home_apps to CustomizationPage.HOME,
    R.string.save_current_layout to CustomizationPage.BACKUP,
    R.string.also_set_as_phone_wallpaper to CustomizationPage.WALLPAPER,
    R.string.change_android_wallpaper to CustomizationPage.WALLPAPER,
    R.string.put_the_buttons_back_at_the_bottom to CustomizationPage.ACCESSIBILITY,
    R.string.refresh_icons to CustomizationPage.STATUS,
    R.string.reset_fold_timing to CustomizationPage.FOLD,
    R.string.restore_from_backup to CustomizationPage.BACKUP,
    R.string.save_backup to CustomizationPage.BACKUP,
    R.string.save_backup_to_files to CustomizationPage.BACKUP,
    R.string.share_diagnostics to CustomizationPage.ADVANCED,
    R.string.capability_open_settings to CustomizationPage.ADVANCED,
    R.string.copy_diagnostics to CustomizationPage.ADVANCED,
    R.string.share_latest to CustomizationPage.ADVANCED,
    R.string.performance_log to CustomizationPage.ADVANCED,
    R.string.performance_start to CustomizationPage.ADVANCED,
    R.string.performance_stop to CustomizationPage.ADVANCED,
    R.string.performance_copy to CustomizationPage.ADVANCED,
    R.string.performance_share to CustomizationPage.ADVANCED,
)

/**
 * A search result scrolls to its row when it is a row: an entry with no keywords, or one with keywords whose title is also
 * a row.
 */
internal fun isRowEntry(title: Int, keywords: Int?): Boolean = keywords == null || SettingsRows.any { it.first == title }

/**
 * Rows already in [SettingsIndex] (with keywords) are listed once, from there; [SettingsRows] still marks them as rows for [isRowEntry].
 */
internal val SettingsEntries: List<Triple<Int, Int?, CustomizationPage>> = SettingsIndex +
    SettingsRows.filter { (title, _) -> SettingsIndex.none { it.first == title } }.map { (title, page) -> Triple(title, null, page) }

private class Found(val title: String, val keywords: String, val page: CustomizationPage, val row: Boolean)

internal fun settingsMatches(query: String, title: String, keywords: String): Boolean {
    // A lone "&" or "-" in the query ("Fold & Displays") has nothing to match, so it must not make the search fail.
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { w -> w.any { it.isLetterOrDigit() } }
    val hay = "$title $keywords".lowercase()
    return words.isNotEmpty() && words.all { it in hay }
}

/**
 * What Settings search matches a tweak on: its name, what it's based on ("Barrel by Aaron Ash"), its one line, and any
 * [TweakFeature.keywords].
 * Tweaks aren't in [SettingsIndex] because each has a page of its own rather than a row on a fixed page.
 */
internal fun searchableTweaks(context: android.content.Context): List<Pair<TweakFeature, String>> =
    TweakFeatures.map { tweak ->
        tweak to listOfNotNull(tweak.inspiredBy, context.getString(tweak.description), tweak.keywords?.let(context::getString)).joinToString(" ")
    }

/** [onOpen] gets the page and, for a setting found by its own name, that name, so the page can scroll to its row. */
@Composable private fun SettingsSearchResults(query: String, onOpen: (CustomizationPage, String?) -> Unit, onOpenTweak: (TweakFeature) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources
    val index = remember(androidx.compose.ui.platform.LocalConfiguration.current) {
        SettingsEntries.map { (title, keywords, page) -> Found(resources.getString(title), keywords?.let(resources::getString).orEmpty(), page, row = isRowEntry(title, keywords)) }
    }
    val tweaks = remember(androidx.compose.ui.platform.LocalConfiguration.current) { searchableTweaks(context) }
    val results = index.filter { settingsMatches(query, it.title, it.keywords) }
    val tweakResults = tweaks.filter { (tweak, keywords) -> settingsMatches(query, tweak.name, keywords) }.map { it.first }
    if (results.isEmpty() && tweakResults.isEmpty()) Text(stringResource(R.string.no_results_for_1, query.trim()), color = androidx.compose.ui.graphics.Color.White.copy(alpha = .55f),
        modifier = Modifier.fillMaxWidth().padding(FolioSpace.XXL.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    else SheetGroup {
        results.forEachIndexed { index, found ->
            if (index > 0) MenuDivider()
            // A page found by its own name needs no "where": it is the page.
            val where = if (found.row) stringResource(found.page.title).takeIf { it != found.title } else null
            SettingsSearchRow(found.title, where) { onOpen(found.page, found.title.takeIf { found.row && where != null }) }
        }
        // A tweak says it's a tweak, since its name alone ("Cabinet", "Harborline") doesn't say where it lives.
        tweakResults.forEachIndexed { index, tweak ->
            if (index > 0 || results.isNotEmpty()) MenuDivider()
            SettingsSearchRow(tweak.name, stringResource(R.string.tweaks)) { onOpenTweak(tweak) }
        }
    }
}

@Composable private fun SettingsSearchRow(title: String, where: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.SMALL.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp, modifier = Modifier.weight(1f))
        where?.let { Text(it, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), fontSize = FolioType.BODY.sp) }
        Icon(Icons.Rounded.ChevronRight, null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = .3f), modifier = Modifier.mirroredForRtl())
    }
}

/** Every permission Folio can use, whether it's allowed, and which features rely on it (shared ownership). */
@Composable private fun PermissionsPage(isDefaultHome: Boolean, onMakeDefault: () -> Unit, onShadeSetup: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) { tick++ } }
    fun open(intent: android.content.Intent) { runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val appSettings = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
    data class Perm(val name: String, val usedBy: String, val allowed: Boolean, val action: () -> Unit)
    val perms = remember(tick, isDefaultHome) { listOf(
        Perm(context.getString(R.string.home_app), context.getString(R.string.home_button_folding_gestures), isDefaultHome, onMakeDefault),
        Perm(context.getString(R.string.notification_access), context.getString(R.string.badges_cabinet),
            NotificationListener.hasAccess(context)) { open(NotificationListener.accessSettingsIntent(context)) },
        Perm(context.getString(R.string.gestures_service_accessibility), context.getString(R.string.pull_downs_dock_and_big_buttons_in_other),
            SystemShadeAccessibilityService.isConnected(), onShadeSetup),
        Perm(context.getString(R.string.do_not_disturb_access), context.getString(R.string.actions),
            context.getSystemService(android.app.NotificationManager::class.java).isNotificationPolicyAccessGranted) {
            open(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
        Perm(context.getString(R.string.usage_access_optional), context.getString(R.string.better_suggestions_counts_apps_you_open), Suggestions.hasUsageAccess(context)) {
            open(Suggestions.usageAccessIntent(context)) },
        Perm(context.getString(R.string.calendar_optional), context.getString(R.string.up_next_widget), UpNext.hasCalendar(context)) { open(appSettings) },
        Perm(context.getString(R.string.contacts), context.getString(R.string.spotlight_contact_search), context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) { open(appSettings) },
    ) }
    CardNote(stringResource(R.string.everything_stays_on_your_phone_folio_has), Modifier.padding(horizontal = FolioSpace.TINY.dp))
    SheetGroup {
        perms.forEachIndexed { index, perm ->
            if (index > 0) MenuDivider()
            Row(Modifier.fillMaxWidth().clickable(onClick = perm.action).padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.COMPACT.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(perm.name, color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp)
                    Text(stringResource(R.string.used_by_1, perm.usedBy), color = androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), fontSize = FolioType.FOOTNOTE.sp)
                }
                Text(stringResource(if (perm.allowed) R.string.allowed else R.string.state_off), color = if (perm.allowed) FolioColors.Green
                    else androidx.compose.ui.graphics.Color.White.copy(alpha = .5f), fontSize = FolioType.SUBHEAD.sp)
                Icon(Icons.Rounded.ChevronRight, null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = .3f), modifier = Modifier.mirroredForRtl())
            }
        }
        // Some banking apps refuse to run while any accessibility service is on, Folio's included (reported on
        // r/GalaxyFold, 18 Sep 2026). Nothing Folio can do from its side, so say so before someone is caught out.
        CardNote(stringResource(R.string.banking_apps_note))
    }
}

/** Tweak preference page: main switch first, per-screen overrides (dimmed when off), credit, reset. */
@Composable private fun TweakPage(tweak: TweakFeature, state: LauncherState, model: LauncherModel) {
    val on = tweak.get(state)
    SettingsCard(tweak.name) {
        SettingsSwitch(stringResource(R.string.enabled), on, { tweak.set(model, it) }, "tweak-enabled-${tweak.id}")
        CardNote(stringResource(tweak.description))
    }
    SettingsCard(stringResource(R.string.use_on)) {
        Column(Modifier.alpha(if (on) 1f else .4f)) {
            FolioScreen.entries.forEach { screen ->
                val value = FeatureScopes.value(state.featureScopes, tweak.id, screen)
                IosMenuRow(stringResource(screen.label), ScopeValue.entries.map { it to stringResource(it.label) }, value, { model.setFeatureScope(tweak.id, screen, it) }, enabled = on, tag = "scope-${tweak.id}-${screen.name.lowercase()}")
            }
        }
        CardNote(stringResource(R.string.default_follows_enabled_on_or_off_applie))
    }
    SettingsCard(stringResource(R.string.about)) {
        TextButton(onClick = { model.resetTweak(tweak) }) { Text(stringResource(R.string.reset_1, tweak.name)) }
        CardNote(tweak.credit?.let { stringResource(it) } ?: stringResource(R.string.inspired_by_re_created_from_scratch_no_t, tweak.inspiredBy))
    }
}

private fun riskLabel(risk: OperationRisk) = when (risk) {
    OperationRisk.OBSERVE -> R.string.risk_observe
    OperationRisk.REVERSIBLE -> R.string.risk_reversible
    OperationRisk.STATEFUL -> R.string.risk_stateful
    OperationRisk.DISRUPTIVE -> R.string.risk_disruptive
}

/** Advanced › Diagnostics: what this phone and your permissions allow, and why a feature might be off (A0 to A2, see Capabilities). */
@Composable private fun CapabilitiesCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun read() = Capabilities.rows(NotificationListener.hasAccess(context), SystemShadeAccessibilityService.isConnected())
    var rows by remember { mutableStateOf(read()) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) { rows = read() } }
    SettingsCard(stringResource(R.string.capabilities)) {
        rows.forEach { row ->
            val (name, uses) = when (row.tier) {
                CapabilityTier.STANDARD -> R.string.capability_standard to R.string.capability_standard_uses
                CapabilityTier.NOTIFICATIONS -> R.string.capability_notifications to R.string.capability_notifications_uses
                CapabilityTier.ACCESSIBILITY -> R.string.capability_accessibility to R.string.capability_accessibility_uses
            }
            Row(Modifier.fillMaxWidth().padding(vertical = FolioSpace.SMALL.dp).testTag("capability-${row.tier.code}"), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${row.tier.code} · ${stringResource(name)}", style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(uses), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.capability_risk_line, stringResource(riskLabel(row.risk))), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("capability-risk-${row.tier.code}"))
                }
                Text(stringResource(if (row.on) R.string.on else R.string.capability_off), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (Capabilities.needsPermission(rows).isNotEmpty()) CardAction(stringResource(R.string.capability_open_settings), onClick = {
            val tier = Capabilities.needsPermission(rows).first()
            val intent = if (tier == CapabilityTier.NOTIFICATIONS) NotificationListener.accessSettingsIntent(context)
                else android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }, modifier = Modifier.testTag("capability-open-settings"))
        CardNote(stringResource(R.string.capabilities_note))
    }
}

/** Advanced › Diagnostics: Folio's own recent activity, newest first, with the failures it carried on from. Kept only on the phone. */
@Composable private fun RecentActivityCard() {
    var failedOnly by remember { mutableStateOf(false) }
    val entries = remember { Inspector.entries(Diagnostics.trailText()) }
    val shown = Inspector.filter(entries, failedOnly).take(15)
    SettingsCard(stringResource(R.string.recent_activity)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(FolioSpace.SMALL.dp)) {
            IosChip(!failedOnly, { failedOnly = false }, label = { Text(stringResource(R.string.all)) }, modifier = Modifier.testTag("activity-all"))
            IosChip(failedOnly, { failedOnly = true }, label = { Text(stringResource(R.string.activity_failed)) }, modifier = Modifier.testTag("activity-failed"))
        }
        if (shown.isEmpty()) Text(stringResource(R.string.no_activity_to_show), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = FolioSpace.SMALL.dp))
        shown.forEach { e ->
            Column(Modifier.fillMaxWidth().padding(vertical = FolioSpace.TINY.dp)) {
                Text(e.text, style = MaterialTheme.typography.bodyMedium, color = if (e.failed) FolioColors.Red else androidx.compose.ui.graphics.Color.Unspecified)
                if (e.time.isNotEmpty()) Text(e.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CardNote(stringResource(R.string.recent_activity_note))
    }
}

@Composable private fun CrashReportsCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var reports by remember { mutableStateOf(CrashLog.reports(context)) }
    SettingsCard(stringResource(R.string.crash_reports)) {
        Text(if (reports.isEmpty()) stringResource(R.string.no_problems_recorded) else pluralStringResource(R.plurals.reports_saved, reports.size, reports.size),
            style = MaterialTheme.typography.bodyMedium)
        reports.firstOrNull()?.let { latest ->
            CardNote(latest.readLines().take(5).joinToString("\n"))
            CardAction(stringResource(R.string.share_latest), Modifier.fillMaxWidth(), onClick = { runCatching { context.startActivity(CrashLog.shareIntent(latest).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } })
            CardAction(stringResource(R.string.clear), Modifier.fillMaxWidth(), destructive = true, onClick = { CrashLog.clear(context); reports = emptyList() })
        }
        val shareScope = rememberCoroutineScope()
        CardAction(stringResource(R.string.share_diagnostics), onClick = {
            shareScope.launch { Diagnostics.send(context) }
        }, modifier = Modifier.testTag("share-diagnostics"))
        // Copy is the other half of Share (#239): the report on the clipboard, to paste into a form, with the same text a shared file has.
        CardAction(stringResource(R.string.copy_diagnostics), onClick = {
            shareScope.launch { runCatching { Diagnostics.copy(context); FolioNotice.show(context, context.getString(R.string.diagnostics_copied)) } }
        }, modifier = Modifier.testTag("copy-diagnostics"))
        CardNote(stringResource(R.string.diagnostics_file_note))
    }
}

@Composable private fun CreditsPage() {
    val credits = listOf(
        stringResource(R.string.duolauncher) to "jakesgoodapps (github.com/jakesgoodapps/DuoLauncher) · MIT · Folio’s starting codebase: iPhone Duo-style Home layouts for both screens, widgets, folders, work profile, wallpapers and Discover",
        "iphone-duo" to "chuspeeism · MIT · fold blur and darkening model",
        "Duo Fold Live" to stringResource(R.string.credit_duo_fold_live), // english-only
        "hingewave" to stringResource(R.string.credit_hingewave), // english-only
        "FoldFX (iamkeeler)" to stringResource(R.string.credit_foldfx_keeler), // english-only
        "Duo Open" to stringResource(R.string.credit_duo_open), // english-only
        "iPhone Duo on Galaxy Z Fold 8 demo" to "u/moomanjohnny · screenshot + shader idea (no code)",
        stringResource(R.string.quicklaunch) to stringResource(R.string.ahmedthegeek_spotlight_ideas_no_code),
        stringResource(R.string.foldfx) to "u/FixHour8452 · fold transition ideas: halfway haptic tick, light sweep, slight scale, following a smooth hinge angle (no code)",
        stringResource(R.string.zfoldduo) to "nnnnnnn0090 · MIT · showed a Fold7/Fold8 can read Samsung's internal hinge angle without root; research for Enhanced Fold Tracking",
        stringResource(R.string.velox) to stringResource(R.string.phillip_tennen_app_panels_idea),
        stringResource(R.string.activator) to stringResource(R.string.ryan_petrich_gestures_and_events_idea),
        stringResource(R.string.axon) to stringResource(R.string.nepeta_notification_app_row_idea),
        stringResource(R.string.velvet) to stringResource(R.string.noisyflake_himynameisubik_tinted_notific),
        stringResource(R.string.colorflow) to stringResource(R.string.david_goldman_album_art_colors_idea),
        stringResource(R.string.harbor) to stringResource(R.string.evan_swick_dock_magnification_idea),
        stringResource(R.string.snowboard) to stringResource(R.string.sparkdev_themes_idea_no_code),
        stringResource(R.string.apex) to stringResource(R.string.sticktron_icon_stacks_idea_no_code),
        stringResource(R.string.icon_restore) to stringResource(R.string.layout_history_idea_no_code),
        stringResource(R.string.lynx_2) to "recent-app dots idea (no code)",
        stringResource(R.string.colorbadges) to "badges that match the app idea (no code)",
        stringResource(R.string.contributor_covenant_3_0) to stringResource(R.string.organization_for_ethical_source_cc_by_sa),
    )
    SettingsCard(stringResource(R.string.thanks_to)) {
        credits.forEach { (name, detail) ->
            Column(Modifier.fillMaxWidth().padding(vertical = FolioSpace.SNUG.dp)) {
                Text(name)
                CardNote(detail)
            }
        }
    }
    CardNote(stringResource(R.string.tweak_ideas_were_re_created_from_scratch), Modifier.padding(horizontal = FolioSpace.TINY.dp))
}

/** The Home page the Settings previews draw: set from the page Home is on, so a widget placed on page 2 shows up. */
internal val LocalPreviewPage = androidx.compose.runtime.compositionLocalOf { 0 }

/** The phone's own status for the Settings previews, which already show the real time; the sample is for previews with no phone behind them. */
internal val LocalDeviceStatus = androidx.compose.runtime.compositionLocalOf { DeviceStatus(battery = 80, wifiConnected = true, wifiLevel = 4, cellularLevel = 4) }

/**
 * Live preview of Home built from real data only: your Home and dock apps (with the current icon shape, pack,
 * tint, badges and live icons), your text, glass and dimming settings, and your background. Android's wallpaper
 * image can't be read by apps, so in that mode the preview uses the wallpaper's own reported colors and says so.
 */
@Composable internal fun MiniHomePreview(stagedBitmap: android.graphics.Bitmap?, state: LauncherState,
    previewHeight: androidx.compose.ui.unit.Dp, framed: Boolean = true,
    /** The Side Bar (status, dock and search): off for the second page of an unfolded preview, which has one Side Bar. */
    sideBar: Boolean = true,
    /** The cover's layout by default; the inner screen's for an unfolded preview. */
    preset: LayoutPreset = state.compact,
    /** Which Home page to draw: the one Home is on (LocalPreviewPage) unless a caller pins one. */
    page: Int = LocalPreviewPage.current) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backgroundRevision = LauncherBackgroundCache.revision.intValue
    val committedBitmap = remember(backgroundRevision) { cachedLauncherBackground(context) }
    val bitmap = stagedBitmap ?: committedBitmap
    val apps = remember(state.apps) { state.apps.associateBy { it.id } }
    val tone = LocalWallpaperTone.current
    val ink = homeInkFor(state.homeInk, tone.prefersDarkText)
    val basePalette = LocalDuoPalette.current
    val glass = tintedGlass(basePalette.glass, tone.primary, GLASS_TINT)
    // Home page 1 drawn at a real cover-screen size with Folio's own layout math and parts (widget cards, icons,
    // status rail, dock, search pill), then scaled down, so the preview matches Home instead of approximating it.
    val refW = 420f; val refH = 720f
    val geometry = homeGeometry(refW, refH, preset, state.labels, statusHeight = if (state.verticalStatus) 180f else 0f, labelHeight = 20f,
        appRows = state.homeAppRows, dockSlots = state.dock.size, statusRail = state.verticalStatus,
        railControls = if (state.searchPill) 0 else 1) // the Home pages' rail, as Home lays it out
    val placements = state.widgetPlacements.filter { it.page == page }
    val shownRows = shownHomeRows(state.homeAppRows, state.homeSlots.drop(homeCellIndex(page, 0).coerceAtLeast(0)).take(HOME_CELLS), placements)
    val cells = HomeCellLayout.forPage(geometry, placements.map { it.row to it.spanY })
    val iconSize = geometry.cellIconSize(state.labels)
    val labels = state.labels
    val scale = previewHeight.value / refH
    val left = state.leftHanded
    val railAlign = if (left) Alignment.TopStart else Alignment.TopEnd
    val railEdge = if (left) Modifier.padding(start = FolioSpace.MEDIUM.dp) else Modifier.padding(end = FolioSpace.MEDIUM.dp)
    // A thin black bezel with the screen's own corners inside it, so the preview reads as the phone, not a card.
    val corner = 26.dp * (previewHeight.value / 260f)
    val bezel = previewHeight * .022f
    Column(if (framed) Modifier.fillMaxWidth() else Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(if (!framed) Modifier else Modifier.clip(RoundedCornerShape(corner + bezel)).background(FolioColors.PreviewBezel)
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .2f), RoundedCornerShape(corner + bezel)).padding(bezel)) {
        Box(Modifier.height(previewHeight).width(previewHeight * (refW / refH))
            .clip(RoundedCornerShape(if (framed) corner else 0.dp))
            .testTag("customization-home-preview").clearedDescription(R.string.preview_of_home), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(refW.dp, refH.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                if (state.systemWallpaper) Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                    androidx.compose.ui.graphics.Color(tone.primary ?: 0xFF5A6B78.toInt()), androidx.compose.ui.graphics.Color(tone.secondary ?: tone.primary ?: 0xFF2E3A42.toInt())))))
                else {
                    DuneWallpaper()
                    bitmap?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                }
                // Drawn over the background rather than inside it, because the preview lays the chosen photo over the
                // background; on Home the same scrim is baked into the background's cached layer.
                HomeScrim.of(state.homeScrim, ink.dark).let { if (it.draws) Box(Modifier.matchParentSize().homeScrim(it)) }
                CompositionLocalProvider(LocalHomeInk provides ink, LocalDuoPalette provides basePalette.copy(glass = glass), LocalHomeIconSize provides geometry.iconSize) {
                    Box(Modifier.offset(x = (if (left) refW - 16f - geometry.gridWidth else 16f).dp, y = geometry.contentTop.dp).width(geometry.gridWidth.dp).height((cells.height(shownRows)).dp)) {
                        placements.forEach { w ->
                            // A freely placed widget (Place Freely) is drawn a part of a cell from its cells, as on Home.
                            val rowPitch = if (w.offsetY < 0f && w.row > 0) cells.y(w.row) - cells.y(w.row - 1)
                                else if (w.row + 1 < GRID_ROWS) cells.y(w.row + 1) - cells.y(w.row) else cells.y(w.row) - cells.y(w.row - 1)
                            Box(Modifier.offset(x = (cells.x(w.column, w.row) + 5f + geometry.cellWidth * w.offsetX).dp, y = (cells.y(w.row) + rowPitch * w.offsetY).dp)
                                .size((geometry.cellWidth * w.spanX - 10f).dp, (cells.spanHeight(w.row, w.spanY) - 18f).coerceAtLeast(48f).dp)) {
                                if (w.id < 0) BuiltinWidgetCard(w.id, w.slot) {}
                                else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(FolioRadius.PANEL.dp)).background(glass.copy(alpha = LIQUID_FROST)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Widgets, null, tint = ink.secondary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        repeat(shownRows * GRID_COLUMNS) { local ->
                            val id = state.homeSlots.getOrNull(homeCellIndex(page, local)) ?: return@repeat
                            val app = apps[id]
                            val folder = if (app == null) state.folders.firstOrNull { it.id == id } ?: return@repeat else null
                            val row = local / GRID_COLUMNS
                            Column(Modifier.offset(x = cells.x(local % GRID_COLUMNS, row).dp, y = cells.y(row).dp).width(geometry.cellWidth.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                if (app != null) AppIcon(app, null, Modifier.size(iconSize.dp), badge = true)
                                else if (folder != null) PreviewFolder(folder, apps, iconSize)
                                if (labels) Text(app?.label ?: folder?.title.orEmpty(), color = ink.primary, fontSize = HOME_LABEL_SP.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = FolioSpace.TINY.dp, start = FolioSpace.HAIR.dp, end = FolioSpace.HAIR.dp),
                                    style = androidx.compose.ui.text.TextStyle(shadow = ink.labelShadow))
                            }
                        }
                    }
                    if (sideBar && state.verticalStatus) StatusRail(LocalDeviceStatus.current,
                        Modifier.align(railAlign).then(railEdge).offset(y = geometry.contentTop.dp).width(DOCK_WIDTH.dp),
                        iconSize = dockIconSize(iconSize).dp)
                    val barShape = RoundedCornerShape(dockCornerRadius(geometry.dockBarHeight, dockIconSize(iconSize)).dp)
                    val railShape = RoundedCornerShape(dockCornerRadius(DOCK_WIDTH, dockIconSize(iconSize)).dp)
                    // A bottom dock bar: beside the status Side Bar, or the whole width when there is none (Full-Width Home).
                    if (sideBar && geometry.horizontalDock) Box(Modifier.align(if (!geometry.dockBesideRail) Alignment.BottomCenter else if (left) Alignment.BottomEnd else Alignment.BottomStart)
                        .width((if (geometry.dockBesideRail) refW - DOCK_WIDTH - 28f else refW).dp).padding(bottom = 58.dp), contentAlignment = Alignment.Center) { Row(Modifier
                        .height(geometry.dockBarHeight.dp).background(glass.copy(alpha = LIQUID_FROST), barShape)
                        .border(1.dp, GlassOutlineColor, barShape).padding(horizontal = FolioSpace.SMALL.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.dock.forEach { id ->
                            Box(Modifier.width(geometry.dockPitch.dp), contentAlignment = Alignment.Center) {
                                id?.let(apps::get)?.let { AppIcon(it, null, Modifier.size(dockIconSize(iconSize).dp), badge = true) }
                            }
                        }
                    } }
                    else if (sideBar) Column(Modifier.align(railAlign).then(railEdge).offset(y = geometry.dockTop.dp).width(DOCK_WIDTH.dp)
                        .height(geometry.dockHeight.dp).background(glass.copy(alpha = LIQUID_FROST), railShape)
                        .border(1.dp, GlassOutlineColor, railShape).padding(vertical = FolioSpace.SMALL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        state.dock.forEach { id ->
                            Box(Modifier.fillMaxWidth().height(geometry.dockRowHeight.dp), contentAlignment = Alignment.Center) {
                                id?.let(apps::get)?.let { AppIcon(it, null, Modifier.size(dockIconSize(iconSize).dp), badge = true) }
                            }
                        }
                    }
                    if (sideBar && state.searchPill) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = FolioSpace.COMFY.dp)
                        .then(if (left) Modifier.padding(start = (DOCK_WIDTH + 24f).dp) else Modifier.padding(end = (DOCK_WIDTH + 24f).dp))) {
                        HomeSearchPill {}
                    }
                }
            }
        }
        }
        if (framed && state.systemWallpaper) CardNote(stringResource(R.string.colors_from_your_android_wallpaper_apps), Modifier.padding(top = FolioSpace.SNUG.dp))
    }
}

/**
 * Hidden apps, like iOS: kept out of the App Library, and listed here only after unlocking with a fingerprint, face or
 * the phone's PIN (straight away on a phone with no screen lock, where there's nothing to ask).
 */
@Composable private fun HiddenAppsRow(state: LauncherState, model: LauncherModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hidden = state.apps.filter { it.id in state.hiddenApps }.sortedBy { it.label.lowercase() }
    var unlocked by remember { mutableStateOf(false) }
    if (hidden.isEmpty()) return
    if (!unlocked) {
        IosActionRow(stringResource(R.string.hidden_apps_count, hidden.size), "hidden-apps") {
            val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            runCatching {
                android.hardware.biometrics.BiometricPrompt.Builder(context).setTitle(context.getString(R.string.hidden_apps_title))
                    .setSubtitle(context.getString(R.string.unlock_to_see_the_apps_you_ve_hidden)).setAllowedAuthenticators(authenticators).build()
                    .authenticate(android.os.CancellationSignal(), context.mainExecutor,
                        object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult) { unlocked = true }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL) unlocked = true
                            }
                        })
            }
        }
        return
    }
    Text(stringResource(R.string.hidden_apps), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = FolioSpace.SNUG.dp))
    hidden.forEach { app ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, null, Modifier.size(32.dp), badge = true)
            Spacer(Modifier.width(12.dp))
            Text(app.label, Modifier.weight(1f))
            TextButton(onClick = { model.setHidden(app.id, false) }) { Text(stringResource(R.string.unhide)) }
        }
    }
}

/** A folder as Home draws it (first four apps on glass or the folder's color), without Home's drag and launch hooks. */
@Composable private fun PreviewFolder(folder: FolderEntry, apps: Map<String, AppEntry>, size: Float) {
    val shape = RoundedCornerShape((size * .24f).dp)
    val tint = LocalFolderColors.current[folder.id]?.let { androidx.compose.ui.graphics.Color(it) }
    Box(Modifier.size(size.dp).clip(shape).background(tint?.copy(alpha = .78f) ?: Glass.copy(alpha = .72f))
        .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .55f), shape)) {
        folder.appIds.take(4).forEachIndexed { index, id ->
            apps[id]?.let { app ->
                AppIcon(app, null, Modifier.align(when (index) {
                    0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; else -> Alignment.BottomEnd
                }).padding(5.dp).size((size * .38f).dp).clip(RoundedCornerShape(6.dp)))
            }
        }
    }
}

@Composable private fun HomeLayoutSettings(state: LauncherState, wide: Boolean, onWide: (Boolean) -> Unit,
    model: LauncherModel, homePage: Int, onEditPins: () -> Unit, onWidget: (Int) -> Unit,
    onAddWidget: (Int) -> Unit, onRemoveWidget: (Int) -> Unit) {
    val screen = if (wide) LayoutScreen.INNER else LayoutScreen.COVER
    val p = state.presetFor(screen)
    IosSegmented(listOf(false to stringResource(R.string.cover), true to stringResource(R.string.inner)), wide, onWide, Modifier.padding(vertical = FolioSpace.TINY.dp), tag = "layout-screen")
    SheetGroup {
        IosActionRow(stringResource(R.string.choose_home_apps), onClick = onEditPins)
    }
    SettingsCard(stringResource(R.string.full_width_home)) {
        // Two settings that already exist, in one tap: a dock along the bottom, and no status Side Bar beside the page.
        val fullWidth = p.dockPlacement == DockPlacement.BOTTOM && !state.verticalStatus
        SettingsSwitch(stringResource(R.string.full_width_home), fullWidth, { on -> model.setFullWidthHome(screen, on) }, "full-width-home-switch")
        CardNote(stringResource(R.string.full_width_home_note))
    }
    SettingsCard(stringResource(R.string.dock)) {
        IosMenuRow(stringResource(R.string.placement), listOf(DockPlacement.AUTOMATIC to stringResource(R.string.automatic), DockPlacement.SIDE to stringResource(R.string.side_rail), DockPlacement.BOTTOM to stringResource(R.string.bottom)),
            p.dockPlacement, { model.setPreset(screen, p.copy(dockPlacement = it)) }, tag = "dock-placement")
        CardNote(when (p.dockPlacement) {
            DockPlacement.AUTOMATIC -> if (wide) stringResource(R.string.the_dock_stays_on_the_side_bar_and_moves) else stringResource(R.string.the_dock_stays_on_the_side_bar)
            DockPlacement.SIDE -> stringResource(R.string.the_dock_stays_on_the_side_bar_even_when)
            DockPlacement.BOTTOM -> if (wide) stringResource(R.string.the_dock_sits_along_the_bottom_under_you) else stringResource(R.string.the_dock_sits_along_the_bottom_in_landsc)
        })
    }
    SheetGroupLabel(stringResource(R.string.widgets_page_number, homePage + 1))
    SheetGroup {
        state.widgetPlacements.filter { it.page == homePage || (wide && it.page == -1) }.forEach { placement ->
            val removeLabel = stringResource(if (placement.page == -1) R.string.remove_widget_from_unfolded_only_page else R.string.remove_widget)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = FolioSpace.LARGE.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (placement.page == -1) stringResource(R.string.unfolded_only_page) else stringResource(R.string.widget_size_row, placement.spanX, placement.spanY, placement.row + 1), Modifier.weight(1f),
                    color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.BODY.sp)
                TextButton(onClick = { onWidget(placement.slot) }) { Text(stringResource(R.string.replace)) }
                IconButton(onClick = { onRemoveWidget(placement.slot) }, modifier = Modifier.semantics { contentDescription = removeLabel }) {
                    Icon(Icons.Rounded.RemoveCircle, null, tint = FolioColors.Red) }
            }
            MenuDivider()
        }
        IosActionRow(stringResource(R.string.add_widget_to_this_page), onClick = { onAddWidget(homePage) })
    }
}

@Composable internal fun SettingsSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, tag: String? = null, enabled: Boolean = true) {
    // One accessible element for TalkBack ("label, switch, on"); the whole row toggles.
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).settingsFocus(label).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = FolioSpace.MEDIUM.dp, top = FolioSpace.SNUG.dp, bottom = FolioSpace.SNUG.dp), fontSize = FolioType.BODY.sp); IosSwitch(checked, onChecked, Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier), enabled)
    }
}

/** [title] is left out when the page already says it, so a page with one group doesn't repeat its own name. */
@Composable internal fun SettingsCard(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FolioSpace.SNUG.dp)) {
        if (title != null) Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = FolioSpace.LARGE.dp, top = FolioSpace.COMPACT.dp).semantics { heading() })
        GroupedCard(MaterialTheme.colorScheme.surfaceContainerHigh, androidx.compose.ui.graphics.Color.White.copy(alpha = .12f),
            Modifier.fillMaxWidth(), content)
    }
}

/** The open Fold for the Inner tab: two Home pages with one Side Bar, laid out with the inner screen's settings. */
@Composable private fun UnfoldedHomePreview(bitmap: android.graphics.Bitmap?, state: LauncherState, height: androidx.compose.ui.unit.Dp) {
    val corner = 18.dp
    val bezel = 5.dp
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(Modifier.clip(RoundedCornerShape(corner + bezel)).background(FolioColors.PreviewBezel)
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .2f), RoundedCornerShape(corner + bezel)).padding(bezel)
            .clip(RoundedCornerShape(corner)).clearedDescription(R.string.preview_of_home_on_the_inner_screen)) {
            MiniHomePreview(bitmap, state, height, framed = false, sideBar = state.leftHanded, preset = state.expanded)
            MiniHomePreview(bitmap, state, height, framed = false, sideBar = !state.leftHanded, preset = state.expanded)
        }
    }
}

/** Folio's Settings icon is in the App Library; this puts it on Home in one tap, in the first free place. */
@Composable private fun FolioSettingsCard(state: LauncherState, model: LauncherModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val folio = state.apps.firstOrNull { it.component.packageName == context.packageName } ?: return
    SettingsCard(stringResource(R.string.app_icon)) {
        val onHome = folio.id in state.homeAppIds()
        CardAction(stringResource(if (onHome) R.string.folio_settings_on_home else R.string.add_folio_settings_to_home), enabled = !onHome,
            modifier = Modifier.testTag("add-settings-to-home"), onClick = { model.setPinned(folio.id, true) })
        CardNote(stringResource(R.string.folio_settings_icon_note))
    }
}

/** Beta label beside a title, like TestFlight features. */
@Composable private fun BetaTag() {
    Text(stringResource(R.string.beta), color = FolioColors.Orange, fontSize = FolioType.CAPTION.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        modifier = Modifier.padding(start = FolioSpace.SMALL.dp).border(1.dp, FolioColors.Orange, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp))
}

/** Whether Layout History lists its saved layouts: when it's on, and always while the saved layout is damaged. */
internal fun layoutSnapshotsShown(layoutHistory: Boolean, layoutDamaged: Boolean) = layoutHistory || layoutDamaged

/** Layout History: automatic snapshots of Home before big changes, each restorable. */
@Composable internal fun LayoutHistoryCard(state: LauncherState, model: LauncherModel, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { LayoutHistory.load(context) }
    val snapshots by LayoutHistory.snapshots.collectAsState()
    var confirm by remember { mutableStateOf<LayoutSnapshot?>(null) }
    SettingsCard(stringResource(R.string.layout_history)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.save_home_before_big_changes), Modifier.weight(1f, fill = false)) }
            IosSwitch(state.layoutHistory, model::setLayoutHistory, Modifier.testTag("layout-history-switch"))
        }
        // While the layout is damaged the setting reads off (Home shows a stand-in), but the damaged-layout notice sends
        // people here to put an earlier layout back, so the saved ones are listed anyway. There's nothing to save then.
        val damaged = model.layoutDamaged
        if (layoutSnapshotsShown(state.layoutHistory, damaged)) {
            // Read here rather than in the click: the name is what the snapshot is called in the list below it.
            val savedByYou = stringResource(R.string.saved_by_you)
            if (!damaged) CardAction(stringResource(R.string.save_current_layout), onClick = { model.saveLayoutSnapshot(savedByYou, force = true) }, modifier = Modifier.testTag("layout-history-save"))
            snapshots.forEach { snapshot ->
                // Merged, like the switch row above: without it every saved layout offers a button TalkBack reads as
                // just "Restore", and there is no way to hear which layout it would put back. Merged, the row is one
                // thing to land on - "Before restoring a backup, 19 Sep 2026 8:14 PM, Restore" - carrying the button's
                // own action, and it is a bigger target besides.
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(snapshot.reason, fontSize = FolioType.CALLOUT.sp)
                        CardNote(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(snapshot.time)))
                    }
                    CardAction(stringResource(R.string.restore), onClick = { confirm = snapshot })
                }
            }
        }
        CardNote(stringResource(R.string.before_restoring_a_backup_or_layout, LayoutHistory.MAX))
    }
    confirm?.let { snapshot ->
        AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.restore_this_layout)) },
            text = { Text(stringResource(R.string.home_goes_back_to_how_it_was_1, snapshot.reason.lowercase())) },
            confirmButton = { TextButton(onClick = { model.restoreLayoutSnapshot(snapshot); confirm = null; onClose() }) { Text(stringResource(R.string.restore)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

/** Recent-app dots (Beta): needs Usage Access, asked for right here when it's turned on. */
@Composable private fun RecentDotsCard(state: LauncherState, model: LauncherModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsCard(stringResource(R.string.dock)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.recent_app_dots), Modifier.weight(1f, fill = false)); BetaTag() }
            IosSwitch(state.dockRecentDots, { on ->
                model.setDockRecentDots(on)
                if (on && !Suggestions.hasUsageAccess(context)) runCatching {
                    context.startActivity(Suggestions.usageAccessIntent(context).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }, Modifier.testTag("dock-recent-dots-switch"))
        }
        CardNote(stringResource(R.string.a_small_dot_beside_dock_apps_you_ve_used))
    }
}

/** A live sample of the badge settings on a plain icon, so each change shows right away. */
/** iOS-style "Save Backup" alert with a name field; the file lands in Download/Folio. */
@Composable private fun BackupNameAlert(onCancel: () -> Unit, onSave: (String) -> Unit) {
    val backupContext = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(backupContext.getString(R.string.default_backup_name, java.time.LocalDate.now().toString())) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.save_backup_2)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.saved_to_1, FolioFiles.displayPath), fontSize = FolioType.FOOTNOTE.sp)
                androidx.compose.foundation.text.BasicTextField(name, { name = it.take(60) },
                    Modifier.padding(top = FolioSpace.MEDIUM.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .1f)).padding(horizontal = FolioSpace.COMPACT.dp, vertical = FolioSpace.SMALL.dp)
                        .focusRequester(focus).testTag("backup-name"),
                    singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.SUBHEAD.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.White),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onSave(name) }))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
}

/** A content description from strings.xml; semantics blocks can't read resources themselves. */
@Composable internal fun Modifier.description(@androidx.annotation.StringRes id: Int): Modifier =
    stringResource(id).let { text -> semantics { contentDescription = text } }

/** Like [description], replacing whatever the children would have said. */
@Composable internal fun Modifier.clearedDescription(@androidx.annotation.StringRes id: Int): Modifier =
    stringResource(id).let { text -> clearAndSetSemantics { contentDescription = text } }

/** Your own search address: a field, and whether it works, so a typo is caught before it is used. */
@Composable private fun CustomSearchField(url: String, onUrl: (String) -> Unit) {
    var text by remember { mutableStateOf(url) }
    val valid = text.isBlank() || customSearchTemplateIsValid(text)
    androidx.compose.foundation.text.BasicTextField(text, { text = it.take(500); onUrl(text) },
        Modifier.padding(vertical = FolioSpace.SNUG.dp).fillMaxWidth().clip(RoundedCornerShape(FolioRadius.CONTROL.dp))
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .1f)).padding(horizontal = FolioSpace.COMPACT.dp, vertical = FolioSpace.MEDIUM.dp)
            .testTag("search-custom-url"),
        singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = androidx.compose.ui.graphics.Color.White, fontSize = FolioType.SUBHEAD.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.White),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        decorationBox = { inner -> Box { if (text.isEmpty()) Text("https://example.com/search?q=%s", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .35f), fontSize = FolioType.SUBHEAD.sp); inner() } })
    CardNote(stringResource(when { text.isBlank() -> R.string.custom_search_blank; valid -> R.string.custom_search_note; else -> R.string.custom_search_invalid }))
}
