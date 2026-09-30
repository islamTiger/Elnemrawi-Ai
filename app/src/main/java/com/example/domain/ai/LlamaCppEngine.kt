package com.example.domain.ai

import kotlinx.coroutines.flow.Flow

enum class LlamaCppEngineStatus {
    NOT_CONFIGURED,
    NATIVE_ENGINE_UNAVAILABLE,
    READY,
    LOADING,
    LOADED,
    GENERATING,
    ERROR
}

interface LlamaCppEngine {
    val status: LlamaCppEngineStatus
    fun initialize(): Boolean
    fun loadModel(modelPath: String): Boolean
    fun generate(prompt: String): String
    fun startStreaming(prompt: String): Flow<String>
    fun cancel()
    fun unload()
}

class LlamaCppEngineImpl : LlamaCppEngine {
    override var status: LlamaCppEngineStatus = LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE
        private set

    override fun initialize(): Boolean {
        // Native JNI libs are not physically packaged in this Android build container,
        // so we honestly report NATIVE_ENGINE_UNAVAILABLE.
        status = LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE
        return false
    }

    override fun loadModel(modelPath: String): Boolean {
        status = LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE
        return false
    }

    override fun generate(prompt: String): String {
        throw IllegalStateException("llama.cpp native inference engine is unavailable.")
    }

    override fun startStreaming(prompt: String): Flow<String> {
        throw IllegalStateException("llama.cpp native inference engine is unavailable.")
    }

    override fun cancel() {
        // No-op since native engine is offline
    }

    override fun unload() {
        // No-op
    }
}
