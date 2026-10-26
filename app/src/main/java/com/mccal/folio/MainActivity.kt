package com.mccal.folio

import androidx.lifecycle.repeatOnLifecycle
import android.app.role.RoleManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val model: LauncherModel by viewModels()
    private lateinit var widgets: WidgetController
    internal lateinit var backups: BackupController
        private set
    internal lateinit var backgrounds: LauncherBackgroundController
        private set
    private val homeRequests = mutableIntStateOf(0)
    /** True when the Home press that made [homeRequests] count up found Folio already in front, with no overlay of its own open. */
    private val homeFromFront = mutableStateOf(false)
    /** Set when Folio stops (an app is in front) and cleared once it is back in front; a Home press while it is false is a press on Home. */
    private var stoppedSinceHome = true
    private val searchRequests = mutableIntStateOf(0)
    /** Opened from Android Settings (Home app gear / "Additional settings in the app"). */
    private val settingsRequests = mutableIntStateOf(0)
    private val defaultHome = mutableStateOf(false)
    private val showWhatsNew = mutableStateOf(false)
    private val whatsNewRequested = mutableStateOf(false)
    private val showDevBuild = mutableStateOf(false)
    private lateinit var status: DeviceStatusMonitor
    private var shadeSetupDialog: android.app.AlertDialog? = null
    private var returningFromShadeSettings = false
    // The gestures dialog asked to show while the build page or What's New was up; it shows when they close.
    private var shadeSetupWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The wallpaper theme must be chosen before the window exists (switching to it starts the screen again).
        if (usesSystemWallpaper(this)) setTheme(R.style.Theme_Duo_Wallpaper)
        super.onCreate(savedInstanceState)
        showWhatsNew.value = savedInstanceState == null && WhatsNew.shouldShow(this)
        Installs.start(this); NewApps.load(this)
        // Folio Dev only: its builds keep one version name, so What's New never shows; this page is keyed to the commit.
        // Not tied to savedInstanceState: after an install the app is killed and Android restores Home from saved state, which
        // is exactly when a new build needs announcing. The commit check keeps it to once per build.
        showDevBuild.value = DevBuild.shouldShow(this)
        returningFromShadeSettings = savedInstanceState?.getBoolean(SHADE_SETTINGS_PENDING) == true
        val restoreShadeDialog = savedInstanceState?.getBoolean(SHADE_DIALOG_VISIBLE) == true
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        widgets = WidgetController(this, model).also { it.restore(savedInstanceState) }
        backups = BackupController(this, model, widgets).also { it.restore() }
        backgrounds = LauncherBackgroundController(this)
        status = DeviceStatusMonitor(this).also { lifecycle.addObserver(it) }
        lifecycle.addObserver(SystemEvents.Observer(this))
        updateDefaultHome()
        if (savedInstanceState == null && intent.getStringExtra("duo_destination") == "search") searchRequests.intValue++
        if (savedInstanceState == null && opensSettings(intent)) settingsRequests.intValue++
        intent.removeExtra("duo_destination")
        setContent {
            val savedState = model.state.collectAsStateWithLifecycle().value
            // Home is fully drawn once its own apps are in; at a start they come ahead of the rest.
            androidx.activity.compose.ReportDrawnWhen { savedState.homeReady }
            val safeMode = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(SafeMode.active) }
            val state = if (safeMode.value) SafeMode.effective(savedState) else savedState
            androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(31_000); SafeMode.markStable(this@MainActivity) }
            // Saveable: a theme or language change recreates the activity, and the answer must not be asked again.
            val safeAcknowledged = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            if (safeMode.value && !safeAcknowledged.value) AlertDialog(onDismissRequest = {},
                title = { androidx.compose.material3.Text(getString(R.string.folio_started_in_safe_mode)) },
                text = { androidx.compose.material3.Text(stringResource(R.string.folio_closed_unexpectedly_twice_so_opt)) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { SafeMode.exit(this@MainActivity); safeMode.value = false }) {
                    androidx.compose.material3.Text(getString(R.string.restart_normally)) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { safeAcknowledged.value = true }) {
                    androidx.compose.material3.Text(getString(R.string.continue_in_safe_mode)) } })
            val deviceStatus = status.state.collectAsStateWithLifecycle().value
            // Folio shows its own status in the rail, so hide Android's status bar on Home (it
            // stays in apps, and a swipe from the very top edge reveals it briefly).
            androidx.compose.runtime.LaunchedEffect(state.verticalStatus) {
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (state.verticalStatus) hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    else show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }
            val overlayOpen = spotlightVisible.value || LauncherSheets.isOpen(this@MainActivity)
            // The trail for bug reports: what was open, and a heartbeat while Home is showing.
            val overlayName = listOfNotNull(getString(R.string.spotlight).takeIf { spotlightVisible.value },
                "sheet".takeIf { LauncherSheets.isOpen(this@MainActivity) })
                .joinToString(" + ").ifEmpty { null }
            androidx.compose.runtime.LaunchedEffect(overlayName) { Diagnostics.event(overlayName?.let { "Open: $it" } ?: getString(R.string.nothing_open_over_home)) }
            val regular = androidx.compose.ui.platform.LocalConfiguration.current.fitsRegularHomeLayout()
            androidx.compose.runtime.LaunchedEffect(regular) { Diagnostics.event(if (regular) getString(R.string.unfolded_layout) else getString(R.string.folded_layout)) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                    while (true) { kotlinx.coroutines.delay(30_000); Diagnostics.checkpoint(this@MainActivity, visible = true) }
                }
            }
            val overlayProgress by rememberSettlingProgress(if (overlayOpen) 1f else 0f,
                FolioMotion.spring(FolioMotion.Settle))
            // A back swipe that closed Spotlight keeps its preview until the close ends; the next one starts clean (MO7).
            val overlayShowing = spotlightVisible.value
            androidx.compose.runtime.LaunchedEffect(overlayShowing) { if (overlayShowing) OverlayBack.swipe.snapTo(0f) }
            val backdropBlurPx = with(androidx.compose.ui.platform.LocalDensity.current) { HOME_BACKDROP_BLUR_DP.dp.toPx() }
            val backdropBlurs = androidx.compose.runtime.remember(backdropBlurPx) { backdropBlurSteps(backdropBlurPx) }
            DuoTheme { val notificationItems = NotificationListener.notifications.collectAsStateWithLifecycle().value
            // Progress ticks for every install session on the phone. Kept as a State that nothing here reads, so a tick
            // reaches only the icons that are installing (see LocalInstallProgress) instead of recomposing all of Home.
            val installProgress = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyMap<String, Float>()) }
            // Only while Folio is on screen (STARTED), as collectAsStateWithLifecycle was: a tick the UI can't draw costs nothing.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    Installs.active.collect { sessions -> installProgress.value = sessions.values.associate { it.packageName to it.progress } }
                }
            }
            val newApps = NewApps.packages.collectAsStateWithLifecycle().value
            val clearedBadges = BadgeClears.cleared.collectAsStateWithLifecycle().value
            // Recent-app dots (Beta): refreshed every minute while Home is showing.
            val recentPackages by androidx.compose.runtime.produceState(emptySet<String>(), state.dockRecentDots) {
                if (!state.dockRecentDots) { value = emptySet(); return@produceState }
                lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                    while (true) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { RecentUse.packages(this@MainActivity) }
                        kotlinx.coroutines.delay(60_000)
                    }
                }
            }
            val iconsAreDark by androidx.compose.runtime.produceState<Boolean?>(null, state.apps) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    iconsMostlyDark(state.apps.filter { LiveIcons.kind(this@MainActivity, it.packageName) == null && it.shortcutId == null }.map { it.icon })
                }
            }
            val postedBadges = androidx.compose.runtime.remember(notificationItems, clearedBadges) { BadgeClears.counts(notificationItems, clearedBadges) }
            // Clear Badges When Opened: the counts already seen are hidden, and a seen count follows its app down when
            // notifications go away elsewhere. Nothing of this runs while the switch is off.
            val seenBadges = if (state.badgesWhenOpened) state.badgesSeen else emptyMap()
            val badgeCounts = androidx.compose.runtime.remember(postedBadges, seenBadges) { BadgesWhenOpened.visible(postedBadges, seenBadges) }
            if (state.badgesWhenOpened) androidx.compose.runtime.LaunchedEffect(postedBadges) { model.trimBadgesSeen(postedBadges) }
            androidx.compose.runtime.SideEffect { latestNotifications = notificationItems }
            val wallpaperTone = rememberWallpaperTone(state.systemWallpaper)
            // Re-read on every resume so turning Remove animations on/off applies without restarting.
            val reduceMotionState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(reduceMotionEnabled(this@MainActivity)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { reduceMotionState.value = reduceMotionEnabled(this@MainActivity); onPauseOrDispose { } }
            // Folio's own Reduce Motion, or Android's Remove animations: the same flag to every screen.
            val reduceMotion = reduceMotionState.value || state.reduceMotion
            val crossWindowBlur = rememberCrossWindowBlur()
            val libraryBlur = crossWindowBlur && state.systemWallpaper && showsWallpaper
            LibraryBackdropBlur(window, libraryBlur)
            androidx.compose.runtime.CompositionLocalProvider(
                LocalWallpaperTone provides wallpaperTone,
                LocalGlassLook provides GlassLook(libraryBlur),
                LocalDeviceStatus provides deviceStatus,
                LocalReduceMotion provides reduceMotion,
                LocalHinge provides rememberHinge(this@MainActivity),
                // Tablets and desktop windows draw Folio proportionally larger instead of a phone-sized layout lost in a
                // big window; phones and foldables stay at exactly the system density (see uiScale).
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.platform.LocalDensity.current.let { d ->
                    val config = androidx.compose.ui.platform.LocalConfiguration.current
                    val scale = uiScale(config.screenWidthDp.toFloat(), config.screenHeightDp.toFloat(), config.classScale)
                    if (scale == 1f) d else androidx.compose.ui.unit.Density(d.density * scale, d.fontScale)
                },
                androidx.compose.ui.platform.LocalHapticFeedback provides (if (state.haptics) androidx.compose.ui.platform.LocalHapticFeedback.current else NoHaptics),
                LocalIconLook provides IconLook(state.iconPack, state.badgeStyle, state.liveIcons),
                LocalAppIconStyles provides state.appIconStyles,
                LocalIconsAreDark provides iconsAreDark,
                LocalRecentPackages provides recentPackages,
                LocalBadgeCounts provides badgeCounts, LocalInstallProgress provides installProgress, LocalNewApps provides newApps, LocalFolderColors provides state.folderColors) { FoldTransitionHost(state.foldEffect, state.stayAwakeOnFold,
                    state.haptics, reduceMotion = reduceMotion) {
                // The launcher blurs behind every overlay with the same spring the overlay uses.
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()
                    .graphicsLayer {
                    val p = overlayProgress
                    renderEffect = if (backdropBlurPx >= 2f && !LauncherPages.isOpen(this@MainActivity)) backdropBlurs.at(p) else null
                }) {
                LauncherScreen(state, model, widgets, homeRequests.intValue, homeFromFront = homeFromFront.value,
                    onLaunch = { launchApp(it) }, onMakeDefault = ::makeDefault, onAppInfo = ::appInfo,
                    isDefaultHome = defaultHome.value, deviceStatus = deviceStatus, onStatusMode = ::setStatusMode, onWallpaperPreview = ::previewWallpaper,
                    searchRequests = searchRequests.intValue, settingsRequests = settingsRequests.intValue,
                    onLaunchFrom = ::launchApp, onGoogleSearch = ::openGoogleSearch,
                    onShadeSetup = ::showShadeSetup, onShowWhatsNew = { whatsNewRequested.value = true })
                }
                // The gestures dialog never opens on top of either sheet: it waits here and shows once both are closed.
                val startupSheet = startupSheetUp()
                androidx.compose.runtime.LaunchedEffect(startupSheet) {
                    if (!startupSheet && shadeSetupWaiting) { shadeSetupWaiting = false; showShadeSetup() }
                }
                // After What's New, not on top of it: the build page waits until that sheet has been closed.
                if ((showDevBuild.value || DevBuild.reopen.intValue > 0) && !showWhatsNew.value && !whatsNewRequested.value) DevBuild.load(this@MainActivity)?.let { dev ->
                    val before = androidx.compose.runtime.remember { DevBuild.seen(this@MainActivity) }
                    DevBuildSheet(dev, before) { showDevBuild.value = false; DevBuild.reopen.intValue = 0; DevBuild.markSeen(this@MainActivity, dev) }
                }
                if (showWhatsNew.value || whatsNewRequested.value) WhatsNewSheet { showWhatsNew.value = false; whatsNewRequested.value = false; WhatsNew.markSeen(this@MainActivity) }
                val basePalette = LocalDuoPalette.current
                val panelInk = homeInkFor(state.homeInk, wallpaperTone.prefersDarkText)
                val panelPalette = androidx.compose.runtime.remember(basePalette, wallpaperTone) { homePalette(basePalette, wallpaperTone.primary) }
                androidx.compose.runtime.CompositionLocalProvider(LocalDuoPalette provides panelPalette) {
                androidx.compose.runtime.CompositionLocalProvider(LocalHomeInk provides panelInk) {
                SpotlightOverlay(spotlightVisible.value, { OverlayBack.shown(overlayProgress, OverlayBack.swipe.value) }, state, onClose = { spotlightVisible.value = false },
                    onLaunch = { launchApp(it) },
                    onAddToHome = { app ->
                        // Stays open, so more can be added; the row turns into "On Home" by itself.
                        model.setPinned(app.id, true)
                        FolioNotice.show(this@MainActivity, getString(R.string.added_to_home, app.label))
                    })
                }
                }
                // Last, so the corners sit above everything in Home's window.
            } } }
        }
        FoldRenderExperiment.attach(this)
        if (restoreShadeDialog) window.decorView.post { if (!isFinishing && !isDestroyed) showShadeSetup() }
    }

    override fun onStart() {
        super.onStart(); widgets.host.startListening()
        // A new build can also arrive while this activity is alive in a restored task, so look again each time it starts.
        if (!showDevBuild.value && DevBuild.shouldShow(this)) showDevBuild.value = true
    }
    override fun onRestart() {
        super.onRestart()
        HomeReturn.returned()
    }
    override fun onStop() {
        stoppedSinceHome = true
        closeOverlays() // never come back to a blurred Home
        widgets.host.stopListening(); model.flushPending(); super.onStop()
    }
    override fun onDestroy() {
        LauncherSheets.closeAllOf(this); LauncherPages.closeAllOf(this) // an entry left by a window that never closed cannot outlive its screen
        shadeSetupDialog?.dismiss()
        super.onDestroy()
    }
    override fun onPause() {
        super.onPause()
        FolioForeground.visible.value = false
        Diagnostics.event(getString(R.string.home_hidden))
        Diagnostics.checkpoint(this, visible = false)
    }
    override fun onResume() {
        super.onResume()
        stoppedSinceHome = false
        FolioForeground.visible.value = true
        Diagnostics.event("Home shown (${Diagnostics.screenSummary(this).substringBefore(',')})")
        Diagnostics.checkpoint(this, visible = true)
        FolioActions.home = java.lang.ref.WeakReference(this)
        if (SpotlightRequest.consume()) openSpotlight()
        FolioActions.pendingPanel?.let { FolioActions.pendingPanel = null; showPanel(it) }
        returningFromShadeSettings = false
        model.refresh(); updateDefaultHome()
    }

    /** The notifications on screen now, for Clear Badge. */
    internal var latestNotifications: List<NotificationItem> = emptyList()
    internal val spotlightVisible = androidx.compose.runtime.mutableStateOf(false)
    /** Opens Spotlight, or Android's notifications or quick settings. */
    internal fun showPanel(panel: ShadePanel) { if (panel == ShadePanel.SEARCH) openSpotlight() else openAndroidShade(panel) }

    internal fun openSpotlight() { spotlightVisible.value = true }
    private fun closeOverlays() { spotlightVisible.value = false }

    internal fun openAndroidShade(panel: ShadePanel) {
        when (SystemShadeAccessibilityService.open(this, panel)) {
            ShadeOpenResult.OPENED -> Unit
            ShadeOpenResult.SERVICE_DISABLED -> showShadeSetup()
            ShadeOpenResult.SERVICE_STARTING -> FolioNotice.show(this, getString(R.string.folio_gestures_are_starting_swipe_down_a))
            ShadeOpenResult.ACTION_REJECTED -> FolioNotice.show(this, getString(R.string.android_couldn_t_open_the_system_panel))
        }
    }

    /** The build page or What's New is up, or about to be: the same conditions that show them in the content above. */
    private fun startupSheetUp() = showWhatsNew.value || whatsNewRequested.value ||
        ((showDevBuild.value || DevBuild.reopen.intValue > 0) && DevBuild.load(this) != null)

    private fun showShadeSetup() {
        if (shadeSetupDialog?.isShowing == true) return
        // Two prompts at once read as one: after an install, a restored gestures dialog came up over the build page.
        if (startupSheetUp()) { shadeSetupWaiting = true; return }
        shadeSetupDialog = android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.turn_on_folio_gestures))
            .setMessage("Android asks you to turn this on yourself:\n\n" +
                "1. Tap Open Settings, find “Folio gestures & overlays” (often under Installed apps) and turn it on.\n" +
                "2. If it's greyed out, or you see “App was denied access” or “Restricted setting”, tap App Info below, open the ⋮ menu " +
                "(top right), choose “Allow restricted settings” and confirm, then come back and turn it on. Android does this for apps " +
                "installed from a browser or file; it's a one-time step.\n\n" +
                getString(R.string.folio_uses_it_to_open_notification_cente) +
                getString(R.string.it_can_t_read_what_s_on_your_screen))
            .setNegativeButton(getString(R.string.not_now), null)
            .setNeutralButton(getString(R.string.app_info)) { _, _ ->
                returningFromShadeSettings = true
                runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) }
                    .onFailure { returningFromShadeSettings = false }
            }
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                returningFromShadeSettings = true
                val opened = runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.isSuccess
                if (!opened) {
                    returningFromShadeSettings = false
                    FolioNotice.show(this, getString(R.string.accessibility_settings_are_unavailable))
                }
            }
            .also { dialog -> dialog.setOnDismissListener { shadeSetupDialog = null } }
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setStatusMode(model.state.value.verticalStatus)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        widgets.save(outState)
        outState.putBoolean(SHADE_DIALOG_VISIBLE, (shadeSetupDialog?.isShowing == true || shadeSetupWaiting) && !returningFromShadeSettings)
        outState.putBoolean(SHADE_SETTINGS_PENDING, returningFromShadeSettings)
        super.onSaveInstanceState(outState)
    }
    /** Android's "Home app settings" gear, or Folio's own app icon (the FolioSettingsApp alias). Until Folio is the
     * Home app, its icon opens Home instead, as a preview you can leave with Back or the Home gesture. */
    private fun opensSettings(intent: Intent) = intent.action == Intent.ACTION_APPLICATION_PREFERENCES ||
        (fromAppIcon(intent) && defaultHome.value)
    private fun fromAppIcon(intent: Intent) = intent.component?.className == "$FOLIO_CLASSES.$FOLIO_SETTINGS_ALIAS"

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        FoldRenderExperiment.onNewIntent(this, intent)
        updateDefaultHome()
        if (intent.getStringExtra("duo_destination") == "search") searchRequests.intValue++
        // One chain: tapping Folio's icon opens Settings *or* goes Home, never both.
        if (opensSettings(intent)) settingsRequests.intValue++
        else if (intent.hasCategory(Intent.CATEGORY_HOME) || fromAppIcon(intent) || intent.getStringExtra("duo_destination") == "home") {
            // Pressing the Home button on Home goes to the first page; from an app (stopped, so true here) it keeps the page you left.
            val overlayOpen = spotlightVisible.value
            homeFromFront.value = intent.hasCategory(Intent.CATEGORY_HOME) && !stoppedSinceHome && !overlayOpen
            closeOverlays(); homeRequests.intValue++
        }
        intent.removeExtra("duo_destination")
    }

    @Deprecated("Widget configuration uses the platform host request-code API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.onActivityResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    /** The badge showing on [packageName] now counts as seen, so it goes away until a new notification arrives. */
    private fun noteBadgeSeen(packageName: String) {
        if (!model.state.value.badgesWhenOpened) return
        model.noteBadgeSeen(packageName, BadgeClears.counts(latestNotifications, BadgeClears.cleared.value)[packageName] ?: 0)
    }

    private fun launchApp(app: AppEntry, bounds: android.graphics.Rect? = null) {
        HomeReturn.launched(bounds)
        RecentApps.record(this, app.id)
        NewApps.opened(this, app.packageName)
        noteBadgeSeen(app.packageName)
        try {
            val user = getSystemService(UserManager::class.java).getUserForSerialNumber(app.userSerial)
                ?: throw IllegalStateException(getString(R.string.profile_is_unavailable))
            val launcherApps = getSystemService(LauncherApps::class.java)
            val shortcut = app.shortcutId
            if (shortcut != null) launcherApps.startShortcut(app.packageName, shortcut, screenBounds(bounds), launchOptions(bounds), user)
            else launcherApps.startMainActivity(app.component, user, screenBounds(bounds), launchOptions(bounds))
        } catch (_: Exception) { FolioNotice.show(this, getString(R.string.app_is_unavailable, app.label)); model.refresh() }
    }

    private fun screenBounds(bounds: android.graphics.Rect?): android.graphics.Rect? = bounds?.takeUnless { it.isEmpty }?.let {
        val location = IntArray(2); window.decorView.getLocationOnScreen(location)
        android.graphics.Rect(it).apply { offset(location[0], location[1]) }
    }
    private fun launchOptions(bounds: android.graphics.Rect?): Bundle? = bounds?.takeUnless { it.isEmpty }?.let {
        android.app.ActivityOptions.makeScaleUpAnimation(window.decorView, it.left, it.top, it.width(), it.height()).toBundle()
    }
    private fun openGoogleSearch(bounds: android.graphics.Rect?): Boolean = try {
        startActivity(googleSearchIntent().apply { sourceBounds = screenBounds(bounds) }, launchOptions(bounds))
        true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }

    private fun makeDefault() {
        // Samsung may immediately cancel a role request; its Home settings is reliable.
        try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        catch (_: android.content.ActivityNotFoundException) {
            val role = getSystemService(RoleManager::class.java)
            if (role.isRoleAvailable(RoleManager.ROLE_HOME)) startActivity(role.createRequestRoleIntent(RoleManager.ROLE_HOME))
            else startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    private fun updateDefaultHome() {
        defaultHome.value = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Folding, Display size, Smallest width or split screen can bring Android's status bar back over the Side Bar
        // status; hide it again once the new layout is in place.
        window.decorView.post { setStatusMode(model.state.value.verticalStatus) }
    }

    private fun setStatusMode(vertical: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (vertical) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
    }

    private fun previewWallpaper() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, DuneWallpaperService::class.java)))
        } catch (_: android.content.ActivityNotFoundException) {
            FolioNotice.show(this, getString(R.string.the_system_wallpaper_preview_is_unavaila))
        }
    }

    private fun appInfo(app: AppEntry) {
        try {
            val user = getSystemService(UserManager::class.java).getUserForSerialNumber(app.userSerial)
                ?: throw IllegalStateException(getString(R.string.profile_is_unavailable))
            getSystemService(LauncherApps::class.java).startAppDetailsActivity(app.component, user, null, null)
        } catch (_: Exception) {
            FolioNotice.show(this, getString(R.string.app_is_unavailable, app.label))
            model.refresh()
        }
    }

    private companion object {
        const val SHADE_DIALOG_VISIBLE = "duo.shade.dialog_visible"
        const val SHADE_SETTINGS_PENDING = "duo.shade.settings_pending"
    }
}
