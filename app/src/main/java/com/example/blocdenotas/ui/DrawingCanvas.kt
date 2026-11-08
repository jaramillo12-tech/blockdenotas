package com.example.blocdenotas.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
) {
    fun toComposePath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
        return path
    }
}

@Composable
fun DrawingCanvas(
    paths: List<DrawnPath>,
    currentPath: List<Offset>,
    currentColor: Color,
    currentStrokeWidth: Float,
    isEraser: Boolean,
    onPathStarted: (Offset) -> Unit,
    onPathUpdated: (Offset) -> Unit,
    onPathEnded: () -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .onSizeChanged { onSizeChanged(it) }
            .pointerInput(currentColor, currentStrokeWidth, isEraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onPathStarted(offset)
                    },
                    onDrag = { change, _ ->
                        onPathUpdated(change.position)
                    },
                    onDragEnd = {
                        onPathEnded()
                    },
                    onDragCancel = {
                        onPathEnded()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (drawnPath in paths) {
                val pathColor = if (drawnPath.isEraser) Color.White else drawnPath.color
                drawPath(
                    path = drawnPath.toComposePath(),
                    color = pathColor,
                    style = Stroke(
                        width = drawnPath.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            if (currentPath.isNotEmpty()) {
                val tempDrawnPath = DrawnPath(
                    points = currentPath,
                    color = currentColor,
                    strokeWidth = currentStrokeWidth,
                    isEraser = isEraser
                )
                val activeColor = if (isEraser) Color.White else currentColor
                drawPath(
                    path = tempDrawnPath.toComposePath(),
                    color = activeColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

fun renderPathsToBitmap(
    paths: List<DrawnPath>,
    currentPath: List<Offset>,
    currentColor: Color,
    currentStrokeWidth: Float,
    isEraser: Boolean,
    width: Int,
    height: Int,
    targetMaxSize: Int = 512
): Bitmap {
    val srcWidth = width.coerceAtLeast(300)
    val srcHeight = height.coerceAtLeast(300)

    val scale = (targetMaxSize.toFloat() / maxOf(srcWidth, srcHeight)).coerceAtMost(1.0f)
    val dstWidth = (srcWidth * scale).toInt().coerceAtLeast(100)
    val dstHeight = (srcHeight * scale).toInt().coerceAtLeast(100)

    val bitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.scale(scale, scale)
    canvas.drawColor(android.graphics.Color.WHITE)

    val allPaths = paths.toMutableList()
    if (currentPath.isNotEmpty()) {
        allPaths.add(
            DrawnPath(
                points = currentPath,
                color = currentColor,
                strokeWidth = currentStrokeWidth,
                isEraser = isEraser
            )
        )
    }

    for (drawnPath in allPaths) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = drawnPath.strokeWidth
            color = if (drawnPath.isEraser) android.graphics.Color.WHITE else drawnPath.color.toArgb()
        }

        val androidPath = AndroidPath()
        if (drawnPath.points.isNotEmpty()) {
            androidPath.moveTo(drawnPath.points.first().x, drawnPath.points.first().y)
            for (i in 1 until drawnPath.points.size) {
                androidPath.lineTo(drawnPath.points[i].x, drawnPath.points[i].y)
            }
        }
        canvas.drawPath(androidPath, paint)
    }

    return bitmap
}
