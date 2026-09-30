package com.example.data.ai.runtime

import com.example.domain.ai.*
import com.example.domain.model.AiErrorCode
import com.example.domain.model.RuntimeType
import kotlinx.coroutines.flow.Flow

class RoutingAiRuntime(
    private val localRuntime: LocalModelRuntime,
    private val openAiRuntime: OpenAiCompatibleRuntime
) : AiRuntime {
    override val id: String = "routing_ai_runtime"

    override val isConfigured: Boolean
        get() = localRuntime.isConfigured || openAiRuntime.isConfigured

    override suspend fun generate(request: AiRuntimeRequest): AiRuntimeResponse {
        val runtime = resolveRuntime(request.model.runtimeType)
        return runtime.generate(request)
    }

    override suspend fun generateStream(request: AiRuntimeRequest): Flow<AiRuntimeStreamEvent> {
        val runtime = resolveRuntime(request.model.runtimeType)
        return runtime.generateStream(request)
    }

    private fun resolveRuntime(type: RuntimeType): AiRuntime {
        return when (type) {
            RuntimeType.LOCAL -> localRuntime
            RuntimeType.OPENAI_COMPATIBLE -> openAiRuntime
            else -> throw AiRuntimeException(
                code = AiErrorCode.NOT_CONFIGURED,
                message = "The selected runtime type ($type) is unconfigured or pending credentials in settings."
            )
        }
    }
}
