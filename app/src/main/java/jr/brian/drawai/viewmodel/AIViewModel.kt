package jr.brian.drawai.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.message.Attachment
import ai.koog.prompt.message.AttachmentContent
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.drawai.BuildConfig
import jr.brian.drawai.model.state.AIChatState
import jr.brian.drawai.model.state.drawingSuggestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream

class AIViewModel : ViewModel() {
    private val _state = MutableStateFlow(AIChatState())
    val state = _state.asStateFlow()

    private val apiKey = BuildConfig.claudeApiKey
    private val model = AnthropicModels.Haiku_3_5

    init {
        generateObjectToDraw()
    }

    private fun resetState() {
        _state.update { currentState ->
            currentState.copy(
                response = null,
                objectToDraw = null
            )
        }
    }

    fun generateObjectToDraw() {
        resetState()
        viewModelScope.launch {
            val agent = AIAgent(
                executor = simpleAnthropicExecutor(apiKey),
                systemPrompt = "You are an artist. " +
                        "You have experience sketching with a finger or stylus.",
                llmModel = model,
            )
            try {
                val result = agent.run(
                    "Give me a simple ${drawingSuggestions.random()} to draw. " +
                            "It should be something that can be drawn with a finger or stylus. " +
                            "Keep the instruction short and concise."
                )
                _state.update { currentState -> currentState.copy(objectToDraw = result) }
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(objectToDraw = "Error: ${e.message}")
                }
            }
        }
    }

    fun saveDrawingToStorage(
        context: Context,
        imageData: ByteArray,
    ) {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "captured_sketch_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val imageUri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        imageUri?.let { uri ->
            try {
                resolver.openOutputStream(uri).use { outputStream: OutputStream? ->
                    if (outputStream == null) {
                        throw IOException("Failed to get output stream for MediaStore URI.")
                    }
                    outputStream.write(imageData)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Failed to create media entry", Toast.LENGTH_SHORT).show()
        }
    }

    fun submitDrawing(byteArray: ByteArray) {
        _state.update { currentState ->
            currentState.copy(
                response = null,
            )
        }
        viewModelScope.launch {
            val prompt = prompt("multimodal_input") {
                system(
                    "You are a harsh but unfair judge for a drawing contest. " +
                            "Keep in mind that this the submitted drawings were " +
                            "made using a finger or stylus. (Who really cares?)"
                )
                user(
                    content = "Judge this drawing. Make harsh, nasty, judgements on quality," +
                            " creativity, and how well it represents ${state.value.objectToDraw}. " +
                            "Give me a score out of 10. Be very rude and harsh if the score is low or average. " +
                            "Be very super nice and complimentary if the score is high. " +
                            "Use offensive humor and sarcasm and be creative with your feedback. " +
                            "Express great disdain if the drawing is bad or completely blank (all white image).",
                    attachments = listOf(
                        Attachment.Image(
                            content = AttachmentContent.Binary.Bytes(byteArray),
                            format = "png",
                        )
                    )
                )
            }
            val promptExecutor = simpleAnthropicExecutor(apiKey)
            try {
                val response = promptExecutor.execute(
                    prompt = prompt,
                    model = model,
                    tools = listOf()
                )
                response.forEach {
                    _state.update { currentState ->
                        currentState.copy(response = it.content)
                    }
                }
            } catch (e: Exception) {
                _state.update { currentState ->
                    currentState.copy(response = "Error: ${e.message}")
                }
            }
        }
    }
}