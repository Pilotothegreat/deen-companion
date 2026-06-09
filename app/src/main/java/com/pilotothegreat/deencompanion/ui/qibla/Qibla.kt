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

    val vibrator = remember(context) { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Sensor tracking
    DisposableEffect(context, lat, lon) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

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
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
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
                            text = if (cityName.isNotEmpty()) cityName else stringResource(R.string.default_location),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.compass_calibration_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.compass_calibration_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Beautiful compass representation
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally),
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
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = -animatedHeading
                        }
                ) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw outer border ring
                    drawCircle(
                        color = compassRingColor,
                        radius = radius,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw Cardinal Ticks & Labels
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Fetch color from theme programmatically if desired, but white or primary works
                        }
                        textSize = 16.sp.toPx()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    // Force the color to match current theme
                    textPaint.color = if (android.graphics.Color.valueOf(0f,0f,0f).hashCode() == 0) {
                        // placeholder
                        0xFF4A4A4A.toInt()
                    } else 0

                    val ticks = 72 // ticks every 5 degrees
                    for (t in 0 until ticks) {
                        val angleDeg = t * 5f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val isMajor = t % 6 == 0 // every 30 degrees
                        val isCardinal = t % 18 == 0 // N, E, S, W
                        val tickLen = if (isCardinal) 15.dp.toPx() else if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                        val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                        val startX = (center.x + (radius - tickLen) * sin(angleRad)).toFloat()
                        val startY = (center.y - (radius - tickLen) * cos(angleRad)).toFloat()
                        val endX = (center.x + radius * sin(angleRad)).toFloat()
                        val endY = (center.y - radius * cos(angleRad)).toFloat()

                        drawOutlineTick(
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            color = compassRingColor.copy(alpha = if (isMajor) 0.8f else 0.4f),
                            width = strokeW
                        )
                    }

                    // N, E, S, W text
                    val labelRadius = radius - 25.dp.toPx()
                    drawCardinalLabel("N", center.x, center.y - labelRadius, primaryColor)
                    drawCardinalLabel("E", center.x + labelRadius, center.y, primaryColor.copy(alpha = 0.7f))
                    drawCardinalLabel("S", center.x, center.y + labelRadius, primaryColor.copy(alpha = 0.7f))
                    drawCardinalLabel("W", center.x - labelRadius, center.y, primaryColor.copy(alpha = 0.7f))
                }

                // Qibla Needle (rotates with qiblaBearing - animatedHeading relative to phone)
                // We draw it separately or on top of the dial so it remains pointed to Makkah.
                // If it is on the dial, it points to the qiblaBearing on the dial. Since the dial rotates by -animatedHeading,
                // the needle rotates relative to the phone by (qiblaBearing - animatedHeading).
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = qiblaBearing - animatedHeading
                        }
                ) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw a prominent Qibla Arrow
                    val path = Path().apply {
                        moveTo(center.x, center.y - radius + 10.dp.toPx()) // Tip of arrow
                        lineTo(center.x - 14.dp.toPx(), center.y - radius + 35.dp.toPx())
                        lineTo(center.x - 4.dp.toPx(), center.y - radius + 30.dp.toPx())
                        lineTo(center.x - 4.dp.toPx(), center.y - 15.dp.toPx())
                        lineTo(center.x + 4.dp.toPx(), center.y - 15.dp.toPx())
                        lineTo(center.x + 4.dp.toPx(), center.y - radius + 30.dp.toPx())
                        lineTo(center.x + 14.dp.toPx(), center.y - radius + 35.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = path,
                        color = centerArrowColor
                    )

                    // Small circle at the bottom of the arrow representing Kaaba/Makkah direction
                    drawCircle(
                        color = centerArrowColor,
                        radius = 8.dp.toPx(),
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

            // Angle Details Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.device_heading),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f°", rawHeading),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.qibla_bearing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f°", qiblaBearing),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = centerArrowColor
                        )
                    }
                }
            }

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

// Draw ticks helper
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutlineTick(
    start: Offset,
    end: Offset,
    color: Color,
    width: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = width
    )
}

// Simple label helper on Canvas
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardinalLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color
) {
    // For drawing text, since Canvas draws pixels, we can also use drawing paths or standard drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = 14.sp.toPx()
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    // Adjust y slightly so it centers vertically
    val fontMetrics = paint.fontMetrics
    val adjustedY = y - (fontMetrics.ascent + fontMetrics.descent) / 2
    
    drawContext.canvas.nativeCanvas.drawText(
        text,
        x,
        adjustedY,
        paint
    )
}

// Function to handle 360 wrap-around smoothly
private fun getSmoothRotation(target: Float, current: Float): Float {
    var diff = (target - current) % 360f
    if (diff < -180f) diff += 360f
    if (diff > 180f) diff -= 360f
    return current + diff
}
