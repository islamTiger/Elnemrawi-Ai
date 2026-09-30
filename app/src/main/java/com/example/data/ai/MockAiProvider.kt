package com.example.data.ai

import com.example.core.di.ServiceLocator
import com.example.core.registry.ModelRegistry
import com.example.domain.ai.AiRuntimeStreamEvent
import com.example.domain.ai.AiRuntimeRequest
import com.example.domain.ai.AiRuntimeException
import com.example.domain.ai.LlamaCppEngineStatus
import com.example.domain.model.ModelInfo
import com.example.domain.repository.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockAiProvider : AiProvider {
    override val providerName: String = "Nemrawy Local AI Engine"
    
    override val isConfigured: Boolean
        get() = ServiceLocator.localModelRuntime.isConfigured

    override suspend fun getAvailableModels(): List<ModelInfo> {
        return ModelRegistry.getModels()
    }

    override suspend fun generateResponse(prompt: String, modelId: String): Flow<String> = flow {
        val model = ModelRegistry.getModel(modelId)
        val runtime = ServiceLocator.localModelRuntime
        
        if (!runtime.isConfigured) {
            emit("Local model runtime is not configured.")
            return@flow
        }
        
        if (ServiceLocator.llamaCppEngine.status == LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE) {
            emit("Local model selected, but the native inference engine is unavailable.")
            return@flow
        }
        
        try {
            val req = AiRuntimeRequest(prompt = prompt, model = model ?: ModelRegistry.getModels().first())
            var currentText = ""
            runtime.generateStream(req).collect { event ->
                when (event) {
                    is AiRuntimeStreamEvent.TOKEN -> {
                        currentText += event.text
                        emit(currentText)
                    }
                    is AiRuntimeStreamEvent.ERROR -> {
                        emit("Error: ${event.message}")
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            emit("Error occurred: ${e.message}")
        }
    }

    override suspend fun generateImage(prompt: String, modelId: String): String {
        return "ERROR: Image Generation is unavailable."
    }

    override suspend fun editImage(imagePath: String, prompt: String, modelId: String): String {
        return "ERROR: Image Editing is unavailable."
    }
}
