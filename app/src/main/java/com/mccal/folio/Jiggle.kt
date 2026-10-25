package com.mccal.folio

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** iPhone-style "jiggle mode" for editing Home: icons wiggle, show a remove button, and move with one drag. */
@Stable
internal class HomeEditMode {
    var active by mutableStateOf(false)
        private set
    /** Removes an item from Home (it stays in the App Library); set by LauncherScreen. */
    var onRemove: (DropTarget) -> Unit = {}
    /** Home cell that was long-pressed to start editing, for "Edit" and "+". */
    var lastEmptyIndex: Int? = null
    fun start() { active = true }
    fun stop() { active = false }
}

internal val LocalHomeEdit = staticCompositionLocalOf { HomeEditMode() }

/** Shared wiggle phase (counted in wiggles) and how far into the wiggle Home has eased, while jiggling. Read only in the draw phase. */
internal class JiggleClock(val phase: State<Float>, val amplitude: State<Float>)

internal val LocalJiggle = compositionLocalOf<JiggleClock?> { null }

internal object JiggleMotion {
    const val WIGGLE_MS = 280
    const val CYCLE = 25
    const val RATE_SPREAD = 2
    const val DEGREES = 1.5f
}

@Composable
internal fun ProvideJiggle(edit: HomeEditMode, content: @Composable () -> Unit) {
    val clock = if (edit.active && !LocalReduceMotion.current) {
        val transition = rememberInfiniteTransition(label = "jiggle")
        val phase = transition.animateFloat(0f, JiggleMotion.CYCLE.toFloat(),
            infiniteRepeatable(tween(JiggleMotion.WIGGLE_MS * JiggleMotion.CYCLE, easing = LinearEasing)), label = "jiggle phase")
        val amplitude = remember { Animatable(0f) }
        LaunchedEffect(Unit) { amplitude.animateTo(1f, FolioMotion.spring(FolioMotion.Settle)) }
        remember(phase) { JiggleClock(phase, amplitude.asState()) }
    } else null
    CompositionLocalProvider(LocalHomeEdit provides edit, LocalJiggle provides clock, content = content)
}

/** Wiggle this element while jiggling; [key] staggers the phase so neighbors don't move in sync. */
@Composable
internal fun Modifier.jiggle(key: Any, amount: Float = 1f): Modifier {
    val clock = LocalJiggle.current ?: return this
    val hash = key.hashCode()
    val seed = (hash and 0xFF) / 255f
    val direction = if (hash and 0x100 == 0) 1f else -1f
    val wiggles = JiggleMotion.CYCLE + ((hash ushr 9) and 0xFF) % (JiggleMotion.RATE_SPREAD * 2 + 1) - JiggleMotion.RATE_SPREAD
    val rate = wiggles.toFloat() / JiggleMotion.CYCLE
    return graphicsLayer {
        val reach = clock.amplitude.value * amount
        val angle = 2f * PI.toFloat() * (clock.phase.value * rate + seed)
        rotationZ = sin(angle) * JiggleMotion.DEGREES * reach * direction
        translationY = cos(angle) * .5f * density * reach
    }
}

/** The gray "–" in an icon's corner while jiggling. */
@Composable
internal fun BoxScope.JiggleRemoveButton(label: String, inset: Dp = 0.dp, onRemove: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    // 44dp touch target (accessibility minimum) around a 22dp visual, centered on the corner; [inset] pulls it in so it
    // stays inside a tight container like the side dock, as iPhone keeps it inside the dock.
    Box(Modifier.align(Alignment.TopStart).offset((-11).dp + inset, (-11).dp + inset).size(44.dp)
        .clickable(role = Role.Button, onClickLabel = label, interactionSource = null, indication = null) {
            haptic.perform(FolioHaptic.Refuse); onRemove()
        }.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(22.dp).shadow(2.dp, CircleShape).background(Color(0xFFD1D1D6), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Remove, null, tint = FolioColors.SecondaryBackground, modifier = Modifier.size(15.dp))
        }
    }
}

/** Glass capsule button for the jiggle-mode bar ("+", "Edit", "Done"). */
@Composable
internal fun JigglePill(label: String, icon: ImageVector? = null, description: String = label, emphasized: Boolean = false,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        val homeInk = LocalHomeInk.current
        Row(Modifier.heightIn(min = 34.dp)
            .liquidGlass(CircleShape, if (emphasized) TOOLBAR_FROST else homeControlFrost(homeInk.dark), GlassRole.CAPSULE, Color.White)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }
            .padding(horizontal = if (label.isEmpty()) 7.dp else FolioSpace.LARGE.dp), verticalAlignment = Alignment.CenterVertically) {
            val ink = if (emphasized) FolioColors.LightLabel else homeInk.primary
            icon?.let { Icon(it, null, tint = ink, modifier = Modifier.size(20.dp)) }
            if (label.isNotEmpty()) Text(label, color = ink, fontSize = FolioType.SUBHEAD.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** iOS 16+ Home "Search" capsule that sits where the page dots are. */
@Composable
internal fun HomeSearchPill(onClick: () -> Unit) {
    val ink = LocalHomeInk.current
    val frost = homeControlFrost(ink.dark)
    val reduceMotion = LocalReduceMotion.current
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, FolioMotion.spring(FolioMotion.Settle)) }
    Box(Modifier
        .layout { measurable, constraints ->
            val collapsed = 30.dp.roundToPx()
            val full = measurable.maxIntrinsicWidth(constraints.maxHeight).coerceAtLeast(collapsed)
            val shown = (collapsed + (full - collapsed) * reveal.value).roundToInt().coerceAtLeast(collapsed)
            val placeable = measurable.measure(constraints.copy(minWidth = shown, maxWidth = shown))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        .heightIn(min = 30.dp).clip(CircleShape).liquidGlass(CircleShape, frost, GlassRole.CAPSULE, Color.White)
        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.search), onClick = onClick),
        contentAlignment = Alignment.Center) {
        Row(Modifier.wrapContentWidth(unbounded = true).padding(horizontal = FolioSpace.COMFY.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = ink.primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.search), color = ink.primary, fontSize = FolioType.FOOTNOTE.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Where the jiggle bar's bottom edge falls (top inset + 48dp touch height + a little air), for Home's first-row clearance. */
internal val JIGGLE_BAR_BOTTOM = FolioSpace.HAIR.dp + 48.dp + 4.dp
