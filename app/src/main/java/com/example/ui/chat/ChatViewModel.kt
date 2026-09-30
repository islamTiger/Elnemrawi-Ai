package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.core.registry.ModelRegistry
import com.example.domain.model.*
import com.example.domain.workspace.Workspace
import com.example.domain.ai.ModelCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val aiProvider = ServiceLocator.aiProvider
    private val agentEngine = ServiceLocator.agentEngine
    private val workspaceManager = ServiceLocator.workspaceManager

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val availableModelSelections = listOf("Auto", "General", "Coding", "Vision", "Image Generation", "Image Editing", "Local GGUF")

    private val _selectedModel = MutableStateFlow("Auto")
    val selectedModel = _selectedModel.asStateFlow()

    // Workspace & Agent State Integration
    val currentWorkspace: StateFlow<Workspace?> = workspaceManager.currentWorkspace
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentAgentTask: StateFlow<AgentTask?> = agentEngine.currentTask
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val agentContext = agentEngine.agentContext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    private fun isAgentRequest(prompt: String): Boolean {
        val keywords = listOf(
            "build", "modify", "change", "create", "git", "commit", "solve", "rebuild", "compile",
            "run", "project", "workspace", "abni", "adell", "adter", "نظام", "بناء", "تطبيق", "عدل",
            "ابني", "اربط", "مشروع", "حل", "تعديل"
        )
        return keywords.any { prompt.contains(it, ignoreCase = true) }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            // Append user message
            val userMsg = ChatMessage(text = prompt, isUser = true)
            _messages.value = _messages.value + userMsg
            _inputText.value = ""
            _isLoading.value = true

            // Check if local model runtime is requested but not configured
            val isConfigured = ServiceLocator.routingAiRuntime.isConfigured
            val selection = _selectedModel.value
            if (!isConfigured && (selection == "Local GGUF" || selection == "Auto" && isAgentRequest(prompt))) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Local model runtime is not configured.",
                    isUser = false
                )
                _isLoading.value = false
                return@launch
            }

            // Automatic Routing: Activate AgentEngine if task requires multiple steps
            if (isAgentRequest(prompt)) {
                // Ensure default project is opened in workspace if empty
                if (workspaceManager.currentWorkspace.value == null) {
                    val projects = ServiceLocator.projectRepository.getProjects().stateIn(viewModelScope).value
                    if (projects.isNotEmpty()) {
                        workspaceManager.openWorkspace(projects.first())
                    }
                }
                agentEngine.startTask(prompt)
                _isLoading.value = false
                return@launch
            }

            // Standard Chat/QA generation Flow
            val aiMsgId = UUID.randomUUID().toString()
            val initialAiMsg = ChatMessage(
                id = aiMsgId,
                text = "",
                isUser = false,
                isCodeBlock = prompt.contains("code", ignoreCase = true) || prompt.contains("optimize", ignoreCase = true)
            )
            _messages.value = _messages.value + initialAiMsg

            // Resolve the model display id
            val modelId = when (selection) {
                "General" -> "llama-3-8b"
                "Coding" -> "qwen-coder-7b"
                "Vision" -> "gemini-1.5-pro"
                "Image Generation" -> "imagen-3"
                "Image Editing" -> "flux-edit"
                "Local GGUF" -> "selected-local-gguf"
                else -> "llama-3-8b" // Auto/Default Fallback
            }

            try {
                aiProvider.generateResponse(prompt, modelId).collect { chunk ->
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == aiMsgId) {
                            msg.copy(text = chunk)
                        } else {
                            msg
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(text = "Error: ${e.message ?: "Could not reach AI model."}")
                    } else {
                        msg
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveAgentStep() {
        viewModelScope.launch {
            _isLoading.value = true
            agentEngine.approveStep()
            _isLoading.value = false
        }
    }

    fun cancelAgentTask() {
        viewModelScope.launch {
            agentEngine.cancelTask()
            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}
