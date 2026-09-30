package com.example.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.AgentTask
import com.example.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {
    private val agentEngine = ServiceLocator.agentEngine
    private val projectRepository = ServiceLocator.projectRepository

    val currentTask: StateFlow<AgentTask?> = agentEngine.currentTask
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val projects: StateFlow<List<Project>> = projectRepository.getProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject = _selectedProject.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting = _isExecuting.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.getProjects().collect { list ->
                if (_selectedProject.value == null && list.isNotEmpty()) {
                    _selectedProject.value = list.first()
                }
            }
        }
    }

    fun selectProject(project: Project) {
        _selectedProject.value = project
    }

    fun submitTask(prompt: String) {
        if (prompt.trim().isEmpty()) return

        viewModelScope.launch {
            _isExecuting.value = true
            // Dynamic workspace activation in context
            _selectedProject.value?.let {
                ServiceLocator.workspaceManager.openWorkspace(it)
            }
            agentEngine.startTask(prompt)
            _isExecuting.value = false
        }
    }

    fun approveStep() {
        if (_isExecuting.value) return
        viewModelScope.launch {
            _isExecuting.value = true
            agentEngine.approveStep()
            _isExecuting.value = false
        }
    }

    fun cancelTask() {
        viewModelScope.launch {
            agentEngine.cancelTask()
            ServiceLocator.workspaceManager.closeWorkspace()
        }
    }
}
