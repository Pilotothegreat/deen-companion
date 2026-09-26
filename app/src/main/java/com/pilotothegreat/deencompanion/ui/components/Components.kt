package com.pilotothegreat.deencompanion.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import java.util.Locale
import com.pilotothegreat.deencompanion.R

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() }.padding(start = Spacing.xlarge, end = Spacing.xlarge, top = Spacing.xxlarge, bottom = Spacing.small),
    )
}

/** Explains why a permission helps and offers a single action to grant it. */
@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(Spacing.xlarge), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                if (secondaryActionLabel != null) TextButton(onClick = onSecondaryAction) { Text(secondaryActionLabel) }
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, modifier: Modifier = Modifier, body: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Surface(
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
}

/** Full-width pill search field with a clear button. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        SearchBarDefaults.InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {},
            expanded = false,
            onExpandedChange = {},
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            },
        )
    }
}

/** Small expressive badge, e.g. a surah number. */
@Composable
fun ShapeBadge(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(shape = shape, color = containerColor, contentColor = contentColor, modifier = modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

/**
 * A count that rolls to its next value: the new number rises in as a count grows and drops in as it
 * falls, so a tap is seen to move something. Under reduce-motion it simply changes.
 */
@Composable
fun RollingNumber(value: Int, locale: Locale, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val still = rememberReducedMotion()
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (still) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (targetState > initialState) 1 else -1
                val slide = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                (slideInVertically(slide) { direction * it / 2 } + fadeIn(tween(120))) togetherWith
                    (slideOutVertically(slide) { -direction * it / 2 } + fadeOut(tween(90))) using SizeTransform(clip = false)
            }
        },
        contentAlignment = Alignment.Center,
        label = "rollingNumber",
        modifier = modifier,
    ) { Text(Formatters.number(it, locale), style = style, color = color) }
}
