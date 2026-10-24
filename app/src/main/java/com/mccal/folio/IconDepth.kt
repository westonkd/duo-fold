package com.mccal.folio

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal object ContinuousCorners {
    const val SPAN = 1.52866483f
    private val curve = floatArrayOf(
        1.08849323f, 0f, .86840689f, 0f, .66993427f, .06549600f,
        .37282392f, .17375683f, .17375683f, .37282392f, .06549600f, .66993427f,
        0f, .86840689f, 0f, 1.08849323f, 0f, SPAN)

    fun radius(width: Float, height: Float, radius: Float): Float = radius.coerceIn(0f, minOf(width, height) / 2f / SPAN)

    fun trace(width: Float, height: Float, radius: Float, moveTo: (Float, Float) -> Unit, lineTo: (Float, Float) -> Unit,
        cubicTo: (Float, Float, Float, Float, Float, Float) -> Unit) {
        val r = radius(width, height, radius)
        val reach = SPAN * r
        fun corner(map: (Float, Float) -> Offset) {
            for (i in curve.indices step 6) {
                val a = map(curve[i], curve[i + 1]); val b = map(curve[i + 2], curve[i + 3]); val c = map(curve[i + 4], curve[i + 5])
                cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
            }
        }
        moveTo(reach, 0f)
        lineTo(width - reach, 0f)
        corner { x, y -> Offset(width - x * r, y * r) }
        lineTo(width, height - reach)
        corner { x, y -> Offset(width - y * r, height - x * r) }
        lineTo(reach, height)
        corner { x, y -> Offset(x * r, height - y * r) }
        lineTo(0f, reach)
        corner { x, y -> Offset(y * r, x * r) }
    }
}

@Immutable
internal data class ContinuousRoundedShape(val fraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        ContinuousCorners.trace(size.width, size.height, size.minDimension * fraction, path::moveTo, path::lineTo, path::cubicTo)
        path.close()
        return Outline.Generic(path)
    }
}

internal val IconContinuousShape = ContinuousRoundedShape(ICON_CORNER_FRACTION)

internal object IconLight {
    const val RIM_LEAD = .5f
    const val RIM_SIDE = .08f
    const val RIM_TRAIL = .24f
    const val SHEEN = .14f
    const val SHADE = .08f
    const val WASH = .1f
    const val EDGE_ALPHA = 200
    const val RIM_WIDTH = .014f
}

@Immutable
internal data class IconLighting(val rimLead: Color, val rimSide: Color, val rimTrail: Color, val sheen: Float, val shade: Float, val wash: Color?)

internal fun iconLighting(wallpaper: Int?, dimmed: Boolean): IconLighting {
    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
    val wash = wallpaper?.let { lerp(Color.White, Color(it).copy(alpha = 1f), IconLight.WASH) }
    return IconLighting(white(IconLight.RIM_LEAD), white(IconLight.RIM_SIDE), white(IconLight.RIM_TRAIL),
        if (dimmed) IconLight.SHEEN / 2f else IconLight.SHEEN, IconLight.SHADE, wash)
}

internal fun fillsEdges(width: Int, height: Int, alphaAt: (Int, Int) -> Int): Boolean {
    if (width < 8 || height < 8) return false
    val inset = maxOf(1, width / 48)
    return listOf(width / 2 to inset, width / 2 to height - 1 - inset, inset to height / 2, width - 1 - inset to height / 2)
        .all { (x, y) -> alphaAt(x, y) >= IconLight.EDGE_ALPHA }
}

internal fun Modifier.iconLighting(shape: Shape, light: IconLighting): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val rimWidth = (size.minDimension * IconLight.RIM_WIDTH).coerceIn(1f, 1.dp.toPx())
    val rimOutline = shape.createOutline(Size(size.width - rimWidth, size.height - rimWidth), layoutDirection, this)
    val rim = Brush.linearGradient(0f to light.rimLead, .4f to light.rimSide, .6f to light.rimSide, 1f to light.rimTrail,
        start = Offset.Zero, end = Offset(size.width, size.height))
    val sheen = Brush.verticalGradient(listOf(Color.White.copy(alpha = light.sheen), Color.Transparent), startY = 0f, endY = size.height * .5f)
    val shade = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = light.shade)), startY = size.height * .6f, endY = size.height)
    val stroke = Stroke(rimWidth)
    onDrawWithContent {
        drawContent()
        light.wash?.let { drawOutline(outline, it, blendMode = BlendMode.Multiply) }
        drawOutline(outline, sheen)
        drawOutline(outline, shade)
        translate(rimWidth / 2f, rimWidth / 2f) { drawOutline(rimOutline, rim, style = stroke) }
    }
}
