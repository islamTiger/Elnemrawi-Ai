package com.example.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel : ViewModel() {
    private val projectRepository = ServiceLocator.projectRepository

    val projects: StateFlow<List<Project>> = projectRepository.getProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject = _selectedProject.asStateFlow()

    fun selectProject(project: Project) {
        _selectedProject.value = project
    }

    fun createProject(name: String, language: String, description: String) {
        viewModelScope.launch {
            val newProj = projectRepository.createProject(name, language, description)
            _selectedProject.value = newProj
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
            if (_selectedProject.value?.id == id) {
                _selectedProject.value = null
            }
        }
    }
}
