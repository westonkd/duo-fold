package com.mccal.folio

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
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
    const val RIM_LEAD = .72f
    const val RIM_SIDE = .1f
    const val RIM_TRAIL = .3f
    const val SHEEN = .2f
    const val SHADE = .14f
    const val WASH = .1f
    const val GLINT = .3f
    const val EDGE = .14f
    const val EDGE_ALPHA = 200
    const val RIM_WIDTH = .014f
    const val SHADOW_DP = 3f
    const val SHADOW_AMBIENT = .18f
    const val SHADOW_SPOT = .32f
    const val RIM_SPOT = .9f
    const val WIDGET_SHEEN = .1f
    const val WIDGET_SHADE = .06f
    const val WIDGET_GLINT = .18f
    const val WIDGET_FILL_ALPHA = 96
}

internal fun Modifier.iconRestingShadow(): Modifier = shadow(IconLight.SHADOW_DP.dp, IconContinuousShape, clip = false,
    ambientColor = Color.Black.copy(alpha = IconLight.SHADOW_AMBIENT), spotColor = Color.Black.copy(alpha = IconLight.SHADOW_SPOT))

@Immutable
internal data class IconLighting(val rimLead: Color, val rimSide: Color, val rimTrail: Color, val sheen: Float, val shade: Float, val wash: Color?,
    val glint: Float = 0f, val edge: Color = Color.Transparent)

internal fun iconLighting(wallpaper: Int?): IconLighting {
    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
    val wash = wallpaper?.let { lerp(Color.White, Color(it).copy(alpha = 1f), IconLight.WASH) }
    return IconLighting(white(IconLight.RIM_LEAD), white(IconLight.RIM_SIDE), white(IconLight.RIM_TRAIL),
        IconLight.SHEEN, IconLight.SHADE, wash, IconLight.GLINT, Color.Black.copy(alpha = IconLight.EDGE))
}

internal fun widgetLighting(): IconLighting {
    fun white(alpha: Float) = Color.White.copy(alpha = alpha)
    return IconLighting(white(IconLight.RIM_LEAD), white(IconLight.RIM_SIDE), white(IconLight.RIM_TRAIL),
        IconLight.WIDGET_SHEEN, IconLight.WIDGET_SHADE, null, IconLight.WIDGET_GLINT, Color.Black.copy(alpha = IconLight.EDGE))
}

internal fun fillsBox(width: Int, height: Int, alphaAt: (Int, Int) -> Int): Boolean {
    if (width < 8 || height < 8) return false
    val inset = maxOf(1, minOf(width, height) / 32)
    val along = listOf(.2f, .5f, .8f)
    val points = along.flatMap { share ->
        val x = (width * share).toInt()
        val y = (height * share).toInt()
        listOf(x to inset, x to height - 1 - inset, inset to y, width - 1 - inset to y)
    }
    return points.all { (x, y) -> alphaAt(x, y) >= IconLight.WIDGET_FILL_ALPHA }
}

internal fun fillsEdges(width: Int, height: Int, alphaAt: (Int, Int) -> Int): Boolean {
    if (width < 8 || height < 8) return false
    val inset = maxOf(1, width / 48)
    return listOf(width / 2 to inset, width / 2 to height - 1 - inset, inset to height / 2, width - 1 - inset to height / 2)
        .all { (x, y) -> alphaAt(x, y) >= IconLight.EDGE_ALPHA }
}

internal fun Modifier.iconLighting(shape: Shape, light: IconLighting): Modifier = this then IconLightingElement(shape, light)

private data class IconLightingElement(val shape: Shape, val light: IconLighting) : ModifierNodeElement<IconLightingNode>() {
    override fun create() = IconLightingNode(shape, light)
    override fun update(node: IconLightingNode) = node.restyle(shape, light)
}

private class IconLightingNode(private var shape: Shape, private var light: IconLighting) : SceneLitNode() {
    private class Cut(val size: Size, val outline: Outline, val rimOutline: Outline, val rimWidth: Float)

    private class Lights(light: IconLighting) {
        val sheen = PlacedGradient.linear(0f to Color.White.copy(alpha = light.sheen), 1f to Color.Transparent)
        val shade = PlacedGradient.linear(0f to Color.Transparent, 1f to Color.Black.copy(alpha = light.shade))
        val glint = PlacedGradient.radial(Color.White.copy(alpha = light.glint))
        val rim = PlacedGradient.linear(0f to light.rimLead, .4f to light.rimSide, .6f to light.rimSide, 1f to light.rimTrail)
        val rimSpot = PlacedGradient.spot(light.rimLead.copy(alpha = light.rimLead.alpha * IconLight.RIM_SPOT))
    }

    private var cut: Cut? = null
    private var lights = Lights(light)

    fun restyle(shape: Shape, light: IconLighting) {
        if (shape == this.shape && light == this.light) return
        if (light != this.light) lights = Lights(light)
        this.shape = shape
        this.light = light
        cut = null
        invalidateDraw()
    }

    private fun DrawScope.cutFor(size: Size): Cut = cut?.takeIf { it.size == size } ?: run {
        val rimWidth = (size.minDimension * IconLight.RIM_WIDTH).coerceIn(1f, 1.dp.toPx())
        Cut(size, shape.createOutline(size, layoutDirection, this),
            shape.createOutline(Size(size.width - rimWidth, size.height - rimWidth), layoutDirection, this), rimWidth).also { cut = it }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val cut = cutFor(size)
        val outline = cut.outline
        val frame = LitFrame(size, toward)
        light.wash?.let { drawOutline(outline, it, blendMode = BlendMode.Multiply) }
        drawOutline(outline, lights.sheen.along(frame.lit, frame.angle, frame.span * .5f).brush)
        drawOutline(outline, lights.shade.along(frame.lit + (frame.dark - frame.lit) * .6f, frame.angle, frame.span * .4f).brush)
        if (light.glint > 0f) drawOutline(outline, lights.glint.around(frame.inward(.14f), size.minDimension * .34f).brush)
        if (light.edge.alpha > 0f) drawOutline(outline, light.edge, style = Stroke(cut.rimWidth * .8f))
        val nudge = cut.rimWidth / 2f
        translate(nudge, nudge) {
            val stroke = Stroke(cut.rimWidth)
            val lit = frame.lit - Offset(nudge, nudge)
            drawOutline(cut.rimOutline, lights.rim.along(lit, frame.angle, frame.span).brush, style = stroke)
            drawOutline(cut.rimOutline, lights.rimSpot.around(lit, size.minDimension * SceneLight.SPOT_REACH).brush, style = stroke)
        }
    }
}
