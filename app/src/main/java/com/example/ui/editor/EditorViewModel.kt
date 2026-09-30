package com.example.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Project
import com.example.domain.model.ProjectFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel : ViewModel() {
    private val projectRepository = ServiceLocator.projectRepository
    private val fileService = ServiceLocator.projectFileService

    private val _project = MutableStateFlow<Project?>(null)
    val project = _project.asStateFlow()

    private val _files = MutableStateFlow<List<ProjectFile>>(emptyList())
    val files = _files.asStateFlow()

    private val _selectedFile = MutableStateFlow<ProjectFile?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent = _fileContent.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val proj = projectRepository.getProjectById(projectId)
            _project.value = proj
            if (proj != null) {
                refreshFileList()
                _selectedFile.value = null
                _fileContent.value = ""
                _hasUnsavedChanges.value = false
            }
        }
    }

    fun selectFile(file: ProjectFile) {
        if (file.isDirectory) return
        viewModelScope.launch {
            val proj = _project.value ?: return@launch
            val content = fileService.getFileContent(proj, file.path)
            _selectedFile.value = file
            _fileContent.value = content
            _hasUnsavedChanges.value = false
        }
    }

    fun updateFileContent(content: String) {
        _fileContent.value = content
        _hasUnsavedChanges.value = true
    }

    fun saveFile() {
        val proj = _project.value ?: return
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            fileService.saveFileContent(proj, file.path, _fileContent.value)
            _hasUnsavedChanges.value = false
            refreshFileList()
        }
    }

    fun createNewFile(parentPath: String, name: String, isDirectory: Boolean) {
        val proj = _project.value ?: return
        viewModelScope.launch {
            fileService.createNewFile(proj, parentPath, name, isDirectory)
            refreshFileList()
        }
    }

    fun deleteFile(path: String) {
        val proj = _project.value ?: return
        viewModelScope.launch {
            fileService.deleteFile(proj, path)
            if (_selectedFile.value?.path == path) {
                _selectedFile.value = null
                _fileContent.value = ""
                _hasUnsavedChanges.value = false
            }
            refreshFileList()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private suspend fun refreshFileList() {
        val proj = _project.value ?: return
        _files.value = fileService.getProjectFiles(proj)
    }
}
