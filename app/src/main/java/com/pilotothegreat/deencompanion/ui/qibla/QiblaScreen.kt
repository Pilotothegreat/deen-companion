package com.pilotothegreat.deencompanion.ui.qibla

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.pilotothegreat.deencompanion.ui.components.toComposePath
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CompassCalibration
import androidx.compose.material.icons.rounded.ExploreOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.LOCATION_PERMISSIONS
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.hasLocationPermission
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.theme.animatedPolygonShape
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private const val ALIGNMENT_TOLERANCE_DEGREES = 5.0

@Composable
fun QiblaScreen(onBack: () -> Unit, viewModel: QiblaViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = currentLocale()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.refreshResults.collect { result ->
            when (result) {
                LocationRepository.Result.UPDATED -> Unit
                LocationRepository.Result.PERMISSION_MISSING ->
                    snackbar.showSnackbar(resources.getString(R.string.location_permission_needed))
                LocationRepository.Result.UNAVAILABLE ->
                    snackbar.showSnackbar(resources.getString(R.string.location_unavailable))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qibla_compass)) },
                subtitle = { Text(state?.cityName ?: stringResource(R.string.default_location)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    if (refreshing) {
                        LoadingIndicator(Modifier.padding(Spacing.medium).size(24.dp))
                    } else {
                        IconButton(onClick = {
                            if (hasLocationPermission(context)) viewModel.refresh() else locationPermission.launch(LOCATION_PERMISSIONS)
                        }) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.refresh_location))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val qibla = state ?: run {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        val compass = rememberCompass(qibla.latitude, qibla.longitude)
        val heading = compass.heading
        val aligned = heading != null && QiblaMath.isAligned(heading.toDouble(), qibla.bearing, ALIGNMENT_TOLERANCE_DEGREES)
        LaunchedEffect(aligned) { if (aligned) haptics.confirm() }
        val bearingText = Formatters.number(qibla.bearing.roundToInt(), locale)

        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xlarge),
        ) {
            when {
                !compass.available -> InfoCard(
                    Icons.Rounded.ExploreOff,
                    stringResource(R.string.compass_unavailable_title),
                    stringResource(R.string.compass_unavailable_body, bearingText),
                )
                compass.needsCalibration -> InfoCard(
                    Icons.Rounded.CompassCalibration,
                    stringResource(R.string.compass_calibration_title),
                    stringResource(R.string.compass_calibration_desc),
                )
            }

            CompassDial(
                heading = heading ?: 0f,
                bearing = qibla.bearing.toFloat(),
                aligned = aligned,
                readout = heading?.let { stringResource(R.string.degrees, Formatters.number(it.roundToInt(), locale)) },
                pitch = compass.pitch,
                roll = compass.roll,
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f),
            )

            val guidance = when {
                heading == null -> stringResource(R.string.qibla_bearing_info, bearingText)
                aligned -> stringResource(R.string.qibla_facing)
                else -> {
                    val relative = QiblaMath.normalize(qibla.bearing - heading)
                    if (relative <= 180) stringResource(R.string.qibla_turn_right, Formatters.number(relative.roundToInt(), locale))
                    else stringResource(R.string.qibla_turn_left, Formatters.number((360 - relative).roundToInt(), locale))
                }
            }
            Text(
                guidance,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                // Spoken as the phone turns: a compass nobody can see is useless without this, and
                // the dial above it is a picture with nothing to say.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                StatCard(stringResource(R.string.qibla_bearing_label), stringResource(R.string.degrees, bearingText), Modifier.weight(1f))
                StatCard(
                    stringResource(R.string.qibla_distance_label),
                    stringResource(R.string.kilometers, Formatters.number(qibla.distanceKm.roundToInt(), locale)),
                    Modifier.weight(1f),
                )
            }
            if (qibla.isDefaultLocation) {
                Text(
                    stringResource(R.string.using_default_location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The dial, as a physical thing. Its rim is a scalloped MaterialShapes cookie with a degree ring inside and
 * the heading in the middle, and the Kaaba marker rides the rim at the Qibla bearing. The dial turns on an
 * underdamped spring fed by the sensor, so a quick swing overshoots and settles instead of gliding. Coming
 * onto the Qibla it locks there with a haptic, a morph and a pulse, and tilting the phone leans it a little.
 * With reduced motion or Simple mode the dial is steady: no bounce, no lean, no pulse.
 */
@Composable
private fun CompassDial(
    heading: Float,
    bearing: Float,
    aligned: Boolean,
    readout: String?,
    pitch: Float,
    roll: Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val reduced = rememberReducedMotion()
    // On the Qibla the dial locks there, rather than trembling inside the tolerance.
    val target = -(if (aligned) bearing else heading)
    val rotation = remember { Animatable(target) }
    val physics: SpringSpec<Float> = when {
        reduced -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        aligned -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
        else -> spring(NEEDLE_DAMPING, Spring.StiffnessLow)
    }
    // Each reading retargets the running spring, which keeps its velocity: that is the swing.
    LaunchedEffect(target, physics) { rotation.animateTo(QiblaMath.unwrap(target, rotation.value), physics) }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(aligned) {
        if (aligned && !reduced) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(PULSE_MILLIS))
        }
    }
    val lean = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val tiltX by animateFloatAsState(if (reduced) 0f else (pitch * TILT).coerceIn(-MAX_TILT, MAX_TILT), lean, label = "tiltX")
    val tiltY by animateFloatAsState(if (reduced) 0f else (-roll * TILT).coerceIn(-MAX_TILT, MAX_TILT), lean, label = "tiltY")
    val rim by animateColorAsState(
        if (aligned) colors.primary else colors.outlineVariant,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "rim",
    )

    val textMeasurer = rememberTextMeasurer()
    val cardinals = listOf(
        stringResource(R.string.compass_n),
        stringResource(R.string.compass_e),
        stringResource(R.string.compass_s),
        stringResource(R.string.compass_w),
    )
    val labelStyle = MaterialTheme.typography.titleMediumEmphasized
    val markerShape = animatedPolygonShape(if (aligned) MaterialShapes.Sunny else MaterialShapes.Cookie9Sided)
    val description = stringResource(R.string.cd_qibla_compass)

    Box(
        modifier.graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            cameraDistance = 12f * density
        },
        contentAlignment = Alignment.Center,
    ) {
        // The pulse when the Qibla is found, spreading out from behind the dial.
        Canvas(Modifier.fillMaxSize()) {
            val progress = pulse.value
            if (progress < 1f) {
                drawCircle(colors.primary.copy(alpha = (1f - progress) * 0.4f), radius = size.minDimension / 2 * (0.85f + 0.2f * progress))
            }
        }
        Canvas(Modifier.fillMaxSize().padding(DIAL_INSET).graphicsLayer { rotationZ = rotation.value }) {
            val dial = DIAL_SHAPE.toComposePath(size)
            drawPath(dial, colors.surfaceContainerHigh)
            drawPath(dial, rim, style = Stroke(3.dp.toPx()))
            val radius = size.minDimension / 2
            for (degree in 0 until 360 step 5) {
                val major = degree % 30 == 0
                val top = center.y - radius + 24.dp.toPx()
                rotate(degree.toFloat()) {
                    drawLine(
                        color = if (major) colors.onSurfaceVariant else colors.outlineVariant,
                        start = Offset(center.x, top),
                        end = Offset(center.x, top + if (major) 12.dp.toPx() else 5.dp.toPx()),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
            }
            cardinals.forEachIndexed { index, label ->
                rotate(index * 90f) {
                    val layout = textMeasurer.measure(label, labelStyle.copy(color = if (index == 0) colors.error else colors.onSurface))
                    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - radius + 42.dp.toPx()))
                }
            }
        }
        // The heading stays upright in the middle while the dial turns around it.
        if (readout != null) {
            Text(
                readout,
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = if (aligned) colors.primary else colors.onSurface,
            )
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value + bearing },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = MARKER_TOP)
                    .size(MARKER_SIZE)
                    .clip(markerShape)
                    .background(if (aligned) colors.primary else colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_kaaba),
                    contentDescription = description,
                    modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = -(rotation.value + bearing) },
                )
            }
        }
        // Fixed marker showing where the top of the phone points, above the dial's rim.
        Canvas(Modifier.fillMaxSize()) {
            val tip = Path().apply {
                moveTo(center.x, 14.dp.toPx())
                lineTo(center.x - 9.dp.toPx(), 0f)
                lineTo(center.x + 9.dp.toPx(), 0f)
                close()
            }
            drawPath(tip, colors.primary)
        }
    }
}

private const val NEEDLE_DAMPING = 0.4f
private const val TILT = 0.2f
private const val MAX_TILT = 10f
private const val PULSE_MILLIS = 900
private val DIAL_INSET = 16.dp
/** The Kaaba rides the rim itself, outside the cardinal letters, so it never sits on top of one. */
private val MARKER_TOP = 4.dp
private val MARKER_SIZE = 48.dp
private val DIAL_SHAPE = MaterialShapes.Cookie12Sided

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.xlarge), horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLargeEmphasized)
        }
    }
}
