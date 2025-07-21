package jr.brian.drawai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.suwasto.capturablecompose.Capturable
import io.github.suwasto.capturablecompose.CompressionFormat
import io.github.suwasto.capturablecompose.rememberCaptureController
import io.github.suwasto.capturablecompose.toByteArray
import jr.brian.drawai.ui.components.CanvasControls
import jr.brian.drawai.model.state.AIChatState
import jr.brian.drawai.model.state.DrawingAction
import jr.brian.drawai.ui.components.DrawingCanvas
import jr.brian.drawai.ui.components.ResultsDialog
import jr.brian.drawai.model.state.DrawingState
import jr.brian.drawai.model.state.defaultColors
import jr.brian.drawai.ui.theme.DrawAITheme
import jr.brian.drawai.viewmodel.AIViewModel
import jr.brian.drawai.viewmodel.DrawViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrawAITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel = viewModel<DrawViewModel>()
                    val aiViewModel = viewModel<AIViewModel>()

                    val drawingState by viewModel.state.collectAsStateWithLifecycle()
                    val aiChatState by aiViewModel.state.collectAsStateWithLifecycle()
                    AppUI(
                        drawingState = drawingState,
                        aiChatState = aiChatState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onAction = viewModel::onAction,
                        onSelectColor = {
                            viewModel.onAction(DrawingAction.OnSelectColor(it))
                        },
                        onClearCanvas = {
                            viewModel.onAction(DrawingAction.OnClearCanvas)
                        },
                        handleCapture = aiViewModel::submitDrawing,
                        onGenerateObjectToDraw = aiViewModel::generateObjectToDraw,
                        onFavorite = { currentDrawing ->
                            aiViewModel.saveDrawingToStorage(
                                context = this,
                                imageData = currentDrawing
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUI(
    drawingState: DrawingState,
    aiChatState: AIChatState,
    modifier: Modifier = Modifier,
    onAction: (DrawingAction) -> Unit,
    onSelectColor: (Color) -> Unit,
    onClearCanvas: () -> Unit,
    handleCapture: (ByteArray) -> Unit,
    onGenerateObjectToDraw: () -> Unit,
    onFavorite: (ByteArray) -> Unit
) {
    var isDrawingFavorite by remember { mutableStateOf(false) }
    var isResultsDialogShowing by remember { mutableStateOf(false) }
    var currentDrawing by remember { mutableStateOf<ByteArray?>(null) }
    ResultsDialog(
        showDialog = isResultsDialogShowing,
        onDismissRequest = { isResultsDialogShowing = false },
        imageData = currentDrawing,
        text = aiChatState.response,
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val captureController = rememberCaptureController()
        Column {
            CanvasControls(
                response = aiChatState.objectToDraw,
                selectedColor = drawingState.selectedColor,
                colors = defaultColors,
                onSelectColor = onSelectColor,
                onClearCanvas = onClearCanvas,
                onSubmitDrawing = {
                    captureController.capture()
                },
                onFavorite = {
                    captureController.capture()
                    currentDrawing?.let {
                        onFavorite(it)
                    }
                    isDrawingFavorite = true
                },
                onGenerateObjectToDraw = onGenerateObjectToDraw,
                isGeneratingObjToDraw = aiChatState.objectToDraw != null,
                modifier = Modifier.fillMaxWidth()
            )
            Capturable(
                captureController = captureController,
                onCaptured = { imageBitmap ->
                    val byteArray = imageBitmap.toByteArray(
                        compressionFormat = CompressionFormat.PNG,
                        quality = 100
                    )
                    currentDrawing = byteArray
                    handleCapture(byteArray)
                    if (!isDrawingFavorite) {
                        isResultsDialogShowing = true
                    }
                    isDrawingFavorite = false
                }
            ) {
                DrawingCanvas(
                    paths = drawingState.paths,
                    currentPath = drawingState.currentPath,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(.85f)
                )
            }
        }
    }
}