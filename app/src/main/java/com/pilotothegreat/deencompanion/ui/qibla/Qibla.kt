package com.pilotothegreat.deencompanion.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import com.pilotothegreat.deencompanion.ui.theme.MorphPolygonShape
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.CornerRounding
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.overview.calculateQiblaDirection
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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

    val distanceToMakkahKm = remember(lat, lon) {
        val makkahLat = Math.toRadians(21.4225)
        val makkahLon = Math.toRadians(39.8262)
        val userLat = Math.toRadians(lat)
        val userLon = Math.toRadians(lon)

        val dLat = userLat - makkahLat
        val dLon = userLon - makkahLon

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(makkahLat) * Math.cos(userLat) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        (6371 * c).toInt()
    }

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
    var manualHeading by remember { mutableStateOf(0f) }
    var smoothHeading by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }

    var sensorAccuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    // Sensor tracking
    DisposableEffect(context, lat, lon) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val geomagneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

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
    val effectiveRawHeading = if (hasCompass) rawHeading else manualHeading
    val effectiveHeading = if (hasCompass) animatedHeading else manualHeading
    val relativeAngle = (qiblaBearing - effectiveRawHeading + 360f) % 360f
    val isAligned = relativeAngle < 4f || relativeAngle > 356f

    // Polygons for morphing cursor indicating alignment status
    val unalignedPoly = remember {
        RoundedPolygon(
            numVertices = 8,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(radius = 0.3f)
        )
    }
    val alignedPoly = remember {
        RoundedPolygon(
            numVertices = 4,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(radius = 0.7f)
        )
    }
    val morph = remember { Morph(unalignedPoly, alignedPoly) }
    val morphProgress by animateFloatAsState(
        targetValue = if (isAligned) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "qiblaMorph"
    )
    val morphShape = remember(morphProgress) {
        MorphPolygonShape(morph = morph, progress = morphProgress, rotationZ = 45f)
    }

    var wasAligned by remember { mutableStateOf(false) }

    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            com.pilotothegreat.deencompanion.util.VibratorHelper.vibrateQiblaAligned(context)
        }
        wasAligned = isAligned
    }



    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val dialFaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val surfaceColor = MaterialTheme.colorScheme.surface
    val compassRingColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
    )
    val centerArrowColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    )
    val cardinalColor = MaterialTheme.colorScheme.onSurface
    val cardinalNorthColor = MaterialTheme.colorScheme.error
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qibla_compass)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location information
            var isRefreshingLocation by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
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

                    IconButton(
                        onClick = {
                            if (!isRefreshingLocation) {
                                isRefreshingLocation = true
                                scope.launch {
                                    try {
                                        var loc = com.pilotothegreat.deencompanion.util.LocationHelper.getDeviceLocation(context)
                                        if (loc == null) {
                                            loc = com.pilotothegreat.deencompanion.util.LocationHelper.fetchIpLocation()
                                        }
                                        if (loc != null) {
                                            appPreferenceRepo.setLatitude(loc.latitude)
                                            appPreferenceRepo.setLongitude(loc.longitude)
                                            appPreferenceRepo.setCityName(loc.cityName)
                                            appPreferenceRepo.setTimezoneId(loc.timezoneId)
                                        }
                                    } catch (e: java.lang.Exception) {
                                        timber.log.Timber.e(e, "Error refreshing location in Qibla")
                                    } finally {
                                        isRefreshingLocation = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isRefreshingLocation) {
                            // M3 Expressive: CircularWavyProgressIndicator replaces
                            // legacy CircularProgressIndicator (SKILL.md component swap table)
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh_location),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                }
            }

            if (!hasCompass) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (appLang == "ar") "محاكاة البوصلة اليدوية" else "Manual Compass Simulation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLang == "ar") {
                                        "اسحب شريط التمرير أدناه لتدوير قرص البوصلة ليطابق اتجاه الشمال الفعلي حولك."
                                    } else {
                                        "Slide the bar below to manually rotate the compass face to match actual North around you."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appLang == "ar") "اتجاه الهاتف الحالي:" else "Phone Heading Direction:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%d°", manualHeading.toInt()),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = manualHeading,
                                onValueChange = { manualHeading = it },
                                valueRange = 0f..360f,
                                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(16.dp))

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

                // Compass Dial (rotates with -effectiveHeading)
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = -effectiveHeading
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)

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

                        // Inner decorative ring
                        drawCircle(
                            color = compassRingColor.copy(alpha = 0.2f),
                            radius = radius - 20.dp.toPx(),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Draw 36 precision compass tick marks
                        for (i in 0 until 36) {
                            val angleDeg = i * 10f
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val isMajor = i % 9 == 0
                            val isMedium = i % 3 == 0
                            val tickLength = if (isMajor) 16.dp.toPx() else if (isMedium) 10.dp.toPx() else 5.dp.toPx()
                            val tickStroke = if (isMajor) 2.5.dp.toPx() else if (isMedium) 1.5.dp.toPx() else 1.dp.toPx()
                            val tickColor = if (isMajor) compassRingColor else compassRingColor.copy(alpha = 0.6f)

                            val outerR = radius - 3.dp.toPx()
                            val innerR = radius - tickLength
                            val startX = (center.x + innerR * sin(angleRad)).toFloat()
                            val startY = (center.y - innerR * cos(angleRad)).toFloat()
                            val endX = (center.x + outerR * sin(angleRad)).toFloat()
                            val endY = (center.y - outerR * cos(angleRad)).toFloat()

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = tickStroke
                            )
                        }
                    }

                    // Cardinal direction labels overlaid on the dial
                    val cardinalLabels = listOf(
                        Triple("N", 0f, cardinalNorthColor),
                        Triple("E", 90f, cardinalColor),
                        Triple("S", 180f, cardinalColor),
                        Triple("W", 270f, cardinalColor)
                    )
                    cardinalLabels.forEach { (label, angle, color) ->
                        val angleRad = Math.toRadians(angle.toDouble())
                        val labelRadius = 90.dp  // distance from center for labels
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = color,
                            modifier = Modifier
                                .offset(
                                    x = (labelRadius.value * sin(angleRad)).dp,
                                    y = (-labelRadius.value * cos(angleRad)).dp
                                )
                        )
                    }
                }

                // Qibla Needle + Kaaba cursor (all rotate together at qiblaBearing - animatedHeading)
                val kaabaColor = if (isAligned) MaterialTheme.colorScheme.tertiary else centerArrowColor
                val kaabaGlowAlpha by animateFloatAsState(
                    targetValue = if (isAligned) 0.25f else 0f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(900),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "kaabaGlow"
                )
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            rotationZ = qiblaBearing - effectiveHeading
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Needle drawn behind the Kaaba icon
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Chiseled 3D M3 arrow needle
                        val leftWing = Path().apply {
                            moveTo(center.x, center.y - radius + 40.dp.toPx())
                            lineTo(center.x - 18.dp.toPx(), center.y + 24.dp.toPx())
                            lineTo(center.x, center.y + 8.dp.toPx())
                            close()
                        }
                        val rightWing = Path().apply {
                            moveTo(center.x, center.y - radius + 40.dp.toPx())
                            lineTo(center.x + 18.dp.toPx(), center.y + 24.dp.toPx())
                            lineTo(center.x, center.y + 8.dp.toPx())
                            close()
                        }

                        drawPath(path = leftWing, color = kaabaColor)
                        drawPath(path = rightWing, color = kaabaColor.copy(alpha = 0.65f))

                        // Center pivot hub
                        drawCircle(color = surfaceColor, radius = 9.dp.toPx(), center = center)
                        drawCircle(color = kaabaColor, radius = 5.dp.toPx(), center = center)
                    }

                    // Kaaba icon at the north tip of the needle
                    Box(
                        modifier = Modifier
                            .offset(y = (-95).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing glow ring
                        Canvas(modifier = Modifier.size(64.dp)) {
                            drawCircle(
                                color = kaabaColor.copy(alpha = kaabaGlowAlpha),
                                radius = size.minDimension / 2
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(morphShape)
                                .background(
                                    if (isAligned) kaabaColor.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isAligned) 3.dp else 1.5.dp,
                                    color = if (isAligned) kaabaColor else MaterialTheme.colorScheme.outline,
                                    shape = morphShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kaaba),
                                contentDescription = stringResource(R.string.cd_qibla_compass),
                                modifier = Modifier
                                    .size(34.dp)
                                    .graphicsLayer {
                                        // Keep Kaaba icon upright by canceling parent rotation
                                        rotationZ = -(qiblaBearing - effectiveHeading)
                                    },
                                colorFilter = if (isAligned) null else ColorFilter.colorMatrix(
                                    ColorMatrix().apply { setToSaturation(0.3f) }
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aligned status text with smooth fade/spring entry
            AnimatedVisibility(
                visible = isAligned,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Small morphic shape indicating Qibla
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.tertiary, morphShape)
                        )
                        Text(
                            text = stringResource(R.string.qibla_aligned),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Qibla & Makkah Distance Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (appLang == "ar") "اتجاه القبلة" else "Qibla Angle",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${qiblaBearing.toInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (appLang == "ar") "المسافة إلى مكة" else "Makkah Distance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,d", distanceToMakkahKm)} km",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
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
