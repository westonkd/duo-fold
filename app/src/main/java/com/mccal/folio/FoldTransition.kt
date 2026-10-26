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
    val draws = enabled && !reduceMotion && shader != null
    val bodily = remember { FoldFeel(context) }
    var landing by remember { mutableStateOf<Landing?>(null) }
    var landedAt by remember { mutableLongStateOf(-1L) }
    var lift by remember { mutableFloatStateOf(0f) }
    var shade by remember { mutableFloatStateOf(0f) }
    var braced by remember { mutableStateOf(false) }
    fun show(pose: PagePose) { lift = pose.lift; shade = pose.shade }
    fun land(kind: Landing, now: Long) {
        braced = false
        if (kind == Landing.OPENED) { if (feel) bodily.open() }
        else if (!fold.shutFelt) { fold.feltShut(); if (feel) bodily.shut() }
        if (kind == Landing.CLOSED && landing == Landing.CLOSED) return
        landing = kind
        landedAt = if (kind == Landing.CLOSED) now - PageLanding.SHUT_FALL_MS.toLong() else now
        show(PageLanding.pose(kind, (now - landedAt).toFloat()))
        fold.wake.trySend(Unit)
    }
    fold.onLanded = { kind -> land(kind, SystemClock.uptimeMillis()) }
    fold.onShut = { if (feel) bodily.shut() }

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
        val now = SystemClock.uptimeMillis()
        braced = !expanded && draws && fold.closing
        landing = if (braced) Landing.CLOSED else null; landedAt = now
        show(if (braced) PageLanding.pose(Landing.CLOSED, 0f) else PagePose.FLAT)
        fold.onDisplaySwitched(now)
    }

    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        var litFrames = 0
        var measuring = false
        try {
            while (true) {
                if (!fold.waitingForPanel && landing == null && !braced) {
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
                        if (litFrames >= 2 || now - fold.switchedAt > LIT_TIMEOUT_MS) { litFrames = 0; fold.onPanelLit(assumeShut = braced) }
                    }
                    val kind = landing
                    if (kind != null) {
                        val since = (now - landedAt).toFloat()
                        if (since >= PageLanding.duration(kind)) { landing = null; show(PagePose.FLAT) } else show(PageLanding.pose(kind, since))
                    }
                }
            }
        } finally { if (measuring) PerfLog.end(PerfScenario.FOLD) }
    } }

    Box(Modifier.fillMaxSize().then(if (draws) Modifier.graphicsLayer {
        val kind = landing
        val pose = PagePose(lift, shade)
        renderEffect = if (kind != null && pose != PagePose.FLAT && Build.VERSION.SDK_INT >= 33)
            shader!!.effect(size.width, size.height, pose, landingPage(kind, rotation, hinge, size.width, size.height)) else null
    } else Modifier)) { content() }
}

internal enum class Landing { OPENED, CLOSED }

internal class FoldFeel(context: Context) {
    private val vibrator = context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
    private val shut = composed(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK to 1f, android.os.VibrationEffect.Composition.PRIMITIVE_THUD to .8f)
        ?: android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK)
    private val open = composed(android.os.VibrationEffect.Composition.PRIMITIVE_TICK to .6f)
        ?: android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)

    fun shut() = play(shut)
    fun open() = play(open)

    private fun composed(vararg parts: Pair<Int, Float>): android.os.VibrationEffect? {
        val v = vibrator ?: return null
        if (!v.areAllPrimitivesSupported(*parts.map { it.first }.toIntArray())) return null
        return android.os.VibrationEffect.startComposition().apply { parts.forEach { (id, scale) -> addPrimitive(id, scale) } }.compose()
    }

    private fun play(effect: android.os.VibrationEffect) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_PHYSICAL_EMULATION))
        else v.vibrate(effect)
    }
}

internal data class PagePose(val lift: Float, val shade: Float) {
    companion object { val FLAT = PagePose(0f, 0f) }
}

internal object PageLanding {
    const val PEAK_DEGREES = 7f
    const val SHUT_FALL_MS = 140f
    const val SHUT_RELEASE_MS = 100f
    const val OPEN_PEAK = .7f
    const val OPEN_RISE_MS = 120f
    const val OPEN_SETTLE_MS = 300f
    const val OPEN_LIGHT_MS = 360f

    fun duration(kind: Landing) = when (kind) {
        Landing.CLOSED -> SHUT_FALL_MS + SHUT_RELEASE_MS
        Landing.OPENED -> OPEN_RISE_MS + maxOf(OPEN_SETTLE_MS, OPEN_LIGHT_MS)
    }

    fun pose(kind: Landing, ms: Float): PagePose {
        if (ms >= duration(kind)) return PagePose.FLAT
        val t = ms.coerceAtLeast(0f)
        return if (kind == Landing.CLOSED) shut(t) else open(t)
    }

    private fun shut(ms: Float): PagePose {
        if (ms < SHUT_FALL_MS) {
            val s = ms / SHUT_FALL_MS
            return PagePose(1f - s * s, s)
        }
        val r = 1f - (ms - SHUT_FALL_MS) / SHUT_RELEASE_MS
        return PagePose(0f, r * r)
    }

    private fun open(ms: Float): PagePose {
        if (ms < OPEN_RISE_MS) {
            val up = kotlin.math.sin(ms / OPEN_RISE_MS * kotlin.math.PI.toFloat() / 2f)
            return PagePose(OPEN_PEAK * up, up)
        }
        val since = ms - OPEN_RISE_MS
        return PagePose(OPEN_PEAK * (1f - smooth(since / OPEN_SETTLE_MS)), 1f - smooth(since / OPEN_LIGHT_MS))
    }

    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
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
    var closing = false; private set
    private var peak = 0f
    private var trough = 0f
    private var shutAssumed = false
    var shutFelt = false; private set
    var onShut: (() -> Unit)? = null

    fun feltShut() { shutFelt = true }

    fun start() { hinge?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }

    fun stop() { sensors?.unregisterListener(this) }

    fun onDisplaySwitched(now: Long) {
        switchedAt = now; waitingForPanel = true; closing = false; shutAssumed = false
        wake.trySend(Unit)
    }

    fun onPanelLit(assumeShut: Boolean = false) {
        waitingForPanel = false
        val settled = tracker.raw == null || (if (expanded) tracker.flat else tracker.closed)
        shutAssumed = !expanded && assumeShut && !settled
        if (settled || shutAssumed) onLanded?.invoke(if (expanded) Landing.OPENED else Landing.CLOSED)
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
        if (!tracker.closed) shutFelt = false
        else if (!wasClosed && !shutFelt && (closing || !expanded)) { shutFelt = true; onShut?.invoke() }
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
        } else if (tracker.closed && !wasClosed && !waitingForPanel) {
            if (shutAssumed) shutAssumed = false else onLanded?.invoke(Landing.CLOSED)
        } else if (!tracker.closed) shutAssumed = false
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
