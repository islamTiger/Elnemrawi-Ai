package com.example.data.workspace

import com.example.domain.model.Project
import com.example.domain.model.ProjectFile
import com.example.domain.repository.ProjectFileService
import com.example.domain.workspace.Workspace
import com.example.domain.workspace.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceManagerImpl(
    private val fileService: ProjectFileService
) : WorkspaceManager {
    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    override val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    override suspend fun openWorkspace(project: Project) {
        val files = fileService.getProjectFiles(project)
        _currentWorkspace.value = Workspace(
            project = project,
            rootPath = project.repositoryUrl ?: "local:///${project.name}",
            files = files,
            activeFile = null,
            gitBranch = "main",
            gitStatus = if (project.gitHubStatus) "Synced" else "Local",
            buildStatus = "Idle"
        )
    }

    override suspend fun closeWorkspace() {
        _currentWorkspace.value = null
    }

    override suspend fun setActiveFile(file: ProjectFile?) {
        val current = _currentWorkspace.value ?: return
        _currentWorkspace.value = current.copy(activeFile = file)
    }

    override suspend fun selectFile(file: ProjectFile, isSelected: Boolean) {
        val current = _currentWorkspace.value ?: return
        val selected = current.selectedFiles.toMutableList()
        if (isSelected) {
            if (!selected.any { it.path == file.path }) {
                selected.add(file)
            }
        } else {
            selected.removeAll { it.path == file.path }
        }
        _currentWorkspace.value = current.copy(selectedFiles = selected)
    }

    override suspend fun refreshFiles() {
        val current = _currentWorkspace.value ?: return
        val files = fileService.getProjectFiles(current.project)
        _currentWorkspace.value = current.copy(files = files)
    }
}
