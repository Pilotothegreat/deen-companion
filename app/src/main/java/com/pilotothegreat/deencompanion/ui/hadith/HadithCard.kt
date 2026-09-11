package com.pilotothegreat.deencompanion.ui.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.hadith.Hadith
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.theme.Amiri

@Composable
fun HadithCard(
    hadith: Hadith,
    bookName: String?,
    isFavorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val number = stringResource(R.string.hadith_number, Formatters.number(hadith.number, locale))
    val header = listOfNotNull(bookName, number).joinToString(" · ")
    val showEnglish = hadith.english.isNotBlank() && (!locale.isArabic || hadith.arabic.isBlank())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(header, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                if (hadith.grade.isNotBlank()) GradeLabel(hadith.grade)
            }
            if (hadith.narrator.isNotBlank()) {
                Text(stringResource(R.string.narrated_by, hadith.narrator), style = MaterialTheme.typography.titleSmall)
            }
            if (hadith.arabic.isNotBlank()) {
                Text(
                    hadith.arabic,
                    fontFamily = Amiri,
                    fontSize = 20.sp,
                    lineHeight = 38.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showEnglish) {
                if (hadith.arabic.isNotBlank()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(hadith.english, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.align(Alignment.End)) {
                IconButton(onClick = {
                    val text = listOf(hadith.arabic, hadith.english).filter { it.isNotBlank() }.joinToString("\n\n")
                    context.startSafely(SystemIntents.shareText("$text\n— $header"))
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                }
                IconToggleButton(checked = isFavorite, onCheckedChange = onFavoriteChange) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeLabel(grade: String) {
    val colors = MaterialTheme.colorScheme
    val normalized = grade.lowercase()
    val (label, container, content) = when {
        "hasan sahih" in normalized -> Triple(stringResource(R.string.grade_hasan_sahih), colors.primaryContainer, colors.onPrimaryContainer)
        "sahih" in normalized -> Triple(stringResource(R.string.grade_sahih), colors.primaryContainer, colors.onPrimaryContainer)
        "hasan" in normalized -> Triple(stringResource(R.string.grade_hasan), colors.secondaryContainer, colors.onSecondaryContainer)
        "da'if" in normalized || "daif" in normalized || "da`if" in normalized ->
            Triple(stringResource(R.string.grade_daif), colors.errorContainer, colors.onErrorContainer)
        "mawdu" in normalized || "fabricated" in normalized -> Triple(stringResource(R.string.grade_mawdu), colors.error, colors.onError)
        else -> Triple<String, Color, Color>(grade, colors.surfaceVariant, colors.onSurfaceVariant)
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
