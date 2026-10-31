package com.mccal.folio

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

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

    fun pivotOn(view: View) {
        zoom(view, 1f)
        val point = origin
        if (point == null || view.width <= 0 || view.height <= 0) {
            view.pivotX = view.width / 2f; view.pivotY = view.height / 2f
            return
        }
        val inWindow = IntArray(2).also(view::getLocationInWindow)
        view.pivotX = (point.x - inWindow[0]).coerceIn(0f, view.width.toFloat())
        view.pivotY = (point.y - inWindow[1]).coerceIn(0f, view.height.toFloat())
    }

    fun zoom(view: View, progress: Float) {
        val s = scale(progress)
        view.scaleX = s; view.scaleY = s
    }
}
