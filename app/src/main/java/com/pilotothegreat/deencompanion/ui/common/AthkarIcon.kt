package com.pilotothegreat.deencompanion.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Prayer beads (misbaha) for the Athkar tab; Material has no such icon. */
val AthkarIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Athkar", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(fill = SolidColor(Color.Black)) {
            // A ring of beads with a gap at the bottom where the tassel hangs.
            for (slot in 0 until 12) {
                if (slot == 6) continue
                val angle = Math.toRadians(slot * 30.0 - 90.0)
                bead(12f + 6.5f * cos(angle).toFloat(), 9.5f + 6.5f * sin(angle).toFloat(), 1.6f)
            }
            bead(12f, 16.6f, 1.9f)
            moveTo(11.35f, 18.2f)
            lineTo(12.65f, 18.2f)
            lineTo(12.65f, 20.6f)
            lineTo(11.35f, 20.6f)
            close()
            bead(12f, 21.6f, 1.4f)
        }
        .build()
}

private fun PathBuilder.bead(cx: Float, cy: Float, radius: Float) {
    moveTo(cx + radius, cy)
    arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * radius, dy1 = 0f)
    arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * radius, dy1 = 0f)
    close()
}
