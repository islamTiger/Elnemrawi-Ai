package com.example.domain.workspace

import com.example.domain.model.Project
import com.example.domain.model.ProjectFile
import kotlinx.coroutines.flow.StateFlow

data class Workspace(
    val project: Project,
    val rootPath: String,
    val files: List<ProjectFile>,
    val activeFile: ProjectFile?,
    val selectedFiles: List<ProjectFile> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val gitBranch: String = "main",
    val gitStatus: String = "Clean",
    val buildStatus: String = "Idle"
)

interface WorkspaceManager {
    val currentWorkspace: StateFlow<Workspace?>
    suspend fun openWorkspace(project: Project)
    suspend fun closeWorkspace()
    suspend fun setActiveFile(file: ProjectFile?)
    suspend fun selectFile(file: ProjectFile, isSelected: Boolean)
    suspend fun refreshFiles()
}
