package com.mccal.folio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

internal const val STATUS_RING_DOTS = 5
internal const val STATUS_RING_DOT_PITCH = 17f
internal const val STATUS_RING_DOT_SCALE = .75f
internal const val STATUS_RING_FAN_APEX = .62f

internal data class RingArc(val start: Float, val sweep: Float)

internal fun statusRingArc(dots: Int): RingArc {
    if (dots <= 0) return RingArc(-90f, 360f)
    val gap = STATUS_RING_DOT_PITCH * (dots + 1)
    return RingArc(90f + gap / 2f, 360f - gap)
}

internal fun statusRingDotAngles(dots: Int): List<Float> =
    List(dots.coerceAtLeast(0)) { i -> 90f + ((dots - 1) / 2f - i) * STATUS_RING_DOT_PITCH }

internal const val STATUS_SPACING = 4f

@Composable
fun StatusRail(
    status: DeviceStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    iconSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
) {
    val homeInk = LocalHomeInk.current
    val ink = homeInk.primary
    val textShadow = homeInk.labelShadow
    // Over light wallpapers (dark text) the frosted capsule is light too, so dim parts and colors need more weight to read.
    val onLight = homeInk.dark
    fun faint(alpha: Float) = if (onLight) (alpha * 1.6f).coerceAtMost(.6f) else alpha
    val look = RingLook.of(androidx.compose.ui.platform.LocalConfiguration.current.fontWeightAdjustment)
    val charging = if (onLight) BatteryChargingOnLight else BatteryCharging
    val low = if (onLight) BatteryLowOnLight else BatteryLow
    // Ticker, not a loop of its own: one clock for the whole app, stopped while nothing is watching (DYN-14).
    val now = displayNow(rememberMinuteTick().value)
    val format = if (android.text.format.DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm"
    val timeFormatter = remember(format) { DateTimeFormatter.ofPattern(format) }
    // Each language's own order for the date (strings.xml), for the rail and for what TalkBack reads.
    val shortDate = stringResource(R.string.mmm_d)
    val dateFormatter = remember(shortDate) { DateTimeFormatter.ofPattern(shortDate) }
    val longDate = stringResource(R.string.eeee_mmmm_d)
    val longDateFormatter = remember(longDate) { DateTimeFormatter.ofPattern(longDate) }
    val description = listOfNotNull(
        if (status.silent) stringResource(R.string.silent_mode) else null,
        "${now.format(longDateFormatter)}, ${now.format(timeFormatter)}",
        status.battery?.let { stringResource(if (status.charging) R.string.battery_1_d_percent_charging else R.string.battery_1_d_percent, it) }
            ?: stringResource(R.string.battery_unavailable),
        if (!status.wifiConnected) stringResource(R.string.wi_fi_disconnected)
        else status.wifiLevel?.let { stringResource(R.string.wi_fi_connected_signal_1_d_of_4, it) } ?: stringResource(R.string.wi_fi_connected),
        if (status.airplane) stringResource(R.string.airplane_mode)
        else status.cellularLevel?.let { stringResource(R.string.cellular_signal_1_d_of_4, it) } ?: stringResource(R.string.cellular_signal_unavailable),
    ).joinToString(". ")
    val fontScale = LocalDensity.current.fontScale
    val wifiVisual = wifiSignalVisual(status.wifiConnected, status.wifiLevel)
    val cellularVisual = cellularSignalVisual(status.cellularLevel, status.airplane)
    val activeDots = (cellularVisual as? CellularSignalVisual.Available)?.activeDots ?: 0
    val batteryColor = when {
        status.charging -> charging
        (status.battery ?: 100) <= 20 -> low
        else -> ink
    }
    val openLabel = stringResource(R.string.quick_settings)
    BoxWithConstraints(modifier.testTag("status-rail").semantics(mergeDescendants = true) { contentDescription = description }
        .then(if (onClick == null) Modifier else Modifier.clickable(remember { MutableInteractionSource() }, indication = null,
            onClickLabel = openLabel, role = Role.Button, onClick = onClick))) {
        val availableWidth = (maxWidth - 16.dp).coerceAtLeast(28.dp)
        val visualSize = minOf(iconSize * .9f, availableWidth, if (compact) 34.dp else 44.dp)
        val timeSize = minOf(17f, availableWidth.value / (2.5f * fontScale)).sp
        val detailSize = minOf(11f, availableWidth.value / (3.3f * fontScale)).sp
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(STATUS_SPACING.dp)) {
            if (status.silent) Icon(Icons.Rounded.NotificationsOff, null,
                tint = if (onLight) SilentOnLight else Silent,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp).testTag("status-silent"))
            Text(now.format(timeFormatter), color = ink, fontSize = timeSize, fontWeight = FontWeight.SemiBold, style = LocalTextStyle.current.copy(shadow = textShadow),
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            if (!compact) Text(now.format(dateFormatter), color = ink.copy(alpha = if (onLight) .85f else .8f), fontSize = detailSize, style = LocalTextStyle.current.copy(shadow = textShadow),
                fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
            Box(Modifier.padding(top = FolioSpace.HAIR.dp).size(visualSize), contentAlignment = Alignment.Center) {
                // Inside the battery ring, like iPhone's status bar: Wi-Fi when joined; otherwise cellular bars,
                // an airplane in Airplane Mode, or a slowly sweeping fan while there's no connection at all.
                val offline = !status.wifiConnected && cellularVisual !is CellularSignalVisual.Available && !status.airplane
                // With Reduce Motion, a still fan with a slash instead.
                val sweep = if (offline && !LocalReduceMotion.current) rememberInfiniteTransition(label = "no connection")
                    .animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "sweep") else null
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val center = Offset(w / 2, w / 2)
                    val radius = w * .44f
                    val ringStroke = w * .06f * look.strokeScale
                    val dots = if (wifiVisual is WifiSignalVisual.Connected && cellularVisual is CellularSignalVisual.Available) STATUS_RING_DOTS else 0
                    val arc = statusRingArc(dots)
                    val ringTopLeft = Offset(center.x - radius, center.y - radius)
                    drawArc(ink.copy(alpha = faint(look.trackAlpha)), arc.start, arc.sweep, false, ringTopLeft, Size(radius * 2, radius * 2),
                        style = Stroke(ringStroke, cap = StrokeCap.Round))
                    status.battery?.let { level ->
                        drawArc(batteryColor, arc.start, arc.sweep * level / 100, false, ringTopLeft,
                            Size(radius * 2, radius * 2), style = Stroke(width = ringStroke, cap = StrokeCap.Round))
                    }
                    statusRingDotAngles(dots).forEachIndexed { i, angle ->
                        val rad = Math.toRadians(angle.toDouble())
                        drawCircle(ink.copy(alpha = if (i < activeDots) 1f else faint(.28f)), ringStroke * STATUS_RING_DOT_SCALE,
                            Offset(center.x + radius * kotlin.math.cos(rad).toFloat(), center.y + radius * kotlin.math.sin(rad).toFloat()))
                    }
                    when {
                        wifiVisual is WifiSignalVisual.Connected -> drawWifiFan(w, wifiVisual, ink, onLight)
                        cellularVisual is CellularSignalVisual.Available -> drawCellBars(w, activeDots, ink, onLight)
                        status.airplane -> Unit // the airplane icon draws on top
                        else -> drawSearchingFan(w, sweep?.value ?: -1f, ink, onLight)
                    }
                }
                if (status.airplane && !status.wifiConnected)
                    Icon(Icons.Rounded.AirplanemodeActive, null, tint = ink, modifier = Modifier.size(visualSize * .42f))
            }
            if (!compact) Text(if (status.airplane) stringResource(R.string.status_airplane) else status.battery?.let { "$it%" } ?: "—",
                color = if (status.charging) charging else ink,
                fontSize = detailSize, fontWeight = FontWeight.Medium, style = LocalTextStyle.current.copy(shadow = textShadow),
                maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWifiFan(w: Float, wifiVisual: WifiSignalVisual.Connected, ink: Color, onLight: Boolean) {
    val cx = size.width / 2
    val fanY = w * STATUS_RING_FAN_APEX
    for (i in 1..3) {
        val r = w * (.07f + i * .075f)
        drawArc(ink.copy(alpha = signalAlpha(wifiVisual.elements[i], onLight)), 225f, 90f, false,
            Offset(cx - r, fanY - r), Size(r * 2, r * 2), style = Stroke(w * .05f, cap = StrokeCap.Round))
    }
    drawCircle(ink.copy(alpha = signalAlpha(wifiVisual.elements[0], onLight)), w * .04f, Offset(cx, fanY))
}

private val BatteryCharging = Color(0xFF6EE39A)
/** iOS shows Silent mode's bell in red. */
private val Silent = FolioColors.Red
private val SilentOnLight = Color(0xFFD70015)
private val BatteryLow = Color(0xFFFFB35C)
/** iOS's darker system green and orange, which keep their contrast on light backgrounds. */
private val BatteryChargingOnLight = Color(0xFF248A3D)
private val BatteryLowOnLight = Color(0xFFC93400)

/** Four rising cellular bars centered in the ring, lit up to [activeDots] of 5. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCellBars(w: Float, activeDots: Int, ink: Color, onLight: Boolean = false) {
    val bar = w * .075f
    val gap = w * .045f
    val left = w / 2 - (4 * bar + 3 * gap) / 2
    val bottom = w * .66f
    for (i in 0 until 4) {
        val h = w * (.12f + i * .07f)
        drawRoundRect(ink.copy(alpha = if (i < (activeDots * 4 + 4) / 5) 1f else if (onLight) .45f else .28f), Offset(left + i * (bar + gap), bottom - h), Size(bar, h),
            androidx.compose.ui.geometry.CornerRadius(bar / 2))
    }
}

/** No connection: a dim Wi-Fi fan whose arcs light one after another, like it's looking; with a slash when still ([phase] < 0). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSearchingFan(w: Float, phase: Float, ink: Color, onLight: Boolean = false) {
    val cx = size.width / 2
    val fanY = w * .6f
    for (i in 1..3) {
        val r = w * (.07f + i * .075f)
        val lit = if (phase < 0f) 0f else (1f - kotlin.math.abs(phase * 4f - i)).coerceIn(0f, 1f)
        drawArc(ink.copy(alpha = (if (onLight) .36f else .22f) + .5f * lit), 225f, 90f, false, Offset(cx - r, fanY - r), Size(r * 2, r * 2),
            style = Stroke(w * .05f, cap = StrokeCap.Round))
    }
    drawCircle(ink.copy(alpha = .3f), w * .04f, Offset(cx, fanY))
    if (phase < 0f) drawLine(ink.copy(alpha = .7f), Offset(cx - w * .16f, fanY - w * .26f), Offset(cx + w * .16f, fanY + w * .02f), w * .045f, StrokeCap.Round)
}

private fun signalAlpha(emphasis: SignalElementEmphasis, onLight: Boolean = false): Float = when (emphasis) {
    SignalElementEmphasis.DIM -> if (onLight) .45f else .3f
    SignalElementEmphasis.NEUTRAL -> if (onLight) .75f else .62f
    SignalElementEmphasis.LIT -> 1f
}
