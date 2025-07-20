package jr.brian.drawai.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.message.Attachment
import ai.koog.prompt.message.AttachmentContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.drawai.BuildConfig
import jr.brian.drawai.model.state.AIChatState
import jr.brian.drawai.model.state.drawingSuggestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                    "Give me something simple to draw. " +
                            "It should be something that can be drawn with a finger or stylus. " +
                            "Use ${drawingSuggestions.joinToString(", ")} as inspiration. " +
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

    fun submitDrawing(byteArray: ByteArray) {
        _state.update { currentState ->
            currentState.copy(
                response = null,
            )
        }
        viewModelScope.launch {
            val prompt = prompt("multimodal_input") {
                system("You are a harsh but fair judge for a drawing contest. " +
                        "Keep in mind that this the submitted drawings were " +
                        "made using a finger or stylus.")
                user(
                    content = "Judge this drawing. Make judgements on quality," +
                            " creativity, and how well it represents ${state.value.objectToDraw}. " +
                            "Give me a score out of 10. Be very rude and harsh if the score is low or average. " +
                            "Be very nice if the score is high. " +
                            "Use humor and be creative with your feedback.",
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