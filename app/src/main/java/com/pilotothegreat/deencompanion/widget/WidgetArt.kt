package com.pilotothegreat.deencompanion.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.graphics.createBitmap
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath as polygonPath

/** Art Glance can't draw itself, rendered as white bitmaps so the theme can tint them. */
internal object WidgetArt {

    /** A MaterialShapes polygon (normalised to a unit square) filling a [sizePx] square. */
    fun shape(polygon: RoundedPolygon, sizePx: Int): Bitmap {
        val path: Path = polygon.polygonPath()
        path.transform(Matrix().apply { setScale(sizePx.toFloat(), sizePx.toFloat()) })
        return createBitmap(sizePx, sizePx).also { Canvas(it).drawPath(path, whitePaint()) }
    }

    /** A Compose [ImageVector] (a Material icon) filling a [sizePx] square. */
    fun icon(vector: ImageVector, sizePx: Int): Bitmap = createBitmap(sizePx, sizePx).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.scale(sizePx / vector.viewportWidth, sizePx / vector.viewportHeight)
        drawGroup(canvas, vector.root, whitePaint())
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        for (node in group) {
            when (node) {
                is VectorGroup -> drawGroup(canvas, node, paint)
                is VectorPath -> {
                    val path = node.pathData.toPath().asAndroidPath()
                    path.fillType = if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                    canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restore()
    }

    private fun whitePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
}
