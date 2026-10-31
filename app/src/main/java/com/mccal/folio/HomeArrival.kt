package com.mccal.folio

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

internal object FirstArrival {
    private const val PREFS = "folio_arrival"
    private const val ARRIVED = "arrived"

    fun take(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, 0)
        if (prefs.getBoolean(ARRIVED, false)) return false
        prefs.edit().putBoolean(ARRIVED, true).apply()
        return !context.getSharedPreferences(SettingKeys.PREFS, 0).contains(SettingKeys.STATE)
    }
}

internal object ArrivalMotion {
    const val STAGGER_MS = 32L
    const val LONGEST_DELAY_MS = 560L
    const val WINDOW_MS = 4_000L
    const val START_SCALE = .55f
    const val RISE_DP = 36f
    const val FAILSAFE_MS = 30_000L
}

internal const val DOCK_ARRIVAL_LEAD = 4

internal fun arrivalDelayMs(order: Int): Long =
    (order.coerceAtLeast(0) * ArrivalMotion.STAGGER_MS).coerceAtMost(ArrivalMotion.LONGEST_DELAY_MS)

internal fun homeArrivalOrder(dockSize: Int, row: Int, column: Int, columns: Int = GRID_COLUMNS): Int =
    dockSize.coerceAtLeast(0) + row.coerceAtLeast(0) + (columns - 1 - column).coerceIn(0, columns - 1)

@Stable
internal class HomeArrival(private val now: () -> Long = SystemClock::uptimeMillis) {
    var started by mutableStateOf(false)
        private set
    private var startedAt = 0L

    fun begin() {
        if (started) return
        startedAt = now()
        started = true
    }

    fun pending(): Boolean = !started || now() - startedAt < ArrivalMotion.WINDOW_MS
}

internal val LocalHomeArrival = staticCompositionLocalOf<HomeArrival?> { null }

@Composable
internal fun Modifier.arrival(order: Int): Modifier {
    val arrival = LocalHomeArrival.current
    val reduceMotion = LocalReduceMotion.current
    val progress = remember(arrival) { Animatable(if (arrival?.pending() == true) 0f else 1f) }
    if (arrival != null) LaunchedEffect(arrival, arrival.started) {
        if (!arrival.started || progress.value >= 1f) return@LaunchedEffect
        delay(arrivalDelayMs(order))
        progress.animateTo(1f, FolioMotion.spring(FolioMotion.Appear))
    }
    if (arrival == null) return this
    return graphicsLayer {
        val shown = progress.value
        alpha = shown.coerceIn(0f, 1f)
        if (!reduceMotion) {
            val scale = ArrivalMotion.START_SCALE + (1f - ArrivalMotion.START_SCALE) * shown
            scaleX = scale; scaleY = scale
            translationY = (1f - shown) * ArrivalMotion.RISE_DP.dp.toPx()
        }
    }
}
