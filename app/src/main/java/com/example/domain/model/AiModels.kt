package com.example.domain.model

enum class ModelType {
    TEXT,
    CODING,
    VISION,
    IMAGE_GENERATION,
    IMAGE_EDITING
}

enum class ModelCapability {
    TEXT_GENERATION,
    CODE_COMPLETION,
    CODE_EXPLANATION,
    IMAGE_TO_TEXT,
    IMAGE_GENERATION,
    IMAGE_EDITING,
    FUNCTION_CALLING
}

enum class RuntimeType {
    LOCAL,
    OPENAI_COMPATIBLE,
    OLLAMA,
    LLAMACPP,
    CUSTOM_HTTP,
    CUSTOM_WEBSOCKET
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: String,
    val type: ModelType,
    val capabilities: List<ModelCapability>,
    val contextWindow: Int,
    val parameterSize: String, // e.g., "8B", "33B", "70B", "N/A"
    val quantization: String,  // e.g., "Q4_K_M", "FP16", "None"
    val isLocal: Boolean,
    val isEnabled: Boolean,
    val isConfigured: Boolean,
    val runtimeType: RuntimeType
)

enum class AiErrorCode {
    NO_MODEL,
    NOT_CONFIGURED,
    NETWORK_ERROR,
    AUTH_ERROR,
    TIMEOUT,
    RATE_LIMITED,
    INVALID_REQUEST,
    CONTEXT_TOO_LARGE,
    MODEL_ERROR,
    UNSUPPORTED_CAPABILITY,
    CANCELLED,
    UNKNOWN
}

enum class AiRuntimeStatus {
    NOT_CONFIGURED,
    MODEL_UNAVAILABLE,
    RUNTIME_ERROR,
    SUCCESS,
    CANCELLED
}
