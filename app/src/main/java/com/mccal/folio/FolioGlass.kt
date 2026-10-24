package com.mccal.folio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.addOutline

/**
 * One glass language for every Folio overlay (Control Center, Notification Center, Spotlight,
 * pickers): a darker, more solid frost than the Home rail, so busy blurred icons behind never
 * compete with the content, plus a hairline light edge like iOS materials.
 */
internal object FolioGlass {
    /** Dim over the blurred Home behind an overlay. */
    val scrim = Color.Black.copy(alpha = .42f)
    /** Large tiles (Control Center modules). */
    val module = FolioColors.SecondaryBackground.copy(alpha = .76f)
    /** Cards and rows (notifications, Spotlight sections, search field). */
    val card = FolioColors.SurfaceCard.copy(alpha = .82f)
    /** Controls sitting on a card (inactive toggles, pills, chips). */
    val raised = Color.White.copy(alpha = .14f)
    /** Hairline edge that separates glass from glass. */
    val edge = BorderStroke(1.dp, Color.White.copy(alpha = .08f))
    /** Secondary text on glass. */
    val secondary = FolioColors.SecondaryLabel
}

/** Dark, iOS-like colors for Folio's sheets (settings, app options, setup). */
internal val FolioSheetColors = androidx.compose.material3.darkColorScheme(
    primary = FolioColors.BlueOnDark, onPrimary = Color.White,
    primaryContainer = FolioColors.BlueOnDark, onPrimaryContainer = Color.White,
    secondary = FolioColors.Cyan, onSecondary = Color.Black,
    secondaryContainer = FolioColors.MenuSurface, onSecondaryContainer = Color.White,
    surface = FolioColors.SecondaryBackground, onSurface = Color.White, onSurfaceVariant = FolioColors.LabelVariant,
    surfaceContainerLowest = FolioColors.SurfaceLowest, surfaceContainerLow = FolioColors.SecondaryBackground,
    surfaceContainer = FolioColors.SurfaceCard, surfaceContainerHigh = FolioColors.SheetSurface, surfaceContainerHighest = FolioColors.MenuSurface,
    outline = FolioColors.Outline, outlineVariant = FolioColors.OutlineVariant, error = FolioColors.Red,
)

/**
 * The sheets, menus or pages one Home screen has open. Each one holds its own entry for as long as it is on screen, so
 * Home can tell which are its own: a hand-counted number shared by every Home instance in the process let one instance's
 * sheet (stopped behind another) keep the other one blurred with nothing in front, until Folio was force-stopped.
 * Closing an entry twice does nothing, and [closeAllOf] clears whatever an instance leaves when it goes.
 */
internal class OpenWindows {
    class Entry internal constructor(internal val owner: Any?)

    private val entries = androidx.compose.runtime.mutableStateListOf<Entry>()

    /** Something belonging to [owner] (the activity showing it) is now open; hand the result to [close] when it is not. */
    fun open(owner: Any?): Entry = Entry(owner).also { entries.add(it) }
    fun close(entry: Entry) { entries.remove(entry) }
    fun closeAllOf(owner: Any?) { entries.removeAll { it.owner === owner } }
    fun isOpen(owner: Any?): Boolean = entries.any { it.owner === owner }
    fun anyOpen(): Boolean = entries.isNotEmpty()
}

/** Open while any launcher sheet is up, so Home blurs behind it like the other overlays. */
internal val LauncherSheets = OpenWindows()

/** Full-screen pages (Setup, Settings pages): nothing behind them is visible, so Home skips its blur while one is up. */
internal val LauncherPages = OpenWindows()

/** Holds an entry in [windows] for the activity showing this composition while the caller is on screen. */
@androidx.compose.runtime.Composable
internal fun RegisterOpenWindow(windows: OpenWindows, active: Boolean = true) {
    val owner = androidx.compose.ui.platform.LocalContext.current.asActivity()
    androidx.compose.runtime.DisposableEffect(owner, active) {
        val entry = if (active) windows.open(owner) else null
        onDispose { entry?.let(windows::close) }
    }
}

/** How far a back swipe has gone over the full-screen Settings page (0–1), for predictive back. */
internal val SheetBackProgress = androidx.compose.runtime.mutableFloatStateOf(0f)

/** A Home layout slider being dragged in Settings, with its bounds in the Settings window (see [SettingsPeek]). */
internal data class PeekSlider(val label: String, val valueLabel: String, val fraction: Float, val bounds: androidx.compose.ui.geometry.Rect)

/** Set while a Home layout slider is dragged: Settings fades so the real Home shows the change, like iOS. */
internal val SettingsPeek = androidx.compose.runtime.mutableStateOf<PeekSlider?>(null)

/**
 * Top-safe insets that respect the real camera cutout. Folio hides the status bar on Home, which
 * makes `statusBarsPadding()` zero, so content slid under the camera; the display cutout is still
 * reported while bars are hidden, so union both.
 */
internal val WindowInsets.Companion.folioSafeTop: WindowInsets
    @androidx.compose.runtime.Composable get() = WindowInsets.statusBars.union(WindowInsets.displayCutout).union(rememberHiddenCameraInsets())

/** Per-folder tint colors, provided from saved settings. */
internal val LocalFolderColors = androidx.compose.runtime.compositionLocalOf { emptyMap<String, Long>() }

/** Velvet/ColorFlow tint options, provided from settings. */
@androidx.compose.runtime.Immutable
internal data class TintOptions(val notifications: Boolean = false, val media: Boolean = true, val notificationAppRow: Boolean = true, val mediaIsland: Boolean = true)
internal val LocalTintOptions = androidx.compose.runtime.staticCompositionLocalOf { TintOptions() }

/** How strong Home's glass is: widget frost and the outline around widgets and Side Bar capsules (Settings › Glass). */
internal data class GlassLook(val widget: Float = .26f, val outline: Float = .16f, val liquid: Boolean = false, val backdropBlur: Boolean = false) {
    val outlineColor get() = androidx.compose.ui.graphics.Color.White.copy(alpha = outline)
}
internal val LocalGlassLook = androidx.compose.runtime.staticCompositionLocalOf { GlassLook() }

/** Settings › Home Screen & Dock › Folders. */
enum class FolderBackground(@androidx.annotation.StringRes val label: Int) { GLASS(R.string.glass), SOLID(R.string.solid), CLEAR(R.string.clear) }
internal data class FolderLook(val columns: Int = 0, val background: FolderBackground = FolderBackground.GLASS)
internal val LocalFolderLook = androidx.compose.runtime.staticCompositionLocalOf { FolderLook() }

/** App name size on Home (Settings › Icons & Side Bar). */
enum class LabelSize(@androidx.annotation.StringRes val label: Int, val sp: Float, val lineSp: Float) { SMALL(R.string.small, 10f, 13f), STANDARD(R.string.standard, 11f, 14f), LARGE(R.string.large, 13f, 16f) }
internal val LocalLabelSize = androidx.compose.runtime.staticCompositionLocalOf { LabelSize.STANDARD }

/**
 * Animation Speed (Settings › Gestures & Actions): scales the stiffness of Folio's springs, so panels, folders, menus
 * and page snaps all move faster or slower together. Android's Remove animations still turns motion off.
 */
enum class MotionSpeed(@androidx.annotation.StringRes val label: Int, val factor: Float) {
    RELAXED(R.string.relaxed, .55f), STANDARD(R.string.standard, 1f), SNAPPY(R.string.snappy, 1.8f);
    companion object {
        @Volatile var current: MotionSpeed = STANDARD
        fun <T> spring(dampingRatio: Float, stiffness: Float, visibilityThreshold: T? = null) =
            androidx.compose.animation.core.spring(dampingRatio = dampingRatio, stiffness = stiffness * current.factor, visibilityThreshold = visibilityThreshold)
    }
}

/**
 * Soft edges on scrolling content, like iOS: rows fade out toward an edge where there's more to scroll. The fade
 * follows an eased curve rather than a straight ramp, and grows in over the first [size] of scrolling instead of
 * popping in, so a list at rest (or barely nudged) keeps crisp first and last rows. [top] and [bottom] return how much
 * of each fade to show, from 0 to 1, given the fade depth in pixels.
 */
internal fun androidx.compose.ui.Modifier.edgeFade(top: (Float) -> Float, bottom: (Float) -> Float,
    size: androidx.compose.ui.unit.Dp = 28.dp): androidx.compose.ui.Modifier =
    // An offscreen layer only while an edge is actually fading, so a list at rest draws normally.
    graphicsLayer {
        val px = size.toPx()
        compositingStrategy = if (top(px) > 0f || bottom(px) > 0f) androidx.compose.ui.graphics.CompositingStrategy.Offscreen
            else androidx.compose.ui.graphics.CompositingStrategy.Auto
    }.drawWithContent {
        drawContent()
        val px = size.toPx().coerceAtMost(this.size.height / 3f)
        val t = top(px).coerceIn(0f, 1f)
        val b = bottom(px).coerceIn(0f, 1f)
        if (t > 0f) drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(*fadeStops(t, towardEnd = false), startY = 0f, endY = px),
            size = androidx.compose.ui.geometry.Size(this.size.width, px), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
        if (b > 0f) drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(*fadeStops(b, towardEnd = true),
            startY = this.size.height - px, endY = this.size.height),
            topLeft = androidx.compose.ui.geometry.Offset(0f, this.size.height - px),
            size = androidx.compose.ui.geometry.Size(this.size.width, px), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
    }

/** Mask stops for one fade: fully kept away from the edge, easing (smoothstep) down to 1 − [amount] at the edge. */
private fun fadeStops(amount: Float, towardEnd: Boolean): Array<Pair<Float, Color>> = Array(FADE_STEPS + 1) { i ->
    val s = i / FADE_STEPS.toFloat()
    val fromEdge = if (towardEnd) 1f - s else s
    val eased = fromEdge * fromEdge * (3f - 2f * fromEdge)
    s to Color.Black.copy(alpha = (1f - amount) + amount * eased)
}

private const val FADE_STEPS = 6

internal fun androidx.compose.ui.Modifier.edgeFade(state: androidx.compose.foundation.ScrollState) =
    edgeFade({ px -> state.value / px }, { px -> if (state.maxValue == Int.MAX_VALUE) 0f else (state.maxValue - state.value) / px })

internal fun androidx.compose.ui.Modifier.edgeFade(state: androidx.compose.foundation.lazy.LazyListState) =
    edgeFade({ px -> if (state.firstVisibleItemIndex > 0) 1f else state.firstVisibleItemScrollOffset / px }, { px ->
        val info = state.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return@edgeFade 0f
        if (last.index < info.totalItemsCount - 1) 1f
        else (last.offset + last.size + info.afterContentPadding - info.viewportEndOffset) / px
    })

internal fun androidx.compose.ui.Modifier.edgeFade(state: androidx.compose.foundation.lazy.grid.LazyGridState) =
    edgeFade({ px -> if (state.firstVisibleItemIndex > 0) 1f else state.firstVisibleItemScrollOffset / px }, { px ->
        val info = state.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return@edgeFade 0f
        if (last.index < info.totalItemsCount - 1) 1f
        else (last.offset.y + last.size.height + info.afterContentPadding - info.viewportEndOffset) / px
    })

/** verticalScroll with soft edges (see edgeFade). */
internal fun androidx.compose.ui.Modifier.fadingVerticalScroll() = composed {
    val state = androidx.compose.foundation.rememberScrollState()
    edgeFade(state).verticalScroll(state)
}

/** Reduce Transparency is in effect (the setting, or Android's high contrast): glass controls show as nearly solid. */
internal val LocalSolidGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Android 14+ high contrast (Settings › Accessibility › Contrast). Earlier versions don't expose it, so it's off. */
@androidx.compose.runtime.Composable
internal fun rememberSystemHighContrast(): Boolean {
    if (android.os.Build.VERSION.SDK_INT < 34) return false
    val context = androidx.compose.ui.platform.LocalContext.current
    val ui = androidx.compose.runtime.remember { context.getSystemService(android.app.UiModeManager::class.java) }
    val contrast = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(ui?.contrast ?: 0f) }
    androidx.compose.runtime.DisposableEffect(ui) {
        val listener = android.app.UiModeManager.ContrastChangeListener { contrast.floatValue = it }
        ui?.addContrastChangeListener(context.mainExecutor, listener)
        onDispose { ui?.removeContrastChangeListener(listener) }
    }
    // Android's levels are 0 (standard), 0.5 (medium) and 1 (high).
    return contrast.floatValue >= .5f
}

enum class GlassMaterial { CLASSIC, LIQUID }

internal fun usesLiquidGlass(material: GlassMaterial, solid: Boolean): Boolean =
    material == GlassMaterial.LIQUID && !solid

internal enum class GlassRole(val rim: Float, val light: Float, val inset: Boolean = false, val lift: Float = 0f) {
    PANEL(1f, 1f), TILE(.9f, .8f), CAPSULE(1f, .9f), FIELD(.7f, 0f, inset = true), FLOATING(1f, .7f, lift = 1f)
}

@androidx.compose.runtime.Immutable
internal data class GlassSpec(val bodyStart: Color, val bodyEnd: Color, val light: Float, val grain: Float,
    val rimLead: Color, val rimSide: Color, val rimTrail: Color, val lift: Float)

internal const val LIQUID_FROST = .30f
internal const val LIBRARY_PANEL_FROST = .48f
internal const val SEARCH_FIELD_FROST = .12f
internal const val LIQUID_OVERLAY_FROST = .72f
internal const val LIBRARY_TILE_FROST = .22f
internal const val LIBRARY_FIELD_FROST = .2f
internal const val LIBRARY_BLUR_DP = 40
internal const val LIBRARY_BLUR_STEPS = 10
private const val BLUR_LAYER_ALPHA = .001f
internal const val LIBRARY_WASH_BLURRED = .3f
internal const val LIBRARY_WASH_FLAT = .42f

internal val LibraryShown = androidx.compose.runtime.mutableFloatStateOf(0f)

internal fun libraryShown(pagerPosition: Float, libraryPage: Int): Float =
    (1f - kotlin.math.abs(pagerPosition - libraryPage)).coerceIn(0f, 1f)

internal fun libraryBlurPx(shown: Float, maxPx: Int): Int {
    val step = kotlin.math.round(shown.coerceIn(0f, 1f) * LIBRARY_BLUR_STEPS)
    return (maxPx * step / LIBRARY_BLUR_STEPS).toInt()
}

internal fun libraryWash(inkIsDark: Boolean, blurred: Boolean): Color =
    (if (inkIsDark) Color.White else Color.Black).copy(alpha = if (blurred) LIBRARY_WASH_BLURRED else LIBRARY_WASH_FLAT)

@androidx.compose.runtime.Composable
internal fun LibraryBackdropBlur(window: android.view.Window, on: Boolean) {
    val maxPx = with(androidx.compose.ui.platform.LocalDensity.current) { LIBRARY_BLUR_DP.dp.roundToPx() }
    androidx.compose.runtime.LaunchedEffect(window, on, maxPx) {
        if (!on) return@LaunchedEffect
        try {
            androidx.compose.runtime.snapshotFlow { libraryBlurPx(LibraryShown.floatValue, maxPx) }.collect { window.blurBehind(it) }
        } finally {
            window.blurBehind(0)
        }
    }
}

private fun android.view.Window.blurBehind(px: Int) {
    val flags = android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
    if (px > 0) addFlags(flags) else clearFlags(flags)
    attributes = attributes.apply { blurBehindRadius = px; dimAmount = BLUR_LAYER_ALPHA }
}

@androidx.compose.runtime.Composable
internal fun rememberCrossWindowBlur(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = androidx.compose.runtime.remember(context) { context.getSystemService(android.view.WindowManager::class.java) }
    val enabled = androidx.compose.runtime.remember(manager) { androidx.compose.runtime.mutableStateOf(manager?.isCrossWindowBlurEnabled == true) }
    androidx.compose.runtime.DisposableEffect(manager) {
        val listener = java.util.function.Consumer<Boolean> { enabled.value = it }
        manager?.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
        onDispose { manager?.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled.value
}

internal object LiquidGlass {
    const val TONE_MIX = .22f
    const val END_FROST = .78f
    const val LIGHT = .14f
    const val GRAIN = .045f
    const val RIM_BRIGHT = .62f
    const val RIM_SIDE = .10f
    const val RIM_FAINT = .26f
    const val LIFT = .22f
}

internal fun liquidGlassSpec(base: Color, frost: Float, role: GlassRole, outline: Float, primary: Int? = null, secondary: Int? = null): GlassSpec {
    val alpha = frost.coerceIn(0f, 1f)
    val start = tintedGlass(base, primary, LiquidGlass.TONE_MIX).copy(alpha = alpha)
    val end = tintedGlass(base, secondary ?: primary, LiquidGlass.TONE_MIX).copy(alpha = alpha * LiquidGlass.END_FROST)
    val edge = (outline / GlassLook().outline).coerceIn(0f, 2f)
    val bright = Color.White.copy(alpha = (LiquidGlass.RIM_BRIGHT * role.rim * edge).coerceIn(0f, 1f))
    val faint = Color.White.copy(alpha = (LiquidGlass.RIM_FAINT * role.rim * edge).coerceIn(0f, 1f))
    val side = Color.White.copy(alpha = (LiquidGlass.RIM_SIDE * edge).coerceIn(0f, 1f))
    return GlassSpec(start, end, LiquidGlass.LIGHT * role.light, LiquidGlass.GRAIN,
        if (role.inset) faint else bright, side, if (role.inset) bright else faint, LiquidGlass.LIFT * role.lift)
}

private object GlassGrain {
    val brush: androidx.compose.ui.graphics.ShaderBrush by lazy {
        val size = 64
        val random = java.util.Random(7)
        val pixels = IntArray(size * size) {
            val v = random.nextInt(256)
            android.graphics.Color.argb(random.nextInt(256), v, v, v)
        }
        val bitmap = android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
        androidx.compose.ui.graphics.ShaderBrush(androidx.compose.ui.graphics.ImageShader(bitmap.asImageBitmap(),
            androidx.compose.ui.graphics.TileMode.Repeated, androidx.compose.ui.graphics.TileMode.Repeated))
    }
}

@androidx.compose.runtime.Composable
internal fun liquidGlassOn(): Boolean = LocalGlassLook.current.liquid && !LocalSolidGlass.current

@androidx.compose.runtime.Composable
internal fun androidx.compose.ui.Modifier.liquidGlassOr(shape: androidx.compose.ui.graphics.Shape, frost: Float, role: GlassRole,
    base: Color = Glass, classic: @androidx.compose.runtime.Composable androidx.compose.ui.Modifier.() -> androidx.compose.ui.Modifier): androidx.compose.ui.Modifier =
    if (liquidGlassOn()) liquidGlass(shape, frost, role, base) else classic()

@androidx.compose.runtime.Composable
internal fun androidx.compose.ui.Modifier.liquidGlass(shape: androidx.compose.ui.graphics.Shape, frost: Float, role: GlassRole,
    base: Color = Glass): androidx.compose.ui.Modifier {
    val tone = LocalWallpaperTone.current
    val outline = LocalGlassLook.current.outline
    val spec = androidx.compose.runtime.remember(base, frost, role, outline, tone.primary, tone.secondary) {
        liquidGlassSpec(base, frost, role, outline, tone.primary, tone.secondary)
    }
    return drawLiquidGlass(shape, spec)
}

internal fun androidx.compose.ui.Modifier.drawLiquidGlass(shape: androidx.compose.ui.graphics.Shape, spec: GlassSpec): androidx.compose.ui.Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rimWidth = 1.2.dp.toPx()
        val rimOutline = shape.createOutline(androidx.compose.ui.geometry.Size(size.width - rimWidth, size.height - rimWidth), layoutDirection, this)
        val body = androidx.compose.ui.graphics.Brush.linearGradient(listOf(spec.bodyStart, spec.bodyEnd),
            androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, size.height))
        val light = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White.copy(alpha = spec.light), Color.Transparent),
            startY = 0f, endY = size.height * .55f)
        val bloom = androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White.copy(alpha = spec.light * .6f), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(size.width * .18f, 0f), radius = maxOf(size.width, size.height) * .6f)
        val rim = androidx.compose.ui.graphics.Brush.linearGradient(
            0f to spec.rimLead, .35f to spec.rimSide, .65f to spec.rimSide, 1f to spec.rimTrail,
            start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width, size.height))
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(rimWidth)
        val shadow = if (spec.lift > 0f) liftShadow(outline, spec.lift, 18.dp.toPx(), 6.dp.toPx()) else null
        onDrawBehind {
            shadow?.let { (path, paint) ->
                drawContext.canvas.nativeCanvas.let { canvas ->
                    canvas.save()
                    canvas.clipOutPath(path)
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
            }
            drawOutline(outline, body)
            if (spec.light > 0f) {
                drawOutline(outline, light)
                drawOutline(outline, bloom)
            }
            drawOutline(outline, GlassGrain.brush, alpha = spec.grain)
            translate(rimWidth / 2f, rimWidth / 2f) { drawOutline(rimOutline, rim, style = stroke) }
        }
    }

private fun liftShadow(outline: androidx.compose.ui.graphics.Outline, lift: Float, blur: Float, drop: Float): Pair<android.graphics.Path, android.graphics.Paint> {
    val path = androidx.compose.ui.graphics.Path().apply { addOutline(outline) }.asAndroidPath()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        setShadowLayer(blur, 0f, drop, Color.Black.copy(alpha = lift).toArgb())
    }
    return path to paint
}
