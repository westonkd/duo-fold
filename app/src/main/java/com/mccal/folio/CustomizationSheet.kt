package com.mccal.folio

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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

/** Where Settings was when it closed, so opening it again picks up there. In memory only, so a restart opens at the top. */
internal object SettingsMemory {
    var bodyScroll = 0
    var sidebarScroll = 0
    /** How far down the list was when Settings last left it, so Back lands in the same place. */
    var overviewScroll = 0
}

/**
 * The scroll of the page Settings shows. Each page gets a new one: the list starts where it was left when Back returns
 * to it, as in iPhone Settings, and any other page at its top. A new state is at its offset before the page is laid out;
 * scrolling the old one back could only have gone as far down as the page it was showing reached.
 */
internal class SettingsScroll(private var page: CustomizationPage, offset: Int) {
    var state = ScrollState(offset)
        private set
    private var parentScroll = 0

    /** The scroll for [next], as it opens. Going back up to the page a child page was opened from lands where it was left. */
    fun show(next: CustomizationPage): ScrollState {
        if (next == page) return state
        if (page == CustomizationPage.OVERVIEW) SettingsMemory.overviewScroll = state.value
        if (next.parent == page) parentScroll = state.value
        state = ScrollState(when {
            next == CustomizationPage.OVERVIEW -> SettingsMemory.overviewScroll
            page.parent == next -> parentScroll
            else -> 0
        })
        page = next
        return state
    }
}

/**
 * Settings' pages: the list, the five pages it opens, and the pages General opens in turn. Back goes to [parent].
 */
internal enum class CustomizationPage(@get:androidx.annotation.StringRes val title: Int) {
    OVERVIEW(R.string.folio), HOME(R.string.home), SEARCH(R.string.search), GESTURES(R.string.gestures), FOLD(R.string.fold), GENERAL(R.string.general),
    DIAGNOSTICS(R.string.diagnostics), CREDITS(R.string.credits_and_licenses);

    val parent: CustomizationPage? get() = when (this) {
        OVERVIEW -> null
        DIAGNOSTICS, CREDITS -> GENERAL
        else -> OVERVIEW
    }

    val topLevel: CustomizationPage get() = if (parent == null || parent == OVERVIEW) this else parent!!.topLevel
}

/**
 * What Back says in Settings' nav bar: the name of the page it goes to, as iOS does. Null where there's no Back: on
 * the list itself, and on the list's own pages in two columns, where the list is already on screen. [columns] is 1 on
 * the phone and while the list slides over the page.
 */
@androidx.annotation.StringRes
internal fun settingsBackLabel(page: CustomizationPage, columns: Int): Int? {
    val parent = page.parent ?: return null
    return if (parent == CustomizationPage.OVERVIEW && columns >= 2) null else parent.title
}

/** Back goes up a page, as the nav bar's Back does. On the list the host closes the sheet. */
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
    var namingBackup by remember { mutableStateOf(false) }
    // Reopening Settings lands where it was, scrolled the same. Back lands where the list was left; any other page
    // opens at its top.
    val scroll = remember { SettingsScroll(page, SettingsMemory.bodyScroll) }
    val bodyScroll = remember(page) { scroll.show(page) }
    val sidebarScroll = rememberScrollState(SettingsMemory.sidebarScroll)
    DisposableEffect(Unit) { onDispose { SettingsMemory.bodyScroll = scroll.state.value; SettingsMemory.sidebarScroll = sidebarScroll.value } }
    val onBack = { onPage(page.parent ?: CustomizationPage.OVERVIEW) }
    SettingsPageBack(page, onBack)

    // The settings list. On the phone it's the first page; in the split view it's the sidebar, with the open page highlighted.
    val overviewRows: @Composable ColumnScope.(selected: CustomizationPage?, sidebar: Boolean) -> Unit = { selected, sidebar ->
                    // Only the sidebar has an open page to mark; on the phone the rows are just places to go.
                    val pick = { row: CustomizationPage -> if (sidebar) selected == row else null }
                    SheetGroup {
                        TweakRow(Icons.Rounded.GridView, FolioColors.Value.Blue, stringResource(R.string.home), "customization-home",
                            if (backgrounds.previewPending) stringResource(R.string.photo_ready) else null, selected = pick(CustomizationPage.HOME), chevron = !sidebar) { onPage(CustomizationPage.HOME) }
                        SheetDivider()
                        TweakRow(Icons.Rounded.Search, FolioColors.Value.Gray, stringResource(R.string.search), "customization-search", selected = pick(CustomizationPage.SEARCH), chevron = !sidebar) { onPage(CustomizationPage.SEARCH) }
                        SheetDivider()
                        TweakRow(Icons.Rounded.Gesture, FolioColors.Value.Teal, stringResource(R.string.gestures), "customization-gestures", selected = pick(CustomizationPage.GESTURES), chevron = !sidebar) { onPage(CustomizationPage.GESTURES) }
                        SheetDivider()
                        TweakRow(Icons.Rounded.Devices, FolioColors.Value.Pink, stringResource(R.string.fold), "customization-fold", selected = pick(CustomizationPage.FOLD), chevron = !sidebar) { onPage(CustomizationPage.FOLD) }
                        SheetDivider()
                        TweakRow(Icons.Rounded.Settings, FolioColors.Value.Gray, stringResource(R.string.general), "customization-general", selected = pick(CustomizationPage.GENERAL), chevron = !sidebar) { onPage(CustomizationPage.GENERAL) }
                    }
    }
    // Home-app actions, above the list.
    val overviewActions: @Composable ColumnScope.() -> Unit = {
                    if (!isDefaultHome || state.canUndoEdit) SheetGroup {
                        if (!isDefaultHome) IosActionRow(stringResource(R.string.set_as_home_app), "default-home-settings", onClick = onMakeDefault)
                        if (!isDefaultHome && state.canUndoEdit) SheetDivider()
                        if (state.canUndoEdit) IosActionRow(stringResource(R.string.undo_last_layout_change), onClick = { model.undoEdit(); onClose() })
                    }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    // iPad Settings / One UI on the unfolded screen: sidebar and page side by side, in either orientation.
    // Regular size class (both dimensions roomy), not a device check: the inner screen in either orientation.
    val fullWidth = maxWidth
    // The keyboard covers Settings; it doesn't make the window smaller (ADP-1, ADP-18). Measured out here, so that
    // tapping a text field can't turn the unfolded screen into a phone-sized one for as long as the keyboard is up (#117).
    val keyboardDp = keyboardDpOverSheet()
    val split = settingsSplits(maxWidth.value, maxHeight.value, androidx.compose.ui.platform.LocalConfiguration.current.classScale, keyboardDp)
    // In the split view the list is always on screen, so the page beside it is Home until another is picked.
    val shown = if (split && page == CustomizationPage.OVERVIEW) CustomizationPage.HOME else page
    val pageContent: @Composable ColumnScope.(CustomizationPage) -> Unit = { target ->
            when (target) {
                CustomizationPage.OVERVIEW -> {
                    overviewActions()
                    overviewRows(null, false)
                }
                CustomizationPage.HOME -> {
                    val wallpaperContext = androidx.compose.ui.platform.LocalContext.current
                    // The preview shows the candidate when there is one, under Home's real chrome, which is what AOSP's
                    // small preview and iOS's Lock Screen preview both do. Art and photos apply the same way: a tap
                    // stages, Set writes.
                    // "Once" means once: leaving the page ends the offer as well as taking it.
                    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { backgrounds.offerTaken() } }
                    val candidate = backgrounds.pendingArtBitmap ?: backgrounds.previewBitmap
                    val staging = backgrounds.pendingArt != null || backgrounds.previewPending
                    if (!state.systemWallpaper) {
                        MiniHomePreview(candidate, state, if (split) 240.dp else 228.dp)
                        if (staging) SheetGroup {
                            IosActionRow(stringResource(R.string.set), "background-set",
                                enabled = candidate != null || backgrounds.pendingArt != null,
                                onClick = { if (backgrounds.previewPending) backgrounds.applyPreview() else backgrounds.setPendingArt() })
                            SheetDivider()
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
                    SettingsSection(stringResource(R.string.wallpaper)) {
                        IosSegmented(listOf(true to stringResource(R.string.android_wallpaper), false to stringResource(R.string.folio_background)),
                            state.systemWallpaper, { system -> model.setSystemWallpaper(system); wallpaperContext.asActivity()?.applyWallpaperWindow(system) },
                            tag = "background-choice")
                    }
                    if (state.systemWallpaper) SettingsCard(null) {
                        CardAction(stringResource(R.string.change_android_wallpaper), onClick = {
                            runCatching { wallpaperContext.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER), wallpaperContext.getString(R.string.change_wallpaper))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }, modifier = Modifier.testTag("background-change-system"))
                        // Only Android's wallpaper drifts now, and the system moves it, so the switch lives with the
                        // wallpaper it moves. Folio's own picture has no spare width to slide (see DuneWallpaper).
                        SettingsSwitch(stringResource(R.string.wallpaper_moves_with_pages), state.wallpaperMotion, model::setWallpaperMotion, "wallpaper-motion-switch")
                        CardNote(stringResource(R.string.uses_the_same_wallpaper_as_your_phone_s))
                    } else SettingsFootnote(stringResource(R.string.wallpaper_group_note))
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
                    HomeLayoutSettings(state, wide, { wide = it }, model, homePage, onEditPins, onWidget, onAddWidget, onRemoveWidget)
                }
                CustomizationPage.SEARCH -> {
                    SettingsCard(null) {
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
                    SettingsCard(stringResource(R.string.app_library)) {
                        SettingsSwitch(stringResource(R.string.group_apps_into_categories), state.libraryCategories, model::setLibraryCategories, "library-categories-switch")
                        if (state.profiles.any { it.isWork }) SettingsSwitch(stringResource(R.string.work_apps), state.libraryWork, model::setLibraryWork, "library-work-switch")
                        HiddenAppsRow(state, model)
                        SettingsSwitch(stringResource(R.string.add_new_apps_to_home_screen), state.addNewAppsToHome, model::setAddNewAppsToHome, "add-new-apps-switch")
                        CardNote(stringResource(R.string.off_new_downloads_go_to_the_app_library))
                    }
                    SettingsCard(stringResource(R.string.search_button)) {
                        SettingsSwitch(stringResource(R.string.search_button_opens_the_google_app), state.googleSearch, model::setGoogleSearch, "google-search-switch")
                        CardNote(stringResource(R.string.when_off_the_search_button_opens_spotlig))
                    }
                    SettingsCard(stringResource(R.string.suggestions)) {
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
                }
                CustomizationPage.GESTURES -> {
                    SettingsCard(null) {
                        IosMenuRow(stringResource(R.string.swipe_down_on_home), listOf("SPOTLIGHT" to stringResource(R.string.spotlight), "NOTIFICATIONS" to stringResource(R.string.notifications_title), "OFF" to stringResource(R.string.nothing)),
                            state.swipeDownHome, model::setSwipeDownHome, tag = "swipe-down-home")
                        SettingsSwitch(stringResource(R.string.drag_page_dots_to_flip_pages), state.pageScrub, model::setPageScrub, "page-scrub-switch")
                        SettingsSwitch(stringResource(R.string.haptic_feedback), state.haptics, model::setHaptics, "haptics-switch")
                        CardNote(stringResource(R.string.pull_down_from_the_top_left_for_notifica))
                    }
                    SettingsCard(stringResource(R.string.actions)) {
                        FolioTrigger.entries.forEach { trigger ->
                            val current = FolioAction.entries.firstOrNull { it.name == state.triggerActions[trigger.name] } ?: FolioAction.NONE
                            IosMenuRow(stringResource(trigger.label), FolioAction.entries.map { it to stringResource(it.label) }, current, { model.setTriggerAction(trigger, it) }, tag = "trigger-${trigger.name.lowercase()}")
                        }
                        CardNote(stringResource(R.string.pick_what_gestures_and_events_do_activat))
                    }
                    SettingsCard(stringResource(R.string.accessibility)) {
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
                        val buttonContext = androidx.compose.ui.platform.LocalContext.current
                        if (state.buttonBar) CardAction(stringResource(R.string.put_the_buttons_back_at_the_bottom), onClick = { ButtonBarPosition.reset(buttonContext) })
                        // The footer says what the switch does, so it shows while the switch is off too.
                        CardNote(stringResource(R.string.big_back_home_and_recents_buttons_float))
                        if (state.buttonBar) {
                            CardNote(stringResource(R.string.long_press_and_drag_the_bar_to_move_it_u))
                            if (!gestureNavigation(buttonContext)) CardNote(stringResource(R.string.android_s_three_buttons_are_on_so_you_ll))
                        }
                    }
                    SettingsCard(null) {
                        SettingsSwitch(stringResource(R.string.dock_handle_on_the_rail_edge), state.dockEverywhere, { on ->
                            model.setDockEverywhere(on); if (on && !SystemShadeAccessibilityService.isConnected()) onShadeSetup()
                        }, "dock-everywhere-switch")
                        CardNote(stringResource(R.string.uses_folios_accessibility_service_the_sa))
                    }
                }
                CustomizationPage.FOLD -> {
                    SettingsCard(stringResource(R.string.fold_animation)) {
                        SettingsSwitch(stringResource(R.string.page_landing), state.foldEffect, model::setFoldEffect, "duet-switch")
                        CardNote(stringResource(R.string.page_landing_detail))
                    }
                    SettingsCard(stringResource(R.string.closing_from_home)) {
                        SettingsSwitch(stringResource(R.string.stay_awake_on_the_cover_screen), state.stayAwakeOnFold, model::setStayAwakeOnFold, "fold-awake-switch")
                        CardNote(stringResource(R.string.samsung_locks_the_phone_when_you_fold_on))
                    }
                }
                CustomizationPage.GENERAL -> {
                    val generalContext = androidx.compose.ui.platform.LocalContext.current
                    SheetGroup {
                        TweakRow(Icons.Rounded.NewReleases, FolioColors.Value.Green, stringResource(R.string.what_s_new), "customization-whats-new",
                            "v" + WhatsNew.currentVersion(generalContext)) { onClose(); onShowWhatsNew() }
                        if (DevBuild.isDevApp(generalContext) && DevBuild.load(generalContext) != null) {
                            SheetDivider()
                            TweakRow(Icons.Rounded.Build, FolioColors.Value.Orange, DevBuild.THIS_BUILD_LABEL, "customization-dev-build",
                                DevBuild.load(generalContext)?.sha) { onClose(); DevBuild.reopen.intValue++ }
                        }
                    }
                    if (namingBackup) BackupNameAlert(onCancel = { namingBackup = false }) { name -> namingBackup = false; onSaveLayoutToFolder(name) }
                    SettingsSection(stringResource(R.string.backup)) {
                        SheetGroup {
                            IosActionRow(stringResource(R.string.save_backup), "layout-save-folder", onClick = { namingBackup = true })
                            SheetDivider()
                            IosActionRow(stringResource(R.string.save_backup_to_files), "layout-export", onClick = onExportLayout)
                            SheetDivider()
                            IosActionRow(stringResource(R.string.restore_from_backup), "layout-import", onClick = onImportLayout)
                        }
                        SettingsFootnote(stringResource(R.string.save_the_current_home_layout_folders_wid) + stringResource(R.string.restore_shows_a_review_before_changing_h))
                    }
                    LayoutHistoryCard(state, model, onClose)
                    PermissionsCard(isDefaultHome, onMakeDefault, onShadeSetup)
                    SettingsSection(stringResource(R.string.about)) {
                        SheetGroup {
                            NavRow(stringResource(R.string.diagnostics), "customization-diagnostics",
                                value = if (SafeMode.active) stringResource(R.string.safe_mode) else null) { onPage(CustomizationPage.DIAGNOSTICS) }
                            SheetDivider()
                            NavRow(stringResource(R.string.credits_and_licenses), "customization-credits") { onPage(CustomizationPage.CREDITS) }
                            SheetDivider()
                            NavRow(stringResource(R.string.privacy_policy), "privacy-policy", external = true) {
                                runCatching { generalContext.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_POLICY_URL))
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }
                        }
                    }
                }
                CustomizationPage.DIAGNOSTICS -> {
                    CrashReportsCard()
                    PerformanceCard()
                    RecentActivityCard()
                    SettingsCard(stringResource(R.string.safe_mode)) {
                        CardNote(if (SafeMode.active) stringResource(R.string.folio_is_running_in_safe_mode_optional_f)
                            else stringResource(R.string.if_folio_closes_unexpectedly_twice_right))
                    }
                }
                CustomizationPage.CREDITS -> {
                    CreditsCard()
                    LicensesCards()
                }
            }
    }
    if (!split) Column(Modifier.fillMaxSize().padding(horizontal = FolioSpace.LARGE.dp)) {
        SettingsNavBar(settingsBackLabel(page, columns = 1)?.let { stringResource(it) }, onBack, onClose)
        val pushReduced = LocalReduceMotion.current
        androidx.compose.animation.AnimatedContent(page, Modifier.weight(1f), label = "settings page",
            transitionSpec = { settingsPush(targetState.ordinal > initialState.ordinal, pushReduced) }) { target ->
            val scroll = if (target == page) bodyScroll else remember { androidx.compose.foundation.ScrollState(bodyScroll.value) }
            Column(Modifier.fillMaxSize().background(FolioColors.LightBackground)) {
                SettingsLargeTitle(stringResource(target.title))
                Column(Modifier.weight(1f).edgeFade(scroll).verticalScroll(scroll).padding(bottom = FolioSpace.XL.dp),
                    verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) { pageContent(target) }
            }
        }
    } else {
        // Half folded with the fold running down the screen, the divider goes on the fold, so no row sits on the
        // crease (iPhone Duo: controls move away from the fold). Flat, it's a share of the width.
        val hinge = LocalHinge.current?.takeIf { it.active && it.vertical }
        val density = LocalDensity.current
        val onFold = hinge?.let { with(density) { it.startPx.toDp() } }?.takeIf { it >= 280.dp && it <= fullWidth * .7f }
        val columns = settingsColumns(maxWidth.value, maxHeight.value, androidx.compose.ui.platform.LocalConfiguration.current.classScale, keyboardDp)
        val tiled = columns >= 2
        // Tiled it shares the width; as an overlay it can be a little wider so rows don't wrap.
        val sidebarWidth = onFold
            ?: if (tiled) (fullWidth * .4f).coerceIn(280.dp, 380.dp) else minOf(360.dp, fullWidth * .6f)
        var sidebarOpen by rememberSaveable { mutableStateOf(tiled || page == CustomizationPage.OVERVIEW) }
        var shownPage by remember { mutableStateOf(page) }
        // Opening a page slides the list away; coming back to the top brings it back, since the list is all that page has.
        SideEffect { if (page != shownPage) { shownPage = page; if (!tiled) sidebarOpen = page == CustomizationPage.OVERVIEW } }
        val sidebar: @Composable () -> Unit = {
            Column(Modifier.width(sidebarWidth).fillMaxHeight().edgeFade(sidebarScroll).verticalScroll(sidebarScroll)
                .padding(horizontal = FolioSpace.LARGE.dp).padding(top = 44.dp, bottom = FolioSpace.XL.dp).testTag("settings-sidebar"), verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) {
                SettingsLargeTitle(stringResource(R.string.folio))
                overviewActions()
                overviewRows(shown.topLevel, true)
            }
        }
        val divider: @Composable () -> Unit = {
            Box(Modifier.fillMaxHeight().width(.5.dp).background(FolioColors.LightSeparator))
        }
        Row(Modifier.fillMaxSize()) {
            if (tiled) {
                sidebar()
                divider()
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = FolioSpace.XL.dp)) {
                // With the list beside it there's nothing for Back to reveal, so the bar keeps only Done. Otherwise every
                // page gets a way back, named for where it goes, not only the sidebar button, which doesn't read as one.
                SettingsNavBar(settingsBackLabel(page, columns)?.let { stringResource(it) }, onBack, onClose,
                    leading = if (tiled) null else ({ SidebarButton { sidebarOpen = !sidebarOpen } }))
                val pushReduced = LocalReduceMotion.current || tiled
                androidx.compose.animation.AnimatedContent(shown, Modifier.weight(1f), label = "settings page",
                    transitionSpec = { settingsPush(targetState.ordinal > initialState.ordinal, pushReduced) }) { target ->
                    val scroll = if (target == shown) bodyScroll else remember { androidx.compose.foundation.ScrollState(bodyScroll.value) }
                    Column(Modifier.fillMaxSize().background(FolioColors.LightBackground)) {
                        SettingsLargeTitle(stringResource(target.title))
                        Column(Modifier.weight(1f).edgeFade(scroll).verticalScroll(scroll).padding(bottom = FolioSpace.XL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FolioSpace.COMPACT.dp)) {
                                pageContent(target)
                            }
                        }
                    }
                }
            }
        }
        if (!tiled) {
            val reduceMotion = LocalReduceMotion.current
            androidx.compose.animation.AnimatedVisibility(sidebarOpen, enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(FolioMotion.FADE_MS)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(FolioMotion.FADE_MS))) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .4f))
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { sidebarOpen = false })
            }
            androidx.compose.animation.AnimatedVisibility(sidebarOpen,
                enter = if (reduceMotion) androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(FolioMotion.FADE_MS))
                    else androidx.compose.animation.slideInHorizontally(FolioMotion.spring(FolioMotion.Sheet, androidx.compose.ui.unit.IntOffset.VisibilityThreshold)) { -it },
                exit = if (reduceMotion) androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(FolioMotion.FADE_MS))
                    else androidx.compose.animation.slideOutHorizontally(FolioMotion.spring(FolioMotion.Firm, androidx.compose.ui.unit.IntOffset.VisibilityThreshold)) { -it }) {
                Box(Modifier.fillMaxHeight().background(FolioColors.LightBackground)) { sidebar() }
            }
        }
    }
    }
}

/** The iPad/iPhone Duo sidebar toggle: shows or hides the settings list over the page. */
@Composable private fun SidebarButton(onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.show_or_hide_the_settings_list), onClick = onClick)
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

@Composable private fun SettingsLargeTitle(title: String) = Text(title, color = FolioColors.LightLabel, fontSize = FolioType.LARGE_TITLE.sp,
    fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = FolioSpace.TINY.dp, bottom = FolioSpace.SMALL.dp))

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
        Box(Modifier.size(30.dp).clip(IconContinuousShape).background(androidx.compose.ui.graphics.Color(color)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        // A long title wraps to two lines rather than being cut off — iOS does the same in a narrow window or at a
        // large text size — and the value drops under it when there isn't room beside it.
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val tight = config.screenWidthDp < 360 || config.fontScale >= 1.3f
        val valueColor = if (open) androidx.compose.ui.graphics.Color.White.copy(alpha = .85f) else FolioColors.LightSecondaryLabel
        Column(Modifier.weight(1f)) {
            Text(
                title, color = if (open) androidx.compose.ui.graphics.Color.White else FolioColors.LightLabel, fontSize = FolioType.BODY.sp,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (tight) value?.let { Text(it, color = valueColor, fontSize = FolioType.SUBHEAD.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
        }
        if (!tight) value?.let {
            Text(it, color = valueColor, fontSize = FolioType.BODY.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = FolioSpace.SMALL.dp))
        }
        if (chevron) Icon(Icons.Rounded.ChevronRight, null, tint = FolioColors.LightTertiaryLabel, modifier = Modifier.mirroredForRtl())
    }
}

/** Every permission Folio can use, whether it's allowed, and which features rely on it (shared ownership). */
@Composable private fun PermissionsCard(isDefaultHome: Boolean, onMakeDefault: () -> Unit, onShadeSetup: () -> Unit) {
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
            NotificationListener.hasAccess(context)) { PermissionDisclosure.notificationAccess(context) },
        Perm(context.getString(R.string.gestures_service_accessibility), context.getString(R.string.pull_downs_dock_and_big_buttons_in_other),
            SystemShadeAccessibilityService.isConnected(), onShadeSetup),
        Perm(context.getString(R.string.do_not_disturb_access), context.getString(R.string.actions),
            context.getSystemService(android.app.NotificationManager::class.java).isNotificationPolicyAccessGranted) {
            open(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
        Perm(context.getString(R.string.usage_access_optional), context.getString(R.string.better_suggestions_counts_apps_you_open), Suggestions.hasUsageAccess(context)) {
            PermissionDisclosure.usageAccess(context) },
        Perm(context.getString(R.string.calendar_optional), context.getString(R.string.up_next_widget), UpNext.hasCalendar(context)) { open(appSettings) },
        Perm(context.getString(R.string.contacts), context.getString(R.string.spotlight_contact_search), context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) { open(appSettings) },
    ) }
    SettingsSection(stringResource(R.string.privacy_permissions)) {
        SheetGroup {
            perms.forEachIndexed { index, perm ->
                if (index > 0) SheetDivider()
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = perm.action)
                    .padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.COMPACT.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = FolioSpace.SMALL.dp)) {
                        Text(perm.name, color = FolioColors.LightLabel, fontSize = FolioType.BODY.sp)
                        Text(stringResource(R.string.used_by_1, perm.usedBy), color = FolioColors.LightSecondaryLabel, fontSize = FolioType.FOOTNOTE.sp)
                    }
                    Text(stringResource(if (perm.allowed) R.string.allowed else R.string.state_off),
                        color = if (perm.allowed) FolioColors.GreenText else FolioColors.LightSecondaryLabel, fontSize = FolioType.BODY.sp)
                    Icon(Icons.Rounded.ChevronRight, null, tint = FolioColors.LightTertiaryLabel, modifier = Modifier.mirroredForRtl())
                }
            }
        }
        SettingsFootnote(stringResource(R.string.everything_stays_on_your_phone_folio_has))
        // Some banking apps refuse to run while any accessibility service is on, Folio's included (reported on
        // r/GalaxyFold, 18 Sep 2026). Nothing Folio can do from its side, so say so before someone is caught out.
        SettingsFootnote(stringResource(R.string.banking_apps_note))
    }
}

/** General › Diagnostics: Folio's own recent activity, newest first, with the failures it carried on from. Kept only on the phone. */
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
        CardText(if (reports.isEmpty()) stringResource(R.string.no_problems_recorded) else pluralStringResource(R.plurals.reports_saved, reports.size, reports.size))
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

/** What Folio is built from: the code it includes and the licences that ask to be named. */
@Composable private fun CreditsCard() {
    val credits = listOf(
        stringResource(R.string.folio) to stringResource(R.string.credit_folio_mccal),
        stringResource(R.string.duolauncher) to "jakesgoodapps (github.com/jakesgoodapps/DuoLauncher) · MIT · Folio’s starting codebase", // english-only
        "Duo Fold Live" to stringResource(R.string.credit_duo_fold_live), // english-only
        "iphone-duo" to "chuspeeism · MIT · ideas for the fold animation, by way of Duo Fold Live", // english-only
        "hingewave" to stringResource(R.string.credit_hingewave), // english-only
    )
    SettingsCard(stringResource(R.string.credits)) {
        credits.forEach { (name, detail) ->
            Column(Modifier.fillMaxWidth().padding(vertical = FolioSpace.SNUG.dp)) {
                Text(name)
                CardNote(detail)
            }
        }
    }
}

private val LICENSE_FILES = listOf(
    R.string.license_third_party to "THIRD-PARTY-NOTICES.txt",
    R.string.license_mit to "MIT-DuoLauncher.txt",
    R.string.license_apache to "Apache-2.0.txt",
)

@Composable private fun LicensesCards() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val texts = remember { LICENSE_FILES.map { (title, file) ->
        title to runCatching { context.assets.open("licenses/$file").bufferedReader().use { it.readText().trim() } }.getOrDefault("")
    } }
    texts.forEach { (title, text) ->
        SettingsSection(stringResource(title)) {
            SheetGroup(Modifier.testTag("license-$title")) {
                Text(text, color = FolioColors.LightSecondaryLabel, fontSize = FolioType.FOOTNOTE.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.MEDIUM.dp))
            }
        }
    }
}

/** The Home page the Settings previews draw: set from the page Home is on, so a widget placed on page 2 shows up. */
internal val LocalPreviewPage = androidx.compose.runtime.compositionLocalOf { 0 }

/** The phone's own status for the Settings previews, which already show the real time; the sample is for previews with no phone behind them. */
internal val LocalDeviceStatus = androidx.compose.runtime.compositionLocalOf { DeviceStatus(battery = 80, wifiConnected = true, wifiLevel = 4, cellularLevel = 4) }

/**
 * Live preview of Home on Folio's own background, built from real data only: your Home and dock apps (with the
 * current icon pack, badges and live icons), your text, glass and dimming settings, and the background, or the one
 * being tried. It draws the page Home is on (LocalPreviewPage) in the cover's layout.
 */
@Composable internal fun MiniHomePreview(stagedBitmap: android.graphics.Bitmap?, state: LauncherState, previewHeight: androidx.compose.ui.unit.Dp) {
    val page = LocalPreviewPage.current
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
    val geometry = homeGeometry(refW, refH, state.coverDock, state.labels, statusHeight = if (state.verticalStatus) 180f else 0f, labelHeight = 20f,
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(corner + bezel)).background(FolioColors.PreviewBezel)
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = .2f), RoundedCornerShape(corner + bezel)).padding(bezel)) {
        Box(Modifier.height(previewHeight).width(previewHeight * (refW / refH))
            .clip(RoundedCornerShape(corner))
            .testTag("customization-home-preview").clearedDescription(R.string.preview_of_home), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(refW.dp, refH.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                DuneWallpaper()
                bitmap?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
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
                                else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(FolioRadius.WIDGET.dp)).background(glass.copy(alpha = LIQUID_FROST)), contentAlignment = Alignment.Center) {
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
                    if (state.verticalStatus) StatusRail(LocalDeviceStatus.current,
                        Modifier.align(railAlign).then(railEdge).offset(y = geometry.contentTop.dp).width(DOCK_WIDTH.dp),
                        iconSize = dockIconSize(iconSize).dp)
                    val barShape = RoundedCornerShape(dockCornerRadius(geometry.dockBarHeight, dockIconSize(iconSize)).dp)
                    val railShape = RoundedCornerShape(dockCornerRadius(DOCK_WIDTH, dockIconSize(iconSize)).dp)
                    // A bottom dock bar: beside the status Side Bar, or the whole width when there is none (Full-Width Home).
                    if (geometry.horizontalDock) Box(Modifier.align(if (!geometry.dockBesideRail) Alignment.BottomCenter else if (left) Alignment.BottomEnd else Alignment.BottomStart)
                        .width((if (geometry.dockBesideRail) refW - DOCK_WIDTH - 28f else refW).dp).padding(bottom = 58.dp), contentAlignment = Alignment.Center) { Row(Modifier
                        .height(geometry.dockBarHeight.dp).background(glass.copy(alpha = LIQUID_FROST), barShape)
                        .border(1.dp, GlassOutlineColor, barShape).padding(horizontal = FolioSpace.SMALL.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.dock.forEach { id ->
                            Box(Modifier.width(geometry.dockPitch.dp), contentAlignment = Alignment.Center) {
                                id?.let(apps::get)?.let { AppIcon(it, null, Modifier.size(dockIconSize(iconSize).dp), badge = true) }
                            }
                        }
                    } }
                    else Column(Modifier.align(railAlign).then(railEdge).offset(y = geometry.dockTop.dp).width(DOCK_WIDTH.dp)
                        .height(geometry.dockHeight.dp).background(glass.copy(alpha = LIQUID_FROST), railShape)
                        .border(1.dp, GlassOutlineColor, railShape).padding(vertical = FolioSpace.SMALL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        state.dock.forEach { id ->
                            Box(Modifier.fillMaxWidth().height(geometry.dockRowHeight.dp), contentAlignment = Alignment.Center) {
                                id?.let(apps::get)?.let { AppIcon(it, null, Modifier.size(dockIconSize(iconSize).dp), badge = true) }
                            }
                        }
                    }
                    if (state.searchPill) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = FolioSpace.COMFY.dp)
                        .then(if (left) Modifier.padding(start = (DOCK_WIDTH + 24f).dp) else Modifier.padding(end = (DOCK_WIDTH + 24f).dp))) {
                        HomeSearchPill {}
                    }
                }
            }
        }
        }
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
    val shape = IconContinuousShape
    val tint = LocalFolderColors.current[folder.id]?.let { androidx.compose.ui.graphics.Color(it) }
    Box(Modifier.size(size.dp).clip(shape).liquidGlass(shape, if (tint != null) FOLDER_TILE_TINTED_FROST else FOLDER_TILE_FROST, GlassRole.TILE, tint ?: Glass)) {
        folder.appIds.take(4).forEachIndexed { index, id ->
            apps[id]?.let { app ->
                AppIcon(app, null, Modifier.align(when (index) {
                    0 -> Alignment.TopStart; 1 -> Alignment.TopEnd; 2 -> Alignment.BottomStart; else -> Alignment.BottomEnd
                }).padding(5.dp).size((size * .38f).dp))
            }
        }
    }
}

@Composable private fun HomeLayoutSettings(state: LauncherState, wide: Boolean, onWide: (Boolean) -> Unit,
    model: LauncherModel, homePage: Int, onEditPins: () -> Unit, onWidget: (Int) -> Unit,
    onAddWidget: (Int) -> Unit, onRemoveWidget: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val screen = if (wide) LayoutScreen.INNER else LayoutScreen.COVER
    val placement = state.dockFor(screen)
    SheetGroup {
        IosActionRow(stringResource(R.string.choose_home_apps), onClick = onEditPins)
    }
    // The dock is placed per screen, and Full-Width Home and the unfolded-only page's widgets with it.
    SettingsSection(stringResource(R.string.settings_screen)) {
        IosSegmented(listOf(false to stringResource(R.string.cover), true to stringResource(R.string.inner)), wide, onWide, tag = "layout-screen")
        SettingsFootnote(stringResource(R.string.settings_screen_note))
    }
    SettingsCard(null) {
        // Two settings that already exist, in one tap: a dock along the bottom, and no status Side Bar beside the page.
        val fullWidth = placement == DockPlacement.BOTTOM && !state.verticalStatus
        SettingsSwitch(stringResource(R.string.full_width_home), fullWidth, { on -> model.setFullWidthHome(screen, on) }, "full-width-home-switch")
        CardNote(stringResource(R.string.full_width_home_note))
    }
    SettingsCard(stringResource(R.string.dock)) {
        IosMenuRow(stringResource(R.string.placement), listOf(DockPlacement.AUTOMATIC to stringResource(R.string.automatic), DockPlacement.SIDE to stringResource(R.string.side_rail), DockPlacement.BOTTOM to stringResource(R.string.bottom)),
            placement, { model.setDockPlacement(screen, it) }, tag = "dock-placement")
        CardNote(when (placement) {
            DockPlacement.AUTOMATIC -> if (wide) stringResource(R.string.the_dock_stays_on_the_side_bar_and_moves) else stringResource(R.string.the_dock_stays_on_the_side_bar)
            DockPlacement.SIDE -> stringResource(R.string.the_dock_stays_on_the_side_bar_even_when)
            DockPlacement.BOTTOM -> if (wide) stringResource(R.string.the_dock_sits_along_the_bottom_under_you) else stringResource(R.string.the_dock_sits_along_the_bottom_in_landsc)
        })
    }
    SettingsCard(null) {
        // Recent-app dots (Beta) need Usage Access, asked for right here when they're turned on.
        Row(Modifier.fillMaxWidth().heightIn(min = FolioRow.ACTION.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.recent_app_dots), Modifier.weight(1f, fill = false), fontSize = FolioType.BODY.sp); BetaTag() }
            IosSwitch(state.dockRecentDots, { on ->
                model.setDockRecentDots(on)
                if (on && !Suggestions.hasUsageAccess(context)) PermissionDisclosure.usageAccess(context)
            }, Modifier.testTag("dock-recent-dots-switch"))
        }
        CardNote(stringResource(R.string.a_small_dot_beside_dock_apps_you_ve_used))
    }
    SettingsCard(null) {
        SettingsSwitch(stringResource(R.string.show_app_names), state.labels, model::setLabels, "label-switch")
        SettingsSwitch(stringResource(R.string.left_handed_layout_rail_on_the_left), state.leftHanded, model::setLeftHanded, "left-handed-switch")
        SettingsSwitch(stringResource(R.string.show_status_in_the_rail), state.verticalStatus, model::setVerticalStatus, "status-switch")
    }
    SettingsCard(stringResource(R.string.app_icons)) {
        // Reading the installed icon packs asks Android about every pack, so it happens off the main thread.
        val packs by androidx.compose.runtime.produceState(emptyList<IconPacks.Pack>(), context) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { IconPacks.installed(context) }
        }
        IosMenuRow(stringResource(R.string.icon_pack), listOf<Pair<String?, String>>(null to stringResource(R.string.app_icons)) + packs.map { it.packageName to it.label },
            state.iconPack, { IconPacks.clear(); model.setIconPack(it) }, tag = "icon-pack")
        SettingsSwitch(stringResource(R.string.settings_live_clock_and_calendar_icons), state.liveIcons, model::setLiveIcons, "live-icons-switch")
        CardAction(stringResource(R.string.refresh_icons), onClick = model::reloadIcons)
        CardNote(stringResource(R.string.icon_packs_and_themes_are_made_by_indepe))
        if (packs.isEmpty()) CardNote(stringResource(R.string.install_any_icon_pack_made_for_nova_styl))
        CardNote(stringResource(R.string.refresh_icons_note))
    }
    SettingsCard(stringResource(R.string.badges)) {
        IosMenuRow(stringResource(R.string.notification_badges), BadgeStyle.entries.map { it to stringResource(it.label) }, state.badgeStyle, model::setBadgeStyle, tag = "badge-style")
        if (state.badgeStyle != BadgeStyle.OFF) {
            SettingsSwitch(stringResource(R.string.clear_badges_when_opened), state.badgesWhenOpened,
                model::setBadgesWhenOpened, "badges-when-opened-switch")
            CardNote(stringResource(R.string.a_badge_goes_away_when_you_open_the_app))
        }
    }
    FolioSettingsCard(state, model)
    SettingsCard(null) {
        SettingsSwitch("Cabinet", state.appPanels, model::setAppPanels, "cabinet-switch") // english-only
        CardNote(stringResource(R.string.tweak_cabinet_detail))
    }
    SettingsCard(null) {
        SettingsSwitch("Harborline", state.dockMagnify, model::setDockMagnify, "harborline-switch") // english-only
        CardNote(stringResource(R.string.tweak_harborline_detail))
    }
    SettingsSection(stringResource(R.string.widgets_page_number, homePage + 1)) { SheetGroup {
        state.widgetPlacements.filter { it.page == homePage || (wide && it.page == -1) }.forEach { placement ->
            val removeLabel = stringResource(if (placement.page == -1) R.string.remove_widget_from_unfolded_only_page else R.string.remove_widget)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = FolioSpace.LARGE.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (placement.page == -1) stringResource(R.string.unfolded_only_page) else stringResource(R.string.widget_size_row, placement.spanX, placement.spanY, placement.row + 1), Modifier.weight(1f),
                    fontSize = FolioType.BODY.sp)
                TextButton(onClick = { onWidget(placement.slot) }) { Text(stringResource(R.string.replace)) }
                IconButton(onClick = { onRemoveWidget(placement.slot) }, modifier = Modifier.semantics { contentDescription = removeLabel }) {
                    Icon(Icons.Rounded.RemoveCircle, null, tint = FolioColors.Red) }
            }
            SheetDivider()
        }
        IosActionRow(stringResource(R.string.add_widget_to_this_page), onClick = { onAddWidget(homePage) })
    } }
}

@Composable internal fun SettingsSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, tag: String? = null, enabled: Boolean = true) {
    // One accessible element for TalkBack ("label, switch, on"); the whole row toggles.
    Row(Modifier.fillMaxWidth().heightIn(min = FolioRow.ACTION.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).padding(end = FolioSpace.MEDIUM.dp, top = FolioSpace.SNUG.dp, bottom = FolioSpace.SNUG.dp), fontSize = FolioType.BODY.sp); IosSwitch(checked, onChecked, Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier), enabled)
    }
}

/** [title] is left out when the page already says it, so a page with one group doesn't repeat its own name. */
@Composable internal fun SettingsCard(title: String?, content: @Composable ColumnScope.() -> Unit) {
    SettingsSection(title) {
        GroupedCard(FolioColors.LightCard, FolioColors.LightSeparator, Modifier.fillMaxWidth(), content)
    }
}

@Composable internal fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FolioSpace.SNUG.dp)) {
        if (title != null) SheetGroupLabel(title)
        content()
    }
}

@Composable internal fun SettingsFootnote(text: String) {
    CardNote(text, Modifier.padding(horizontal = FolioSpace.LARGE.dp))
}

@Composable private fun NavRow(title: String, tag: String, value: String? = null, external: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)
        .padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.COMPACT.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = FolioColors.LightLabel, fontSize = FolioType.BODY.sp)
        value?.let { Text(it, color = FolioColors.LightSecondaryLabel, fontSize = FolioType.BODY.sp, modifier = Modifier.padding(horizontal = FolioSpace.SMALL.dp)) }
        if (external) Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = FolioColors.LightTertiaryLabel, modifier = Modifier.size(20.dp))
        else Icon(Icons.Rounded.ChevronRight, null, tint = FolioColors.LightTertiaryLabel, modifier = Modifier.mirroredForRtl())
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
    Text(stringResource(R.string.beta), color = FolioColors.OrangeText, fontSize = FolioType.CAPTION.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        modifier = Modifier.padding(start = FolioSpace.SMALL.dp).border(1.dp, FolioColors.OrangeText, CircleShape)
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
        Row(Modifier.fillMaxWidth().heightIn(min = FolioRow.ACTION.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
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
                    Modifier.padding(top = FolioSpace.MEDIUM.dp).fillMaxWidth().clip(RoundedCornerShape(FolioRadius.CONTROL.dp))
                        .background(FolioColors.LightFill).padding(horizontal = FolioSpace.COMPACT.dp, vertical = FolioSpace.SMALL.dp)
                        .focusRequester(focus).testTag("backup-name"),
                    singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = FolioColors.LightLabel, fontSize = FolioType.SUBHEAD.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalAccent.current.ink),
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
            .background(FolioColors.LightFill).padding(horizontal = FolioSpace.COMPACT.dp, vertical = FolioSpace.MEDIUM.dp)
            .testTag("search-custom-url"),
        singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = FolioColors.LightLabel, fontSize = FolioType.SUBHEAD.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalAccent.current.ink),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        decorationBox = { inner -> Box { if (text.isEmpty()) Text("https://example.com/search?q=%s", color = FolioColors.LightTertiaryLabel, fontSize = FolioType.SUBHEAD.sp); inner() } })
    CardNote(stringResource(when { text.isBlank() -> R.string.custom_search_blank; valid -> R.string.custom_search_note; else -> R.string.custom_search_invalid }))
}

private fun settingsPush(forward: Boolean, reduced: Boolean): androidx.compose.animation.ContentTransform {
    val fadeIn = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(FolioMotion.FADE_MS))
    val fadeOut = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(FolioMotion.FADE_MS))
    if (reduced) return androidx.compose.animation.ContentTransform(fadeIn, fadeOut)
    val slide = FolioMotion.spring(FolioMotion.Sheet, androidx.compose.ui.unit.IntOffset.VisibilityThreshold)
    val enter = androidx.compose.animation.slideInHorizontally(slide) { if (forward) it else -it / 3 }
    val exit = androidx.compose.animation.slideOutHorizontally(slide) { if (forward) -it / 3 else it } + fadeOut
    return androidx.compose.animation.ContentTransform(enter, exit, targetContentZIndex = if (forward) 1f else -1f)
}
