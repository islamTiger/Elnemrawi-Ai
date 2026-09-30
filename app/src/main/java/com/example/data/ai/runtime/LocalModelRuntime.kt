package com.example.data.ai.runtime

import com.example.core.di.ServiceLocator
import com.example.domain.ai.*
import com.example.domain.model.AiErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException

class LocalModelRuntime : AiRuntime {
    override val id: String = "local_llama_cpp"

    private val _status = MutableStateFlow("NOT_CONFIGURED")
    val statusState = _status.asStateFlow()

    override val isConfigured: Boolean
        get() {
            val manager = ServiceLocator.localModelManager
            return manager.currentConfig.value != null
        }

    fun getStatus(): String {
        val manager = ServiceLocator.localModelManager
        val config = manager.currentConfig.value
        val state = manager.modelState.value
        
        return when {
            config == null -> "NOT_CONFIGURED"
            state == LocalModelState.SELECTING -> "MODEL_SELECTED"
            state == LocalModelState.VALIDATING -> "VALIDATING"
            state == LocalModelState.READY -> {
                if (ServiceLocator.llamaCppEngine.status == LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE) {
                    "NATIVE_ENGINE_UNAVAILABLE"
                } else {
                    "READY"
                }
            }
            state == LocalModelState.ERROR -> "ERROR"
            else -> _status.value
        }
    }

    suspend fun initialize(): Boolean {
        _status.value = "VALIDATING"
        val ok = ServiceLocator.llamaCppEngine.initialize()
        if (!ok) {
            _status.value = "NATIVE_ENGINE_UNAVAILABLE"
            return false
        }
        _status.value = "READY"
        return true
    }

    suspend fun loadModel(): Boolean {
        if (!isConfigured) {
            _status.value = "NOT_CONFIGURED"
            return false
        }
        _status.value = "LOADING"
        val config = ServiceLocator.localModelManager.currentConfig.value ?: return false
        val ok = ServiceLocator.llamaCppEngine.loadModel(config.path)
        if (!ok) {
            _status.value = "ERROR"
            return false
        }
        _status.value = "LOADED"
        return true
    }

    override suspend fun generate(request: AiRuntimeRequest): AiRuntimeResponse {
        val manager = ServiceLocator.localModelManager
        if (manager.currentConfig.value == null) {
            throw AiRuntimeException(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "Local model runtime is not configured."
            )
        }
        
        if (ServiceLocator.llamaCppEngine.status == LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE) {
            throw AiRuntimeException(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "Local model selected, but the native inference engine is unavailable."
            )
        }

        _status.value = "GENERATING"
        try {
            val text = ServiceLocator.llamaCppEngine.generate(request.prompt)
            _status.value = "COMPLETED"
            return AiRuntimeResponse(text = text, modelId = request.model.id)
        } catch (e: CancellationException) {
            _status.value = "CANCELLED"
            throw e
        } catch (e: Exception) {
            _status.value = "ERROR"
            throw AiRuntimeException(code = AiErrorCode.MODEL_ERROR, message = e.message ?: "Execution error")
        }
    }

    override suspend fun generateStream(request: AiRuntimeRequest): Flow<AiRuntimeStreamEvent> = flow {
        val manager = ServiceLocator.localModelManager
        if (manager.currentConfig.value == null) {
            emit(AiRuntimeStreamEvent.ERROR(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "Local model runtime is not configured."
            ))
            return@flow
        }

        if (ServiceLocator.llamaCppEngine.status == LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE) {
            emit(AiRuntimeStreamEvent.ERROR(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "Local model selected, but the native inference engine is unavailable."
            ))
            return@flow
        }

        _status.value = "GENERATING"
        emit(AiRuntimeStreamEvent.STARTED)
        try {
            ServiceLocator.llamaCppEngine.startStreaming(request.prompt).collect { token ->
                emit(AiRuntimeStreamEvent.TOKEN(token))
            }
            _status.value = "COMPLETED"
            emit(AiRuntimeStreamEvent.COMPLETED)
        } catch (e: CancellationException) {
            _status.value = "CANCELLED"
            emit(AiRuntimeStreamEvent.CANCELLED)
        } catch (e: Exception) {
            _status.value = "ERROR"
            emit(AiRuntimeStreamEvent.ERROR(AiErrorCode.MODEL_ERROR, e.message ?: "Stream error"))
        }
    }

    suspend fun generate(prompt: String): String {
        val model = ServiceLocator.modelSelector.selectModel(ModelCategory.CODING)
            ?: throw IllegalStateException("No coding model found.")
        val res = generate(AiRuntimeRequest(prompt = prompt, model = model))
        return res.text
    }

    suspend fun stream(prompt: String): Flow<AiRuntimeStreamEvent> {
        val model = ServiceLocator.modelSelector.selectModel(ModelCategory.CODING)
            ?: throw IllegalStateException("No coding model found.")
        return generateStream(AiRuntimeRequest(prompt = prompt, model = model))
    }

    suspend fun cancel() {
        ServiceLocator.llamaCppEngine.cancel()
        _status.value = "CANCELLED"
    }

    suspend fun unload() {
        ServiceLocator.llamaCppEngine.unload()
        _status.value = "READY"
    }
}
