package com.pilotothegreat.deencompanion.ui.qibla

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
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
                        LoadingIndicator(Modifier.padding(12.dp).size(24.dp))
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
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

/** Dial that turns with the phone; the Kaaba marker sits at the Qibla bearing and morphs when aligned. */
@Composable
private fun CompassDial(heading: Float, bearing: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val rotation = remember { Animatable(-heading) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(heading) { rotation.animateTo(QiblaMath.unwrap(-heading, rotation.value), spec) }

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

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }) {
            val radius = size.minDimension / 2
            drawCircle(colors.surfaceContainerHigh, radius)
            drawCircle(if (aligned) colors.primary else colors.outlineVariant, radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            for (degree in 0 until 360 step 5) {
                val major = degree % 30 == 0
                val top = center.y - radius + 8.dp.toPx()
                rotate(degree.toFloat()) {
                    drawLine(
                        color = if (major) colors.onSurfaceVariant else colors.outlineVariant,
                        start = Offset(center.x, top),
                        end = Offset(center.x, top + if (major) 14.dp.toPx() else 6.dp.toPx()),
                        strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                    )
                }
            }
            cardinals.forEachIndexed { index, label ->
                rotate(index * 90f) {
                    val layout = textMeasurer.measure(label, labelStyle.copy(color = if (index == 0) colors.error else colors.onSurface))
                    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - radius + 28.dp.toPx()))
                }
            }
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value + bearing },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 64.dp)
                    .size(56.dp)
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
        // Fixed marker showing where the top of the phone points.
        Canvas(Modifier.fillMaxSize()) {
            val tip = Path().apply {
                moveTo(center.x, 0f)
                lineTo(center.x - 10.dp.toPx(), 16.dp.toPx())
                lineTo(center.x + 10.dp.toPx(), 16.dp.toPx())
                close()
            }
            drawPath(tip, colors.primary)
        }
    }
}

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
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLargeEmphasized)
        }
    }
}
