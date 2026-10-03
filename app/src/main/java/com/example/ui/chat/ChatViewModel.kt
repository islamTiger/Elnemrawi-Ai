package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.*
import com.example.domain.workspace.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingHighImpactAction(
    val prompt: String,
    val warningMessage: String
)

class ChatViewModel : ViewModel() {
    private val aiProvider = ServiceLocator.aiProvider
    private val agentEngine = ServiceLocator.agentEngine
    private val workspaceManager = ServiceLocator.workspaceManager
    private val capabilityDetector = ServiceLocator.deviceCapabilityDetector

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingHighImpactAction?>(null)
    val pendingConfirmation = _pendingConfirmation.asStateFlow()

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

    fun isDeviceRequest(prompt: String): Boolean {
        val keywords = listOf(
            "افتح", "launch", "open", "screenshot", "شاشة", "سكرين شوت", "لقطة", "انقر", "اضغط",
            "tap", "click", "اكتب", "type", "ابحث", "scroll", "ارجع", "back", "home", "هات الملف",
            "ملف", "شارك", "share", "احفظ", "save", "تطبيق", "apps", "device"
        )
        return keywords.any { prompt.contains(it, ignoreCase = true) }
    }

    private fun isWorkspaceAgentRequest(prompt: String): Boolean {
        val keywords = listOf(
            "build", "modify", "change", "create", "git", "commit", "solve", "rebuild", "compile",
            "run", "project", "workspace", "abni", "adell", "adter", "نظام", "بناء", "عدل",
            "ابني", "اربط", "مشروع", "حل", "تعديل"
        )
        return keywords.any { prompt.contains(it, ignoreCase = true) }
    }

    private fun isHighImpactRequest(prompt: String): String? {
        return when {
            prompt.contains("حذف", ignoreCase = true) || prompt.contains("delete", ignoreCase = true) || prompt.contains("مسح", ignoreCase = true) ->
                "سيتم حذف الملفات المحددة الآن."
            prompt.contains("إرسال", ignoreCase = true) || prompt.contains("send", ignoreCase = true) ->
                "سيتم إرسال هذه الرسالة الآن."
            prompt.contains("نشر", ignoreCase = true) || prompt.contains("post", ignoreCase = true) ->
                "سيتم نشر المحتوى الآن."
            prompt.contains("شراء", ignoreCase = true) || prompt.contains("buy", ignoreCase = true) || prompt.contains("purchase", ignoreCase = true) ->
                "سيتم تأكيد عملية الشراء الآن."
            prompt.contains("تغيير الإعدادات", ignoreCase = true) || prompt.contains("change settings", ignoreCase = true) ->
                "سيتم تغيير إعدادات الجهاز المهمة الآن."
            else -> null
        }
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty() || _isLoading.value) return

        // Check for high-impact confirmation requirement
        val highImpactWarning = isHighImpactRequest(prompt)
        if (highImpactWarning != null) {
            val userMsg = ChatMessage(text = prompt, isUser = true)
            _messages.value = _messages.value + userMsg
            _inputText.value = ""
            _pendingConfirmation.value = PendingHighImpactAction(prompt, highImpactWarning)
            return
        }

        executePrompt(prompt)
    }

    fun confirmHighImpactAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        executePrompt(pending.prompt)
    }

    fun cancelHighImpactAction() {
        _pendingConfirmation.value = null
        _messages.value = _messages.value + ChatMessage(
            text = "تم إلغاء الإجراء بناءً على طلبك.",
            isUser = false
        )
    }

    private fun executePrompt(prompt: String) {
        viewModelScope.launch {
            // Append user message if not already appended
            if (_messages.value.lastOrNull()?.text != prompt || _messages.value.lastOrNull()?.isUser != true) {
                val userMsg = ChatMessage(text = prompt, isUser = true)
                _messages.value = _messages.value + userMsg
            }
            _inputText.value = ""
            _isLoading.value = true

            val isDev = isDeviceRequest(prompt)
            val isWork = isWorkspaceAgentRequest(prompt)

            // Device Agent Execution Flow
            if (isDev) {
                // If it is a device action, launch device agent engine task
                agentEngine.startTask(prompt)
                _isLoading.value = false
                return@launch
            }

            // Check if local model runtime is requested for workspace coding tasks
            val isConfigured = ServiceLocator.routingAiRuntime.isConfigured
            val selection = _selectedModel.value
            if (!isConfigured && (selection == "Local GGUF" || selection == "Auto" && isWork)) {
                _messages.value = _messages.value + ChatMessage(
                    text = "Local model runtime is not configured.",
                    isUser = false
                )
                _isLoading.value = false
                return@launch
            }

            // Automatic Routing: Activate AgentEngine if task requires multiple workspace steps
            if (isWork) {
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

            val modelId = when (selection) {
                "General" -> "llama-3-8b"
                "Coding" -> "qwen-coder-7b"
                "Vision" -> "gemini-1.5-pro"
                "Image Generation" -> "imagen-3"
                "Image Editing" -> "flux-edit"
                "Local GGUF" -> "selected-local-gguf"
                else -> "llama-3-8b"
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
        _pendingConfirmation.value = null
    }
}
