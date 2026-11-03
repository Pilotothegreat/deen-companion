package com.pilotothegreat.deencompanion.ui.qibla

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath

/**
 * The compass, read live. Each property reads its own snapshot state, so only the composable — or the
 * draw or layer block — that reads a value is invalidated when it changes. Before 2.5 this was a plain
 * data class built in composition, so every sensor event (about sixteen a second) recomposed the whole
 * Qibla screen, stat cards and all.
 */
@Stable
class CompassReading internal constructor(
    private val headingState: State<Float?>,
    private val accuracyState: State<Int>,
    val available: Boolean,
    private val pitchState: State<Float>,
    private val rollState: State<Float>,
) {
    /** Degrees clockwise from true north, or null before the first sensor event. */
    val heading: Float? get() = headingState.value
    val accuracy: Int get() = accuracyState.value

    /** Degrees the phone is tipped forward or back, and to either side; 0 while it lies flat. */
    val pitch: Float get() = pitchState.value
    val roll: Float get() = rollState.value

    val needsCalibration: Boolean
        get() = available && heading != null && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
}

/**
 * True-north heading from the fused rotation-vector sensor, corrected for magnetic declination and
 * screen rotation and low-pass filtered. The sensor is registered only while in composition.
 */
@Composable
fun rememberCompass(latitude: Double, longitude: Double): CompassReading {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    }
    val headingState = remember { mutableStateOf<Float?>(null) }
    val accuracyState = remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    val pitchState = remember { mutableFloatStateOf(0f) }
    val rollState = remember { mutableFloatStateOf(0f) }
    var heading by headingState
    var accuracy by accuracyState
    var pitch by pitchState
    var roll by rollState
    val declination = remember(latitude, longitude) {
        GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }

    DisposableEffect(sensor, declination) {
        val manager = sensorManager
        if (sensor == null || manager == null) return@DisposableEffect onDispose { }
        val display = ContextCompat.getDisplayOrDefault(context)
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                val screenRotation = when (display.rotation) {
                    Surface.ROTATION_90 -> 90f
                    Surface.ROTATION_180 -> 180f
                    Surface.ROTATION_270 -> 270f
                    else -> 0f
                }
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination + screenRotation
                val target = QiblaMath.normalize(azimuth.toDouble()).toFloat()
                val previous = heading
                heading = if (previous == null) {
                    target
                } else {
                    val smoothed = previous + (QiblaMath.unwrap(target, previous) - previous) * SMOOTHING
                    QiblaMath.normalize(smoothed.toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                accuracy = value
            }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return remember(sensor) { CompassReading(headingState, accuracyState, sensor != null, pitchState, rollState) }
}

private const val SMOOTHING = 0.2f
