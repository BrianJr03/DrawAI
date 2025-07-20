package jr.brian.drawai.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import jr.brian.drawai.model.state.DrawingAction
import jr.brian.drawai.model.state.DrawingState
import jr.brian.drawai.model.state.PathData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DrawViewModel : ViewModel() {
    private val _state = MutableStateFlow(DrawingState())
    val state = _state.asStateFlow()

    fun onAction(action: DrawingAction) {
        when (action) {
            DrawingAction.OnClearCanvas -> clearCanvas()
            is DrawingAction.OnDraw -> onDraw(action.offset)
            is DrawingAction.OnNewPathStart -> onNewPathStart()
            DrawingAction.OnPathEnd -> onPathEnd()
            is DrawingAction.OnSelectColor -> onSelectColor(action.color)
        }
    }

    private fun clearCanvas() {
        _state.update {
            it.copy(
                currentPath = null,
                paths = emptyList()
            )
        }
    }

    private fun onDraw(offset: Offset) {
        val currentPath = _state.value.currentPath ?: return
        _state.update {
            it.copy(
                currentPath = currentPath.copy(
                    offsets = currentPath.offsets + offset
                )
            )
        }
    }

    private fun onNewPathStart() {
        _state.update {
            it.copy(
                currentPath = PathData(
                    id = System.currentTimeMillis().toString(),
                    color = it.selectedColor,
                    offsets = emptyList()
                )
            )
        }
    }

    private fun onPathEnd() {
        val currentPath = _state.value.currentPath ?: return
        _state.update {
            it.copy(
                currentPath = null,
                paths = it.paths + currentPath
            )
        }
    }

    private fun onSelectColor(color: Color) {
        _state.update { it.copy(selectedColor = color) }
    }
}