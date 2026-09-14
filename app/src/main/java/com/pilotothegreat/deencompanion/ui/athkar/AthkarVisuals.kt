package com.pilotothegreat.deencompanion.ui.athkar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.graphics.shapes.RoundedPolygon
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.ui.common.AthkarIcon

/** Icon for a core category, or for an everyday one by its group. */
fun athkarIcon(categoryId: String, groupId: String? = null): ImageVector = when (categoryId) {
    AthkarIds.MORNING -> Icons.Rounded.WbSunny
    AthkarIds.EVENING -> Icons.Rounded.NightsStay
    AthkarIds.AFTER_PRAYER -> Icons.Rounded.Mosque
    AthkarIds.WAKING -> Icons.Rounded.WbTwilight
    AthkarIds.SLEEP -> Icons.Rounded.Bedtime
    else -> when (groupId) {
        "home" -> Icons.Rounded.Home
        "prayer" -> Icons.Rounded.Mosque
        "food" -> Icons.Rounded.Restaurant
        "travel" -> Icons.Rounded.Luggage
        "hardship" -> Icons.Rounded.Healing
        "nature" -> Icons.Rounded.Cloud
        "social" -> Icons.Rounded.Groups
        else -> AthkarIcon
    }
}

fun athkarShape(categoryId: String): RoundedPolygon = when (categoryId) {
    AthkarIds.MORNING -> MaterialShapes.Sunny
    AthkarIds.EVENING -> MaterialShapes.Cookie12Sided
    AthkarIds.AFTER_PRAYER -> MaterialShapes.Flower
    AthkarIds.WAKING -> MaterialShapes.SoftBurst
    AthkarIds.SLEEP -> MaterialShapes.PuffyDiamond
    else -> MaterialShapes.Cookie9Sided
}

/** Container and content colors that give each core tile its own tone. */
fun athkarColors(categoryId: String, scheme: ColorScheme): Pair<Color, Color> = when (categoryId) {
    AthkarIds.MORNING -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    AthkarIds.EVENING -> scheme.secondaryContainer to scheme.onSecondaryContainer
    AthkarIds.AFTER_PRAYER -> scheme.primaryContainer to scheme.onPrimaryContainer
    else -> scheme.surfaceContainerHigh to scheme.onSurface
}
