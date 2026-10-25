package com.mccal.folio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Fold motion: three small touches on the open screen as the hinge moves, none of them blur. Each follows the same value the fold
 * effect already follows (so they run both ways, and a half-closed phone opened again is one motion).
 *
 *  - **Ripple**: icons settle in a wave that starts at the hinge and spreads outward, a little smaller and dimmer at first.
 *  - **Depth**: the wallpaper and the icons move by slightly different amounts, so the content feels locked in space.
 *  - **Light**: a faint band of light travels down the hinge as it opens.
 *
 * All of it is a transform in a graphics layer or one gradient in a draw lambda, read per frame without recomposing, and nothing at
 * all is applied at rest. Mocked in the Lab (scene fold-motion) on 7 Oct 2026.
 */
internal data class RippleValues(val scale: Float, val translateYDp: Float, val alpha: Float)

internal object FoldMotionMath {
    /** How far apart the icons nearest and farthest from the hinge start, as a share of the whole unfold. */
    const val RIPPLE_SPREAD = .45f
    const val RIPPLE_MIN_SCALE = .88f
    const val RIPPLE_LIFT_DP = 10f
    const val RIPPLE_MIN_ALPHA = .7f
    const val WALLPAPER_SHIFT_DP = 16f
    /** The wallpaper grows a little as it moves, so its edges never show; exactly 1 at rest. */
    const val WALLPAPER_GROW = .045f
    const val ICON_SHIFT_DP = 8f
    /** The brightest the hinge light gets. */
    const val LIGHT_ALPHA = .3f

    /** 0 while the screen is closing or opening (amount 1), 1 flat (amount 0). */
    fun progress(amount: Float) = 1f - amount.coerceIn(0f, 1f)

    private fun easeOutBack(t: Float): Float { val c1 = 1.2f; val c3 = c1 + 1f; val u = t - 1f; return 1f + c3 * u * u * u + c1 * u * u }

    /**
     * An icon's own progress is later the farther it is from the hinge ([distance] 0 at the hinge, 1 at the far edge). Folding it is the
     * same wave backwards: the far icons are the first to leave their place.
     */
    fun ripple(amount: Float, distance: Float): RippleValues {
        val p = progress(amount)
        val q = ((p - distance.coerceIn(0f, 1f) * RIPPLE_SPREAD) / (1f - RIPPLE_SPREAD)).coerceIn(0f, 1f)
        return RippleValues(RIPPLE_MIN_SCALE + (1f - RIPPLE_MIN_SCALE) * easeOutBack(q), (1f - q) * RIPPLE_LIFT_DP, RIPPLE_MIN_ALPHA + (1f - RIPPLE_MIN_ALPHA) * q)
    }

    fun wallpaperShiftDp(amount: Float) = -WALLPAPER_SHIFT_DP * amount.coerceIn(0f, 1f)
    fun wallpaperScale(amount: Float) = 1f + WALLPAPER_GROW * amount.coerceIn(0f, 1f)
    fun iconShiftDp(amount: Float) = ICON_SHIFT_DP * amount.coerceIn(0f, 1f)

    /** Nothing at either end, strongest half way. */
    fun lightAlpha(amount: Float) = sin(PI.toFloat() * progress(amount)) * LIGHT_ALPHA
    /** Where along the hinge the light is, 0 at the start of the hinge to 1 at the end. */
    fun lightPosition(amount: Float) = progress(amount)
}

/** What the motion needs to know about the screen: the live amount (read only in layer and draw lambdas) and where the hinge is. */
internal class FoldMotionScope(
    val amount: () -> Float,
    val hingeIsHorizontal: Boolean, val hingePx: Float, val farthestPx: Float,
) {
    /** 0 on the hinge, 1 at the farthest edge, for a point at window position ([x], [y]) in pixels. */
    fun distance(x: Float, y: Float): Float = (abs((if (hingeIsHorizontal) y else x) - hingePx) / farthestPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
}

internal val LocalFoldMotion = compositionLocalOf<FoldMotionScope?> { null }

/** Put on an icon next to `.jiggle(...)`: the ripple and the icon half of the depth. Costs nothing at rest. */
@Composable
internal fun Modifier.foldMotionIcon(): Modifier {
    val scope = LocalFoldMotion.current ?: return this
    val center = remember { FloatArray(2) }
    return this
        .onGloballyPositioned { c -> val p = c.positionInWindow(); center[0] = p.x + c.size.width / 2f; center[1] = p.y + c.size.height / 2f }
        .graphicsLayer {
            val a = scope.amount()
            if (a <= .001f) return@graphicsLayer
            val v = FoldMotionMath.ripple(a, scope.distance(center[0], center[1]))
            scaleX = v.scale; scaleY = v.scale; translationY = v.translateYDp * density; alpha = v.alpha
            translationX = FoldMotionMath.iconShiftDp(a) * density
        }
}

/** Put on Folio's own wallpaper: the wallpaper half of the depth. */
@Composable
internal fun Modifier.foldMotionWallpaper(): Modifier {
    val scope = LocalFoldMotion.current ?: return this
    return graphicsLayer {
        val a = scope.amount()
        if (a <= .001f) return@graphicsLayer
        val s = FoldMotionMath.wallpaperScale(a); scaleX = s; scaleY = s
        translationX = FoldMotionMath.wallpaperShiftDp(a) * density
    }
}

/** The light: a soft band along the hinge that travels from one end of it to the other, brightest half way. Drawn only while it is visible. */
@Composable
internal fun FoldMotionLight(scope: FoldMotionScope) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
        val amount = scope.amount()
        val alpha = FoldMotionMath.lightAlpha(amount)
        if (amount <= .001f || alpha <= .004f) return@Canvas
        val along = if (scope.hingeIsHorizontal) size.width else size.height
        val half = 46.dp.toPx()
        val at = FoldMotionMath.lightPosition(amount) * along
        val reach = (along * .22f).coerceAtLeast(1f)
        val white = Color.White
        // The band across the hinge, then a mask along it so the light fades toward both ends of its travel.
        val band = Brush.linearGradient(
            0f to Color.Transparent, .5f to white.copy(alpha = alpha), 1f to Color.Transparent,
            start = if (scope.hingeIsHorizontal) Offset(0f, scope.hingePx - half) else Offset(scope.hingePx - half, 0f),
            end = if (scope.hingeIsHorizontal) Offset(0f, scope.hingePx + half) else Offset(scope.hingePx + half, 0f))
        drawRect(band)
        val mask = Brush.linearGradient(
            0f to Color.Transparent, .5f to Color.Black, 1f to Color.Transparent,
            start = if (scope.hingeIsHorizontal) Offset(at - reach, 0f) else Offset(0f, at - reach),
            end = if (scope.hingeIsHorizontal) Offset(at + reach, 0f) else Offset(0f, at + reach))
        drawRect(mask, blendMode = BlendMode.DstIn)
    }
}
