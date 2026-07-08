package com.pilotothegreat.deencompanion.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.overview.calculateQiblaDirection
import org.koin.compose.koinInject
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Qibla() {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()

    val lat by appPreferenceRepo.latitude.collectAsState(initial = 21.3891)
    val lon by appPreferenceRepo.longitude.collectAsState(initial = 39.8579)
    val cityName by appPreferenceRepo.cityName.collectAsState(initial = "")
    val appLang by appPreferenceRepo.appLanguage.collectAsState(initial = "ar")

    val qiblaBearing = remember(lat, lon) { calculateQiblaDirection(lat, lon).toFloat() }

    val declination = remember(lat, lon) {
        try {
            val geoField = android.hardware.GeomagneticField(
                lat.toFloat(),
                lon.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            geoField.declination
        } catch (e: Exception) {
            0f
        }
    }

    val hasCompass = remember(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
                (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
    }
    var rawHeading by remember { mutableStateOf(0f) }
    var smoothHeading by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }

    var sensorAccuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    val vibrator = remember(context) { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Sensor tracking
    DisposableEffect(context, lat, lon) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val geomagneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var gravity = FloatArray(3)
        var geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val heading = (azimuth + declination + 360f) % 360f
                    rawHeading = heading
                    smoothHeading = getSmoothRotation(heading, smoothHeading)
                    pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        gravity = event.values.clone()
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        geomagnetic = event.values.clone()
                    }
                    val g = gravity
                    val m = geomagnetic
                    if (g != null && m != null) {
                        val r = FloatArray(9)
                        val i = FloatArray(9)
                        if (SensorManager.getRotationMatrix(r, i, g, m)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(r, orientation)
                            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            val heading = (azimuth + declination + 360f) % 360f
                            rawHeading = heading
                            smoothHeading = getSmoothRotation(heading, smoothHeading)
                            pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                            roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorAccuracy = accuracy
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (geomagneticFieldSensor != null) {
                sensorManager.registerListener(listener, geomagneticFieldSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Spring animation for smooth movement
    val animatedHeading by animateFloatAsState(
        targetValue = smoothHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Smoothed tilt for parallax (low-pass to avoid jitter)
    var smoothedPitch by remember { mutableStateOf(0f) }
    var smoothedRoll by remember { mutableStateOf(0f) }
    LaunchedEffect(pitch, roll) {
        smoothedPitch += (pitch - smoothedPitch) * 0.15f
        smoothedRoll += (roll - smoothedRoll) * 0.15f
    }
    val tiltPitch = smoothedPitch.coerceIn(-25f, 25f)
    val tiltRoll = smoothedRoll.coerceIn(-25f, 25f)

    // Alignment verification (aligned when phone is pointed at Makkah +/- 4 degrees)
    val relativeAngle = (qiblaBearing - rawHeading + 360f) % 360f
    val isAligned = relativeAngle < 4f || relativeAngle > 356f

    var wasAligned by remember { mutableStateOf(false) }

    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            } catch (e: Exception) {
                // Ignore vibration failures in test/emulator
            }
        }
        wasAligned = isAligned
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val dialFaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val compassRingColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
    val centerArrowColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qibla_compass)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = com.pilotothegreat.deencompanion.util.localizeCityName(
                                if (cityName.isNotEmpty()) cityName else stringResource(R.string.default_location),
                                appLang
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "Lat: %.4f • Lon: %.4f", lat, lon),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!hasCompass) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = if (appLang == "ar") "البوصلة غير متوفرة" else "Compass Sensor Unavailable",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (appLang == "ar") {
                                    "يفتقر جهازك إلى مستشعر الاتجاه المغناطيسي. البوصلة لن تدور تلقائيًا. يرجى استخدام زاوية اتجاه القبلة لتوجيه جهازك يدويًا."
                                } else {
                                    "Your device lacks magnetic/compass sensors. The compass needle cannot rotate automatically. Use the bearing angle value to orient yourself manually."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Calibration & guidance text
            val showCalibrationWarning = hasCompass && (sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            if (showCalibrationWarning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                text = if (appLang == "ar") "دقة البوصلة منخفضة" else "Low Compass Accuracy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (appLang == "ar") {
                                    "يرجى تحريك الهاتف في مسار يشبه الرقم (8) لمعايرة مستشعر الاتجاه."
                                } else {
                                    "Please move your phone in a figure-8 motion to calibrate the compass sensor."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (hasCompass) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (appLang == "ar") "دقة البوصلة: ممتازة" else "Compass Accuracy: Calibrated",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Beautiful compass representation
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer {
                        rotationX = tiltPitch      // forward/back parallax
                        rotationY = -tiltRoll      // left/right parallax
                        cameraDistance = 12 * density
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background subtle ring glow when aligned
                if (isAligned) {
                    Canvas(modifier = Modifier.size(290.dp)) {
                        drawCircle(
                            color = centerArrowColor.copy(alpha = 0.15f),
                            radius = size.minDimension / 2
                        )
                    }
                }

                // Compass Dial (rotates with -animatedHeading)
                // Rotating Dial Face (rotates with animatedHeading relative to phone)
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = -animatedHeading
                        }
                ) {
                    val radius = size.minDimension / 2

                    // Draw dial face background
                    drawCircle(
                        color = dialFaceColor,
                        radius = radius
                    )

                    // Draw outer border ring
                    drawCircle(
                        color = compassRingColor,
                        radius = radius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Qibla Needle (rotates with qiblaBearing - animatedHeading relative to phone)
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = qiblaBearing - animatedHeading
                        }
                ) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw chiseled 3D M3 arrow needle
                    val leftWing = Path().apply {
                        moveTo(center.x, center.y - radius + 15.dp.toPx())
                        lineTo(center.x - 22.dp.toPx(), center.y + 24.dp.toPx())
                        lineTo(center.x, center.y + 8.dp.toPx())
                        close()
                    }
                    val rightWing = Path().apply {
                        moveTo(center.x, center.y - radius + 15.dp.toPx())
                        lineTo(center.x + 22.dp.toPx(), center.y + 24.dp.toPx())
                        lineTo(center.x, center.y + 8.dp.toPx())
                        close()
                    }

                    drawPath(
                        path = leftWing,
                        color = centerArrowColor
                    )
                    drawPath(
                        path = rightWing,
                        color = centerArrowColor.copy(alpha = 0.75f)
                    )

                    // Draw center pivot hub
                    drawCircle(
                        color = surfaceColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = centerArrowColor,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                val crosshairColor = if (isAligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                // Inner static alignment crosshair to help user line up
                Canvas(modifier = Modifier.size(260.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    // Top pointer (marker indicating top of screen direction)
                    val pointerPath = Path().apply {
                        moveTo(center.x, 8.dp.toPx())
                        lineTo(center.x - 6.dp.toPx(), 18.dp.toPx())
                        lineTo(center.x + 6.dp.toPx(), 18.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = crosshairColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Aligned status text with smooth fade/spring entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAligned) {
                    Text(
                        text = stringResource(R.string.qibla_aligned),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Function to handle 360 wrap-around smoothly
private fun getSmoothRotation(target: Float, current: Float): Float {
    var diff = (target - current) % 360f
    if (diff < -180f) diff += 360f
    if (diff > 180f) diff -= 360f
    return current + diff
}
