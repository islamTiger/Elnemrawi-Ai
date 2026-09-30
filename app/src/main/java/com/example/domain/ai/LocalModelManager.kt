package com.example.domain.ai

import kotlinx.coroutines.flow.StateFlow

enum class LocalModelState {
    NO_MODEL,
    SELECTING,
    VALIDATING,
    READY,
    INVALID,
    UNAVAILABLE,
    LOADING,
    LOADED,
    ERROR
}

data class LocalModelConfig(
    val name: String,
    val path: String, // Android SAF Uri String or file path
    val format: String = "GGUF",
    val size: Long = 0L,
    val quantization: String = "UNKNOWN",
    val contextLength: Int = 0,
    val isEnabled: Boolean = true,
    val parameterCount: String = "UNKNOWN"
)

interface LocalModelRepository {
    fun getLocalModelConfig(): LocalModelConfig?
    fun saveLocalModelConfig(config: LocalModelConfig?)
}

interface LocalModelManager {
    val modelState: StateFlow<LocalModelState>
    val currentConfig: StateFlow<LocalModelConfig?>
    
    suspend fun selectModel(uriString: String, name: String, size: Long)
    suspend fun removeModel()
    suspend fun validateActiveModel(): Boolean
}
