package com.example.core.registry

import com.example.domain.model.ModelCapability
import com.example.domain.model.ModelInfo
import com.example.domain.model.ModelType
import com.example.domain.model.RuntimeType

object ModelRegistry {
    private val models = mutableMapOf<String, ModelInfo>()

    init {
        // Register default models with full technical metadata
        registerModel(
            ModelInfo(
                id = "llama-3-8b",
                displayName = "Llama-3.1-8B-Instruct (Local / Offline)",
                provider = "Meta AI",
                type = ModelType.TEXT,
                capabilities = listOf(ModelCapability.TEXT_GENERATION, ModelCapability.CODE_EXPLANATION),
                contextWindow = 8192,
                parameterSize = "8B",
                quantization = "Q4_K_M",
                isLocal = true,
                isEnabled = true,
                isConfigured = false, // Not configured by default
                runtimeType = RuntimeType.LOCAL
            )
        )
        registerModel(
            ModelInfo(
                id = "deepseek-coder-33b",
                displayName = "DeepSeek-Coder-33B (Remote / Secure)",
                provider = "DeepSeek",
                type = ModelType.CODING,
                capabilities = listOf(ModelCapability.CODE_COMPLETION, ModelCapability.CODE_EXPLANATION, ModelCapability.FUNCTION_CALLING),
                contextWindow = 16384,
                parameterSize = "33B",
                quantization = "FP16",
                isLocal = false,
                isEnabled = true,
                isConfigured = false,
                runtimeType = RuntimeType.OPENAI_COMPATIBLE
            )
        )
        registerModel(
            ModelInfo(
                id = "qwen-coder-7b",
                displayName = "Qwen2.5-Coder-7B (Local / Optimized)",
                provider = "Alibaba Cloud",
                type = ModelType.CODING,
                capabilities = listOf(ModelCapability.CODE_COMPLETION, ModelCapability.CODE_EXPLANATION),
                contextWindow = 32768,
                parameterSize = "7B",
                quantization = "Q5_K_M",
                isLocal = true,
                isEnabled = true,
                isConfigured = false,
                runtimeType = RuntimeType.LOCAL
            )
        )
        registerModel(
            ModelInfo(
                id = "gemini-1.5-pro",
                displayName = "Gemini-1.5-Pro (Cloud / Advanced)",
                provider = "Google",
                type = ModelType.VISION,
                capabilities = listOf(ModelCapability.TEXT_GENERATION, ModelCapability.IMAGE_TO_TEXT, ModelCapability.FUNCTION_CALLING),
                contextWindow = 1048576,
                parameterSize = "N/A",
                quantization = "None",
                isLocal = false,
                isEnabled = true,
                isConfigured = false,
                runtimeType = RuntimeType.CUSTOM_HTTP
            )
        )
        registerModel(
            ModelInfo(
                id = "imagen-3",
                displayName = "Imagen-3 (Cloud / Image)",
                provider = "Google",
                type = ModelType.IMAGE_GENERATION,
                capabilities = listOf(ModelCapability.IMAGE_GENERATION),
                contextWindow = 0,
                parameterSize = "N/A",
                quantization = "None",
                isLocal = false,
                isEnabled = true,
                isConfigured = false,
                runtimeType = RuntimeType.CUSTOM_HTTP
            )
        )
        registerModel(
            ModelInfo(
                id = "flux-edit",
                displayName = "Flux-Instruct (Cloud / Edit)",
                provider = "Black Forest Labs",
                type = ModelType.IMAGE_EDITING,
                capabilities = listOf(ModelCapability.IMAGE_EDITING),
                contextWindow = 0,
                parameterSize = "N/A",
                quantization = "None",
                isLocal = false,
                isEnabled = true,
                isConfigured = false,
                runtimeType = RuntimeType.CUSTOM_HTTP
            )
        )
    }

    fun registerModel(model: ModelInfo) {
        models[model.id] = model
    }

    fun getModels(): List<ModelInfo> = models.values.toList()

    fun getModelsByType(type: ModelType): List<ModelInfo> = models.values.filter { it.type == type }

    fun getModel(id: String): ModelInfo? = models[id]
}
