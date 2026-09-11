package com.pilotothegreat.deencompanion.ui.qibla

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath

data class CompassReading(
    /** Degrees clockwise from true north, or null before the first sensor event. */
    val heading: Float?,
    val accuracy: Int,
    val available: Boolean,
) {
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
    var heading by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
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
    return CompassReading(heading, accuracy, available = sensor != null)
}

private const val SMOOTHING = 0.2f
