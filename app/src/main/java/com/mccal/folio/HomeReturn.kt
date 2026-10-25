package com.mccal.folio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin

internal object HomeReturn {
    const val ZOOM = 1.08f

    var origin by mutableStateOf<Offset?>(null)
        private set
    var plays by mutableIntStateOf(0)
        private set
    private var launched = false

    fun launched(bounds: android.graphics.Rect?) {
        origin = bounds?.takeUnless { it.isEmpty }?.let { Offset(it.exactCenterX(), it.exactCenterY()) }
        launched = true
    }

    fun returned() {
        if (!launched) return
        launched = false
        plays++
    }

    fun scale(progress: Float) = ZOOM + (1f - ZOOM) * progress.coerceIn(0f, 1f)

    fun transformOrigin(rootInWindow: Offset, size: Size): TransformOrigin {
        val point = origin ?: return TransformOrigin.Center
        if (size.width <= 0f || size.height <= 0f) return TransformOrigin.Center
        return TransformOrigin(((point.x - rootInWindow.x) / size.width).coerceIn(0f, 1f), ((point.y - rootInWindow.y) / size.height).coerceIn(0f, 1f))
    }
}
