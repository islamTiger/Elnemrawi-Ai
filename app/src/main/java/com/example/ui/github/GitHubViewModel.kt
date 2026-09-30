package com.example.ui.github

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

class GitHubViewModel : ViewModel() {
    private val gitService = ServiceLocator.gitService
    private val projectRepository = ServiceLocator.projectRepository

    val isConnected: StateFlow<Boolean> = gitService.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val accountName: StateFlow<String?> = gitService.connectedAccountName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val remoteRepositories: StateFlow<List<Project>> = gitService.getRepositories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _tokenInput = MutableStateFlow("")
    val tokenInput = _tokenInput.asStateFlow()

    private val _cloneUrlInput = MutableStateFlow("")
    val cloneUrlInput = _cloneUrlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun updateTokenInput(token: String) {
        _tokenInput.value = token
    }

    fun updateCloneUrlInput(url: String) {
        _cloneUrlInput.value = url
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun connectGitHub() {
        val token = _tokenInput.value.trim()
        if (token.isEmpty()) {
            _statusMessage.value = "Please enter a GitHub Personal Access Token."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = gitService.connectAccount(token)
            _isLoading.value = false
            if (success) {
                _tokenInput.value = ""
                _statusMessage.value = "GitHub successfully connected!"
            } else {
                _statusMessage.value = "Invalid token format. Token must start with 'ghp_' for mock validation."
            }
        }
    }

    fun disconnectGitHub() {
        viewModelScope.launch {
            gitService.disconnectAccount()
            _statusMessage.value = "GitHub disconnected."
        }
    }

    fun cloneRepo(url: String) {
        if (url.trim().isEmpty()) {
            _statusMessage.value = "Please specify a repository URL to clone."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val newProject = gitService.cloneRepository(url)
            // Add cloned project to local project list!
            projectRepository.createProject(
                name = newProject.name,
                language = newProject.language,
                description = newProject.description
            )
            _cloneUrlInput.value = ""
            _isLoading.value = false
            _statusMessage.value = "Successfully cloned '${newProject.name}' into projects workspace!"
        }
    }
}
