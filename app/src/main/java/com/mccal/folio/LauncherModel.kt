package com.mccal.folio

import android.app.Application
import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.BreakIterator
import java.text.Collator

data class AppEntry(
    val id: String,
    val label: String,
    val icon: Bitmap,
    val component: ComponentName = ComponentName.unflattenFromString(parseProfileAppId(id)?.component ?: id)
        ?: ComponentName("", ""),
    val user: UserHandle = Process.myUserHandle(),
    val userSerial: Long = 0,
    val profileLabel: String = "Personal",
    /** Not from the personal profile: a work profile or a clone. [kind] says which. */
    val isWork: Boolean = false,
    val available: Boolean = true,
    /** The name Android reports, kept so a custom name can be changed or cleared without reloading apps. */
    val systemLabel: String = label,
    val kind: ProfileKind = if (isWork) ProfileKind.WORK else ProfileKind.PERSONAL,
) {
    val packageName: String get() = component.packageName
    /** A pinned shortcut (a website or app action someone added to Home) rather than an app. */
    val isShortcut: Boolean get() = component.className.startsWith(SHORTCUT_CLASS_PREFIX)
    val shortcutId: String? get() = component.className.takeIf { isShortcut }?.removePrefix(SHORTCUT_CLASS_PREFIX)
}

/** Pinned shortcuts live on Home as entries whose component names the owning app and "#shortcut:" plus the shortcut id. */
const val SHORTCUT_CLASS_PREFIX = "#shortcut:"

/** Custom names are capped so a renamed icon still reads as a label under it. */
const val MAX_APP_NAME = 40

/**
 * The first [max] characters of a name, cut where a reader would see a character rather than where the string
 * happens to have one. An emoji is two chars to Java and a family is eleven, so `take` alone can leave half of
 * one behind at the cap, which draws as a hollow box.
 *
 * The JDK's character breaks predate emoji joined with U+200D, so a cut inside a family lands between its people;
 * dropping a joiner left at the end turns that into the first of them standing alone, which at least reads.
 */
internal fun String.takeAppName(max: Int = MAX_APP_NAME): String {
    if (length <= max) return this
    val characters = BreakIterator.getCharacterInstance().also { it.setText(this) }
    // A cluster longer than the whole cap (sixty combining marks on one letter) has no break to fall back to, so
    // it is cut at the cap: a degenerate glyph stacked up the screen is worse than one cut mid-cluster.
    val end = characters.preceding(max + 1).takeIf { it > 0 }
        ?: characters.following(0).takeIf { it in 1..max } ?: minOf(max, length)
    return substring(0, end).trimEnd(JOINER)
}

/** Zero-width joiner: the thread between the people in a family emoji, and between the parts of many others. */
private const val JOINER = '\u200D'

/** Sets the custom name for [id]; a blank name clears it, so the app goes back to the name Android reports. */
fun editAppName(names: Map<String, String>, id: String, name: String): Map<String, String> {
    val trimmed = name.trim().takeAppName()
    return if (trimmed.isEmpty()) names - id else names + (id to trimmed)
}

/**
 * Custom names over the labels Android reports. Every screen reads [AppEntry.label], so renaming once here
 * covers Home, the dock, folders, the App Library and search.
 */
internal fun List<AppEntry>.withAppNames(names: Map<String, String>): List<AppEntry> {
    if (names.isEmpty() && none { it.label != it.systemLabel }) return this
    val collator = Collator.getInstance()
    return map { app -> (names[app.id] ?: app.systemLabel).let { if (it == app.label) app else app.copy(label = it) } }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}

data class LauncherState(
    val apps: List<AppEntry> = emptyList(),
    val profiles: List<AppProfile> = emptyList(),
    val folders: List<FolderEntry> = emptyList(),
    val homeSlots: List<String?> = emptyList(),
    val leadingSlots: List<String?> = List(HOME_CELLS) { null },
    val editRevision: Int = 0,
    val canUndoEdit: Boolean = false,
    /** The dock's apps, one per slot: [MIN_DOCK_SLOTS] to [MAX_DOCK_SLOTS] of them, four until the person asks for more. */
    val dock: List<String?> = List(MIN_DOCK_SLOTS) { null },
    val widgetPlacements: List<WidgetPlacement> = DEFAULT_WIDGET_PLACEMENTS,
    val widgetRestores: List<WidgetRestore> = emptyList(),
    val googleSearch: Boolean = true,
    /** Where the dock goes on the cover screen and on the inner one. */
    val coverDock: DockPlacement = DockPlacement.AUTOMATIC,
    val innerDock: DockPlacement = DockPlacement.AUTOMATIC,
    val labels: Boolean = true,
    val verticalStatus: Boolean = true,
    /**
     * What Full-Width Home replaced, so turning it off puts it back: the dock placement of each screen it was turned on
     * for (by [LayoutScreen] name) and whether the status Side Bar was showing ("status"). Empty when it is off.
     */
    val fullWidthRestore: Map<String, String> = emptyMap(),
    /** Mirror the side rail (dock, status, controls) to the left edge. */
    val leftHanded: Boolean = false,
    /** App ids kept out of All apps and app search (Home shortcuts are unaffected). */
    val hiddenApps: Set<String> = emptySet(),
    val minPages: Int = 1,
    val foldEffect: Boolean = true,
    val stayAwakeOnFold: Boolean = true,
    val dockEverywhere: Boolean = false,
    /** Buttons in Every App: a large Back / Home / Recents bar over other apps (Folio's accessibility service). */
    val buttonBar: Boolean = false,
    /** Spotlight sections the user turned off (names of [SpotlightSection]). */
    val spotlightHidden: Set<String> = emptySet(),
    /** Engine for Enter in search: a [WebSearchTarget] name. */
    val searchEngine: String = "GOOGLE",
    /** Your own search address for Search with Enter, used when [searchEngine] is CUSTOM: see [customSearchUrl]. */
    val searchCustomUrl: String = "",
    val libraryCategories: Boolean = true,
    /** The Personal and Work switch in the App Library (only shown with a work profile); off shows personal apps only. */
    val libraryWork: Boolean = true,
    /** Package of the selected third-party icon pack, or null for app icons. */
    val iconPack: String? = null,
    val badgeStyle: BadgeStyle = BadgeStyle.DOT,
    /** Clear Badges When Opened: an app's badge goes quiet once you open it (see [BadgesWhenOpened]). */
    val badgesWhenOpened: Boolean = false,
    /** Badge counts already seen, by package name: the count that was showing when the app was last opened. */
    val badgesSeen: Map<String, Int> = emptyMap(),
    /** iOS "Search" capsule on Home in place of the page dots. */
    val homeStrip: HomeStrip = HomeStrip.DOTS,
    /** Swipe down on Home: SPOTLIGHT, NOTIFICATIONS (Android's notification shade) or OFF. */
    val swipeDownHome: String = "SPOTLIGHT",
    /** App for messaging contacts from Spotlight: null = default texting app, or OpenBubbles/BlueBubbles. */
    val messagesApp: String? = null,
    /** Smart Stacks: widget placement slot → extra widget ids stacked behind that placement's widget. */
    val widgetStacks: Map<Int, List<Int>> = emptyMap(),
    /** Smart Rotate: stacks move to the widget that matters now. */
    val stackRotate: Boolean = true,
    /** iOS "Newly Downloaded Apps": false = App Library only (Android's way), true = also add to Home. */
    val addNewAppsToHome: Boolean = false,
    /** Beta: save Home before big changes so they can be restored (Settings › General › Layout History). */
    val layoutHistory: Boolean = false,
    /** Beta: a dot under dock apps used in the last hour (needs Usage Access). */
    val dockRecentDots: Boolean = false,
    /** Show Android's own home-screen wallpaper behind Folio (live wallpapers included) instead of Folio's background. */
    val systemWallpaper: Boolean = false,
    /** Text drawn on the wallpaper: "AUTO" follows the wallpaper, "LIGHT" white, "DARK" dark. */
    val homeInk: String = "AUTO",
    /** Settings › Gestures › Reduce Motion: fades instead of slides and pops, no wiggle. Android's Remove animations turns it on too. */
    val reduceMotion: Boolean = false,
    /**
     * iOS's legibility gradient: darken the top and bottom of the background so white text reads over a pale
     * wallpaper (A11Y-9). See [HomeScrim] for what it draws and how it composes with [homeInk].
     */
    val homeScrim: Boolean = true,
    /** Harbor-style: dock icons magnify under your finger. */
    val dockMagnify: Boolean = false,
    /** Velox-style: swipe up on a Home app icon for its quick panel. */
    val appPanels: Boolean = false,
    /** Activator-style trigger → action (names of [FolioTrigger] → [FolioAction]). */
    val triggerActions: Map<String, String> = emptyMap(),
    val haptics: Boolean = true,
    /** Drag along the Search pill or page dots to scrub pages. */
    val pageScrub: Boolean = true,
    /** Android wallpaper shifts slightly as pages change. */
    val wallpaperMotion: Boolean = true,
    /** Live Clock and Calendar icons. */
    val liveIcons: Boolean = true,
    /** Optional tint per folder id (ARGB). */
    val folderColors: Map<String, Long> = emptyMap(),
    /** Icon Stacks: anchor app id → the apps that fan out when you swipe down on it. */
    val iconStacks: Map<String, List<String>> = emptyMap(),
    /** Custom app names by app id; apps without an entry keep the name Android reports. */
    val appNames: Map<String, String> = emptyMap(),
    /** Pictures chosen for single apps (Edit Icon); an app not here keeps its own icon. */
    val appIconStyles: Map<String, AppIconOverride> = emptyMap(),
    /** Swipe up, swipe down and double tap actions chosen for single apps; an app not here keeps today's behavior. */
    val iconActions: Map<String, IconActions> = emptyMap(),
    /** App rows that last fit the cover-class and the inner-class Home (0 = not measured yet); see [homeAppRows]. */
    val homeFitCompact: Int = 0,
    val homeFitExpanded: Int = 0,
    /** The same for the upright inner screen, kept apart so it and the sideways inner screen stop overwriting each other. */
    val homeFitUpright: Int = 0,
    val loading: Boolean = true,
    /** Home's own apps are in. At a start they come ahead of the rest, while [loading] stays true (not saved). */
    val homeAppsLoaded: Boolean = false,
    val error: String? = null,
) {
    val order: List<String> get() = homeSlots.filterNotNull()
    val widgets: List<Int> get() = layout.widgets
    val layout: HomeLayout get() = HomeLayout(homeSlots, dock, widgetPlacements, folders, widgetRestores, leadingSlots, minPages)
    val homePages get() = layout.pageCount
    /** App rows every Home page shows, the same on both screens so pages don't change when you fold. */
    val homeAppRows: Int get() = effectiveHomeRows(homeFitCompact, homeFitExpanded, homeFitUpright)
    /** Home can be drawn complete: its apps are in, or loading has ended (or failed). */
    val homeReady: Boolean get() = homeAppsLoaded || !loading
}

/**
 * Every app the saved state remembers something about: where it is on Home, the dock, folders and stacks, and a name or
 * icon look chosen for it. When one is no longer installed, all of that goes, so nothing stale comes back with it.
 */
internal fun LauncherState.trackedAppIds(): List<String> =
    homeSlots.filterNotNull() + leadingSlots.filterNotNull() + dock.filterNotNull() + folders.flatMap { it.appIds } +
        iconStacks.keys + iconStacks.values.flatten() + appNames.keys + appIconStyles.keys + iconActions.keys

/**
 * Full-Width Home turned on or off for [screen]: a bottom dock and no status Side Bar, or what they were before. Turning
 * it on remembers the dock placement (and whether the Side Bar showed) so turning it off puts back exactly that, not a
 * guess; with nothing remembered, off goes back to the automatic dock and the Side Bar.
 */
internal fun LauncherState.withFullWidthHome(screen: LayoutScreen, on: Boolean): LauncherState {
    val now = dockFor(screen)
    val key = screen.name
    return if (on) {
        val remembered = fullWidthRestore.toMutableMap()
        // Even a dock that was already at the bottom: the entry is also what marks this screen as in Full-Width Home.
        remembered[key] = now.name
        if ("status" !in remembered) remembered["status"] = verticalStatus.toString()
        withDock(screen, DockPlacement.BOTTOM).copy(verticalStatus = false, fullWidthRestore = remembered)
    } else {
        val before = DockPlacement.parse(fullWidthRestore[key] ?: DockPlacement.AUTOMATIC.name)
        val rest = fullWidthRestore - key
        // The Side Bar comes back once no screen is still in Full-Width Home by way of a remembered placement.
        val bringBack = rest.keys.none { it != "status" }
        withDock(screen, before).copy(
            verticalStatus = if (bringBack) fullWidthRestore["status"]?.toBooleanStrictOrNull() ?: true else verticalStatus,
            fullWidthRestore = if (bringBack) emptyMap() else rest)
    }
}

/**
 * Forgets that [screen] was in Full-Width Home, for when the screen itself goes away (the upright layout is merged back
 * into the inner one). The Side Bar returns once no screen is left in it.
 */
internal fun LauncherState.withoutFullWidthEntry(screen: LayoutScreen): LauncherState {
    if (screen.name !in fullWidthRestore) return this
    val rest = fullWidthRestore - screen.name
    val bringBack = rest.keys.none { it != "status" }
    return copy(verticalStatus = if (bringBack) fullWidthRestore["status"]?.toBooleanStrictOrNull() ?: true else verticalStatus,
        fullWidthRestore = if (bringBack) emptyMap() else rest)
}

/** [preset] saved for [screen]; the upright inner screen without a layout of its own edits the inner one. */
/** The app rows last measured for [screen] (0 = not measured yet). */
internal fun LauncherState.homeFitFor(screen: LayoutScreen): Int = when (screen) {
    LayoutScreen.COVER -> homeFitCompact
    LayoutScreen.INNER -> homeFitExpanded
    LayoutScreen.INNER_UPRIGHT -> homeFitUpright
}

internal fun LauncherState.withHomeFit(screen: LayoutScreen, rows: Int): LauncherState = when (screen) {
    LayoutScreen.COVER -> copy(homeFitCompact = rows)
    LayoutScreen.INNER -> copy(homeFitExpanded = rows)
    LayoutScreen.INNER_UPRIGHT -> copy(homeFitUpright = rows)
}

internal fun LauncherState.withDock(screen: LayoutScreen, placement: DockPlacement): LauncherState =
    if (screen == LayoutScreen.COVER) copy(coverDock = placement) else copy(innerDock = placement)

/** The dock placement [screen] uses: the upright inner screen shares the inner one. */
fun LauncherState.dockFor(screen: LayoutScreen): DockPlacement = if (screen == LayoutScreen.COVER) coverDock else innerDock

/** Every app id Home shows without opening an app: its pages, the dock, and the apps in its folders and icon stacks. */
internal fun LauncherState.homeAppIds(): Set<String> =
    ((homeSlots + leadingSlots + dock).filterNotNull().filterNot(::isFolderId) + folders.flatMap { it.appIds } +
        iconStacks.keys + iconStacks.values.flatten()).toSet()

/**
 * Home's apps, shown ahead of the full list at a start. [LauncherState.loading] stays true, so nothing that needs
 * every app (reconciling, restoring, the App Library) acts on the part, and a full list is never replaced by it.
 */
internal fun LauncherState.withHomeApps(home: List<AppEntry>): LauncherState =
    if (!loading || apps.isNotEmpty() || home.isEmpty()) this else copy(apps = home.withAppNames(appNames), homeAppsLoaded = true)

/**
 * The fewest rows any of this device's screens has room for (screens not measured yet don't count, and nothing measured
 * means four).
 */
fun effectiveHomeRows(fitCompact: Int, fitExpanded: Int, fitUpright: Int = 0): Int =
    (listOf(fitCompact, fitExpanded, fitUpright).filter { it > 0 }.minOrNull() ?: BASE_APP_ROWS).coerceIn(BASE_APP_ROWS, MAX_APP_ROWS)

/**
 * The newest saved-state schema this build reads. 11: Hide in Landscape on the unfolded screen (removed with the island); 10: icon actions; 9: 36-cell Home pages (More rows); 6–8 had 24.
 * A save is written with [stateSchemaFor], not always with this number.
 */
const val STATE_SCHEMA = 11

/**
 * The schema a save is written with: the lowest one that holds everything in it. A phone that uses nothing from schema 10
 * keeps writing 9, so an older Folio still reads it. A phone with icon actions writes 10, and an older Folio then refuses
 * the file and says so (the damaged-state path) instead of reading it, dropping the actions on its next save, and losing
 * them without a word when the newer Folio comes back.
 */
internal fun stateSchemaFor(s: LauncherState) = when {
    s.iconActions.isNotEmpty() -> 10
    else -> 9
}

/** Whether this save should keep the schema 9 file it is about to replace: it writes schema 10 over an older read and no copy exists yet. */
internal fun v9BackupNeeded(sourceSchema: Int, writtenSchema: Int, hasBackup: Boolean) = sourceSchema < 10 && writtenSchema >= 10 && !hasBackup

/** Home's state. */
class LauncherModel(application: Application) : AndroidViewModel(application) {
    private data class RefreshedApps(val entries: List<AppEntry>, val profiles: List<AppProfile>,
        val authoritativeProfiles: Set<Long>, val removedProfiles: Set<Long>, val starter: StarterApps = StarterApps.KNOWN)
    private data class UndoImportSettings(val coverDock: DockPlacement, val innerDock: DockPlacement, val labels: Boolean,
        val googleSearch: Boolean, val verticalStatus: Boolean)
    private val prefs = application.getSharedPreferences("launcher", 0)
    private val launcherApps = application.getSystemService(LauncherApps::class.java)
    private val userManager = application.getSystemService(UserManager::class.java)
    private val appCatalogPrefs = application.getSharedPreferences("app_catalog", 0)
    private val legacyRaw = traced("Folio.readState") { prefs.getString("state", null) }
    private val sourceSchema = runCatching { JSONObject(legacyRaw ?: "{}").optInt("schema", 1) }.getOrDefault(1)
    private var needsMigration = sourceSchema < 2
    private var statePayloadInvalid = false
    private val mutable = MutableStateFlow(load())
    val state = mutable.asStateFlow()
    private var undoLayout: Pair<HomeLayout, HomeLayout>? = null
    private var undoImportSettings: UndoImportSettings? = null
    /**
     * Smart Stacks a restored backup took off Home because an old overflow widget in it kept their slot number, with the
     * layout that restore made. They count only while that restore is still the one Undo would take back.
     */
    private var setAsideStacks: Pair<HomeLayout, Map<Int, List<Int>>>? = null
    private fun stacksForUndo(): Map<Int, List<Int>> = setAsideStacks?.takeIf { it.first == undoLayout?.second }?.second.orEmpty()
    private var refreshing = false
    private var refreshPending = false
    private val invalidatedPackages = mutableSetOf<Pair<Long, String>>()
    private val removedPackages = mutableSetOf<Pair<Long, String>>()
    private val unavailablePackages = mutableSetOf<Pair<Long, String>>()
    /** New downloads waiting for the refresh that brings their apps in (for "Add to Home Screen"). */
    private val addedPackages = mutableSetOf<String>()
    /** Shortcuts just accepted from a pin request, to put on Home once the refresh brings them in (package, shortcut id). */
    private val pendingShortcuts = mutableSetOf<Pair<String, String>>()

    /** After Android pins a shortcut for Folio, refresh and place it in the first free spot on Home. */
    fun placePinnedShortcut(packageName: String, shortcutId: String) {
        pendingShortcuts += packageName to shortcutId
        refresh(packageName)
    }

    /** Deletes a pinned shortcut (iOS "Delete Bookmark"): unpins it in Android, which removes it from Home and the App Library. */
    fun deleteShortcut(app: AppEntry) {
        val id = app.shortcutId ?: return
        runCatching {
            val remaining = launcherApps.getShortcuts(LauncherApps.ShortcutQuery().setPackage(app.packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED), app.user)?.map { it.id }.orEmpty() - id
            launcherApps.pinShortcuts(app.packageName, remaining, app.user)
        }
        setPinned(app.id, false)
        removedPackages += app.userSerial to app.packageName
        refresh(app.packageName, app.user)
    }
    // Accessed only in the serialized IO refresh. Returning Home reuses existing bitmaps.
    private val iconCache = mutableMapOf<String, AppEntry>()
    private var iconConfiguration = ""
    /** Set by [reloadIcons]; the refresh that follows drops every cached icon. */
    @Volatile private var iconsStale = false
    /** Icons kept between starts; used in a refresh only once their fingerprint was checked. */
    private val savedIcons = SavedIcons(java.io.File(application.cacheDir, "icons"))
    @Volatile private var savedIconsChecked = false
    /** Icons loaded in this refresh, saved once its list is shown. */
    private val unsavedIcons = java.util.Collections.synchronizedList(mutableListOf<SavedIcons.Saved>())
    private val folioVersionCode by lazy {
        runCatching { getApplication<Application>().let { it.packageManager.getPackageInfo(it.packageName, 0).longVersionCode } }.getOrDefault(0L)
    }
    internal var completedRefreshes = 0
        private set
    /** Whether a refresh has listed this phone's apps. A damaged layout's stand-in starts with none, and with loading already false. */
    val appsListed get() = completedRefreshes > 0
    /** Whether apps are being listed right now (so not listed yet is still loading, not a failure). */
    val listingApps get() = refreshing
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            LibraryCategory.forget(packageName)
            // A package Folio didn't know is a new download (not an update or a profile coming back).
            if (mutable.value.apps.none { it.packageName == packageName }) {
                NewApps.mark(getApplication(), packageName)
                addedPackages += packageName
            }
            refresh(packageName, user)
        }
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            LibraryCategory.forget(packageName)
            removedPackages += userManager.getSerialNumberForUser(user) to packageName
            refresh(packageName, user)
        }
        override fun onPackageChanged(packageName: String, user: UserHandle) { LibraryCategory.forget(packageName); refresh(packageName, user) }
        // Apps update their dynamic shortcuts often; only pinned ones matter to Home.
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<android.content.pm.ShortcutInfo>, user: UserHandle) {
            if (shortcuts.any { it.isPinned } || mutable.value.apps.any { it.isShortcut && it.packageName == packageName }) refresh(packageName, user)
        }
        override fun onPackagesAvailable(packages: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
            packages.forEach {
                LibraryCategory.forget(it)
                val key = userManager.getSerialNumberForUser(user) to it
                invalidatedPackages += key; unavailablePackages -= key
            }; refresh()
        }
        override fun onPackagesUnavailable(packages: Array<out String>, user: android.os.UserHandle, replacing: Boolean) {
            // A package update temporarily hides activities; don't erase its pins or dock slot.
            packages.forEach {
                val key = userManager.getSerialNumberForUser(user) to it
                invalidatedPackages += key; unavailablePackages += key
            }; refresh()
        }
    }

    init {
        launcherApps.registerCallback(callback); refresh()
        FolioSettingsBridge.liveModel = java.lang.ref.WeakReference(this)
    }

    /** Settings › Refresh Icons: for a theme app that changes icons without telling launchers (#19). */
    fun reloadIcons() { iconsStale = true; IconPacks.clear(); refresh() }

    fun refresh(invalidatedPackage: String? = null, user: UserHandle = Process.myUserHandle()) {
        invalidatedPackage?.let { invalidatedPackages += userManager.getSerialNumberForUser(user) to it }
        if (refreshing) { refreshPending = true; return }
        refreshing = true
        val invalidated = invalidatedPackages.toSet()
        val removed = removedPackages.toSet()
        val temporarilyUnavailable = unavailablePackages.toSet()
        invalidatedPackages.clear()
        removedPackages.clear()
        val resources = getApplication<Application>().resources
        val configuration = resources.configuration.let { "${it.densityDpi}|${it.locales.toLanguageTags()}|${it.uiMode}|${assetsSequence(it.toString())}" }
        // At a start nothing is cached yet: Home's own apps come first (J1 in docs/standards/performance.md).
        val homeFirst = mutable.value.takeIf { it.apps.isEmpty() && !needsMigration && !statePayloadInvalid }?.homeAppIds().orEmpty()
        viewModelScope.launch {
            try {
                val home = withContext(Dispatchers.IO) {
                    if (iconsStale) caught("Home: forgetting saved icons") { savedIcons.clear() }
                    if (configuration != iconConfiguration || iconsStale) { iconCache.clear(); iconConfiguration = configuration; iconsStale = false }
                    savedIconsChecked = caught("Home: checking saved icons") {
                        savedIcons.check("$configuration|${Build.FINGERPRINT}|$folioVersionCode")
                    }.isSuccess
                    // Only what this refresh loads is saved: an earlier one that failed may have drawn under another fingerprint.
                    unsavedIcons.clear()
                    iconCache.keys.removeAll { key -> parseProfileAppId(key)?.let { identity ->
                        val serial = identity.userSerial ?: userManager.getSerialNumberForUser(Process.myUserHandle())
                        serial to (ComponentName.unflattenFromString(identity.component)?.packageName ?: "") in invalidated
                    } == true }
                    if (homeFirst.isEmpty()) emptyList() else caught("Home: loading its apps first") {
                        traced("Folio.loadHomeApps") { homeEntries(homeFirst, resources.displayMetrics.densityDpi, invalidated) }
                    }.getOrDefault(emptyList())
                }
                mutable.update { it.withHomeApps(home) }
                val apps = withContext(Dispatchers.IO) {
                    val collator = Collator.getInstance()
                    val application = getApplication<Application>()
                    val (personal, personalSerial, associatedSerials, handles, profiles) = appProfiles()
                    val cachedBeforeProfiles = loadCachedApps().filterNot { entry -> entry.userSerial to entry.packageName in removed }
                    val removedProfileSerials = removedProfileSerials(cachedBeforeProfiles, associatedSerials)
                    val cached = cachedBeforeProfiles.filterNot { it.isWork && it.userSerial in removedProfileSerials }
                    val authoritativeProfiles = mutableSetOf<Long>()
                    authoritativeProfiles += removedProfileSerials
                    val liveApps = handles.flatMap { profile ->
                        val serial = userManager.getSerialNumberForUser(profile)
                        val descriptor = profiles.first { it.userSerial == serial }
                        val activityList = if (descriptor.available) runCatching { launcherApps.getActivityList(null, profile) }.getOrNull() else null
                        if (activityList == null) emptyList() else activityList.also { authoritativeProfiles += serial }.mapNotNull { info ->
                            activityId(info, serial, personalSerial)?.takeIf { listedInProfile(descriptor.kind, info.applicationInfo?.flags ?: 0) }
                                ?.let { id -> activityEntry(info, id, profile, descriptor, invalidated) }
                        }
                    }
                    // Pinned shortcuts (e.g. Chrome's "Add to Home screen"). Only the default Home app may read them;
                    // if the query fails, the shortcuts Folio already knows stay put rather than vanish from Home.
                    val shortcuts = handles.flatMap { profile ->
                        val serial = userManager.getSerialNumberForUser(profile)
                        val descriptor = profiles.first { it.userSerial == serial }
                        if (!descriptor.available) return@flatMap emptyList()
                        val pinned = runCatching { launcherApps.getShortcuts(LauncherApps.ShortcutQuery()
                            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED), profile) }.getOrNull()
                            ?: return@flatMap cached.filter { it.isShortcut && it.userSerial == serial }.map { it.copy(available = true) }
                        pinned.filter { it.isEnabled && it.`package` != application.packageName }.map { info ->
                            shortcutEntry(info, profile, descriptor, personalSerial, resources.displayMetrics.densityDpi)
                        }
                    }
                    val live = liveApps + shortcuts
                    val liveIds = live.mapTo(mutableSetOf(), AppEntry::id)
                    val profileBySerial = profiles.associateBy(AppProfile::userSerial)
                    val unavailable = cached.filter { it.id !in liveIds }.mapNotNull { cachedEntry ->
                        val profile = profileBySerial[cachedEntry.userSerial]
                        val key = cachedEntry.userSerial to cachedEntry.packageName
                        // A successful profile query is authoritative except while Android explicitly
                        // reports a package unavailable (for example during an update).
                        if (cachedEntry.userSerial in authoritativeProfiles && key !in temporarilyUnavailable) null
                        else cachedEntry.copy(user = profile?.let { p -> handles.firstOrNull { userManager.getSerialNumberForUser(it) == p.userSerial } } ?: personal,
                            profileLabel = profile?.label ?: cachedEntry.profileLabel, available = false)
                    }
                    val entries = (live + unavailable).distinctBy(AppEntry::id)
                        .sortedWith { a, b -> collator.compare(a.label, b.label) }
                    saveCachedApps(entries)
                    // Spell Chinese names in pinyin here, off the main thread, so search and the A–Z list don't wait.
                    entries.forEach { Pinyin.syllables(it.label) }
                    iconCache.keys.retainAll(entries.map { it.id }.toSet())
                    RefreshedApps(entries, (profiles + unavailable.map { AppProfile(it.userSerial, it.profileLabel, false, it.kind == ProfileKind.WORK,
                        quiet = true, unlocked = false, available = false, kind = it.kind) }).distinctBy(AppProfile::userSerial),
                        authoritativeProfiles.toSet(), removedProfileSerials,
                        if (!prefs.getBoolean("initialized", false) || needsMigration) StarterApps.resolve(application) else StarterApps.KNOWN)
                }
                mutable.update { old ->
                    val entries = apps.entries.withAppNames(old.appNames)
                    val profiles = apps.profiles
                    val dock = if (!prefs.getBoolean("initialized", false)) initialDock(entries, apps.starter) else old.dock
                    val installed = entries.map { it.id }
                    val legacyPins = if (needsMigration) migrateHomePins(old.order, installed, suggestedPins(entries, dock, apps.starter))
                        else old.homeSlots
                    val pins = if (sourceSchema < 6 && needsMigration) migrateSchema5Apps(legacyPins) else legacyPins
                    val availableIds = entries.mapTo(mutableSetOf(), AppEntry::id)
                    val authoritative = apps.authoritativeProfiles
                    val removedIds = removedAppIds(old.trackedAppIds(), availableIds,
                        authoritative, temporarilyUnavailable, removed, userManager.getSerialNumberForUser(Process.myUserHandle()),
                        apps.removedProfiles)
                    // iOS "Add to Home Screen": a newly downloaded app also goes to the first free spot on Home.
                    val oldIds = old.apps.mapTo(mutableSetOf(), AppEntry::id)
                    val arrivals = entries.filter { it.packageName in addedPackages && it.id !in oldIds && !it.isWork && !it.isShortcut }
                    val newShortcuts = entries.filter { app -> app.isShortcut && pendingShortcuts.any { it.first == app.packageName && it.second == app.shortcutId } }
                    pendingShortcuts.removeAll { p -> newShortcuts.any { it.packageName == p.first && it.shortcutId == p.second } }
                    addedPackages.removeAll(arrivals.map { it.packageName }.toSet())
                    val homeArrivals = (if (old.addNewAppsToHome) arrivals else emptyList()) + newShortcuts
                    val blockedForArrivals = old.widgetPlacements.filter { it.page >= 0 }.flatMap { it.coveredIndices() }.toSet()
                    val withArrivals = homeArrivals.fold(pins) { slots, app ->
                        if (app.id in old.dock || app.id in old.leadingSlots || old.folders.any { app.id in it.appIds }) slots
                        else pinHomeApp(slots, app.id, true, blockedForArrivals, old.homeAppRows)
                    }
                    val validPins = withArrivals.map { it?.takeUnless(removedIds::contains) }
                    val validDock = dock.map { it?.takeUnless(removedIds::contains) }
                    val reconciled = reconcileFolders(HomeLayout(validPins, validDock, old.widgetPlacements, old.folders,
                        old.widgetRestores, old.leadingSlots, old.minPages), removedIds)
                    old.copy(apps = entries, profiles = profiles, homeSlots = reconciled.slots, leadingSlots = reconciled.leadingSlots,
                        dock = trimmedDock(reconciled.dock), folders = reconciled.folders,
                        iconStacks = IconStacks.prune(old.iconStacks, old.iconStacks.keys + old.iconStacks.values.flatten() - removedIds),
                        appNames = old.appNames - removedIds, iconActions = old.iconActions - removedIds, appIconStyles = old.appIconStyles - removedIds.also { gone -> AppIconPictures.deleteAll(getApplication(), gone.filter { old.appIconStyles[it]?.hasPicture == true }) },
                        canUndoEdit = old.canUndoEdit && old.layout == reconciled, loading = false, homeAppsLoaded = true,
                        error = if (statePayloadInvalid) old.error else null)
                }
                if (needsMigration && legacyRaw != null && !prefs.contains("state_v1_backup"))
                    prefs.edit().putString("state_v1_backup", legacyRaw).apply()
                needsMigration = false
                persist()
                completedRefreshes++
                val unsaved = synchronized(unsavedIcons) { unsavedIcons.toList().also { unsavedIcons.clear() } }
                val listed = apps.entries.mapTo(mutableSetOf(), AppEntry::id)
                if (savedIconsChecked) viewModelScope.launch(Dispatchers.IO) {
                    caught("Home: saving icons") { traced("Folio.saveIcons") { savedIcons.write(unsaved); savedIcons.prune(listed) } }
                }
            } catch (_: Exception) {
                mutable.update { it.copy(loading = false, error = getApplication<Application>().getString(R.string.apps_could_not_be_loaded_tap_to_retry)) }
            } finally {
                refreshing = false
                if (refreshPending) { refreshPending = false; refresh() }
            }
        }
    }

    private fun loadCachedApps(): List<AppEntry> = runCatching {
        val application = getApplication<Application>()
        val personal = Process.myUserHandle()
        val array = JSONArray(appCatalogPrefs.getString("apps", "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val id = item.getString("id")
            val identity = parseProfileAppId(id) ?: error("Invalid cached app identity")
            val component = ComponentName.unflattenFromString(identity.component) ?: error("Invalid cached component")
            val serial = item.getLong("serial").takeIf { it >= 0 } ?: error("Invalid cached profile")
            val user = userManager.getUserForSerialNumber(serial) ?: personal
            val isWork = item.optBoolean("work", identity.userSerial != null)
            val baseIcon = application.packageManager.defaultActivityIcon
            val icon = runCatching { application.packageManager.getUserBadgedIcon(baseIcon, user) }.getOrDefault(baseIcon)
            AppEntry(id, item.getString("label"), launcherIcon(icon), component, user, serial,
                item.optString("profile", if (isWork) "Work" else "Personal"), isWork, available = false,
                kind = storedProfileKind(item.optString("kind").takeIf(String::isNotEmpty), isWork))
        }
    }.getOrDefault(emptyList())

    private data class AppProfiles(val personal: UserHandle, val personalSerial: Long, val associatedSerials: Set<Long>,
        val handles: List<UserHandle>, val profiles: List<AppProfile>)

    /** The profiles Folio lists apps from (personal, a work profile, clones; see [ProfileKind]), and whether each can list them now. */
    private fun appProfiles(): AppProfiles {
        val personal = Process.myUserHandle()
        val personalSerial = userManager.getSerialNumberForUser(personal)
        val associatedSerials = userManager.userProfiles.mapTo(mutableSetOf(), userManager::getSerialNumberForUser)
        val kinds = launcherApps.profiles
            .filter { profile -> userManager.getSerialNumberForUser(profile) in associatedSerials }
            .distinctBy(userManager::getSerialNumberForUser)
            .associateWith { profile ->
                if (userManager.getSerialNumberForUser(profile) == personalSerial) ProfileKind.PERSONAL
                else launcherProfileKind(launcherApps, profile)
            }
            .filterValues { it != ProfileKind.SKIP }
        val handles = kinds.keys.toList()
        val profiles = handles.map { profile ->
            val serial = userManager.getSerialNumberForUser(profile)
            val kind = kinds.getValue(profile)
            val isPersonal = kind == ProfileKind.PERSONAL
            val quiet = !isPersonal && runCatching { userManager.isQuietModeEnabled(profile) }.getOrDefault(false)
            val unlocked = runCatching { userManager.isUserUnlocked(profile) }.getOrDefault(isPersonal)
            AppProfile(serial, when (kind) { ProfileKind.PERSONAL -> "Personal"; ProfileKind.CLONE -> "Clone"; else -> "Work" },
                isPersonal, kind == ProfileKind.WORK, quiet, unlocked, !quiet && unlocked, kind)
        }
        return AppProfiles(personal, personalSerial, associatedSerials, handles, profiles)
    }

    /**
     * The id Home and the App Library know [info] by, or null for Folio's own components: Folio lists itself only as
     * its Settings app, like iOS Settings in the App Library.
     */
    private fun activityId(info: LauncherActivityInfo, serial: Long, personalSerial: Long): String? {
        val own = getApplication<Application>().packageName
        val component = info.componentName
        if (component.packageName == own && component.className != "$FOLIO_CLASSES.$FOLIO_SETTINGS_ALIAS") return null
        return profileAppId(component.flattenToString(), serial, personalSerial)
    }

    /**
     * [info]'s entry: from the icon cache while its label and component still match; else from the icons saved at an
     * earlier start, while the app is unchanged and Android hasn't just said it changed ([invalidated]); else loaded,
     * and saved once the list is shown. A saved icon skips loading the app's label and icon, which is most of a start.
     */
    private fun activityEntry(info: LauncherActivityInfo, id: String, profile: UserHandle, descriptor: AppProfile,
        invalidated: Set<Pair<Long, String>>): AppEntry {
        val sourceDir = info.applicationInfo?.sourceDir.orEmpty()
        val canSave = savedIconsChecked && sourceDir.isNotEmpty()
        if (canSave && iconCache[id] == null && (descriptor.userSerial to info.componentName.packageName) !in invalidated) {
            savedIcons.read(id, info.componentName, sourceDir)?.let { (label, icon) ->
                return AppEntry(id, label, icon, info.componentName, profile, descriptor.userSerial, descriptor.label,
                    !descriptor.isPersonal, available = true, kind = descriptor.kind).also { iconCache[id] = it }
            }
        }
        val label = info.label.toString()
        return iconCache[id]?.takeIf { it.label == label && it.available && it.component == info.componentName } ?: run {
            val icon = runCatching { info.getBadgedIcon(0) }.getOrElse { getApplication<Application>().packageManager.defaultActivityIcon }
            AppEntry(id, label, launcherIcon(icon), info.componentName, profile, descriptor.userSerial, descriptor.label,
                !descriptor.isPersonal, available = true, kind = descriptor.kind).also {
                iconCache[id] = it
                if (canSave) unsavedIcons += SavedIcons.Saved(id, label, it.icon, info.componentName, sourceDir)
            }
        }
    }

    /** A pinned shortcut's id on Home: its app's package, and "#shortcut:" plus the shortcut's own id. */
    private fun shortcutAppId(info: ShortcutInfo, serial: Long, personalSerial: Long): String =
        profileAppId(ComponentName(info.`package`, SHORTCUT_CLASS_PREFIX + info.id).flattenToString(), serial, personalSerial)

    /** A pinned shortcut's entry: from the icon cache while its label still matches, else loaded and cached. */
    private fun shortcutEntry(info: ShortcutInfo, profile: UserHandle, descriptor: AppProfile, personalSerial: Long, densityDpi: Int): AppEntry {
        val id = shortcutAppId(info, descriptor.userSerial, personalSerial)
        val label = (info.shortLabel ?: info.longLabel ?: "Shortcut").toString()
        return iconCache[id]?.takeIf { it.label == label && it.available } ?: run {
            val icon = runCatching { launcherApps.getShortcutBadgedIconDrawable(info, densityDpi) }.getOrNull()
                ?: getApplication<Application>().packageManager.defaultActivityIcon
            AppEntry(id, label, launcherIcon(icon), ComponentName(info.`package`, SHORTCUT_CLASS_PREFIX + info.id), profile,
                descriptor.userSerial, descriptor.label, !descriptor.isPersonal, available = true, kind = descriptor.kind).also { iconCache[id] = it }
        }
    }

    /**
     * Home's apps and pinned shortcuts, built into the icon cache the full list reads, so none is loaded twice. Before,
     * a start loaded every installed app's icon in turn before Home had any.
     */
    private fun homeEntries(ids: Set<String>, densityDpi: Int, invalidated: Set<Pair<Long, String>>): List<AppEntry> {
        val (_, personalSerial, _, handles, profiles) = appProfiles()
        val own = getApplication<Application>().packageName
        return handles.zip(profiles).filter { (_, descriptor) -> descriptor.available }.flatMap { (profile, descriptor) ->
            val serial = descriptor.userSerial
            val apps = runCatching { launcherApps.getActivityList(null, profile) }.getOrNull().orEmpty().mapNotNull { info ->
                activityId(info, serial, personalSerial)?.takeIf { it in ids && listedInProfile(descriptor.kind, info.applicationInfo?.flags ?: 0) }
                    ?.let { activityEntry(info, it, profile, descriptor, invalidated) }
            }
            val shortcuts = runCatching { launcherApps.getShortcuts(LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED), profile) }.getOrNull().orEmpty()
                .filter { it.isEnabled && it.`package` != own && shortcutAppId(it, serial, personalSerial) in ids }
                .map { shortcutEntry(it, profile, descriptor, personalSerial, densityDpi) }
            apps + shortcuts
        }
    }

    private fun saveCachedApps(apps: List<AppEntry>) {
        val array = JSONArray().also { result -> apps.forEach { app -> result.put(JSONObject()
            .put("id", app.id).put("label", app.label).put("serial", app.userSerial)
            .put("profile", app.profileLabel).put("work", app.isWork).putOpt("kind", storedProfileKind(app.kind))) } }
        appCatalogPrefs.edit().putString("apps", array.toString()).apply()
    }

    private fun initialDock(apps: List<AppEntry>, starter: StarterApps): List<String?> {
        val chosen = mutableSetOf<String>()
        return starter.dock.map { choices ->
            choices.firstNotNullOfOrNull { pkg -> apps.firstOrNull { !it.isWork && it.packageName == pkg && it.id !in chosen }?.id }?.also(chosen::add)
        }
    }

    private fun suggestedPins(apps: List<AppEntry>, dock: List<String?>, starter: StarterApps): List<String> =
        starter.pins.mapNotNull { choices -> choices.firstNotNullOfOrNull { pkg ->
            apps.firstOrNull { !it.isWork && it.packageName == pkg && it.id !in dock }?.id
        } }.distinct()

    fun setPinned(id: String, pinned: Boolean) {
        if (statePayloadInvalid) return
        if (mutable.value.folders.any { id in it.appIds }) return
        mutable.update { old ->
            val enable = pinned && old.apps.any { it.id == id }
            old.copy(homeSlots = if (enable && id in old.leadingSlots) old.homeSlots else pinHomeApp(old.homeSlots, id,
                enable, old.widgetPlacements.flatMapTo(mutableSetOf()) { it.coveredIndices() }.filterTo(mutableSetOf()) { it >= 0 }, old.homeAppRows),
                leadingSlots = if (enable) old.leadingSlots else old.leadingSlots.map { it?.takeUnless(id::equals) },
                canUndoEdit = false)
        }
        persist()
    }

    fun turnOnWork(userSerial: Long): Boolean = runCatching {
        val user = userManager.getUserForSerialNumber(userSerial) ?: return false
        if (userManager.getSerialNumberForUser(Process.myUserHandle()) == userSerial || user !in launcherApps.profiles) return false
        userManager.requestQuietModeEnabled(false, user).also { refresh() }
    }.getOrDefault(false)

    fun setDock(slot: Int, id: String?) {
        if (statePayloadInvalid) return
        if (slot !in mutable.value.dock.indices) return
        if (id != null && (isReservedFolderId(id) || mutable.value.folders.any { id in it.appIds })) return
        mutable.update { old -> old.copy(canUndoEdit = false,
            homeSlots = if (id == null) old.homeSlots else old.homeSlots.map { it?.takeUnless(id::equals) }.dropLastWhile { it == null },
            leadingSlots = if (id == null) old.leadingSlots else old.leadingSlots.map { it?.takeUnless(id::equals) },
            dock = trimmedDock(old.dock.mapIndexed { index, value ->
                when { index == slot -> id; value == id && id != null -> null; else -> value }
            })) }
        persist()
    }

    fun move(id: String, offset: Int) {
        val old = mutable.value
        val from = old.layout.indexOfShortcut(id) ?: return
        val page = homeCellPage(from)
        // Steps over cells in rows Home doesn't show.
        var target = from + offset
        val step = if (offset < 0) -1 else 1
        val range = if (page == -1) homeCellIndex(-1, 0)..homeCellIndex(-1, HOME_CELLS - 1) else 0..maxOf(old.homeSlots.lastIndex, from)
        while (target in range && target != from && !homeCellShown(target, old.homeAppRows)) target += step
        applyDrop(id, DropTarget.Home(target.coerceIn(range)))
    }

    fun applyDrop(id: String, target: DropTarget): Boolean {
        val old = mutable.value
        val folder = old.layout.folder(id)
        if (old.apps.none { it.id == id } && folder == null) return false
        if (target is DropTarget.Folder) return folder == null && addAppToFolder(target.id, id)
        if (folder != null && target !is DropTarget.Home) return false
        return commitLayout(dropApp(old.layout, id, target, old.homeAppRows))
    }

    fun createFolder(firstAppId: String, secondAppId: String, targetIndex: Int, title: String = "Folder"): String? {
        val installed = mutable.value.apps.mapTo(mutableSetOf(), AppEntry::id)
        if (firstAppId !in installed || secondAppId !in installed) return null
        val id = newFolderId()
        return id.takeIf { commitLayout(com.mccal.folio.createFolder(mutable.value.layout, FolderEntry(id, title, emptyList()),
            firstAppId, secondAppId, targetIndex)) }
    }
    fun renameFolder(folderId: String, title: String) = commitLayout(com.mccal.folio.renameFolder(mutable.value.layout, folderId, title))
    fun addAppToFolder(folderId: String, appId: String, index: Int? = null): Boolean {
        if (mutable.value.apps.none { it.id == appId }) return false
        return commitLayout(com.mccal.folio.addAppToFolder(mutable.value.layout, folderId, appId, index))
    }
    /** Several apps into one folder as a single change, so Undo takes them all back. Apps that aren't installed are left out. */
    fun addAppsToFolder(folderId: String, appIds: List<String>): Boolean {
        val installed = mutable.value.apps.mapTo(mutableSetOf()) { it.id }
        return commitLayout(com.mccal.folio.addAppsToFolder(mutable.value.layout, folderId, appIds.filter { it in installed }))
    }
    fun removeAppFromFolder(folderId: String, appId: String, target: DropTarget) =
        commitLayout(com.mccal.folio.removeAppFromFolder(mutable.value.layout, folderId, appId, target, mutable.value.homeAppRows))
    fun moveFolderApp(folderId: String, appId: String, index: Int) =
        commitLayout(com.mccal.folio.moveFolderApp(mutable.value.layout, folderId, appId, index))
    fun sortFolderAlphabetically(folderId: String): Boolean {
        val folder = mutable.value.layout.folder(folderId) ?: return false
        val labels = mutable.value.apps.associate { it.id to it.label }
        val sorted = folder.appIds.sortedBy { labels[it]?.lowercase() ?: "" }
        return commitLayout(com.mccal.folio.reorderFolder(mutable.value.layout, folderId, sorted))
    }
    fun folder(id: String) = mutable.value.layout.folder(id)

    fun applyImportedLayout(preview: LayoutImportPreview): Boolean {
        // Over a damaged layout this is the way back (the notice's Restore): the basic Home shown in its place is
        // nothing to undo to or keep in Layout History.
        val recovering = statePayloadInvalid
        // Before this phone's apps are listed, a backup reads every app as missing; leaving the damaged state then would save an empty Home.
        if (recovering && !appsListed) return false
        val old = mutable.value
        if (!recovering && old.layout == preview.layout && old.coverDock == preview.coverDock && old.innerDock == preview.innerDock &&
            old.labels == preview.labels && old.googleSearch == preview.googleSearch && old.verticalStatus == preview.verticalStatus &&
            old.appNames + preview.appNames == old.appNames &&
            old.iconActions + preview.iconActions == old.iconActions) return false
        if (recovering) leaveDamagedLayout(restoring = true)
        else if (old.layoutHistory && !old.loading) LayoutHistory.add(getApplication(), "Before restoring a backup", old.layout)
        // The backup's widgets get slot numbers nothing here uses, so a stack kept for Undo stays on its own widget.
        val restored = renumberRestoredWidgets(preview.layout,
            maxOf(nextWidgetSlot(), old.widgetPlacements.maxOfOrNull { it.slot + 1 } ?: 0)).first
        val restoredSlots = restored.widgetPlacements.mapTo(mutableSetOf()) { it.slot }
        undoLayout = (old.layout to restored).takeUnless { recovering }
        // A stack on a number the restored layout still uses can't stay on Home, but Undo brings it back whole.
        setAsideStacks = (restored to old.widgetStacks.filterKeys { it in restoredSlots }).takeUnless { recovering }
        undoImportSettings = UndoImportSettings(old.coverDock, old.innerDock, old.labels, old.googleSearch, old.verticalStatus).takeUnless { recovering }
        // A restored name replaces the one on this phone; names this backup says nothing about are left alone.
        val names = old.appNames + preview.appNames
        mutable.value = old.copy(appNames = names, iconActions = old.iconActions + preview.iconActions, apps = old.apps.withAppNames(names),
            homeSlots = restored.slots, leadingSlots = restored.leadingSlots, dock = trimmedDock(restored.dock),
            widgetPlacements = restored.widgetPlacements, folders = restored.folders,
            widgetRestores = restored.widgetRestores, coverDock = preview.coverDock, innerDock = preview.innerDock,
            // Kept for Undo, except on a number a restored widget still uses (only an old overflow widget keeps its number).
            widgetStacks = WidgetStacks.prune(old.widgetStacks, old.widgetPlacements.map { it.slot }.toSet() - restoredSlots),
            labels = preview.labels, googleSearch = preview.googleSearch, verticalStatus = preview.verticalStatus,
            // The backup brings its own presets and Side Bar, so what Full-Width Home remembered no longer applies.
            fullWidthRestore = emptyMap(), error = old.error.takeUnless { recovering },
            editRevision = old.editRevision + 1, canUndoEdit = !recovering)
        persist()
        return true
    }

    fun moveWidget(from: Int, to: Int): Boolean {
        val target = mutable.value.layout.placement(to) ?: return false
        return moveWidgetTo(from, target.page * HOME_CELLS + target.row * GRID_COLUMNS + target.column)
    }
    fun moveWidgetTo(slot: Int, index: Int) = mutable.value.let { commitLayout(moveWidgetMakingRoom(it.layout, slot, index, it.homeAppRows)) }
    fun placeWidgetFreely(slot: Int, column: Float, row: Float) = commitLayout(placeWidgetFreely(mutable.value.layout, slot, column, row))
    fun resizeWidget(slot: Int, spanX: Int, spanY: Int) = mutable.value.let { commitLayout(resizeWidgetMakingRoom(it.layout, slot, spanX, spanY, it.homeAppRows)) }
    fun placeWidget(placement: WidgetPlacement) = commitLayout(placeWidget(mutable.value.layout, placement))
    fun placement(slot: Int) = mutable.value.layout.placement(slot)
    // Never reuse a slot number that a (possibly undoable) stack still refers to.
    fun nextWidgetSlot() = maxOf(mutable.value.widgetPlacements.maxOfOrNull { it.slot } ?: -1,
        mutable.value.widgetStacks.keys.maxOrNull() ?: -1) + 1

    fun stackCards(slot: Int): List<Int> = mutable.value.layout.placement(slot)
        ?.let { WidgetStacks.cards(it.id, mutable.value.widgetStacks[slot]) }.orEmpty()

    /** Adds a bound (or built-in) widget behind the widget at [slot]. */
    fun addToStack(slot: Int, id: Int): Boolean {
        if (statePayloadInvalid) return false
        val placement = mutable.value.layout.placement(slot) ?: return false
        mutable.update { it.copy(widgetStacks = it.widgetStacks + (slot to WidgetStacks.add(it.widgetStacks[slot], placement.id, id))) }
        persist()
        return true
    }

    fun removeFromStack(slot: Int, id: Int) = rearrangeStack(slot) { primary, extras -> WidgetStacks.remove(primary, extras, id) }
    fun showFirstInStack(slot: Int, id: Int) = rearrangeStack(slot) { primary, extras -> WidgetStacks.showFirst(primary, extras, id) }

    private fun rearrangeStack(slot: Int, change: (Int, List<Int>) -> Pair<Int, List<Int>>?): Boolean {
        if (statePayloadInvalid) return false
        val old = mutable.value
        val placement = old.layout.placement(slot) ?: return false
        val (primary, extras) = change(placement.id, old.widgetStacks[slot].orEmpty()) ?: return false
        // Changing which widget a placement shows isn't itself undoable, but it must not take an earlier Undo with it:
        // the removal before it still has bindings waiting on that Undo. So the new id goes into both sides of the Undo.
        val pending = undoLayout.takeIf { old.canUndoEdit && it?.second == old.layout }
        fun HomeLayout.withPrimary() = copy(widgetPlacements = widgetPlacements.map { if (it.slot == slot) it.copy(id = primary) else it })
        val next = old.copy(
            widgetPlacements = old.widgetPlacements.map { if (it.slot == slot) it.copy(id = primary) else it },
            widgetStacks = if (extras.isEmpty()) old.widgetStacks - slot else old.widgetStacks + (slot to extras),
            canUndoEdit = pending != null, editRevision = old.editRevision + 1)
        if (pending != null) {
            val swapped = pending.first.withPrimary() to next.layout
            setAsideStacks = setAsideStacks?.takeIf { it.first == pending.second }?.let { next.layout to it.second }
            undoLayout = swapped
        } else {
            undoLayout = null
            undoImportSettings = null
        }
        mutable.value = next
        persist()
        return true
    }

    fun setStackRotate(value: Boolean) = updateSettings(soon = false) { it.copy(stackRotate = value) }
    fun toggleStackApp(anchor: String, app: String) = updateSettings(soon = false) { it.copy(iconStacks = IconStacks.toggle(it.iconStacks, anchor, app)) }
    fun setAddNewAppsToHome(value: Boolean) = updateSettings(soon = false) { it.copy(addNewAppsToHome = value) }
    fun setLayoutHistory(value: Boolean) = updateSettings(soon = false) { it.copy(layoutHistory = value) }
    fun setDockRecentDots(value: Boolean) = updateSettings(soon = false) { it.copy(dockRecentDots = value) }

    /** Saves the current Home to Layout History when it's on (or always, when [force] is set). */
    fun saveLayoutSnapshot(reason: String, force: Boolean = false) {
        val s = mutable.value
        if ((s.layoutHistory || force) && !s.loading && !statePayloadInvalid) LayoutHistory.add(getApplication(), reason, s.layout)
    }

    /**
     * Restores a Layout History snapshot: the current Home is saved first, apps that are no longer installed and
     * widgets Android no longer has are left out, and Undo is offered like any other layout change.
     */
    fun restoreLayoutSnapshot(snapshot: LayoutSnapshot): Boolean {
        if (mutable.value.loading) return false
        // Over a damaged layout this is the way back, like a restored backup (see applyImportedLayout).
        val recovering = statePayloadInvalid
        saveLayoutSnapshot("Before restoring", force = true)
        val old = mutable.value
        val before = snapshot.layout
        val manager = android.appwidget.AppWidgetManager.getInstance(getApplication())
        val widgets = before.widgetPlacements.filter { it.id < 0 || runCatching { manager.getAppWidgetInfo(it.id) != null }.getOrDefault(false) }
        // Same rules as a refresh: only apps from profiles Android can see right now count as removed (a paused work
        // profile's apps stay), and folders are dissolved properly, so a restored layout always loads again.
        val savedIds = before.slots.filterNotNull() + before.leadingSlots.filterNotNull() + before.dock.filterNotNull() + before.folders.flatMap { it.appIds }
        val removed = removedAppIds(savedIds, old.apps.mapTo(mutableSetOf(), AppEntry::id),
            old.profiles.filter { it.available }.mapTo(mutableSetOf(), AppProfile::userSerial), emptySet(), emptySet(),
            userManager.getSerialNumberForUser(Process.myUserHandle()))
        val next = reconcileFolders(before.copy(widgetPlacements = widgets), removed)
        if (!recovering && old.layout == next) return false
        if (recovering) leaveDamagedLayout(restoring = true)
        undoLayout = (old.layout to next).takeUnless { recovering }
        undoImportSettings = null
        mutable.value = old.copy(homeSlots = next.slots, leadingSlots = next.leadingSlots, dock = trimmedDock(next.dock),
            widgetPlacements = next.widgetPlacements, folders = next.folders, widgetRestores = next.widgetRestores, minPages = next.minPages,
            // Only stacks of the restored widgets: a snapshot widget landing in a reused slot mustn't inherit another stack.
            widgetStacks = WidgetStacks.prune(old.widgetStacks, next.widgetPlacements.map { it.slot }.toSet()),
            error = old.error.takeUnless { recovering }, editRevision = old.editRevision + 1, canUndoEdit = !recovering)
        persist()
        return true
    }
    fun setSystemWallpaper(value: Boolean) = updateSettings(soon = false) { it.copy(systemWallpaper = value) }
    fun setHomeInk(value: String) = updateSettings(soon = false) { it.copy(homeInk = value) }
    /** A package installed an effect (or an update replaced it): one entry per package id. */
    fun setHomeScrim(value: Boolean) = updateSettings(soon = false) { it.copy(homeScrim = value) }
    fun setDockMagnify(value: Boolean) = updateSettings(soon = false) { it.copy(dockMagnify = value) }
    fun setAppPanels(value: Boolean) = updateSettings(soon = false) { it.copy(appPanels = value) }
    fun setHaptics(value: Boolean) = updateSettings(soon = false) { it.copy(haptics = value) }
    fun setPageScrub(value: Boolean) = updateSettings(soon = false) { it.copy(pageScrub = value) }
    fun setReduceMotion(value: Boolean) = updateSettings(soon = false) { it.copy(reduceMotion = value) }
    fun setWallpaperMotion(value: Boolean) = updateSettings(soon = false) { it.copy(wallpaperMotion = value) }
    fun setLiveIcons(value: Boolean) = updateSettings(soon = false) { it.copy(liveIcons = value) }
    fun setTriggerAction(trigger: FolioTrigger, action: FolioAction) = updateSettings(soon = false) {
        it.copy(triggerActions = if (action == FolioAction.NONE) it.triggerActions - trigger.name else it.triggerActions + (trigger.name to action.name))
    }
    fun removePlacement(source: DropTarget) = commitLayout(removePlacement(mutable.value.layout, source))
    private fun commitLayout(requested: HomeLayout): Boolean {
        if (statePayloadInvalid) return false
        // The dock is as big as what is in it (four at least), so a place left empty at its end closes.
        val next = requested.copy(dock = trimmedDock(requested.dock))
        val old = mutable.value
        if (old.layout == next) return false
        undoLayout = old.layout to next
        undoImportSettings = null
        mutable.value = old.copy(homeSlots = next.slots, leadingSlots = next.leadingSlots, dock = next.dock,
            widgetPlacements = next.widgetPlacements, folders = next.folders,
            widgetRestores = next.widgetRestores,
            // Keep stacks of the undoable previous layout too, so undoing a removal brings the whole stack back.
            widgetStacks = WidgetStacks.prune(old.widgetStacks, (next.widgetPlacements + old.widgetPlacements).map { it.slot }.toSet()),
            editRevision = old.editRevision + 1, canUndoEdit = true)
        persist()
        return true
    }

    fun undoEdit(): Boolean {
        val (before, after) = undoLayout ?: return false
        val old = mutable.value
        if (!old.canUndoEdit || old.layout != after) return false
        val installed = (old.apps.map { it.id } + before.folders.map { it.id }).toSet()
        val settings = undoImportSettings
        mutable.value = old.copy(homeSlots = reconcileHomeSlots(before.slots, installed),
            leadingSlots = before.leadingSlots.map { it?.takeIf(installed::contains) },
            dock = trimmedDock(before.dock.map { it?.takeIf(installed::contains) }), widgetPlacements = before.widgetPlacements, folders = before.folders,
            widgetStacks = WidgetStacks.prune(old.widgetStacks + stacksForUndo(), before.widgetPlacements.map { it.slot }.toSet()),
            widgetRestores = before.widgetRestores, coverDock = settings?.coverDock ?: old.coverDock,
            innerDock = settings?.innerDock ?: old.innerDock, labels = settings?.labels ?: old.labels,
            googleSearch = settings?.googleSearch ?: old.googleSearch, verticalStatus = settings?.verticalStatus ?: old.verticalStatus,
            canUndoEdit = false,
            editRevision = old.editRevision + 1)
        undoLayout = null
        undoImportSettings = null
        setAsideStacks = null
        persist()
        return true
    }
    fun setLabels(value: Boolean) { if (statePayloadInvalid) return; undoLayout = null; undoImportSettings = null; mutable.update { it.copy(labels = value, canUndoEdit = false) }; persist() }
    /** Adds an empty Home page after the last one; returns its index. */
    fun addPage(): Int { if (statePayloadInvalid) return -1; val next = state.value.homePages
        undoLayout = null; mutable.update { it.copy(minPages = (next + 1).coerceAtMost(20), canUndoEdit = false) }; persist(); return next }
    /** Removes the last page when it has no apps or widgets. */
    fun removeLastEmptyPage(): Boolean { if (statePayloadInvalid) return false; val s = state.value
        if (s.homePages <= 1 || s.layout.contentPageCount >= s.homePages) return false
        undoLayout = null; mutable.update { it.copy(minPages = maxOf(1, it.homePages - 1), canUndoEdit = false) }; persist(); return true }
    fun setSpotlightSection(section: String, visible: Boolean) = updateSettings(soon = false) {
        it.copy(spotlightHidden = if (visible) it.spotlightHidden - section else it.spotlightHidden + section) }
    fun setSearchEngine(engine: String) = updateSettings(soon = false) { it.copy(searchEngine = engine) }
    fun setSearchCustomUrl(url: String) = updateSettings(soon = true) { it.copy(searchCustomUrl = url.take(500)) }
    fun setLibraryCategories(value: Boolean) = updateSettings(soon = false) { it.copy(libraryCategories = value) }
    fun setLibraryWork(value: Boolean) = updateSettings(soon = false) { it.copy(libraryWork = value) }
    fun setIconPack(pack: String?) = updateSettings(soon = false) { it.copy(iconPack = pack) }
    fun setBadgeStyle(style: BadgeStyle) = updateSettings(soon = false) { it.copy(badgeStyle = style) }
    /** Clear Badges When Opened; turning it off forgets the counts it remembered, so every badge comes back. */
    fun setBadgesWhenOpened(value: Boolean) = updateSettings(soon = false) {
        it.copy(badgesWhenOpened = value, badgesSeen = if (value) it.badgesSeen else emptyMap()) }
    /** An app was opened: the [count] showing on it now is seen, so its badge stays away until a new notification. */
    fun noteBadgeSeen(packageName: String, count: Int) = updateSettings(soon = true) {
        it.copy(badgesSeen = if (count <= 0) it.badgesSeen - packageName else it.badgesSeen + (packageName to count)) }
    /** Keeps the seen counts in step with the notifications showing now (see [BadgesWhenOpened.trimmed]). */
    fun trimBadgesSeen(counts: Map<String, Int>) {
        val seen = mutable.value.badgesSeen
        if (seen.isEmpty()) return
        val trimmed = BadgesWhenOpened.trimmed(counts, seen)
        if (trimmed != seen) updateSettings(soon = true) { it.copy(badgesSeen = trimmed) }
    }
    fun setSwipeDownHome(value: String) = updateSettings(soon = false) { it.copy(swipeDownHome = value) }
    fun setMessagesApp(pkg: String?) = updateSettings(soon = false) { it.copy(messagesApp = pkg) }
    fun setFolderColor(folderId: String, color: Long?) = updateSettings(soon = false) {
        it.copy(folderColors = if (color == null) it.folderColors - folderId else it.folderColors + (folderId to color)) }
    // soon = true: the weight slider's onValueChange fires on every pixel of drag, each one a call here - soon
    // coalesces those into one write after they stop (persistSoon's own purpose), instead of a synchronous
    // persist() blocking the UI thread on every tick, which is what made the slider look stuck.
    fun setButtonBar(value: Boolean) = updateSettings(soon = false) { it.copy(buttonBar = value) }

    fun setDockEverywhere(value: Boolean) = updateSettings(soon = false) { it.copy(dockEverywhere = value) }
    /** Remembers how many app rows fit this screen class's full-screen Home (see [effectiveHomeRows]). */
    fun recordHomeFit(screen: LayoutScreen, rows: Int) {
        val value = rows.coerceIn(BASE_APP_ROWS, MAX_APP_ROWS)
        val old = mutable.value
        if (old.homeFitFor(screen) == value) return
        updateSettings(soon = false) { it.withHomeFit(screen, value) }
    }
    fun setFoldEffect(value: Boolean) = updateSettings(soon = false) { it.copy(foldEffect = value) }
    fun setStayAwakeOnFold(value: Boolean) = updateSettings(soon = false) { it.copy(stayAwakeOnFold = value) }
    fun setHidden(id: String, hidden: Boolean) = updateSettings(soon = false) { it.copy(hiddenApps = if (hidden) it.hiddenApps + id else it.hiddenApps - id) }
    /** Renames one app everywhere it appears; a blank name puts the name Android reports back. */
    fun setAppIconStyle(id: String, override: AppIconOverride) {
        // A picture that is no longer chosen (Reset Icon, Remove Picture) is deleted, so nothing stale is kept.
        if (!override.hasPicture && mutable.value.appIconStyles[id]?.hasPicture == true) AppIconPictures.delete(getApplication(), id)
        updateSettings(soon = false) { it.copy(appIconStyles = editAppIcon(it.appIconStyles, id, override)) }
    }
    fun setIconActions(id: String, actions: IconActions) = updateSettings(soon = false) { it.copy(iconActions = editIconActions(it.iconActions, id, actions)) }
    fun renameApp(id: String, name: String) = updateSettings(soon = false) { s ->
        val names = editAppName(s.appNames, id, name)
        s.copy(appNames = names, apps = s.apps.withAppNames(names))
    }
    fun setLeftHanded(value: Boolean) = updateSettings(soon = false) { it.copy(leftHanded = value) }
    fun setVerticalStatus(value: Boolean) { if (statePayloadInvalid) return; undoLayout = null; undoImportSettings = null; mutable.update { it.copy(verticalStatus = value, canUndoEdit = false) }; persist() }
    fun setGoogleSearch(value: Boolean) { if (statePayloadInvalid) return; undoLayout = null; undoImportSettings = null; mutable.update { it.copy(googleSearch = value, canUndoEdit = false) }; persist() }
    fun setDockPlacement(screen: LayoutScreen, value: DockPlacement) {
        if (statePayloadInvalid) return
        undoLayout = null; undoImportSettings = null
        mutable.update { it.withDock(screen, value).copy(canUndoEdit = false) }
        persist()
    }

    /** Full-Width Home for [screen] on or off, as one change that remembers and restores what it replaced. */
    fun setFullWidthHome(screen: LayoutScreen, on: Boolean) {
        if (statePayloadInvalid) return
        undoLayout = null; undoImportSettings = null
        mutable.update { it.withFullWidthHome(screen, on).copy(canUndoEdit = false) }
        persist()
    }

    val retainedWidgetIds: Set<Int> get() {
        val state = mutable.value
        val undoPlacements = if (state.canUndoEdit) undoLayout?.first?.widgetPlacements.orEmpty() else emptyList()
        val placements = state.widgetPlacements + undoPlacements
        val slots = placements.map { it.slot }.toSet()
        val setAside = if (state.canUndoEdit) WidgetStacks.retained(stacksForUndo(), undoPlacements.map { it.slot }.toSet()) else emptySet()
        // Stacked widgets are retained exactly as long as their placement (or its undoable copy) exists.
        return (placements.map { it.id }.filter { it >= 0 } + WidgetStacks.retained(state.widgetStacks, slots) + setAside).toSet()
    }
    val canPruneWidgetIds get() = !statePayloadInvalid
    fun setWidget(slot: Int, id: Int) {
        if (statePayloadInvalid) return
        val old = mutable.value
        val existing = old.layout.placement(slot)
        val next = when {
            id == EMPTY_WIDGET -> removePlacement(old.layout, DropTarget.Widget(slot))
            existing != null -> old.layout.copy(widgetPlacements = old.widgetPlacements.map { if (it.slot == slot) it.copy(id = id) else it },
                widgetRestores = old.widgetRestores.filterNot { it.slot == slot })
            else -> placeWidget(old.layout, migrateSchema5Widgets(List(slot) { EMPTY_WIDGET } + id).single())
        }
        mutable.update { it.copy(widgetPlacements = next.widgetPlacements, widgetRestores = next.widgetRestores,
            canUndoEdit = false, editRevision = it.editRevision + 1) }
        undoLayout = null
        undoImportSettings = null
        persist()
    }

    internal fun restoreLayout(layout: HomeLayout) {
        if (statePayloadInvalid) return
        val installed = (mutable.value.apps.map { it.id } + layout.folders.map { it.id }).toSet()
        mutable.update { it.copy(homeSlots = reconcileHomeSlots(layout.slots, installed),
            leadingSlots = layout.slotsForPage(-1).map { id -> id?.takeIf { it in installed || isFolderId(it) } },
            dock = trimmedDock(layout.dock.map { id -> id?.takeIf { it in installed || isFolderId(it) } }), widgetPlacements = layout.widgetPlacements,
            folders = layout.folders, widgetRestores = layout.widgetRestores, widgetStacks = emptyMap(),
            canUndoEdit = false, editRevision = it.editRevision + 1) }
        undoLayout = null
        undoImportSettings = null
        persist()
    }

    /** Appearance/behaviour settings: no-op while the saved state is unreadable, then saved. */
    private inline fun updateSettings(soon: Boolean, change: (LauncherState) -> LauncherState) {
        if (statePayloadInvalid) return
        mutable.update(change)
        if (soon) persistSoon() else persist()
    }

    private var persistJob: kotlinx.coroutines.Job? = null
    private var persistPending = false
    /** Coalesces rapid changes (sliders) into one write shortly after they stop. */
    private fun persistSoon() {
        persistJob?.cancel()
        persistPending = true
        persistJob = viewModelScope.launch { kotlinx.coroutines.delay(300); persist() }
    }

    /** Writes a change that is still waiting out its delay, so leaving Home within that moment doesn't lose it. */
    fun flushPending() {
        if (!persistPending) return
        persistJob?.cancel()
        persist()
    }

    /** Whether the saved Home layout couldn't be read (it's kept untouched and editing is paused until the user decides). */
    val layoutDamaged get() = statePayloadInvalid

    /**
     * Whether the layout that couldn't be read was saved by a newer Folio (a downgrade). It isn't broken: updating Folio
     * reads it again, so Home offers no Start Fresh for it.
     */
    val layoutFromNewerFolio get() = statePayloadInvalid && sourceSchema > STATE_SCHEMA

    /** Starts over with a default Home after a damaged save; the damaged copy stays as `state_damaged_backup`. */
    fun resetDamagedLayout() {
        if (!statePayloadInvalid) return
        leaveDamagedLayout(restoring = false)
        mutable.update { it.copy(error = null) }
        persist()
        refresh()
    }

    /**
     * Keeps the damaged save as `state_damaged_backup` and lets Home save again. A restored layout replaces what was
     * loaded, so there is nothing left to migrate either, and a refresh that follows must not migrate it.
     */
    private fun leaveDamagedLayout(restoring: Boolean) {
        // Once, like the schema backups beside it: a second damaged layout must not write over the first one,
        // which may be the only copy of a Home someone spent an evening arranging.
        if (!prefs.contains("state_damaged_backup")) {
            prefs.getString("state", null)?.let { prefs.edit().putString("state_damaged_backup", it).apply() }
        }
        statePayloadInvalid = false
        if (restoring) needsMigration = false
    }

    /**
     * What a Layout Backup saves, or null while the layout is damaged: Home then shows a basic stand-in, and a backup
     * of it would be an empty Home.
     */
    fun stateForBackup(): LauncherState? = mutable.value.takeUnless { statePayloadInvalid }

    private fun persist() {
        persistPending = false
        if (needsMigration || statePayloadInvalid) return
        traced("Folio.persist", ::writeState)
    }

    private fun writeState() {
        val s = mutable.value
        fun preset(p: DockPlacement) = JSONObject().put("dockPlacement", p.name)
        val widgets = JSONArray().also { array -> s.widgetPlacements.forEach { w -> array.put(JSONObject()
            .put("slot", w.slot).put("id", w.id).put("page", w.page).put("column", w.column).put("row", w.row)
            .put("spanX", w.spanX).put("spanY", w.spanY)
            .apply { if (w.offsetX != 0f) put("offsetX", w.offsetX.toDouble()); if (w.offsetY != 0f) put("offsetY", w.offsetY.toDouble()) }) } }
        val folders = JSONArray().also { array -> s.folders.forEach { folder -> array.put(JSONObject()
            .put("id", folder.id).put("title", folder.title).put("apps", JSONArray(folder.appIds))) } }
        val restores = JSONArray().also { array -> s.widgetRestores.forEach { restore -> array.put(JSONObject()
            .put("slot", restore.slot).put("provider", restore.providerComponent).put("userSerial", restore.userSerial)
            .put("title", restore.title).put("profileLabel", restore.profileLabel).put("work", restore.isWork)
            .put("sourceScope", restore.sourceScope)) } }
        val data = JSONObject().put("schema", stateSchemaFor(s)).put("pinned", JSONArray(s.order)).put("homeSlots", JSONArray(s.homeSlots))
            .put("leadingSlots", JSONArray(s.leadingSlots)).put("dock", JSONArray(s.dock))
            .put("widgets", widgets).put("labels", s.labels)
            .put("folders", folders)
            .put("restores", restores)
            .put("googleSearch", s.googleSearch)
            .put("verticalStatus", s.verticalStatus)
            .also { o -> if (s.fullWidthRestore.isNotEmpty()) o.put("fullWidthRestore", JSONObject(s.fullWidthRestore)) }
            .put(SettingKeys.LEFT_HANDED, s.leftHanded)
            .put("hiddenApps", JSONArray(s.hiddenApps.toList()))
            .put("minPages", s.minPages)
            .put("foldEffect", s.foldEffect).put("stayAwakeOnFold", s.stayAwakeOnFold)
            .put("searchCustomUrl", s.searchCustomUrl).put("spotlightHidden", JSONArray(s.spotlightHidden.toList())).put("searchEngine", s.searchEngine)
            .put("libraryCategories", s.libraryCategories).put("libraryWork", s.libraryWork)
            .put("iconPack", s.iconPack ?: JSONObject.NULL).put("badgeStyle", s.badgeStyle.name).put("badgesWhenOpened", s.badgesWhenOpened)
            .put("badgesSeen", JSONObject().apply { s.badgesSeen.forEach { (pkg, count) -> put(pkg, count) } }).put("searchPill", s.searchPill).put("homeStrip", s.homeStrip.key).put("swipeDownHome", s.swipeDownHome).put("messagesApp", s.messagesApp ?: JSONObject.NULL)
            .put("widgetStacks", JSONObject().apply { s.widgetStacks.forEach { (slot, ids) -> put(slot.toString(), JSONArray(ids)) } })
            .put("stackRotate", s.stackRotate).put("addNewAppsToHome", s.addNewAppsToHome)
            .put("layoutHistory", s.layoutHistory).put("dockRecentDots", s.dockRecentDots)
            .put("systemWallpaper", s.systemWallpaper).put("homeInk", s.homeInk)
            .put("homeScrim", s.homeScrim)
            .put("dockMagnify", s.dockMagnify).put("appPanels", s.appPanels).put("haptics", s.haptics)
            .put("pageScrub", s.pageScrub).put("reduceMotion", s.reduceMotion).put("wallpaperMotion", s.wallpaperMotion).put("liveIcons", s.liveIcons)
            .put("triggerActions", JSONObject().apply { s.triggerActions.forEach { (k, v) -> put(k, v) } })
            .put("folderColors", JSONObject().apply { s.folderColors.forEach { (id, c) -> put(id, c) } })
            .put("iconStacks", JSONObject().apply { s.iconStacks.forEach { (id, apps) -> put(id, JSONArray(apps)) } })
            .put("appNames", JSONObject().apply { s.appNames.forEach { (id, name) -> put(id, name) } })
            .put("appIconStyles", appIconStylesToJson(s.appIconStyles))
            // Only when there are some, so a save from before this existed keeps its shape.
            .also { o -> if (s.iconActions.isNotEmpty()) o.put("iconActions", iconActionsToJson(s.iconActions)) }
            .put(SettingKeys.DOCK_EVERYWHERE, s.dockEverywhere)
            .put(SettingKeys.BUTTON_BAR, s.buttonBar)
            .put("compact", preset(s.coverDock)).put("expanded", preset(s.innerDock))
            .put("homeFitCompact", s.homeFitCompact).put("homeFitExpanded", s.homeFitExpanded).put("homeFitUpright", s.homeFitUpright)
        val editor = prefs.edit()
        if (legacyRaw != null && sourceSchema == 2 && !prefs.contains("state_v2_backup"))
            editor.putString("state_v2_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 4 && !prefs.contains("state_v3_backup"))
            editor.putString("state_v3_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 5 && !prefs.contains("state_v4_backup"))
            editor.putString("state_v4_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 6 && !prefs.contains("state_v5_backup"))
            editor.putString("state_v5_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 7 && !prefs.contains("state_v6_backup"))
            editor.putString("state_v6_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 8 && !prefs.contains("state_v7_backup"))
            editor.putString("state_v7_backup", legacyRaw)
        if (legacyRaw != null && sourceSchema < 9 && !prefs.contains("state_v8_backup"))
            editor.putString("state_v8_backup", legacyRaw)
        // The first save that writes schema 10 (one with icon actions) keeps the schema 9 file it replaces, like every step before it.
        if (legacyRaw != null && v9BackupNeeded(sourceSchema, stateSchemaFor(s), prefs.contains("state_v9_backup")))
            editor.putString("state_v9_backup", legacyRaw)
        editor.putString("state", data.toString()).putBoolean("initialized", true)
            // Kept beside the state so a restore that changes it chooses the right window next time.
            .putBoolean(SettingKeys.SYSTEM_WALLPAPER, s.systemWallpaper).apply()
    }

    private fun load(): LauncherState = traced("Folio.decodeState") { runCatching {
        decodeLauncherState(prefs.getString("state", "{}") ?: "{}", legacyRaw)
    }.getOrElse {
        statePayloadInvalid = legacyRaw != null
        LauncherState(loading = false, error = getApplication<Application>().getString(
            if (statePayloadInvalid && sourceSchema > STATE_SCHEMA) R.string.home_layout_saved_by_newer_folio_short
            else R.string.saved_home_layout_could_not_be_read))
    } }

    override fun onCleared() {
        flushPending()
        launcherApps.unregisterCallback(callback)
        if (FolioSettingsBridge.liveModel?.get() === this) FolioSettingsBridge.liveModel = null
    }
}

/**
 * Render adaptive layers through our rounded-square mask, preserving original app artwork. Drawing it differently
 * needs [SavedIcons.FORMAT] bumped, or starts keep showing icons saved the old way.
 */
private fun launcherIcon(drawable: Drawable): Bitmap {
    if (drawable !is AdaptiveIconDrawable) return drawable.toBitmap(144, 144)
    val bitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, 144, 144)
    drawable.background?.draw(canvas)
    drawable.foreground?.draw(canvas)
    val outside = Path().also { ContinuousCorners.trace(144f, 144f, 144f * ICON_CORNER_FRACTION, it::moveTo, it::lineTo, it::cubicTo); it.close() }
    outside.fillType = Path.FillType.INVERSE_WINDING
    canvas.drawPath(outside, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) })
    return bitmap
}

/**
 * Reads Folio's saved state (any schema it knows), upgrading older layouts. Throws when the saved text is damaged, so
 * the caller can keep it untouched and tell the user. [legacyRaw] is the pre-schema-2 payload, when there was one.
 */
internal fun decodeLauncherState(raw: String, legacyRaw: String?): LauncherState {
    val j = JSONObject(raw)
    fun preset(key: String) = DockPlacement.parse(j.optJSONObject(key)?.optString("dockPlacement"))
    val order = j.optJSONArray(if (j.optInt("schema", 1) >= 2) "pinned" else "order") ?: JSONArray()
    val cells = j.optJSONArray("homeSlots").takeIf { j.optInt("schema", 1) >= 4 } ?: order
    val schema = j.optInt("schema", 1)
    require(schema <= STATE_SCHEMA) { "Unsupported saved-state schema $schema" }
    // Schemas 6–8 saved 24-cell pages; the same cells keep their place on 36-cell pages (More rows).
    val legacyGrid = schema in 6..8
    val rawSlots = List(cells.length()) { cells.optString(it).takeIf { id -> id.isNotBlank() && id != "null" } }
        .let { if (legacyGrid) migrateLegacyHomeSlots(it) else it }
    val legacySlots = normalizeHomeSlots(rawSlots)
    val rawLeadingSlots = if (schema >= 8) {
        val leading = j.optJSONArray("leadingSlots") ?: error("Schema 8 requires a leading slot array")
        val size = if (legacyGrid) LEGACY_HOME_CELLS else HOME_CELLS
        require(leading.length() == size)
        migrateLegacyLeadingSlots(List(size) { leading.optString(it).takeIf { id -> id.isNotBlank() && id != "null" } })
    } else List(HOME_CELLS) { null }
    val dockArray = j.optJSONArray("dock")
    // Four slots, or as many as were saved up to the most the dock holds; a damaged length can never be fewer than four.
    val loadedDock = trimmedDock(List((dockArray?.length() ?: MIN_DOCK_SLOTS).coerceIn(MIN_DOCK_SLOTS, MAX_DOCK_SLOTS)) { dockArray?.optString(it)?.takeIf { it.isNotBlank() && it != "null" } })
    val widgetArray = j.optJSONArray("widgets")
    val placements = if (schema >= 6) {
        require(widgetArray != null) { "Schema $schema requires a widget placement array" }
        fun strictInt(objectValue: JSONObject, key: String): Int {
            val number = objectValue.get(key) as? Number ?: error("$key must be an integer")
            val value = number.toDouble()
            require(value.isFinite() && value % 1.0 == 0.0 && value >= Int.MIN_VALUE && value <= Int.MAX_VALUE) {
                "$key must be a finite integer"
            }
            return value.toInt()
        }
        List(widgetArray.length()) { index ->
            val w = widgetArray.getJSONObject(index)
            WidgetPlacement(strictInt(w, "slot"), strictInt(w, "id"), strictInt(w, "page"), strictInt(w, "column"), strictInt(w, "row"),
                strictInt(w, "spanX"), strictInt(w, "spanY"),
                w.optDouble("offsetX", 0.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-MAX_WIDGET_OFFSET, MAX_WIDGET_OFFSET) ?: 0f,
                w.optDouble("offsetY", 0.0).toFloat().takeIf { it.isFinite() }?.coerceIn(-MAX_WIDGET_OFFSET, MAX_WIDGET_OFFSET) ?: 0f).let { if (legacyGrid) migrateLegacyWidgetPlacement(it) else it }
        }.also { loaded ->
            require(loaded.map { it.slot }.distinct().size == loaded.size) { "Widget placement slots must be unique" }
            loaded.forEach { placement ->
                val baseGeometry = placement.slot >= 0 && placement.id != EMPTY_WIDGET && placement.page >= -1 &&
                    placement.column >= 0 && placement.row >= 0 && placement.spanX in 1..GRID_COLUMNS &&
                    placement.spanY in 1..GRID_ROWS && placement.column + placement.spanX <= GRID_COLUMNS
                val insideGrid = placement.row + placement.spanY <= GRID_ROWS
                val migratedOverflow = placement.page > 0 && placement.slot / 3 == placement.page && placement.slot % 3 == 2 &&
                    placement.column == 0 && placement.row == GRID_ROWS && placement.spanX == GRID_COLUMNS && placement.spanY == 4
                require(baseGeometry && (insideGrid || migratedOverflow)) { "Invalid widget placement" }
            }
        }
    } else {
        val ids = if (schema < 5) List(3) { index ->
            (widgetArray?.optInt(index, -1) ?: -1).let {
                if (it < 0) listOf(CLOCK_WIDGET, DATE_WIDGET, INFO_WIDGET)[index] else it
            }
        } else List(widgetArray?.length() ?: 0) { widgetArray!!.optInt(it, EMPTY_WIDGET) }
        migrateSchema5Widgets(ids)
    }.filterNot { placement -> schema < 8 && placement == WidgetPlacement(2, INFO_WIDGET, -1, 0, 0, 4, 6) }
    val folders = if (schema >= 7) {
        val array = j.optJSONArray("folders") ?: error("Schema 7 requires a folder array")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val apps = item.getJSONArray("apps")
            FolderEntry(item.getString("id"), item.getString("title"), List(apps.length()) { apps.getString(it) })
        }.also { loaded ->
            require(loaded.map(FolderEntry::id).distinct().size == loaded.size)
            require(loaded.flatMap(FolderEntry::appIds).distinct().size == loaded.sumOf { it.appIds.size })
            loaded.forEach { folder ->
                require(isFolderId(folder.id) && folder.title.isNotBlank() && folder.appIds.size >= 2)
                require(folder.appIds.none { it.isBlank() || isReservedFolderId(it) })
            }
            val children = loaded.flatMapTo(mutableSetOf(), FolderEntry::appIds)
            val folderIds = loaded.mapTo(mutableSetOf(), FolderEntry::id)
            val rawFolderRefs = (rawSlots + rawLeadingSlots).filterNotNull().filter(::isReservedFolderId)
            require(rawFolderRefs.all(::isFolderId))
            require(rawFolderRefs.size == folderIds.size && rawFolderRefs.toSet() == folderIds)
            require((rawSlots + rawLeadingSlots).none { it in children } &&
                loadedDock.none { it in children || (it != null && isReservedFolderId(it)) })
        }
    } else emptyList()
    if (schema >= 8) {
        val leadingIds = rawLeadingSlots.filterNotNull()
        require(leadingIds.distinct().size == leadingIds.size) {
            "An unfolded-only shortcut appears more than once"
        }
        val leadingApps = leadingIds.filterNot(::isReservedFolderId)
        val otherApps = rawSlots.filterNotNull().filterNot(::isReservedFolderId) +
            loadedDock.filterNotNull() + folders.flatMap(FolderEntry::appIds)
        require(leadingApps.none { it in otherApps }) {
            "An unfolded-only app shortcut appears on another surface"
        }
        val occupiedLeadingCells = rawLeadingSlots.indices
            .filterTo(mutableSetOf()) { rawLeadingSlots[it] != null }
            .mapTo(mutableSetOf()) { homeCellIndex(-1, it) }
        require(placements.filter { it.page == -1 }.none { placement ->
            placement.coveredIndices().any { it in occupiedLeadingCells }
        }) { "An unfolded-only shortcut overlaps a widget" }
    }
    val restores = if (schema >= 7) {
        val array = j.optJSONArray("restores") ?: JSONArray()
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            WidgetRestore(item.getInt("slot"), item.getString("provider"), item.getLong("userSerial"),
                item.getString("title"), item.getString("profileLabel"), item.optBoolean("work", false),
                item.optString("sourceScope").takeIf { it.isNotBlank() && it != "null" })
        }.also { loaded ->
            require(loaded.map(WidgetRestore::slot).distinct().size == loaded.size)
            loaded.forEach { restore ->
                require(restore.slot >= 0 && restore.userSerial >= 0 && restore.title.isNotBlank() &&
                    restore.profileLabel.isNotBlank() && ComponentName.unflattenFromString(restore.providerComponent) != null)
            }
            require(placements.filter { it.id == NEEDS_BINDING_WIDGET }.map { it.slot }.toSet() == loaded.map { it.slot }.toSet())
        }
    } else emptyList()
    return LauncherState(homeSlots = if (schema in 2..5) migrateSchema5Apps(legacySlots) else legacySlots,
        leadingSlots = rawLeadingSlots,
        dock = loadedDock,
        widgetPlacements = placements, folders = folders, widgetRestores = restores,
        googleSearch = j.optBoolean("googleSearch", true),
        labels = j.optBoolean("labels", true), coverDock = preset("compact"),
        innerDock = preset("expanded"), fullWidthRestore = j.optJSONObject("fullWidthRestore")?.let { o -> o.keys().asSequence().associateWith { o.optString(it) }.filterValues(String::isNotBlank) }.orEmpty(),
        verticalStatus = j.optBoolean("verticalStatus", true),
        leftHanded = j.optBoolean("leftHanded", false),
        hiddenApps = j.optJSONArray("hiddenApps")?.let { a -> (0 until a.length()).map(a::getString).toSet() } ?: emptySet(),
        minPages = j.optInt("minPages", 1).coerceIn(1, 20),
        foldEffect = j.optBoolean("foldEffect", true),
        stayAwakeOnFold = j.optBoolean("stayAwakeOnFold", true),
        spotlightHidden = j.optJSONArray("spotlightHidden")?.let { a -> (0 until a.length()).map(a::getString).toSet() } ?: emptySet(),
        searchEngine = j.optString("searchEngine", "GOOGLE").takeIf { it == CUSTOM_SEARCH || runCatching { WebSearchTarget.valueOf(it) }.isSuccess } ?: "GOOGLE",
        searchCustomUrl = j.optString("searchCustomUrl", "").take(500),
        libraryCategories = j.optBoolean("libraryCategories", true),
        libraryWork = j.optBoolean("libraryWork", true),
        iconPack = j.optString("iconPack").takeIf { it.isNotBlank() && it != "null" },
        badgeStyle = runCatching { BadgeStyle.valueOf(j.optString("badgeStyle")) }.getOrDefault(BadgeStyle.DOT),
        badgesWhenOpened = j.optBoolean("badgesWhenOpened", false),
        badgesSeen = j.optJSONObject("badgesSeen")?.let { o -> o.keys().asSequence().associateWith { o.getInt(it) }.filterValues { it > 0 } } ?: emptyMap(),
        homeStrip = HomeStrip.DOTS,
        // Up to 0.6.0 this was a switch for Spotlight alone.
        swipeDownHome = j.optString("swipeDownHome", "").ifBlank { if (j.optBoolean("swipeDownSearch", true)) "SPOTLIGHT" else "OFF" },
        messagesApp = j.optString("messagesApp").takeIf { it.isNotBlank() && it != "null" },
        widgetStacks = j.optJSONObject("widgetStacks")?.let { o -> o.keys().asSequence().mapNotNull { key ->
            val ids = o.optJSONArray(key) ?: return@mapNotNull null
            key.toIntOrNull()?.let { slot -> slot to (0 until ids.length()).map(ids::getInt) }
        }.toMap() } ?: emptyMap(),
        stackRotate = j.optBoolean("stackRotate", true),
        addNewAppsToHome = j.optBoolean("addNewAppsToHome", false),
        layoutHistory = j.optBoolean("layoutHistory", false), dockRecentDots = j.optBoolean("dockRecentDots", false),
        systemWallpaper = j.optBoolean("systemWallpaper", false),
        homeInk = j.optString("homeInk", "AUTO").takeIf { it in setOf("AUTO", "LIGHT", "DARK") } ?: "AUTO",
        homeScrim = j.optBoolean("homeScrim", true),
        dockMagnify = j.optBoolean("dockMagnify", false), appPanels = j.optBoolean("appPanels", false), haptics = j.optBoolean("haptics", true),
        pageScrub = j.optBoolean("pageScrub", true), reduceMotion = j.optBoolean("reduceMotion", false),
        wallpaperMotion = j.optBoolean("wallpaperMotion", true), liveIcons = j.optBoolean("liveIcons", true),
        triggerActions = j.optJSONObject("triggerActions")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } } ?: emptyMap(),
        folderColors = j.optJSONObject("folderColors")?.let { o -> o.keys().asSequence().associateWith { o.getLong(it) } } ?: emptyMap(),
        // Held to the same shape as a name typed into the rename field: settings written by an older build, or a
        // file someone edited by hand, shouldn't be able to hang a paragraph under an icon.
        appNames = j.optJSONObject("appNames")?.let { o -> o.keys().asSequence()
            .associateWith { o.optString(it).trim().takeAppName() }
            .filterValues(String::isNotBlank) } ?: emptyMap(),
        appIconStyles = appIconStylesFromJson(j.optJSONObject("appIconStyles")),
        iconActions = iconActionsFromJson(j.optJSONObject("iconActions")),
        iconStacks = j.optJSONObject("iconStacks")?.let { o -> o.keys().asSequence().associateWith { key ->
            o.optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) } }.orEmpty()
        }.filterValues { it.isNotEmpty() } } ?: emptyMap(),
        dockEverywhere = j.optBoolean("dockEverywhere", false),
        buttonBar = j.optBoolean(SettingKeys.BUTTON_BAR, false),
        homeFitCompact = j.optInt("homeFitCompact", 0).takeIf { it in BASE_APP_ROWS..MAX_APP_ROWS } ?: 0,
        homeFitExpanded = j.optInt("homeFitExpanded", 0).takeIf { it in BASE_APP_ROWS..MAX_APP_ROWS } ?: 0,
        homeFitUpright = j.optInt("homeFitUpright", 0).takeIf { it in BASE_APP_ROWS..MAX_APP_ROWS } ?: 0)
        .withoutFullWidthEntry(LayoutScreen.INNER_UPRIGHT)
}

/**
 * Android counts changes to installed themes and overlays (Samsung's Theme Park among them) in the configuration's
 * assets sequence. It isn't public API, but the configuration prints it as "as.N", so a theme that swaps icons without
 * a package change still clears the icon cache, while folding and rotating don't. 0 when it isn't there.
 */
internal fun assetsSequence(configuration: String): Int =
    Regex("""\bas\.(\d+)""").find(configuration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
