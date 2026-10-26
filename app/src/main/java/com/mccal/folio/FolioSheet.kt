package com.mccal.folio

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Fades or slides an overlay into place, and never leaves it invisible.
 *
 * These entrances run on the frame clock, which stops while Folio isn't drawing. A sheet opened just as Folio went
 * behind a system permission screen could stay at its starting value: fully transparent, or slid off screen. The
 * window was still there and still took every tap, so Home sat blurred behind an overlay nobody could see or close,
 * until the phone restarted. The timeout here puts the overlay in place even when no frame ever arrived.
 */
@Composable
internal fun rememberEntrance(spring: Pair<Float, Float>, from: Float = 0f, to: Float = 1f, scenario: PerfScenario? = null) =
    rememberEntrance(stiffness = spring.second, dampingRatio = spring.first, from = from, to = to, scenario = scenario)

@Composable
/** [scenario] tells the Performance log what these frames are, so a slow sheet or menu shows up by name. */
internal fun rememberEntrance(stiffness: Float, dampingRatio: Float = 1f, from: Float = 0f, to: Float = 1f, scenario: PerfScenario? = null):
    androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val reduceMotion = LocalReduceMotion.current
    val entrance = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (reduceMotion) to else from) }
    // The animation runs in the composition, which is where the frame clock lives.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        scenario?.let(PerfLog::begin)
        try {
            entrance.animateTo(to, FolioMotion.spring<Float>(dampingRatio to stiffness))
        } finally { scenario?.let(PerfLog::end) }
    }
    // The timeout must not depend on frames, so it runs on the main thread alone. Snapping cancels the animation.
    DisposableEffect(Unit) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
        scope.launch { kotlinx.coroutines.delay(ENTRANCE_TIMEOUT_MS); if (entrance.value != to) entrance.snapTo(to) }
        onDispose { scope.cancel() }
    }
    return entrance
}

private const val ENTRANCE_TIMEOUT_MS = 900L

/**
 * Reduce Motion's entrance, used next to [rememberEntrance]: with Reduce Motion on, the entrance starts in place and this
 * fades the surface in over [FolioMotion.FADE_MS]; with it off this stays at 1. Like the entrance, it never leaves the
 * surface invisible when frames stop.
 */
@Composable
internal fun rememberReducedFade(): androidx.compose.runtime.State<Float> {
    val reduceMotion = LocalReduceMotion.current
    val fade = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (fade.value < 1f) fade.animateTo(1f, androidx.compose.animation.core.tween(FolioMotion.FADE_MS)) }
    DisposableEffect(Unit) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
        scope.launch { kotlinx.coroutines.delay(ENTRANCE_TIMEOUT_MS); if (fade.value != 1f) fade.snapTo(1f) }
        onDispose { scope.cancel() }
    }
    return fade.asState()
}

/**
 * A 0–1 overlay progress that springs to [target] and, like [rememberEntrance], snaps there if frames stall (Folio in
 * the background, a stuck window), so a closed panel can never stay on screen or leave Home blurred.
 */
@Composable
internal fun rememberSettlingProgress(target: Float, spec: androidx.compose.animation.core.AnimationSpec<Float>,
    /** Longer than the slowest spring takes to settle, so a normal close never jumps. */
    timeoutMs: Long = 1_500L): androidx.compose.runtime.State<Float> {
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(target) }
    androidx.compose.runtime.LaunchedEffect(target) { progress.animateTo(target, spec) }
    DisposableEffect(target) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate)
        scope.launch { kotlinx.coroutines.delay(timeoutMs); if (progress.value != target) progress.snapTo(target) }
        onDispose { scope.cancel() }
    }
    return progress.asState()
}

@Composable
internal fun FolioSheetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FolioSheetColors, typography = MaterialTheme.typography) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLightSheet provides true, LocalAccent provides FolioAccents.TealOnLight,
            androidx.compose.material3.LocalContentColor provides FolioColors.LightLabel, content = content)
    }
}

internal val LocalLightSheet = androidx.compose.runtime.staticCompositionLocalOf { false }

private fun android.view.Window.lightSystemBars() {
    androidx.core.view.WindowCompat.getInsetsController(this, decorView).apply {
        isAppearanceLightNavigationBars = true
        isAppearanceLightStatusBars = true
    }
}

/**
 * Folio's sheet: same API as Material's ModalBottomSheet (this package-level function shadows the
 * star-imported one in launcher files), but iOS's light grouped sheet, a slim handle, and Home blurred behind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Unspecified,
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    /** iOS Settings-style full-screen page that slides in, instead of a bottom sheet. */
    fullScreen: Boolean = false,
    /** Widest the sheet gets when shown as a centered form sheet on a regular-size screen. */
    formWidth: androidx.compose.ui.unit.Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    RegisterOpenWindow(LauncherSheets)
    if (fullScreen) { FullScreenPage(onDismissRequest, content); return }
    // Regular size (inner screen, either orientation): an iPad-style form sheet centered over Home instead of a stretched bottom sheet.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    if (configuration.fitsRegularHomeLayout()) {
        FormSheet(onDismissRequest, properties.shouldDismissOnBackPress, formWidth, modifier, content); return
    }
    FolioSheetTheme {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = FolioRadius.SHEET_TOP.dp, topEnd = FolioRadius.SHEET_TOP.dp),
            containerColor = FolioColors.LightBackground, contentColor = FolioColors.LightLabel,
            scrimColor = Color.Black.copy(alpha = .35f),
            dragHandle = { Box(Modifier.padding(top = FolioSpace.COMPACT.dp, bottom = FolioSpace.SNUG.dp).size(width = 36.dp, height = 5.dp)
                .background(FolioColors.LightTertiaryLabel, CircleShape)) },
            properties = properties, content = {
                // The sheet is its own window; hide the status bar there too so Home stays edge to edge.
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.LaunchedEffect(view) {
                    (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { w ->
                        androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                        }
                        w.lightSystemBars()
                    }
                }
                content()
            },
        )
    }
}

@Composable
private fun FullScreenPage(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnBackPress = false)) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.LaunchedEffect(view) {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { w ->
                w.setDimAmount(0f)
                w.setWindowAnimations(0)
                androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
                w.lightSystemBars()
            }
        }
        val slide = rememberEntrance(FolioMotion.Sheet, from = 1f, to = 0f, scenario = PerfScenario.SHEET)
        val pageFade = rememberReducedFade()
        // Once the page covers Home, drawing and blurring Home is work nobody sees, and it shows up as stutter
        // while you're moving between Folio and Android's permission screens. It comes back the moment it's needed.
        val covering = slide.value == 0f
        RegisterOpenWindow(LauncherPages, active = covering)
        FolioSheetTheme {
            Box(Modifier.fillMaxSize()) {
                // Predictive back: the page eases right and shrinks a little with the swipe, like Android's own screens.
                val back = androidx.compose.animation.core.animateFloatAsState(SheetBackProgress.floatValue,
                    FolioMotion.spring(FolioMotion.Follow), label = "sheet back").value
                androidx.compose.material3.Surface(Modifier.fillMaxSize().graphicsLayer {
                    translationX = size.width * slide.value + size.width * .08f * back
                    alpha = pageFade.value
                    scaleX = 1f - .1f * back; scaleY = scaleX
                    shape = androidx.compose.foundation.shape.RoundedCornerShape((FolioRadius.SHEET_TOP * back).dp); clip = back > 0f
                }, color = FolioColors.LightBackground, contentColor = FolioColors.LightLabel) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.folioSafeTop).navigationBarsPadding()
                        .windowInsetsPadding(WindowInsets.ime), content = content)
                }
            }
        }
    }
}

@Composable
private fun FormSheet(onDismissRequest: () -> Unit, dismissOnBack: Boolean, width: androidx.compose.ui.unit.Dp, modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnBackPress = dismissOnBack)) {
        FolioDialogWindow(dim = 0f)
        val appear = rememberEntrance(FolioMotion.Sheet, scenario = PerfScenario.SHEET)
        val fade = rememberReducedFade()
        FolioSheetTheme {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value * fade.value }.background(Color.Black.copy(alpha = .35f))
                .clickable(androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = onDismissRequest))
            FoldAvoidingBox(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(FolioSpace.XXL.dp)) {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val maxSheetHeight = minOf(maxHeight, 760.dp)
                    androidx.compose.material3.Surface(modifier.widthIn(max = width).fillMaxWidth().heightIn(max = maxSheetHeight)
                        .graphicsLayer {
                            // Slides up into place, like a sheet presented on iPad.
                            alpha = appear.value * fade.value
                            translationY = (1f - appear.value) * size.height * .12f
                        },
                        shape = RoundedCornerShape(FolioRadius.SHEET_TOP.dp), color = FolioColors.LightBackground, contentColor = FolioColors.LightLabel) {
                        androidx.compose.foundation.layout.Column(Modifier.padding(top = FolioSpace.COMFY.dp), content = content)
                    }
                }
            }
        }
    }
}

/**
 * iOS alert with Material's AlertDialog API (this package-level function shadows the star-imported one): iOS 26's
 * light alert, a rounded card with the title and message centered, and its buttons as capsules side by side (stacked
 * when there are three, the preferred one first). The preferred button is in the accent's ink and semibold, like iOS.
 */
@Composable
internal fun AlertDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null,
    /** A third choice between the two: with it the buttons stack, the preferred one first and in bold, like iOS. */
    neutralButton: (@Composable () -> Unit)? = null,
    width: androidx.compose.ui.unit.Dp = 300.dp,
    buttons: (@Composable ColumnScope.() -> Unit)? = null) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FolioDialogWindow(dim = .3f)
        val appear = rememberEntrance(FolioMotion.Menu, scenario = PerfScenario.MENU)
        val fade = rememberReducedFade()
        val base = MaterialTheme.typography
        fun buttons(weight: androidx.compose.ui.text.font.FontWeight) = base.copy(labelLarge = androidx.compose.ui.text.TextStyle(fontSize = FolioType.BODY.sp, fontWeight = weight))
        FoldAvoidingBox(Modifier.windowInsetsPadding(WindowInsets.safeDrawing), role = FoldRole.INFO) {
            FolioSheetTheme {
                val preferred = FolioSheetColors.copy(primary = LocalAccent.current.ink)
                val plain = FolioSheetColors.copy(primary = FolioColors.LightLabel)
                @Composable fun capsule(rowModifier: Modifier, colors: androidx.compose.material3.ColorScheme,
                    weight: androidx.compose.ui.text.font.FontWeight, content: @Composable () -> Unit) {
                    Box(rowModifier.heightIn(min = FolioRow.ACTION.dp).clip(CircleShape).background(FolioColors.LightFill),
                        contentAlignment = androidx.compose.ui.Alignment.Center, propagateMinConstraints = true) {
                        MaterialTheme(colorScheme = colors, typography = buttons(weight), content = content)
                    }
                }
                androidx.compose.foundation.layout.Column(modifier.width(width)
                    .graphicsLayer { alpha = appear.value * fade.value; scaleX = 1.12f - .12f * appear.value; scaleY = scaleX }
                    .clip(RoundedCornerShape(FolioRadius.ALERT.dp)).background(FolioColors.LightCard.copy(alpha = .98f))
                    .padding(FolioSpace.LARGE.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FolioSpace.LARGE.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = FolioSpace.SMALL.dp, start = FolioSpace.SMALL.dp, end = FolioSpace.SMALL.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FolioSpace.SNUG.dp)) {
                        title?.let { androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(color = FolioColors.LightLabel, fontSize = FolioType.BODY.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center), it) }
                        text?.let { androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(color = FolioColors.LightLabel, fontSize = FolioType.FOOTNOTE.sp,
                            lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)) { Box(Modifier.heightIn(max = 420.dp)) { it() } } }
                    }
                    if (buttons != null) {
                        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FolioSpace.SMALL.dp), content = buttons)
                    } else if (neutralButton != null) {
                        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FolioSpace.SMALL.dp)) {
                            capsule(Modifier.fillMaxWidth(), preferred, androidx.compose.ui.text.font.FontWeight.SemiBold, confirmButton)
                            capsule(Modifier.fillMaxWidth(), plain, androidx.compose.ui.text.font.FontWeight.Normal, neutralButton)
                            dismissButton?.let { capsule(Modifier.fillMaxWidth(), plain, androidx.compose.ui.text.font.FontWeight.Normal, it) }
                        }
                    } else androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FolioSpace.SMALL.dp)) {
                        dismissButton?.let { capsule(Modifier.weight(1f), plain, androidx.compose.ui.text.font.FontWeight.Normal, it) }
                        capsule(Modifier.weight(1f), preferred, androidx.compose.ui.text.font.FontWeight.SemiBold, confirmButton)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlertButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, destructive: Boolean = false, preferred: Boolean = false) {
    Box(modifier.fillMaxWidth().heightIn(min = FolioRow.ACTION.dp).clip(CircleShape).background(FolioColors.LightFill)
        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.Text(text, fontSize = FolioType.BODY.sp,
            color = when { destructive -> FolioColors.RedLight; preferred -> LocalAccent.current.ink; else -> FolioColors.LightLabel },
            fontWeight = if (preferred || destructive) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal)
    }
}

/**
 * Folio's overlay windows (form sheets, alerts, folders): no system window animation, Android's status bar
 * stays hidden like on Home (so nothing shifts), and Home dims and blurs behind, like iOS materials.
 */
@Composable
internal fun FolioDialogWindow(dim: Float, blurRadiusDp: Int = 0) {
    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.DisposableEffect(view) {
        (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { w ->
            if (dim > 0f) w.setDimAmount(dim) else w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setWindowAnimations(0)
            if (blurRadiusDp > 0) {
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                w.attributes = w.attributes.apply { blurBehindRadius = with(density) { blurRadiusDp.dp.roundToPx() } }
            }
            androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose { }
    }
}
