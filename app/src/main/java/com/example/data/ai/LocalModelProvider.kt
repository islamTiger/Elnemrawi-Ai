package com.example.data.ai

import com.example.domain.ai.ModelProvider
import com.example.domain.ai.ModelRequest
import com.example.domain.ai.ModelResponse
import com.example.domain.ai.ModelResponseDelta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class LocalModelProvider : ModelProvider {
    override val providerId: String = "local_provider"
    
    // Explicitly unconfigured to represent preparation status safely
    override val isConfigured: Boolean = false

    override suspend fun generate(request: ModelRequest): ModelResponse {
        throw IllegalStateException("ERROR: CAPABILITY_NOT_CONFIGURED - Local llama.cpp model inference engine is not configured in this runtime.")
    }

    override suspend fun generateStreaming(request: ModelRequest): Flow<ModelResponseDelta> = flow {
        emit(ModelResponseDelta(textDelta = "", error = "ERROR: CAPABILITY_NOT_CONFIGURED - Local inference server is unavailable."))
    }

    override suspend fun structuredOutput(request: ModelRequest, responseSchema: JSONObject): ModelResponse {
        throw IllegalStateException("ERROR: CAPABILITY_NOT_CONFIGURED - Local structured output engine is unavailable.")
    }

    override suspend fun toolCalling(request: ModelRequest): ModelResponse {
        throw IllegalStateException("ERROR: CAPABILITY_NOT_CONFIGURED - Local tool calling is unavailable.")
    }

    override suspend fun modelCapabilities(modelId: String): List<String> {
        return emptyList()
    }

    override suspend fun healthCheck(): Boolean {
        return false
    }
}
