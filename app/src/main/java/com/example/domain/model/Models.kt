package com.example.domain.model

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val localStatus: Boolean = true,
    val gitHubStatus: Boolean = false,
    val lastModified: String = "Just now",
    val language: String,
    val description: String,
    val repositoryUrl: String? = null
)

data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val content: String = "",
    val children: List<ProjectFile> = emptyList()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isCodeBlock: Boolean = false,
    val language: String? = null
)

enum class StepStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

enum class StepType {
    REQUEST, PLAN, READ_PROJECT, MODIFY_FILES, BUILD_TEST, FIX_ERRORS, SHOW_CHANGES, COMMIT,
    DEVICE_CHECK_PERMISSIONS, DEVICE_LAUNCH_APP, DEVICE_INSPECT_UI, DEVICE_INTERACT, DEVICE_CAPTURE_SCREEN, DEVICE_FILE_OPERATION
}

data class AgentStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: StepType,
    val status: StepStatus = StepStatus.PENDING
)

data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val status: StepStatus = StepStatus.PENDING,
    val steps: List<AgentStep> = emptyList(),
    val logs: String = "",
    val filesChanged: List<String> = emptyList()
)
