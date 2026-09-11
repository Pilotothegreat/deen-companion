package com.pilotothegreat.deencompanion.ui.location

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.settings.LocationSource
import com.pilotothegreat.deencompanion.data.settings.SavedLocation
import com.pilotothegreat.deencompanion.ui.common.LOCATION_PERMISSIONS
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.hasLocationPermission
import com.pilotothegreat.deencompanion.ui.components.EmptyState
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.components.SearchField
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Choose a city by name (offline) or go back to following the device's location. */
@Composable
fun LocationPickerScreen(onBack: () -> Unit, viewModel: LocationViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val locating by viewModel.isLocating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.useDeviceLocation()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LocationEvent.Done -> onBack()
                LocationEvent.Unavailable -> snackbar.showSnackbar(resources.getString(R.string.location_unavailable))
                LocationEvent.PermissionMissing -> snackbar.showSnackbar(resources.getString(R.string.location_permission_needed))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_city_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val cities = results ?: run {
                LoadingBox()
                return@Column
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (query.isBlank()) {
                    settings?.let { s ->
                        item(key = "current") { CurrentLocationCard(s.location, Modifier.padding(bottom = 12.dp)) }
                    }
                    item(key = "device") {
                        SegmentedListItem(
                            onClick = {
                                if (hasLocationPermission(context)) viewModel.useDeviceLocation() else permission.launch(LOCATION_PERMISSIONS)
                            },
                            shapes = ListItemDefaults.segmentedShapes(0, 1),
                            leadingContent = { Icon(Icons.Rounded.MyLocation, contentDescription = null) },
                            supportingContent = { Text(stringResource(R.string.use_my_location_desc)) },
                            trailingContent = { if (locating) LoadingIndicator(Modifier.size(24.dp)) },
                        ) { Text(stringResource(R.string.use_my_location)) }
                    }
                }
                item(key = "header") {
                    SectionHeader(
                        stringResource(if (query.isBlank()) R.string.suggested_cities else R.string.search_results),
                        Modifier.padding(start = 4.dp),
                    )
                }
                if (cities.isEmpty()) {
                    item(key = "empty") { EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.no_cities_found)) }
                }
                itemsIndexed(cities, key = { _, city -> "${city.name}|${city.latitude}|${city.longitude}" }) { index, city ->
                    SegmentedListItem(
                        onClick = { viewModel.choose(city) },
                        shapes = ListItemDefaults.segmentedShapes(index, cities.size),
                        leadingContent = { Icon(Icons.Rounded.LocationCity, contentDescription = null) },
                        supportingContent = { Text("${city.countryName(locale)} · ${utcOffset(city.timezoneId, locale)}") },
                        modifier = Modifier.animateItem(),
                    ) { Text(city.displayName(locale)) }
                }
            }
        }
    }
}

@Composable
private fun CurrentLocationCard(location: SavedLocation, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                shape = MaterialShapes.Cookie7Sided.toShape(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.LocationOn, contentDescription = null) }
            }
            Column(Modifier.weight(1f)) {
                Text(location.cityName ?: stringResource(R.string.default_location), style = MaterialTheme.typography.titleMedium)
                Text(locationStatus(location), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** "Chosen manually", or "Automatic · updated 5 minutes ago". */
@Composable
fun locationStatus(location: SavedLocation): String = when (location.source) {
    LocationSource.MANUAL -> stringResource(R.string.location_mode_manual)
    LocationSource.DEFAULT -> stringResource(R.string.location_not_set)
    LocationSource.DEVICE, LocationSource.IP -> {
        val ago = DateUtils.getRelativeTimeSpanString(location.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        stringResource(R.string.location_auto_updated, ago)
    }
}

/** "UTC+04:00", kept left-to-right inside Arabic text. */
private fun utcOffset(timezoneId: String, locale: Locale): String {
    val offset = runCatching { ZoneId.of(timezoneId).rules.getOffset(Instant.now()) }.getOrNull() ?: return timezoneId
    val suffix = if (offset.totalSeconds == 0) "" else offset.id
    return "⁦" + Numerals.localize("UTC$suffix", locale) + "⁩"
}
