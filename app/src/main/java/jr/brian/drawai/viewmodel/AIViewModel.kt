package jr.brian.drawai.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
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

    private fun agentStreamingStrategy(onCollect: (String) -> Unit): AIAgentStrategy<String, String> {
        return strategy("library-assistant") {
            val getMdOutput by node<String, String> { input ->
                llm.writeSession {
                    updatePrompt { user(input) }
                    val markdownStream = requestLLMStreaming()
                    markdownStream.collect { str ->
                        onCollect(str)
                    }
                }
                ""
            }
            edge(nodeStart forwardTo getMdOutput)
            edge(getMdOutput forwardTo nodeFinish)
        }
    }

    fun generateObjectToDraw() {
        resetState()
        viewModelScope.launch {
            val agentStrategy = agentStreamingStrategy { str ->
                _state.update { currentState ->
                    val currentObjectToDraw = currentState.objectToDraw ?: ""
                    currentState.copy(objectToDraw = currentObjectToDraw + str)
                }
            }
            val agent = AIAgent(
                executor = simpleAnthropicExecutor(apiKey),
                systemPrompt = "You are an artist. " +
                        "You have experience sketching with a finger or stylus.",
                llmModel = model,
                strategy = agentStrategy,
            )
            try {
                agent.run(
                    "Give me a simple ${drawingSuggestions.random()} to draw. " +
                            "It should be something that can be drawn with a finger or stylus. " +
                            "Keep the instruction short and concise."
                )
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
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "captured_sketch_${System.currentTimeMillis()}"
            )
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
        _state.update { currentState -> currentState.copy(response = null) }
        viewModelScope.launch {
            val prompt = prompt("multimodal_input") {
                system(
                    "You are an egotistical, harsh, and unfair judge for a drawing contest. " +
                            "You love to insult people and their artistic abilities." +
                            "You are very critical and love to point out the flaws in people's work."
                )
                user(
                    content = "Judge this drawing. Make harsh, nasty, judgements on quality," +
                            " creativity, and how well it represents ${state.value.objectToDraw}. " +
                            "Give me a score out of 10. Be very rude and harsh regarding the artist's abilities. " +
                            "Use offensive humor, sarcasm and be creative with your feedback. " +
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
                promptExecutor.executeStreaming(
                    prompt = prompt,
                    model = model,
                ).collect { chunk ->
                    _state.update { currentState ->
                        val currentResponse = currentState.response ?: ""
                        currentState.copy(response = currentResponse + chunk)
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