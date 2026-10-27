package com.mccal.folio

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import android.widget.RemoteViews
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

internal data class WidgetCatalogEntry(
    val provider: AppWidgetProviderInfo,
    val providerLabel: String,
    val appLabel: String,
    val description: String,
    val userSerial: Long = 0,
    val profileLabel: String,
    val isWork: Boolean = false,
    /** The app's icon, for the app row over the gallery; null if Android won't give one. */
    val appIcon: Bitmap? = null,
)

internal data class WidgetPickerSession(
    val provider: AppWidgetProviderInfo?,
    val slot: Int,
    val span: WidgetSpan,
    val pointer: Offset,
    val dragging: Boolean,
    val targetIndex: Int? = null,
    val candidate: WidgetPlacement? = null,
    val builtinId: Int? = null,
)

internal fun widgetCatalog(context: Context, providers: List<AppWidgetProviderInfo>, profile: AppProfile): List<WidgetCatalogEntry> {
    val pm = context.packageManager
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    // One icon per app, not per widget: the pack with 430 widgets is still one icon.
    val icons = HashMap<String, Bitmap?>()
    return providers.map { provider ->
        val packageName = provider.provider.packageName
        val appLabel = runCatching {
            launcherApps.getApplicationInfo(packageName, 0, provider.profile).loadLabel(pm).toString()
        }.recoverCatching {
            if (provider.profile != Process.myUserHandle()) throw it
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }
            .getOrDefault(packageName)
        val providerLabel = provider.loadLabel(pm).toString()
        val description = runCatching { provider.loadDescription(context)?.toString().orEmpty() }.getOrDefault("")
        val icon = icons.getOrPut("$packageName/${provider.profile.hashCode()}") {
            runCatching { launcherApps.getApplicationInfo(packageName, 0, provider.profile).loadIcon(pm).iconBitmap() }.getOrNull()
        }
        WidgetCatalogEntry(provider, providerLabel, appLabel, description, profile.userSerial, profile.label, profile.isWork, icon)
    }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.providerLabel.lowercase() }))
}

private sealed interface CatalogPreview {
    data class Remote(val views: RemoteViews) : CatalogPreview
    data class Picture(val bitmap: Bitmap) : CatalogPreview
    data object Missing : CatalogPreview
}

private object WidgetPreviewCache {
    private const val MAX_BYTES = 16 * 1024 * 1024
    private val values = object : LinkedHashMap<String, CatalogPreview>(16, .75f, true) {}
    private var bytes = 0
    @Synchronized fun get(key: String) = values[key]
    @Synchronized fun put(key: String, value: CatalogPreview) {
        values.remove(key)?.let { bytes -= it.cost }
        values[key] = value; bytes += value.cost
        val iterator = values.entries.iterator()
        while (bytes > MAX_BYTES && iterator.hasNext()) { bytes -= iterator.next().value.cost; iterator.remove() }
    }
    private val CatalogPreview.cost get() = when (this) {
        is CatalogPreview.Picture -> bitmap.allocationByteCount
        is CatalogPreview.Remote -> 64 * 1024
        CatalogPreview.Missing -> 1
    }
}

private suspend fun loadWidgetPreview(context: Context, provider: AppWidgetProviderInfo, span: WidgetSpan): CatalogPreview {
    val density = context.resources.displayMetrics.densityDpi
    val key = "${provider.provider.flattenToString()}|${provider.profile.hashCode()}|$density|${span.width}x${span.height}"
    WidgetPreviewCache.get(key)?.let { return it }
    val loaded = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        var remote: RemoteViews? = null
        if (android.os.Build.VERSION.SDK_INT >= 35 &&
            provider.generatedPreviewCategories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0) {
            remote = runCatching { manager.getWidgetPreview(provider.provider, provider.profile,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) }.getOrNull()
        }
        if (remote == null && provider.previewLayout != 0)
            remote = runCatching { RemoteViews(provider.provider.packageName, provider.previewLayout) }.getOrNull()
        remote?.let(CatalogPreview::Remote) ?: run {
            val drawable = runCatching { provider.loadPreviewImage(context, density) }.getOrNull()
                ?: runCatching { provider.loadIcon(context, density) }.getOrNull()
            drawable
        }
    }
    // Drawable.draw may touch theme/view state. Convert it on Compose's UI thread; only resource
    // discovery and generated-preview IPC belong on the worker dispatcher.
    val result = when (loaded) {
        is CatalogPreview -> loaded
        is Drawable -> CatalogPreview.Picture(loaded.catalogBitmap())
        else -> CatalogPreview.Missing
    }
    WidgetPreviewCache.put(key, result)
    return result
}

private suspend fun loadPicturePreview(context: Context, provider: AppWidgetProviderInfo): CatalogPreview {
    val density = context.resources.displayMetrics.densityDpi
    val drawable = withContext(Dispatchers.IO) {
        runCatching { provider.loadPreviewImage(context, density) }.getOrNull()
            ?: runCatching { provider.loadIcon(context, density) }.getOrNull()
    }
    return drawable?.let { CatalogPreview.Picture(it.catalogBitmap()) } ?: CatalogPreview.Missing
}

/** An app icon at the size the app row draws it (44 dp on a dense screen), so 800 widgets don't cost 800 icons. */
private fun Drawable.iconBitmap(size: Int = 144): Bitmap =
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        setBounds(0, 0, size, size)
        draw(android.graphics.Canvas(bitmap))
    }

private fun Drawable.catalogBitmap(): Bitmap {
    val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: 144
    val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: 144
    val scale = minOf(1f, 720f / sourceWidth, 480f / sourceHeight)
    val w = (sourceWidth * scale).toInt().coerceAtLeast(1)
    val h = (sourceHeight * scale).toInt().coerceAtLeast(1)
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, w, h)
        draw(canvas)
    }
}

@Composable
internal fun WidgetProviderPreview(entry: WidgetCatalogEntry, span: WidgetSpan, modifier: Modifier = Modifier,
    inset: Dp = FolioSpace.SNUG.dp, background: Color = FolioColors.LightBackground) {
    val context = LocalContext.current
    var remoteFailed by remember(entry.provider) { mutableStateOf(false) }
    val preview by produceState<CatalogPreview?>(null, entry.provider, span, context.resources.displayMetrics.densityDpi, remoteFailed) {
        val loaded = loadWidgetPreview(context, entry.provider, span)
        value = if (loaded is CatalogPreview.Remote && remoteFailed) loadPicturePreview(context, entry.provider) else loaded
    }
    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        when (val value = preview) {
            is CatalogPreview.Remote -> AndroidView(factory = { previewContext ->
                object : android.widget.FrameLayout(previewContext) {
                    override fun dispatchTouchEvent(event: android.view.MotionEvent?): Boolean = false
                }.apply {
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    isFocusable = false
                    isClickable = false
                    runCatching { addView(value.views.apply(previewContext, this)) }
                        .onFailure { post { remoteFailed = true } }
                }
            }, modifier = Modifier.fillMaxSize().padding(inset))
            is CatalogPreview.Picture -> Image(value.bitmap.asImageBitmap(), null, Modifier.fillMaxSize().padding(inset),
                contentScale = ContentScale.Fit)
            CatalogPreview.Missing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.providerLabel, style = MaterialTheme.typography.bodySmall, color = FolioColors.LightLabel)
                Text("${span.width} × ${span.height}", style = MaterialTheme.typography.labelSmall, color = FolioColors.LightSecondaryLabel)
            }
            null -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp, color = FolioColors.LightSecondaryLabel)
        }
    }
}

internal sealed interface GalleryWidget {
    val key: String
    data class Provider(val entry: WidgetCatalogEntry) : GalleryWidget {
        override val key get() = "provider-${entry.provider.provider.flattenToString()}-${entry.userSerial}"
    }
    data class Builtin(val id: Int) : GalleryWidget {
        override val key get() = "builtin-$id"
    }
}

internal data class GallerySizes(val preferred: WidgetSpan, val options: List<WidgetSpan>)

internal val GALLERY_BUILTINS = listOf(CLOCK_WIDGET, DATE_WIDGET, UP_NEXT_WIDGET, BIG_CLOCK_WIDGET, SUGGESTIONS_WIDGET)

internal fun builtinGallerySizes(id: Int): GallerySizes = when (id) {
    UP_NEXT_WIDGET -> GallerySizes(WidgetSpan(2, 2), listOf(WidgetSpan(2, 2), WidgetSpan(4, 2)))
    BIG_CLOCK_WIDGET -> GallerySizes(WidgetSpan(4, 2), listOf(WidgetSpan(4, 2)))
    SUGGESTIONS_WIDGET -> GallerySizes(WidgetSpan(2, 2), WIDGET_SIZE_PRESETS)
    else -> GallerySizes(WidgetSpan(2, 2), listOf(WidgetSpan(2, 2)))
}

internal fun gallerySizes(constraints: WidgetSpanConstraints?): GallerySizes? =
    constraints?.takeIf { it.minimumFitsGrid }?.let { GallerySizes(it.preferred, placementSizeOptions(it.preferred, it)) }

internal fun gallerySizeFor(sizes: GallerySizes, wanted: WidgetSpan?): WidgetSpan =
    wanted?.takeIf { it in sizes.options } ?: sizes.preferred

@StringRes
private fun builtinLabel(id: Int) = when (id) {
    CLOCK_WIDGET -> R.string.clock
    DATE_WIDGET -> R.string.date
    UP_NEXT_WIDGET -> R.string.up_next
    BIG_CLOCK_WIDGET -> R.string.big_clock
    SUGGESTIONS_WIDGET -> R.string.suggestions
    else -> R.string.widget_panel
}

@StringRes
private fun builtinDescription(id: Int) = when (id) {
    CLOCK_WIDGET -> R.string.clock_widget_description
    DATE_WIDGET -> R.string.date_widget_description
    UP_NEXT_WIDGET -> R.string.up_next_widget_description
    BIG_CLOCK_WIDGET -> R.string.big_clock_widget_description
    else -> R.string.suggestions_widget_description
}

private const val FOLIO_WIDGETS = "\u0000folio"
private const val MAX_PAGE_DOTS = 12
private const val TWO_PANE_MIN_WIDTH = 700
private const val LIST_PANE_WIDTH = 300

/**
 * The widget gallery, iOS's way: a sheet over Home, the apps that have widgets in a list, and one app's widgets a page
 * at a time with their sizes and an Add Widget button. Wide windows show the list and the widgets side by side.
 * Holding a widget's preview lifts it out of the sheet so it can be dropped anywhere on Home.
 */
@Composable
internal fun VisualWidgetPicker(
    entries: List<WidgetCatalogEntry>?,
    profiles: List<AppProfile>,
    selectedProfile: AppProfile,
    onSelectProfile: (AppProfile) -> Unit,
    onTurnOnWork: (Long) -> Unit,
    hiddenForDrag: Boolean,
    sizesOf: (AppWidgetProviderInfo) -> GallerySizes?,
    cellWidthDp: Float,
    rowHeightDp: Float,
    onBack: () -> Unit,
    onAdd: (GalleryWidget, WidgetSpan) -> Unit,
    onDragStart: (GalleryWidget, WidgetSpan, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancelDrag: () -> Unit,
    currentSpan: WidgetSpan? = null,
    oneApp: Boolean = false,
) {
    val folioLabel = stringResource(R.string.folio)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    var query by rememberSaveable { mutableStateOf("") }
    val words = query.trim().lowercase()
    var openApp by rememberSaveable { mutableStateOf<String?>(null) }
    var openPage by rememberSaveable { mutableIntStateOf(0) }
    val folioIcon = remember { runCatching { context.packageManager.getApplicationIcon(context.packageName).iconBitmap() }.getOrNull() }
    val showFolio = selectedProfile.isPersonal && !oneApp
    val builtinNames = GALLERY_BUILTINS.associateWith { stringResource(builtinLabel(it)) }
    val byApp: Map<String, List<GalleryWidget>> = remember(entries, showFolio, words, builtinNames) {
        val folio = if (!showFolio) emptyList() else GALLERY_BUILTINS
            .filter { matchesWidgetSearch(words, builtinNames.getValue(it), folioLabel) }.map(GalleryWidget::Builtin)
        val apps = entries.orEmpty().filter {
            matchesWidgetSearch(words, it.appLabel, it.providerLabel, it.description, it.provider.provider.packageName)
        }.groupBy { it.appLabel }.mapValues { (_, group) -> group.map(GalleryWidget::Provider) }
        (if (folio.isEmpty()) emptyMap() else mapOf(FOLIO_WIDGETS to folio)) + apps
    }
    val icons = remember(entries, folioIcon) {
        mapOf(FOLIO_WIDGETS to folioIcon) + entries.orEmpty().groupBy { it.appLabel }.mapValues { it.value.first().appIcon }
    }
    fun labelOf(app: String) = if (app == FOLIO_WIDGETS) folioLabel else app
    var openedOneApp by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(byApp.keys, oneApp) {
        if (oneApp && !openedOneApp && byApp.size == 1) { openApp = byApp.keys.first(); openPage = 0; openedOneApp = true }
    }
    fun hideKeyboard() { focusManager.clearFocus(); keyboard?.hide() }

    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val pull = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear.animateTo(1f, FolioMotion.spring(FolioMotion.Sheet)) }
    DisposableEffect(Unit) {
        val timeout = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
        timeout.launch { kotlinx.coroutines.delay(900); if (!leaving && appear.value < 1f) appear.snapTo(1f) }
        onDispose { timeout.cancel() }
    }
    val hidden by rememberUpdatedState(hiddenForDrag)
    fun leave(then: () -> Unit) {
        if (leaving) return
        leaving = true
        hideKeyboard()
        scope.launch {
            if (!reduceMotion) kotlinx.coroutines.withTimeoutOrNull(400) { appear.animateTo(0f, FolioMotion.spring(FolioMotion.Firm)) }
            then()
            withFrameNanos { }
            leaving = false
            pull.snapTo(0f)
            if (!hidden) appear.animateTo(1f, FolioMotion.spring(FolioMotion.Sheet))
        }
    }
    LaunchedEffect(hiddenForDrag) {
        if (!hiddenForDrag && !leaving && appear.value < 1f) appear.animateTo(1f, FolioMotion.spring(FolioMotion.Sheet))
    }
    val close = { leave(onBack) }
    val add: (GalleryWidget, WidgetSpan) -> Unit = { widget, span -> leave { onAdd(widget, span) } }
    val dragStart: (GalleryWidget, WidgetSpan, Offset) -> Unit = { widget, span, point -> hideKeyboard(); onDragStart(widget, span, point) }
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDrop by rememberUpdatedState(onDrop)
    val latestCancel by rememberUpdatedState(onCancelDrag)
    val dragCallbacks = remember { GalleryDrag({ latestDrag(it) }, { latestDrop() }, { latestCancel() }) }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BoxWithConstraints(Modifier.extendBelow(navigationBarHeight).fillMaxSize().alpha(if (hiddenForDrag) 0f else 1f)
        .then(if (hiddenForDrag) Modifier.clearAndSetSemantics { }.focusProperties { canFocus = false } else Modifier)
        .testTag("visual-widget-picker")) {
        val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH.dp
        val sheetHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        BackHandler(enabled = !twoPane && openApp != null && !hiddenForDrag) { openApp = null }
        Box(Modifier.fillMaxSize()
            .graphicsLayer { alpha = (appear.value * (1f - pull.value / sheetHeightPx * 1.5f)).coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = .35f))
            .then(if (hiddenForDrag) Modifier else Modifier.clickable(remember { MutableInteractionSource() }, null,
                onClickLabel = stringResource(R.string.close), onClick = close)))
        val panelShape = if (twoPane) RoundedCornerShape(FolioRadius.SHEET_TOP.dp)
            else RoundedCornerShape(topStart = FolioRadius.SHEET_TOP.dp, topEnd = FolioRadius.SHEET_TOP.dp)
        val panel = if (twoPane) Modifier.align(Alignment.Center)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .padding(horizontal = 48.dp, vertical = FolioSpace.XXL.dp).widthIn(max = if (oneApp) 620.dp else 960.dp).heightIn(max = 820.dp).fillMaxSize()
        else Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.folioSafeTop)
            .padding(top = FolioSpace.SMALL.dp).fillMaxSize()
        FolioSheetTheme {
            Surface(panel.graphicsLayer {
                    val hidden = 1f - appear.value
                    if (twoPane) {
                        alpha = appear.value
                        translationY = hidden * size.height * .12f + pull.value
                    } else translationY = hidden * size.height + pull.value
                }.clickable(remember { MutableInteractionSource() }, null, onClick = {})
                .testTag("widget-gallery-sheet"),
                shape = panelShape, color = FolioColors.LightBackground, contentColor = FolioColors.LightLabel) {
                val pullToClose = Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (pull.value > 120.dp.toPx()) leave(onBack)
                            else scope.launch { pull.animateTo(0f, FolioMotion.spring(FolioMotion.Settle)) }
                        },
                        onDragCancel = { scope.launch { pull.animateTo(0f, FolioMotion.spring(FolioMotion.Settle)) } },
                    ) { change, amount -> change.consume(); scope.launch { pull.snapTo((pull.value + amount).coerceAtLeast(0f)) } }
                }
                Column(Modifier.fillMaxSize()) {
                    if (!twoPane) Box(pullToClose.fillMaxWidth().padding(top = FolioSpace.SMALL.dp, bottom = FolioSpace.TINY.dp),
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.size(width = 36.dp, height = 5.dp).background(FolioColors.LightTertiaryLabel, CircleShape))
                    }
                    val list: @Composable (Modifier) -> Unit = { modifier ->
                        GalleryAppList(byApp, icons, ::labelOf, query, { query = it },
                            selected = if (twoPane) openApp ?: byApp.keys.firstOrNull() else null,
                            featured = !twoPane && words.isEmpty() && showFolio,
                            loading = entries == null, profiles = profiles, selectedProfile = selectedProfile,
                            onSelectProfile = onSelectProfile, onTurnOnWork = onTurnOnWork,
                            onClose = close.takeIf { !twoPane }, cellWidthDp = cellWidthDp, rowHeightDp = rowHeightDp,
                            modifier = modifier) { app, page ->
                            hideKeyboard(); openApp = app; openPage = page
                        }
                    }
                    if (twoPane) Row(Modifier.fillMaxSize()) {
                        if (!oneApp) {
                            list(Modifier.width(LIST_PANE_WIDTH.dp).fillMaxHeight())
                            Box(Modifier.fillMaxHeight().width(.5.dp).background(FolioColors.LightSeparator))
                        }
                        val shown = openApp?.takeIf { it in byApp } ?: byApp.keys.firstOrNull()
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            if (shown != null) key(shown, words) {
                                GalleryAppDetail(labelOf(shown), icons[shown], byApp.getValue(shown),
                                    initialPage = if (shown == openApp) openPage else 0, sizesOf = sizesOf, currentSpan = currentSpan,
                                    cellWidthDp = cellWidthDp, rowHeightDp = rowHeightDp, onBack = null, onClose = close,
                                    onAdd = add, onDragStart = dragStart, drag = dragCallbacks, headerModifier = Modifier)
                            } else if (entries != null) Text(stringResource(R.string.no_widgets_found), color = FolioColors.LightSecondaryLabel,
                                modifier = Modifier.align(Alignment.Center))
                        }
                    } else AnimatedContent(openApp?.takeIf { it in byApp }, Modifier.fillMaxSize(), transitionSpec = {
                        val forward = targetState != null
                        val spec = FolioMotion.spring<IntOffset>(FolioMotion.Sheet, IntOffset.VisibilityThreshold)
                        (slideInHorizontally(spec) { if (forward) it else -it / 3 } togetherWith
                            slideOutHorizontally(spec) { if (forward) -it / 3 else it }).using(SizeTransform(clip = false))
                    }, label = "widget gallery page") { app ->
                        if (app == null) list(Modifier.fillMaxSize())
                        else GalleryAppDetail(labelOf(app), icons[app], byApp.getValue(app), initialPage = openPage,
                            sizesOf = sizesOf, currentSpan = currentSpan, cellWidthDp = cellWidthDp, rowHeightDp = rowHeightDp,
                            onBack = { openApp = null }, onClose = close, onAdd = add, onDragStart = dragStart, drag = dragCallbacks,
                            headerModifier = pullToClose)
                    }
                }
            }
        }
    }
}

private fun Modifier.extendBelow(extra: Dp) = layout { measurable, constraints ->
    val extraPx = if (constraints.hasBoundedHeight) extra.roundToPx() else 0
    val placeable = measurable.measure(constraints.copy(minHeight = constraints.minHeight + extraPx, maxHeight = constraints.maxHeight + extraPx))
    layout(placeable.width, placeable.height - extraPx) { placeable.place(0, 0) }
}

private class GalleryDrag(val move: (Offset) -> Unit, val drop: () -> Unit, val cancel: () -> Unit)

@Composable
private fun GalleryAppList(
    byApp: Map<String, List<GalleryWidget>>,
    icons: Map<String, Bitmap?>,
    labelOf: (String) -> String,
    query: String,
    onQuery: (String) -> Unit,
    selected: String?,
    featured: Boolean,
    loading: Boolean,
    profiles: List<AppProfile>,
    selectedProfile: AppProfile,
    onSelectProfile: (AppProfile) -> Unit,
    onTurnOnWork: (Long) -> Unit,
    onClose: (() -> Unit)?,
    cellWidthDp: Float,
    rowHeightDp: Float,
    modifier: Modifier,
    onOpen: (String, Int) -> Unit,
) {
    val ink = FolioColors.LightLabel
    val secondary = FolioColors.LightSecondaryLabel
    val listState = rememberLazyListState()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier.imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = FolioSpace.LARGE.dp, end = if (onClose != null) FolioSpace.SMALL.dp else FolioSpace.LARGE.dp,
            top = FolioSpace.MEDIUM.dp, bottom = FolioSpace.SMALL.dp), verticalAlignment = Alignment.CenterVertically) {
            IosSearchField(query, onQuery, stringResource(R.string.search_widgets), Modifier.weight(1f),
                fieldModifier = Modifier.testTag("widget-catalog-search"))
            onClose?.let { GalleryCircleButton(Icons.Rounded.Close, stringResource(R.string.close), it, Modifier.padding(start = FolioSpace.SNUG.dp)) }
        }
        if (profiles.any { it.isWork }) Row(Modifier.fillMaxWidth().padding(horizontal = FolioSpace.LARGE.dp, vertical = FolioSpace.TINY.dp)) {
            IosSegmented(profiles.map { it.userSerial to it.label }, selectedProfile.userSerial,
                { serial -> profiles.firstOrNull { it.userSerial == serial }?.let(onSelectProfile) }, tag = "widget-profile")
        }
        if (!selectedProfile.available || !selectedProfile.unlocked || selectedProfile.quiet) {
            Column(Modifier.fillMaxWidth().padding(FolioSpace.XXL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (selectedProfile.quiet) stringResource(R.string.work_apps_are_paused)
                    else stringResource(R.string.work_profile_is_unavailable), color = secondary, textAlign = TextAlign.Center)
                if (selectedProfile.isWork) FolioButton(stringResource(R.string.turn_on), { onTurnOnWork(selectedProfile.userSerial) },
                    Modifier.padding(top = FolioSpace.MEDIUM.dp))
            }
        }
        LazyColumn(Modifier.fillMaxSize().edgeFade(listState).testTag("widget-catalog-list"), state = listState,
            contentPadding = PaddingValues(start = FolioSpace.LARGE.dp, end = FolioSpace.LARGE.dp, top = FolioSpace.SMALL.dp,
                bottom = FolioSpace.XXL.dp + navigationBar)) {
            val folio = byApp[FOLIO_WIDGETS]
            if (featured && folio != null) {
                item("featured-label") { SheetGroupLabel(stringResource(R.string.folio)) }
                item("featured") {
                    LazyRow(Modifier.fillMaxWidth().padding(bottom = FolioSpace.XL.dp).testTag("widget-featured"),
                        horizontalArrangement = Arrangement.spacedBy(FolioSpace.MEDIUM.dp)) {
                        itemsIndexed(folio, key = { _, it -> it.key }) { index, widget ->
                            val id = (widget as GalleryWidget.Builtin).id
                            val span = builtinGallerySizes(id).preferred
                            Column(Modifier.width(if (span.width > 2) 248.dp else 120.dp)
                                .clip(RoundedCornerShape(FolioRadius.CARD.dp))
                                .clickable(onClickLabel = stringResource(builtinLabel(id))) { onOpen(FOLIO_WIDGETS, index) }
                                .testTag("widget-featured-$id")) {
                                WidgetStage(widget, span, cellWidthDp, rowHeightDp, Modifier.fillMaxWidth().height(120.dp), shadow = 6.dp)
                                Text(stringResource(builtinLabel(id)), color = ink, fontSize = FolioType.FOOTNOTE.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth().padding(top = FolioSpace.SNUG.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            if (byApp.isNotEmpty() && featured) item("apps-label") { SheetGroupLabel(stringResource(R.string.apps)) }
            val keys = byApp.keys.toList()
            itemsIndexed(keys, key = { _, it -> "app-$it" }) { index, app ->
                GalleryAppRow(labelOf(app), icons[app], byApp.getValue(app).size, first = index == 0, last = index == keys.lastIndex,
                    selected = app == selected, showChevron = selected == null,
                    tag = "widget-app-${if (app == FOLIO_WIDGETS) "folio" else app}") { onOpen(app, 0) }
            }
            if (loading) item("catalog-loading") {
                Box(Modifier.fillMaxWidth().padding(FolioSpace.XXL.dp), contentAlignment = Alignment.TopCenter) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp, color = secondary)
                }
            }
            if (!loading && byApp.isEmpty()) item("empty") {
                Text(stringResource(R.string.no_widgets_found), color = secondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(FolioSpace.XXL.dp))
            }
        }
    }
}

@Composable
private fun GalleryAppRow(label: String, icon: Bitmap?, count: Int, first: Boolean, last: Boolean, selected: Boolean, showChevron: Boolean,
    tag: String, onClick: () -> Unit) {
    val corner = FolioRadius.GROUPED_CARD.dp
    val shape = RoundedCornerShape(topStart = if (first) corner else 0.dp, topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp, bottomEnd = if (last) corner else 0.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(if (selected) FolioColors.LightFill else Color.White)
        .selectable(selected, role = Role.Tab, onClick = onClick).testTag(tag)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = FolioSpace.COMFY.dp, vertical = FolioSpace.SNUG.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(IconContinuousShape).background(FolioColors.LightFill), contentAlignment = Alignment.Center) {
                icon?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                    ?: Icon(Icons.Rounded.Widgets, null, tint = FolioColors.LightLabel, modifier = Modifier.size(22.dp))
            }
            Text(label, color = FolioColors.LightLabel, fontSize = FolioType.BODY.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = FolioSpace.MEDIUM.dp))
            Text("$count", color = FolioColors.LightSecondaryLabel, fontSize = FolioType.BODY.sp,
                modifier = Modifier.padding(start = FolioSpace.SMALL.dp))
            if (showChevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = FolioColors.LightTertiaryLabel,
                modifier = Modifier.padding(start = FolioSpace.TINY.dp).size(22.dp))
        }
        if (!last) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(.5.dp).background(FolioColors.LightSeparator))
    }
}

@Composable
private fun GalleryCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(FolioTouch.MIN.dp).clip(CircleShape).clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(FolioColors.LightFill), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FolioColors.LightSecondaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GalleryAppDetail(
    appLabel: String,
    appIcon: Bitmap?,
    widgets: List<GalleryWidget>,
    initialPage: Int,
    sizesOf: (AppWidgetProviderInfo) -> GallerySizes?,
    currentSpan: WidgetSpan?,
    cellWidthDp: Float,
    rowHeightDp: Float,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
    onAdd: (GalleryWidget, WidgetSpan) -> Unit,
    onDragStart: (GalleryWidget, WidgetSpan, Offset) -> Unit,
    drag: GalleryDrag,
    headerModifier: Modifier,
) {
    val pager = rememberPagerState(initialPage.coerceIn(0, (widgets.size - 1).coerceAtLeast(0))) { widgets.size }
    val sizes = remember(widgets) { widgets.map { widget ->
        when (widget) {
            is GalleryWidget.Builtin -> builtinGallerySizes(widget.id)
            is GalleryWidget.Provider -> sizesOf(widget.entry.provider)
        }
    } }
    val chosen = remember(widgets) { mutableStateMapOf<Int, WidgetSpan>() }
    fun spanAt(page: Int): WidgetSpan? = sizes.getOrNull(page)?.let { chosen[page] ?: gallerySizeFor(it, currentSpan) }
    val names = widgets.map { widget ->
        when (widget) {
            is GalleryWidget.Builtin -> stringResource(builtinLabel(widget.id))
            is GalleryWidget.Provider -> widget.entry.providerLabel
        }
    }
    val duplicateNames = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val current = pager.currentPage.coerceIn(0, (widgets.size - 1).coerceAtLeast(0))
    Column(Modifier.fillMaxSize().testTag("widget-detail")) {
        Box(headerModifier.fillMaxWidth().padding(horizontal = FolioSpace.SMALL.dp, vertical = FolioSpace.TINY.dp)) {
            onBack?.let { GalleryCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.widgets), it,
                Modifier.align(Alignment.CenterStart).testTag("widget-detail-back")) }
            Row(Modifier.align(if (onBack != null) Alignment.Center else Alignment.CenterStart)
                .padding(horizontal = if (onBack != null) 56.dp else FolioSpace.SMALL.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(IconContinuousShape).background(FolioColors.LightFill), contentAlignment = Alignment.Center) {
                    appIcon?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                        ?: Icon(Icons.Rounded.Widgets, null, tint = FolioColors.LightLabel, modifier = Modifier.size(18.dp))
                }
                Text(appLabel, color = FolioColors.LightLabel, fontSize = FolioType.TITLE3.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = FolioSpace.SMALL.dp).semantics { heading() })
            }
            GalleryCircleButton(Icons.Rounded.Close, stringResource(R.string.close), onClose, Modifier.align(Alignment.CenterEnd))
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().testTag("widget-detail-pager"), pageSpacing = FolioSpace.XXL.dp,
            key = { widgets[it].key }) { page ->
            val widget = widgets[page]
            val span = spanAt(page)
            val description = when (widget) {
                is GalleryWidget.Builtin -> stringResource(builtinDescription(widget.id))
                is GalleryWidget.Provider -> widget.entry.description.ifBlank {
                    if (names[page] in duplicateNames) span?.let { "${it.width} × ${it.height}" }.orEmpty() else ""
                }
            }
            Column(Modifier.fillMaxSize().padding(horizontal = FolioSpace.XXL.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(names[page], color = FolioColors.LightLabel, fontSize = FolioType.TITLE2.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FolioSpace.LARGE.dp))
                if (description.isNotBlank()) Text(description, color = FolioColors.LightSecondaryLabel, fontSize = FolioType.SUBHEAD.sp,
                    textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FolioSpace.TINY.dp).widthIn(max = 420.dp))
                val profileSuffix = (widget as? GalleryWidget.Provider)?.entry?.takeIf { it.isWork }?.let { "-profile-${it.userSerial}" }.orEmpty()
                val stageTag = when (widget) {
                    is GalleryWidget.Builtin -> "widget-builtin-${widget.id}"
                    is GalleryWidget.Provider -> "widget-provider-${widget.entry.provider.provider.flattenToString()}$profileSuffix"
                }
                val previewTag = (widget as? GalleryWidget.Provider)?.let { "widget-preview-${it.entry.provider.provider.flattenToString()}$profileSuffix" }
                if (span == null) Box(Modifier.weight(1f).fillMaxWidth().testTag(stageTag), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.doesn_t_fit_this_layout), color = FolioColors.LightSecondaryLabel)
                } else {
                    var origin by remember { mutableStateOf(Offset.Zero) }
                    WidgetStage(widget, span, cellWidthDp, rowHeightDp, Modifier.weight(1f).fillMaxWidth()
                        .padding(vertical = FolioSpace.XL.dp).testTag(stageTag), shadow = 14.dp, previewTag = previewTag) {
                        Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }
                            .semantics { contentDescription = names[page] }
                            .pointerInput(widget.key, span) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { point -> onDragStart(widget, span, origin + point) },
                                    onDrag = { change, _ -> change.consume(); drag.move(origin + change.position) },
                                    onDragEnd = { drag.drop() }, onDragCancel = { drag.cancel() })
                            })
                    }
                }
            }
        }
        if (widgets.size > 1) GalleryPageIndicator(widgets.size, current, Modifier.align(Alignment.CenterHorizontally).padding(vertical = FolioSpace.SMALL.dp))
        val options = sizes.getOrNull(current)?.options.orEmpty()
        val span = spanAt(current)
        val presetNames = listOf(stringResource(R.string.small), stringResource(R.string.medium), stringResource(R.string.large))
        val wideName = stringResource(R.string.wide)
        fun sizeName(option: WidgetSpan) = WIDGET_SIZE_PRESETS.indexOf(option).takeIf { it >= 0 }?.let { presetNames[it] }
            ?: if (option.width > option.height * 1.5f) wideName else "${option.width} × ${option.height}"
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            if (options.size > 1 && span != null) IosSegmented(options.map { it to sizeName(it) }, span, { chosen[current] = it },
                Modifier.width((options.size * 96).dp), tag = "widget-size")
            else if (span != null) Text(sizeName(span), color = FolioColors.LightSecondaryLabel, fontSize = FolioType.FOOTNOTE.sp,
                modifier = Modifier.testTag("widget-size-label"))
        }
        FolioButton(stringResource(R.string.add_widget_button), { span?.let { onAdd(widgets[current], it) } },
            Modifier.align(Alignment.CenterHorizontally).padding(top = FolioSpace.SMALL.dp, bottom = FolioSpace.XL.dp + navigationBar)
                .widthIn(min = 240.dp, max = 420.dp).fillMaxWidth(.7f),
            icon = Icons.Rounded.Add, tag = "widget-add", enabled = span != null)
    }
}

@Composable
private fun GalleryPageIndicator(count: Int, current: Int, modifier: Modifier) {
    val position = stringResource(R.string.widget_page_position, current + 1, count)
    if (count > MAX_PAGE_DOTS) {
        Text(position, color = FolioColors.LightSecondaryLabel, fontSize = FolioType.FOOTNOTE.sp, modifier = modifier.testTag("widget-page-position"))
        return
    }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = position }.testTag("widget-page-dots"),
        horizontalArrangement = Arrangement.spacedBy(FolioSpace.SMALL.dp)) {
        repeat(count) { index ->
            val on by animateColorAsState(if (index == current) FolioColors.LightLabel else FolioColors.LightTertiaryLabel, label = "page dot")
            Box(Modifier.size(7.dp).clip(CircleShape).background(on))
        }
    }
}

/**
 * A widget drawn at its real size on Home and scaled down to fit, so a 4 × 2 reads like it will on Home instead of
 * squeezed into a card. Folio's own widgets are drawn on the Home background, since they are glass.
 */
@Composable
internal fun WidgetStage(widget: GalleryWidget, span: WidgetSpan, cellWidthDp: Float, rowHeightDp: Float, modifier: Modifier,
    shadow: Dp = 0.dp, previewTag: String? = null, overlay: @Composable BoxScope.() -> Unit = {}) {
    val reduceMotion = LocalReduceMotion.current
    val spec = FolioMotion.spring<Float>(FolioMotion.calm(FolioMotion.Appear, reduceMotion))
    val naturalWidth by animateFloatAsState((span.width * cellWidthDp - 10f).coerceAtLeast(48f), spec, label = "stage width")
    val naturalHeight by animateFloatAsState((span.height * rowHeightDp - 18f).coerceAtLeast(48f), spec, label = "stage height")
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = minOf(1f, maxWidth.value / naturalWidth, maxHeight.value / naturalHeight)
        val shape = RoundedCornerShape(FolioRadius.WIDGET.dp * scale)
        Box(Modifier.size((naturalWidth * scale).dp, (naturalHeight * scale).dp)
            .then(if (previewTag != null) Modifier.testTag(previewTag) else Modifier)
            .shadow(shadow, shape, clip = false, ambientColor = Color.Black.copy(alpha = .25f), spotColor = Color.Black.copy(alpha = .25f))) {
            Box(Modifier.requiredSize(naturalWidth.dp, naturalHeight.dp).align(Alignment.Center)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(FolioRadius.WIDGET.dp))
                .clearAndSetSemantics { }) {
                when (widget) {
                    is GalleryWidget.Builtin -> BuiltinWidgetPreview(widget.id, Modifier.fillMaxSize())
                    is GalleryWidget.Provider -> WidgetProviderPreview(widget.entry, span, Modifier.fillMaxSize(),
                        inset = 0.dp, background = Color.White)
                }
            }
            overlay()
        }
    }
}

/** One of Folio's own widgets as it looks on Home: the card over the Home background, not touchable. */
@Composable
internal fun BuiltinWidgetPreview(id: Int, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(FolioRadius.WIDGET.dp))) {
        DuneWallpaper(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .14f)))
        BuiltinWidgetCard(id, slot = -1) {}
    }
}

/** True when [words] (already lowercase and trimmed) is empty or appears in any of [texts]. */
internal fun matchesWidgetSearch(words: String, vararg texts: String): Boolean =
    words.isEmpty() || texts.any { it.lowercase().contains(words) }

/** The "No Widgets Found" message belongs only to a loaded gallery with nothing to show, Folio's own widgets included. */
internal fun galleryIsEmpty(loaded: Boolean, systemWidgets: Int, folioWidgets: Int): Boolean =
    loaded && systemWidgets == 0 && folioWidgets == 0
