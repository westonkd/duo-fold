package com.mccal.folio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

internal object LightTiltMotion {
    const val REACH = 1.6f
    const val LIMIT = .5f
    const val SMOOTHING = .4f
    const val RECENTER = .025f
    const val STEP = .004f
    const val SAMPLE_US = 33_333
}

internal class TiltFollower {
    private var x = Float.NaN
    private var y = Float.NaN
    private var restX = 0f
    private var restY = 0f

    fun follow(gravityX: Float, gravityY: Float): Offset {
        if (x.isNaN()) {
            x = gravityX; y = gravityY; restX = gravityX; restY = gravityY
        }
        x += (gravityX - x) * LightTiltMotion.SMOOTHING
        y += (gravityY - y) * LightTiltMotion.SMOOTHING
        restX += (x - restX) * LightTiltMotion.RECENTER
        restY += (y - restY) * LightTiltMotion.RECENTER
        return Offset(((restX - x) * LightTiltMotion.REACH).coerceIn(-LightTiltMotion.LIMIT, LightTiltMotion.LIMIT),
            ((y - restY) * LightTiltMotion.REACH).coerceIn(-LightTiltMotion.LIMIT, LightTiltMotion.LIMIT))
    }
}

@Stable
internal class LightTilt {
    var shift by mutableStateOf(Offset.Zero)
        private set

    fun follow(next: Offset) {
        if ((next - shift).getDistance() > LightTiltMotion.STEP) shift = next
    }

    fun settle() {
        shift = Offset.Zero
    }
}

internal val LocalLightTilt = staticCompositionLocalOf<LightTilt?> { null }

@Composable
internal fun rememberLightTilt(active: Boolean, held: () -> Boolean): LightTilt {
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    val tilt = remember { LightTilt() }
    LifecycleResumeEffect(active, reduceMotion) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = if (active && !reduceMotion && !context.savingPower() && sensor != null) object : SensorEventListener {
            private val follower = TiltFollower()
            override fun onSensorChanged(event: SensorEvent) {
                val shift = follower.follow(event.values[0] / SensorManager.GRAVITY_EARTH, event.values[1] / SensorManager.GRAVITY_EARTH)
                if (!held()) tilt.follow(shift)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }.also { sensors.registerListener(it, sensor, LightTiltMotion.SAMPLE_US) } else null
        onPauseOrDispose {
            listener?.let { sensors?.unregisterListener(it) }
            tilt.settle()
        }
    }
    return tilt
}

private fun Context.savingPower(): Boolean = getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
