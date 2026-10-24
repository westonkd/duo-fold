package com.mccal.folio

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * How an icon answers a finger: one dip for apps, dock icons and folders; a lift with a soft shadow while an icon's menu is open; and, after a
 * tap, the icon stays dipped for a moment while the app starts (Samsung gives a launcher no opening animation, so a slow app would otherwise look
 * like nothing happened). Mocked in the Lab (scene press-feedback) on 8 Oct 2026.
 *
 * Done the way motion is done elsewhere in Folio: a spring that follows the finger and can be interrupted at any point, only scale, alpha and a
 * shadow (all graphics-layer values read when drawing, so nothing recomposes per frame), and no scale at all under Reduce Motion, only a dim.
 */
internal object PressFeedback {
    const val DIP = .9f
    const val DIP_ALPHA = .9f
    const val LIFT = 1.06f
    const val LIFT_SHADOW_DP = 12f
    /** The longest an icon stays dipped after a tap; the app is normally up well before this. */
    const val HOLD_MS = 600L

    /** How far the icon shrinks. */
    fun scale(pressed: Boolean, lifted: Boolean, reduceMotion: Boolean): Float = when {
        reduceMotion -> 1f
        lifted -> LIFT
        pressed -> DIP
        else -> 1f
    }

    fun alpha(pressed: Boolean, lifted: Boolean): Float = if (!lifted && pressed) DIP_ALPHA else 1f

    fun shadowDp(lifted: Boolean, reduceMotion: Boolean): Float = if (lifted && !reduceMotion) LIFT_SHADOW_DP else 0f
}

/** The id of the app or folder whose menu is open, so its icon can lift while the menu is up. */
internal val LocalOpenMenuId = compositionLocalOf<String?> { null }

/** Dip, lift and shadow for one icon. [cornerDp] is the icon's corner radius, so the shadow follows its shape. */
@Composable
internal fun Modifier.pressFeedback(pressed: Boolean, lifted: Boolean = false, cornerDp: Float = 0f, dimOnly: Boolean = false): Modifier {
    val reduce = LocalReduceMotion.current
    val spec = FolioMotion.spring<Float>(FolioMotion.Quick)
    val scale = animateFloatAsState(PressFeedback.scale(pressed, lifted, reduce || dimOnly), spec, label = "icon press scale")
    val alpha = animateFloatAsState(PressFeedback.alpha(pressed, lifted), spec, label = "icon press alpha")
    val shadow = animateFloatAsState(PressFeedback.shadowDp(lifted, reduce || dimOnly), spec, label = "icon lift shadow")
    return graphicsLayer {
        scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
        shadowElevation = shadow.value * density
        if (shadowElevation > 0f) { shape = RoundedCornerShape(cornerDp.dp); clip = false }
    }
}
