package com.pilotothegreat.deencompanion.ui.theme

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotationZ: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val matrix = Matrix()
        // The default normalized shape coordinates are in the range of -1..1 centered at 0,0.
        // Thus, the scale is size / 2.
        val scale = minOf(size.width, size.height) / 2f
        matrix.postScale(scale, scale)
        if (rotationZ != 0f) {
            matrix.postRotate(rotationZ)
        }
        matrix.postTranslate(size.width / 2f, size.height / 2f)

        // Generate the android.graphics.Path for the current progress
        val androidPath = morph.toPath(progress = progress)
        androidPath.transform(matrix)

        return Outline.Generic(androidPath.asComposePath())
    }
}
