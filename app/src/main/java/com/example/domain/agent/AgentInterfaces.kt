package com.example.domain.agent

import com.example.domain.model.AgentStep
import com.example.domain.model.AgentTask
import com.example.domain.model.ChatMessage
import com.example.domain.workspace.Workspace
import com.example.domain.build.BuildResult
import kotlinx.coroutines.flow.StateFlow

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    DENIED,
    NOT_REQUIRED
}

data class ApprovalRequest(
    val id: String,
    val toolId: String,
    val description: String,
    val status: ApprovalStatus = ApprovalStatus.PENDING
)

data class AgentContext(
    val currentRequest: String,
    val workspace: Workspace?,
    val selectedModelId: String?,
    val messages: List<ChatMessage> = emptyList(),
    val executionPlan: List<AgentStep> = emptyList(),
    val executedTools: List<String> = emptyList(),
    val filesChanged: List<String> = emptyList(),
    val buildResults: List<BuildResult> = emptyList(),
    val errors: List<String> = emptyList(),
    val approvals: Map<String, ApprovalStatus> = emptyMap()
)

data class AgentResult(
    val success: Boolean,
    val message: String,
    val filesChanged: List<String> = emptyList(),
    val logs: String = ""
)

interface AgentPlanner {
    suspend fun planTask(prompt: String, context: AgentContext): List<AgentStep>
}

interface AgentExecutor {
    suspend fun executeStep(step: AgentStep, context: AgentContext): AgentContext
}

interface ApprovalManager {
    fun requestApproval(toolId: String, description: String): ApprovalStatus
    fun updateApproval(id: String, status: ApprovalStatus)
}

interface AgentEngine {
    val currentTask: StateFlow<AgentTask?>
    val agentContext: StateFlow<AgentContext?>
    suspend fun startTask(prompt: String)
    suspend fun cancelTask()
    suspend fun approveStep()
}
