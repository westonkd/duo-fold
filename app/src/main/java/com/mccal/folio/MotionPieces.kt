package com.mccal.folio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * The 0.6.9 motion pieces (docs/standards/motion-references.md): small moves with a little personality, drawn from the
 * Mockup Lab's "Motion ideas". Every piece honors Reduce Motion by jumping to the end.
 */

/**
 * A count that rolls: a new number slides in from below when it goes up and from above when it goes down, while the old
 * one leaves the other way, on [FolioMotion.Menu]. The first number simply appears. With Reduce Motion on it is plain
 * text.
 */
@Composable
internal fun RollingCount(count: Int, text: (Int) -> String, color: Color, fontSize: TextUnit, style: TextStyle, modifier: Modifier = Modifier) {
    fun plain(value: Int) = @Composable { Text(text(value), color = color, fontWeight = FontWeight.SemiBold, fontSize = fontSize, maxLines = 1, softWrap = false, style = style, modifier = modifier) }
    if (LocalReduceMotion.current) { plain(count)(); return }
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            val up = targetState > initialState
            val spring = FolioMotion.spring<androidx.compose.ui.unit.IntOffset>(FolioMotion.Menu)
            val fade = androidx.compose.animation.core.tween<Float>(120)
            (slideInVertically(spring) { h -> if (up) h else -h } + fadeIn(fade)) togetherWith
                (slideOutVertically(spring) { h -> if (up) -h else h } + fadeOut(fade)) using SizeTransform(clip = true)
        },
        label = "rolling count",
    ) { value -> plain(value)() }
}
