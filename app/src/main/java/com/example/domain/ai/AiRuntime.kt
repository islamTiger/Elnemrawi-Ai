package com.example.domain.ai

import com.example.domain.model.AiErrorCode
import com.example.domain.model.ModelInfo
import kotlinx.coroutines.flow.Flow

sealed class AiRuntimeStreamEvent {
    object STARTED : AiRuntimeStreamEvent()
    data class TOKEN(val text: String) : AiRuntimeStreamEvent()
    data class THINKING(val thought: String) : AiRuntimeStreamEvent()
    data class TOOL_CALL(val toolId: String, val arguments: String) : AiRuntimeStreamEvent()
    data class TOOL_RESULT(val toolId: String, val result: String) : AiRuntimeStreamEvent()
    object COMPLETED : AiRuntimeStreamEvent()
    data class ERROR(val code: AiErrorCode, val message: String) : AiRuntimeStreamEvent()
    object CANCELLED : AiRuntimeStreamEvent()
}

class AiRuntimeException(
    val code: AiErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception("$code: $message", cause)

data class AiRuntimeRequest(
    val prompt: String,
    val model: ModelInfo,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val systemInstructions: String? = null
)

data class AiRuntimeResponse(
    val text: String,
    val usageTokens: Int = 0,
    val modelId: String
)

interface AiRuntime {
    val id: String
    val isConfigured: Boolean
    suspend fun generate(request: AiRuntimeRequest): AiRuntimeResponse
    suspend fun generateStream(request: AiRuntimeRequest): Flow<AiRuntimeStreamEvent>
}
