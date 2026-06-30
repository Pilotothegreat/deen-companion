package com.pilotothegreat.deencompanion.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.lazy.LazyListScope
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.ui.theme.googleSans
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.enums.enumEntries

inline val Dp.px: Float
    @Composable get() = with(LocalDensity.current) { this@px.toPx() }

inline val Int.toDp: Dp
    @Composable get() = with(LocalDensity.current) { this@toDp.toDp() }

inline val Dp.toSp: TextUnit
    @Composable get() = with(LocalDensity.current) { this@toSp.toSp() }

fun LocalDateTime.toTimestamp(): Long = toInstant(currentTimezone()).toEpochMilli()
fun LocalDate.toTimestamp(): Long = atStartOfDay().toInstant(currentTimezone()).toEpochMilli()
fun fromTimestamp(stamp: Long): LocalDateTime {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(stamp),
        ZoneId.systemDefault()
    )
}

fun LocalTime.toLocaleHourString(context: Context, short: Boolean = false): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm"
        else (if (short) "hh a" else "hh:mm a")
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return format(formatter)
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun PageTitle(
    backButton: Boolean = false,
    hazeState: HazeState? = null,
    text: String,
    customElement: @Composable (BoxScope.() -> Unit)? = null,
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                hazeState?.let {
                    Modifier.hazeEffect(state = it, style = HazeMaterials.ultraThin()) {
                        progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                } ?: Modifier
            )
    ) {
        Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp).padding(bottom = 6.dp).fillMaxWidth()) {
            CategoryTitleText(text, backButton)
            customElement?.let { it() }
        }
    }
}

val TOP_BAR_HEIGHT: Dp = 52.dp
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryTitleText(text: String, backButton: Boolean = false) {
    val navigator: Navigator = koinInject()
    Row (modifier = Modifier.height(TOP_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically){
        if (backButton) {
            IconButton(onClick = { navigator.goBack() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        }
        Text(
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            text = text
        )
    }
}

@Composable
fun CategoryTitleSmallText(text: String) {
    Text(
        modifier = Modifier.padding(8.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = colorScheme.tertiary
    )
}

fun LazyListScope.categoryTitleSmall(textProvider: @Composable () -> String) {
    item {
        CategoryTitleSmallText(textProvider())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(
    textFieldState: TextFieldState,
    placeholderText: String = stringResource(R.string.search_hint)
) {
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = textFieldState.text.toString(),
                onQueryChange = { newQuery ->
                    textFieldState.edit {
                        replace(0, length, newQuery)
                    }
                },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                textFieldState.edit {
                                    replace(0, length, "")
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.close),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        content = {}
    )
}

inline fun <reified T : Enum<T>> valueOfOrNull(name: String): T? {
    return enumEntries<T>().find { it.name.equals(name, ignoreCase = true) }
}

fun normalizeArabic(text: String): String {
    if (text.isEmpty()) return text
    var normalized = text.replace(Regex("[\\u064B-\\u065F\\u0640\\u0670]"), "")
    normalized = normalized.replace(Regex("[أإآٱ]"), "ا")
    normalized = normalized.replace(Regex("ة"), "ه")
    normalized = normalized.replace(Regex("ى"), "ي")
    return normalized.trim()
}

fun currentTimezone(): ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())

fun openLink(activity: Activity?, link: String) {
    activity?.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            link.toUri()
        )
    )
}

fun convertFontFamilyToTypeface(context: Context, fontFamily: FontFamily): android.graphics.Typeface {
    val resolver = createFontFamilyResolver(context)

    val result = resolver.resolve(
        fontFamily = fontFamily
    )

    return result.value as android.graphics.Typeface
}

@Composable
fun EqualHeightRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val spacingPx = spacing.roundToPx()
        val colWidth = (constraints.maxWidth - spacingPx) / 2
        val colConstraints = constraints.copy(
            minWidth = colWidth, maxWidth = colWidth,
            minHeight = 0, maxHeight = Constraints.Infinity
        )

        // Pass 1: measure natural height
        val firstHeight = subcompose("first_measure", first)
            .sumOf { it.measure(colConstraints).height }
        val secondHeight = subcompose("second_measure", second)
            .sumOf { it.measure(colConstraints).height }
        val maxHeight = maxOf(firstHeight, secondHeight)

        // Pass 2: re-measure at equal height
        val fixedConstraints = Constraints.fixed(colWidth, maxHeight)
        val firstPlaceables = subcompose("first_place", first)
            .map { it.measure(fixedConstraints) }
        val secondPlaceables = subcompose("second_place", second)
            .map { it.measure(fixedConstraints) }

        layout(constraints.maxWidth, maxHeight) {
            firstPlaceables.forEach { it.placeRelative(0, 0) }
            secondPlaceables.forEach { it.placeRelative(colWidth + spacingPx, 0) }
        }
    }
}


enum class MiniCardState {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}

@Composable
fun RowScope.MiniCard(
    state: MiniCardState,
    baseColor: Color = colorScheme.surfaceContainer,
    icon: Painter,
    title: String,
    description: @Composable (font: FontFamily) -> Unit
) {
    val fontFamily = remember { googleSans(weight = 600f) }
    val color by animateColorAsState(
        when(state) {
            MiniCardState.NEGATIVE -> colorScheme.errorContainer
            MiniCardState.POSITIVE -> colorScheme.primaryContainer
            MiniCardState.NEUTRAL -> baseColor
        }
    )
    Column(
        modifier = Modifier
            .card()
            .background(color)
            .padding(16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null)
            Text(title)
        }
        description(fontFamily)
    }
}

inline val shelfShape: RoundedCornerShape
    @Composable get() = shapes.large.copy(
        bottomEnd = CornerSize(0.dp),
        bottomStart = CornerSize(0.dp)
    ) as RoundedCornerShape

fun localizeCityName(cityName: String, lang: String): String {
    if (!lang.startsWith("ar")) return cityName
    var result = cityName
    val translationMap = mapOf(
        "Alawabi" to "العوابي",
        "Al Awabi" to "العوابي",
        "Al-Awabi" to "العوابي",
        "Awabi" to "العوابي",
        "Muscat" to "مسقط",
        "Salalah" to "صلالة",
        "Sohar" to "صحار",
        "Nizwa" to "نزوى",
        "Sur" to "صور",
        "Ibri" to "عبري",
        "Rustaq" to "الرستاق",
        "Buraimi" to "البريمي",
        "Khasab" to "خصب",
        "Barka" to "بركاء",
        "Seeb" to "السيب",
        "Bawshar" to "بوشر",
        "Muttrah" to "مطرح",
        "Oman" to "عمان",
        "Saudi Arabia" to "المملكة العربية السعودية",
        "Makkah" to "مكة المكرمة",
        "Riyadh" to "الرياض"
    )
    for ((english, arabic) in translationMap) {
        result = result.replace(english, arabic, ignoreCase = true)
    }
    return result
}

