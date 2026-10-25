package com.mccal.folio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.view.Display
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun FoldTransitionHost(enabled: Boolean = true, stayAwake: Boolean = true, haptics: Boolean = true, reduceMotion: Boolean = false,
    content: @Composable () -> Unit) {
    val expanded = LocalConfiguration.current.fitsRegularHomeLayout()
    val view = LocalView.current
    val context = LocalContext.current
    val hinge = LocalHinge.current
    val rotation = view.display?.rotation ?: android.view.Surface.ROTATION_0
    val shader = remember { if (Build.VERSION.SDK_INT >= 33) PageLandingShader() else null }
    val fold = remember { FoldWatcher(context).also { it.expanded = expanded } }
    fold.stayAwake = stayAwake
    val fastest = remember(view, expanded) { view.display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 0f }
    var fast by remember { mutableStateOf(false) }
    fun animating(on: Boolean) {
        if (Build.VERSION.SDK_INT < 35 || on == fast || fastest <= 0f) return
        fast = on
        view.requestedFrameRate = if (on) fastest else android.view.View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
    }
    DisposableEffect(view) { onDispose { animating(false) } }

    val feel by rememberUpdatedState(enabled && haptics)
    val haptic = LocalHapticFeedback.current
    var landing by remember { mutableStateOf<Landing?>(null) }
    var landedAt by remember { mutableLongStateOf(-1L) }
    var lift by remember { mutableFloatStateOf(0f) }
    fold.onLanded = { kind ->
        if (feel) haptic.perform(FolioHaptic.Land)
        landing = kind; landedAt = SystemClock.uptimeMillis(); lift = 0f
        fold.wake.trySend(Unit)
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> fold.start()
                Lifecycle.Event.ON_STOP -> fold.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); fold.stop() }
    }

    if (expanded != fold.expanded) {
        fold.expanded = expanded
        landing = null; lift = 0f
        fold.onDisplaySwitched(SystemClock.uptimeMillis())
    }

    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var litFrames = 0
        var measuring = false
        var secondTapFor = -1L
        try {
            while (true) {
                if (!fold.waitingForPanel && landing == null) {
                    if (measuring) { PerfLog.end(PerfScenario.FOLD); measuring = false }
                    animating(false)
                    withTimeoutOrNull(IDLE_WAIT_MS) { fold.wake.receive() }
                    continue
                }
                if (landing != null && !measuring) { PerfLog.noteFold(); PerfLog.begin(PerfScenario.FOLD); measuring = true }
                animating(enabled && landing != null)
                withFrameNanos {
                    val now = SystemClock.uptimeMillis()
                    if (fold.waitingForPanel) {
                        litFrames = if (view.display?.state == Display.STATE_ON) litFrames + 1 else 0
                        if (litFrames >= 2 || now - fold.switchedAt > LIT_TIMEOUT_MS) { litFrames = 0; fold.onPanelLit() }
                    }
                    if (landing != null) {
                        val since = (now - landedAt).toFloat()
                        if (secondTapFor != landedAt && since >= PageLanding.FIRST_BOUNCE_MS) {
                            secondTapFor = landedAt
                            if (feel) haptic.perform(FolioHaptic.Step)
                        }
                        if (since >= PageLanding.DURATION_MS) { landing = null; lift = 0f } else lift = PageLanding.lift(since)
                    }
                }
            }
        } finally { if (measuring) PerfLog.end(PerfScenario.FOLD) }
    } }

    val draws = enabled && !reduceMotion && shader != null
    Box(Modifier.fillMaxSize().then(if (draws) Modifier.graphicsLayer {
        val kind = landing
        val amount = lift
        renderEffect = if (kind != null && amount > 0f && Build.VERSION.SDK_INT >= 33)
            shader!!.effect(size.width, size.height, amount, landingPage(kind, rotation, hinge, size.width, size.height)) else null
    } else Modifier)) { content() }
}

internal enum class Landing { OPENED, CLOSED }

internal object PageLanding {
    const val PEAK_DEGREES = 7f
    const val FIRST_BOUNCE_MS = 230f
    const val RESTITUTION = .42f
    private const val LAST_HEIGHT = .01f

    val DURATION_MS: Float = run {
        var total = 0f; var span = FIRST_BOUNCE_MS; var height = 1f
        while (height >= LAST_HEIGHT) { total += span; span *= RESTITUTION; height *= RESTITUTION * RESTITUTION }
        total
    }

    fun lift(ms: Float): Float {
        if (ms <= 0f) return 0f
        var start = 0f; var span = FIRST_BOUNCE_MS; var height = 1f
        while (ms >= start + span) {
            start += span; span *= RESTITUTION; height *= RESTITUTION * RESTITUTION
            if (height < LAST_HEIGHT) return 0f
        }
        val s = (ms - start) / span
        return height * 4f * s * (1f - s)
    }
}

internal data class LandingPage(val horizontal: Boolean, val hingePx: Float, val movingAfterHinge: Boolean)

internal fun landingPage(landing: Landing, rotation: Int, hinge: Hinge?, width: Float, height: Float): LandingPage {
    val g = foldGeometry(rotation, hinge, width, height)
    if (landing == Landing.OPENED) return LandingPage(g.horizontal, g.hingePx, g.movingAfterHinge)
    return when (coverHingeEdge(g)) {
        CoverEdge.LEFT -> LandingPage(false, 0f, true)
        CoverEdge.RIGHT -> LandingPage(false, width, false)
        CoverEdge.TOP -> LandingPage(true, 0f, true)
        CoverEdge.BOTTOM -> LandingPage(true, height, false)
    }
}

internal class FoldWatcher(context: Context) : SensorEventListener {
    val wake = Channel<Unit>(Channel.CONFLATED)
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val hinge: Sensor? = sensors?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private val prefs = context.getSharedPreferences("folio", 0)
    private val appContext = context.applicationContext

    var expanded = false
    var stayAwake = true
    var switchedAt = -1L; private set
    var waitingForPanel = false; private set
    var onLanded: ((Landing) -> Unit)? = null

    private val tracker = HingeTracker(if (hinge == null) HingeCapability.POSTURE_ONLY
        else if (prefs.getString(HINGE_CAPABILITY_KEY, null) == HingeCapability.CONTINUOUS.name) HingeCapability.CONTINUOUS else HingeCapability.STEPPED)
    private var closing = false
    private var peak = 0f
    private var trough = 0f

    fun start() { hinge?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }

    fun stop() { sensors?.unregisterListener(this) }

    fun onDisplaySwitched(now: Long) {
        switchedAt = now; waitingForPanel = true; closing = false
        wake.trySend(Unit)
    }

    fun onPanelLit() {
        waitingForPanel = false
        val settled = tracker.raw == null || (if (expanded) tracker.flat else tracker.closed)
        if (settled) onLanded?.invoke(if (expanded) Landing.OPENED else Landing.CLOSED)
    }

    override fun onSensorChanged(event: SensorEvent) {
        onAngle(event.values.firstOrNull() ?: return, event.timestamp)
        wake.trySend(Unit)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    internal fun onAngle(value: Float, timestampNs: Long) {
        val previous = tracker.raw
        val wasFlat = tracker.flat
        val wasClosed = tracker.closed
        if (tracker.feed(value, timestampNs)) prefs.edit().putString(HINGE_CAPABILITY_KEY, tracker.capability.name).apply()
        if (previous == null) { peak = value; return }
        if (previous == value) return
        if (expanded) {
            when {
                tracker.flat && !wasFlat -> {
                    if (closing) FoldBridgeActivity.cancel()
                    closing = false; peak = value
                    if (!waitingForPanel) onLanded?.invoke(Landing.OPENED)
                }
                wasFlat && !tracker.flat -> startClosing(value)
                !closing && peak - value >= MIN_FOLD_DROP_DEG -> startClosing(value)
                closing && tracker.closed && !wasClosed -> if (stayAwake) FoldBridgeActivity.start(appContext)
                closing && value - trough >= REOPEN_DEG -> { closing = false; peak = value; FoldBridgeActivity.cancel() }
            }
            if (closing) trough = minOf(trough, value) else peak = maxOf(peak, value)
        } else if (tracker.closed && !wasClosed && !waitingForPanel) onLanded?.invoke(Landing.CLOSED)
    }

    private fun startClosing(value: Float) {
        closing = true; trough = value
        if (stayAwake) FoldBridgeActivity.start(appContext)
    }
}

internal enum class CoverEdge { LEFT, RIGHT, TOP, BOTTOM }
internal fun coverHingeEdge(g: FoldGeometry): CoverEdge = when {
    !g.horizontal && !g.movingAfterHinge -> CoverEdge.LEFT
    !g.horizontal -> CoverEdge.RIGHT
    !g.movingAfterHinge -> CoverEdge.TOP
    else -> CoverEdge.BOTTOM
}

internal data class FoldGeometry(val horizontal: Boolean, val hingePx: Float, val movingAfterHinge: Boolean)

internal fun foldGeometry(rotation: Int, hinge: Hinge?, width: Float, height: Float): FoldGeometry {
    val horizontal = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
    val after = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_180
    val middle = if (horizontal) height / 2f else width / 2f
    val position = hinge?.takeIf { it.vertical != horizontal }?.let { (it.startPx + it.endPx) / 2f } ?: middle
    return FoldGeometry(horizontal, position, after)
}

private const val HINGE_CAPABILITY_KEY = "fold_hinge_capability"
private const val MIN_FOLD_DROP_DEG = 20f
private const val REOPEN_DEG = 15f
private const val LIT_TIMEOUT_MS = 1_200L
private const val IDLE_WAIT_MS = 500L
