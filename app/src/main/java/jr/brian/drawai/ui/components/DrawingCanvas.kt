package jr.brian.drawai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastForEach
import jr.brian.drawai.model.state.DrawingAction
import jr.brian.drawai.model.state.PathData
import kotlin.math.abs

@Composable
fun DrawingCanvas(
    paths: List<PathData>,
    currentPath: PathData?,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(Color.White)
            .pointerInput(true) {
                detectDragGestures(
                    onDragStart = {
                        onAction(DrawingAction.OnNewPathStart)
                    },
                    onDragEnd = {
                        onAction(DrawingAction.OnPathEnd)
                    },
                    onDrag = { change, _ ->
                        onAction(DrawingAction.OnDraw(change.position))
                    },
                    onDragCancel = {
                        onAction(DrawingAction.OnPathEnd)
                    },
                )
            }
    ) {
        paths.fastForEach { pathData ->
            drawPath(
                path = pathData.offsets,
                color = pathData.color,
            )
        }
        currentPath?.let {
            drawPath(
                path = it.offsets,
                color = it.color
            )
        }
    }
}

private fun DrawScope.drawPath(
    color: Color,
    path: List<Offset>,
    thickness: Float = 10f
) {
    val smoothedPath = Path().apply {
        if (path.isNotEmpty()) {
            moveTo(path.first().x, path.first().y)
            val smoothness = 5
            for (i in 1..path.lastIndex) {
                val from = path[i - 1]
                val to = path[i]
                val dx = abs(from.x - to.x)
                val dy = abs(from.y - to.y)
                if (dx >= smoothness || dy >= smoothness) {
                    val controlX = (from.x + to.x) / 2f
                    val controlY = (from.y + to.y) / 2f
                    cubicTo(
                        x1 = controlX,
                        y1 = controlY,
                        x2 = controlX,
                        y2 = controlY,
                        x3 = to.x,
                        y3 = to.y
                    )
                }
            }
        }
    }
    drawPath(
        path = smoothedPath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}