package com.mccal.folio

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

internal object SceneLight {
    const val ACROSS = .3f
    const val ABOVE = .04f
    const val SETTLE = .003f
    const val SPOT_REACH = .38f
    val UNPLACED = unit(Offset(-.45f, -1f))

    fun source(window: Size, layoutDirection: LayoutDirection): Offset {
        val across = if (layoutDirection == LayoutDirection.Rtl) 1f - ACROSS else ACROSS
        return Offset(window.width * across, -window.height * ABOVE)
    }

    fun toward(size: Size, light: Offset): Offset = unit(light - size.center)

    fun litEdge(size: Size, toward: Offset): Offset {
        val half = size.center
        val reachX = if (toward.x != 0f) half.x / abs(toward.x) else Float.POSITIVE_INFINITY
        val reachY = if (toward.y != 0f) half.y / abs(toward.y) else Float.POSITIVE_INFINITY
        val reach = min(reachX, reachY).takeIf { it.isFinite() } ?: 0f
        return half + toward * reach
    }

    private fun unit(v: Offset): Offset {
        val length = hypot(v.x, v.y)
        return if (length > 0f) v / length else Offset(0f, -1f)
    }

    private val Size.center get() = Offset(width / 2f, height / 2f)
}

internal class LitFrame(size: Size, toward: Offset) {
    private val middle = Offset(size.width / 2f, size.height / 2f)
    val lit = SceneLight.litEdge(size, toward)
    val dark = Offset(size.width - lit.x, size.height - lit.y)
    val span = (dark - lit).getDistance()
    val angle = Math.toDegrees(atan2((dark - lit).y, (dark - lit).x).toDouble()).toFloat()

    fun inward(share: Float): Offset = lit + (middle - lit) * share
}

internal class PlacedGradient private constructor(private val shader: android.graphics.Shader) {
    private val matrix = android.graphics.Matrix()
    val brush = ShaderBrush(shader)

    fun along(from: Offset, degrees: Float, length: Float): PlacedGradient = apply {
        matrix.setScale(length.coerceAtLeast(1f), length.coerceAtLeast(1f))
        matrix.postRotate(degrees)
        matrix.postTranslate(from.x, from.y)
        shader.setLocalMatrix(matrix)
    }

    fun around(center: Offset, radius: Float): PlacedGradient = apply {
        matrix.setScale(radius.coerceAtLeast(1f), radius.coerceAtLeast(1f))
        matrix.postTranslate(center.x, center.y)
        shader.setLocalMatrix(matrix)
    }

    companion object {
        fun linear(vararg stops: Pair<Float, Color>) = PlacedGradient(android.graphics.LinearGradient(0f, 0f, 1f, 0f,
            IntArray(stops.size) { stops[it].second.toArgb() }, FloatArray(stops.size) { stops[it].first },
            android.graphics.Shader.TileMode.CLAMP))

        fun radial(color: Color) = PlacedGradient(android.graphics.RadialGradient(0f, 0f, 1f,
            color.toArgb(), color.copy(alpha = 0f).toArgb(), android.graphics.Shader.TileMode.CLAMP))

        fun spot(color: Color) = PlacedGradient(android.graphics.RadialGradient(0f, 0f, 1f,
            intArrayOf(color.toArgb(), color.copy(alpha = color.alpha * .45f).toArgb(), color.copy(alpha = 0f).toArgb()),
            floatArrayOf(0f, .3f, 1f), android.graphics.Shader.TileMode.CLAMP))
    }
}

internal abstract class SceneLitNode : Modifier.Node(), GlobalPositionAwareModifierNode, DrawModifierNode {
    protected var toward = SceneLight.UNPLACED
        private set

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!coordinates.isAttached) return
        val root = coordinates.findRootCoordinates()
        val light = coordinates.localPositionOf(root, SceneLight.source(root.size.toSize(), requireLayoutDirection()))
        val next = SceneLight.toward(coordinates.size.toSize(), light)
        if ((next - toward).getDistance() > SceneLight.SETTLE) {
            toward = next
            invalidateDraw()
        }
    }
}
