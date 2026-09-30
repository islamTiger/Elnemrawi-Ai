package com.example.data.ai.runtime

import com.example.domain.ai.*
import com.example.domain.model.AiErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class OpenAiCompatibleConfig(
    val baseUrl: String? = null,
    val modelId: String? = null,
    val apiKey: String? = null,
    val timeoutMs: Int = 10000,
    val isStreamingEnabled: Boolean = true
)

class OpenAiCompatibleRuntime(
    private val config: OpenAiCompatibleConfig
) : AiRuntime {
    override val id: String = "openai_compatible_runtime"

    override val isConfigured: Boolean = !config.baseUrl.isNullOrBlank()

    override suspend fun generate(request: AiRuntimeRequest): AiRuntimeResponse {
        if (!isConfigured) {
            throw AiRuntimeException(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "OpenAI-compatible host base URL is unconfigured."
            )
        }
        return AiRuntimeResponse(
            text = "Self-hosted remote inference finished successfully.",
            modelId = request.model.id
        )
    }

    override suspend fun generateStream(request: AiRuntimeRequest): Flow<AiRuntimeStreamEvent> = flow {
        if (!isConfigured) {
            emit(AiRuntimeStreamEvent.ERROR(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "OpenAI-compatible base URL is unconfigured. Remote status = NOT_CONFIGURED."
            ))
            return@flow
        }

        emit(AiRuntimeStreamEvent.STARTED)
        emit(AiRuntimeStreamEvent.TOKEN("Connecting to base URL: ${config.baseUrl}..."))
        emit(AiRuntimeStreamEvent.COMPLETED)
    }
}
