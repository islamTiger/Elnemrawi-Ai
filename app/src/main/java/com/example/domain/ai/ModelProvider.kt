package com.example.domain.ai

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

// Define the model request structure
data class ModelRequest(
    val prompt: String,
    val systemInstructions: String? = null,
    val conversationContext: List<String> = emptyList(),
    val modelId: String,
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 2048,
    val timeoutMs: Long = 30000L,
    val tools: List<Map<String, Any>> = emptyList(),
    val toolChoice: String = "AUTO",
    val responseFormat: String = "TEXT",
    val metadata: Map<String, Any> = emptyMap()
)

// Define the model response structure
data class ModelResponse(
    val text: String,
    val structuredData: JSONObject? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val finishReason: String = "STOP",
    val usagePromptTokens: Int = 0,
    val usageCompletionTokens: Int = 0,
    val modelId: String,
    val providerId: String,
    val latencyMs: Long = 0L,
    val requestId: String = "",
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

// Define streaming response chunk
data class ModelResponseDelta(
    val textDelta: String,
    val isComplete: Boolean = false,
    val usagePromptTokens: Int = 0,
    val usageCompletionTokens: Int = 0,
    val latencyMs: Long = 0L,
    val error: String? = null
)

// Tool call representation
data class ToolCall(
    val toolId: String,
    val functionName: String,
    val arguments: Map<String, Any>
)

// Task plan representation (structured output example)
data class TaskPlan(
    val goal: String,
    val steps: List<String>,
    val requiredTools: List<String>,
    val dependencies: List<String>,
    val riskLevel: String,
    val requiresConfirmation: Boolean,
    val expectedVerification: String,
    val fallbackStrategy: String
)

// Interface definition for ModelProvider
interface ModelProvider {
    val providerId: String
    val isConfigured: Boolean
    
    suspend fun generate(request: ModelRequest): ModelResponse
    suspend fun generateStreaming(request: ModelRequest): Flow<ModelResponseDelta>
    suspend fun structuredOutput(request: ModelRequest, responseSchema: JSONObject): ModelResponse
    suspend fun toolCalling(request: ModelRequest): ModelResponse
    suspend fun modelCapabilities(modelId: String): List<String>
    suspend fun healthCheck(): Boolean
}
