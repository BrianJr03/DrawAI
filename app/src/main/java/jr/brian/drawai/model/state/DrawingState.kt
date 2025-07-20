package jr.brian.drawai.model.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

val defaultColors = listOf(
    Color.Black,
    Color.Red,
    Color.Green,
    Color.Blue,
    Color.Yellow,
    Color.Cyan,
    Color.Magenta,
    Color.Gray
)

data class DrawingState(
    val selectedColor: Color = Color.Black,
    val currentPath: PathData? = null,
    val paths: List<PathData> = emptyList()
)

data class PathData(
    val id: String,
    val color: Color,
    val offsets: List<Offset>
)

sealed interface DrawingAction {
    data object OnNewPathStart: DrawingAction
    data class OnDraw(val offset: Offset): DrawingAction
    data object OnPathEnd: DrawingAction
    data class OnSelectColor(val color: Color): DrawingAction
    data object OnClearCanvas: DrawingAction
}