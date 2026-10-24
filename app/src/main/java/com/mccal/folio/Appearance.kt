package com.mccal.folio

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class DuoPalette(val ink: Color, val glass: Color)
val LightDuoPalette = DuoPalette(Color(0xFF243A46), Color(0xFFE8EFF2))
val LocalDuoPalette = staticCompositionLocalOf { LightDuoPalette }
