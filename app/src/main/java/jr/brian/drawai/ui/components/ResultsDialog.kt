package jr.brian.drawai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import jr.brian.drawai.util.Util

@Composable
fun ResultsDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    imageData: ByteArray?,
    text: String?,
) {
    val title = if (text.isNullOrBlank())
        "You're being judged ${Util.EYES_EMOJI}" else "The results are in!"
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (imageData != null) {
                        val bitmap = remember(imageData) {
                            BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Captured Image",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(bottom = 16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Text("No image available", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val scrollState = rememberScrollState()

                    LaunchedEffect(text) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        Text(
                            text = text ?: "Judging your work...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedVisibility(text.isNullOrBlank()) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text("OK")
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
