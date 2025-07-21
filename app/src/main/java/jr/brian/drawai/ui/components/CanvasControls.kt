package jr.brian.drawai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CanvasControls(
    response: String?,
    selectedColor: Color,
    colors: List<Color>,
    isGeneratingObjToDraw: Boolean = true,
    onSelectColor: (Color) -> Unit,
    onClearCanvas: () -> Unit,
    onSubmitDrawing: () -> Unit,
    onGenerateObjectToDraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isResponseVisible by remember { mutableStateOf(true) }
    val toggleResponseVisibility = { isResponseVisible = !isResponseVisible }
    Column(
        modifier = Modifier
            .background(Color.DarkGray)
            .clickable(onClick = toggleResponseVisibility),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(isResponseVisible) {
            Text(
                response ?: "Brainstorming what to draw...",
                modifier = Modifier
                    .padding(16.dp)

            )
        }
        AnimatedVisibility(isResponseVisible) {
            HorizontalDivider(
                color = Color.White,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
            )
        }
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(
                12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Spacer(modifier = Modifier.width(8.dp))
            }

            items(colors) { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = if (isSelected) 1.2f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            onSelectColor(color)
                        }
                )
            }

            item {
                VerticalDivider(
                    color = Color.White,
                    modifier = Modifier
                        .height(40.dp)
                        .padding(horizontal = 8.dp)
                )
            }

            item {
                IconButton(
                    enabled = isGeneratingObjToDraw,
                    onClick = {
                        isResponseVisible = true
                        onGenerateObjectToDraw()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Generate new object to draw",
                    )
                }
            }

            item {
                IconButton(
                    enabled = isGeneratingObjToDraw,
                    onClick = onSubmitDrawing
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Submit drawing",
                    )
                }
            }

            item {
                IconButton(
                    onClick = onClearCanvas
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Clear canvas",
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}