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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AIViewModel : ViewModel() {
    private val _state = MutableStateFlow(AIChatState())
    val state = _state.asStateFlow()

    private val apiKey = BuildConfig.claudeApiKey

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
                systemPrompt = "You are an artist.",
                llmModel = AnthropicModels.Sonnet_3_5,
            )
            try {
                val result = agent.run(
                    "Give me a simple object to draw. " +
                            "It should be something that can be drawn with a finger or stylus. " +
                            "Keep the instruction short."
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
                system("You are an artist.")
                user(
                    content = "Judge this drawing. Make judgements on quality," +
                            " creativity, and how well it represents ${state.value.objectToDraw}. " +
                            "Give me a score out of 10. Feel to be rude and harsh if the score is low." +
                            " Keep in mind that this is drawn using a finger or stylus.",
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
                    model = AnthropicModels.Sonnet_3_5,
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