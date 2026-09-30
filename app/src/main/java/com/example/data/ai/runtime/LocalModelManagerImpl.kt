package com.example.data.ai.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.core.registry.ModelRegistry
import com.example.domain.ai.*
import com.example.domain.model.ModelCapability
import com.example.domain.model.ModelInfo
import com.example.domain.model.ModelType
import com.example.domain.model.RuntimeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesLocalModelRepository(private val context: Context) : LocalModelRepository {
    private val prefs = context.getSharedPreferences("local_model_prefs", Context.MODE_PRIVATE)

    override fun getLocalModelConfig(): LocalModelConfig? {
        val name = prefs.getString("name", null) ?: return null
        val path = prefs.getString("path", null) ?: return null
        val format = prefs.getString("format", "GGUF") ?: "GGUF"
        val size = prefs.getLong("size", 0L)
        val quantization = prefs.getString("quantization", "UNKNOWN") ?: "UNKNOWN"
        val contextLength = prefs.getInt("contextLength", 0)
        val isEnabled = prefs.getBoolean("isEnabled", true)
        val parameterCount = prefs.getString("parameterCount", "UNKNOWN") ?: "UNKNOWN"
        return LocalModelConfig(name, path, format, size, quantization, contextLength, isEnabled, parameterCount)
    }

    override fun saveLocalModelConfig(config: LocalModelConfig?) {
        if (config == null) {
            prefs.edit().clear().apply()
        } else {
            prefs.edit()
                .putString("name", config.name)
                .putString("path", config.path)
                .putString("format", config.format)
                .putLong("size", config.size)
                .putString("quantization", config.quantization)
                .putInt("contextLength", config.contextLength)
                .putBoolean("isEnabled", config.isEnabled)
                .putString("parameterCount", config.parameterCount)
                .apply()
        }
    }
}

class LocalModelManagerImpl(
    private val context: Context,
    private val repository: LocalModelRepository
) : LocalModelManager {
    private val _modelState = MutableStateFlow(LocalModelState.NO_MODEL)
    override val modelState: StateFlow<LocalModelState> = _modelState.asStateFlow()

    private val _currentConfig = MutableStateFlow<LocalModelConfig?>(null)
    override val currentConfig: StateFlow<LocalModelConfig?> = _currentConfig.asStateFlow()

    private val reader = GgufMetadataReader()

    init {
        // Load initial state from repository
        val saved = repository.getLocalModelConfig()
        if (saved != null) {
            _currentConfig.value = saved
            val uri = Uri.parse(saved.path)
            try {
                reader.readMetadata(context, uri)
                _modelState.value = LocalModelState.READY
                registerInRegistry(saved)
            } catch (e: Exception) {
                _modelState.value = LocalModelState.ERROR
            }
        } else {
            _modelState.value = LocalModelState.NO_MODEL
        }
    }

    override suspend fun selectModel(uriString: String, name: String, size: Long) {
        _modelState.value = LocalModelState.SELECTING
        _modelState.value = LocalModelState.VALIDATING
        try {
            val uri = Uri.parse(uriString)
            
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // SAF uri persist error ignored in unit test environments
            }

            val meta = reader.readMetadata(context, uri)
            
            val config = LocalModelConfig(
                name = name,
                path = uriString,
                format = "GGUF",
                size = size,
                quantization = meta.quantization,
                contextLength = 2048,
                isEnabled = true,
                parameterCount = meta.parameterCount
            )
            
            repository.saveLocalModelConfig(config)
            _currentConfig.value = config
            _modelState.value = LocalModelState.READY
            
            registerInRegistry(config)
        } catch (e: Exception) {
            _modelState.value = LocalModelState.INVALID
            throw e
        }
    }

    override suspend fun removeModel() {
        repository.saveLocalModelConfig(null)
        _currentConfig.value = null
        _modelState.value = LocalModelState.NO_MODEL
        
        // Mark selected-local-gguf as unconfigured
        val registeredModels = ModelRegistry.getModels()
        registeredModels.forEach { model ->
            if (model.id == "selected-local-gguf") {
                ModelRegistry.registerModel(
                    model.copy(
                        displayName = "No Local Model Selected",
                        isConfigured = false,
                        isEnabled = false
                    )
                )
            }
        }
    }

    override suspend fun validateActiveModel(): Boolean {
        val config = _currentConfig.value ?: return false
        _modelState.value = LocalModelState.VALIDATING
        return try {
            val uri = Uri.parse(config.path)
            reader.readMetadata(context, uri)
            _modelState.value = LocalModelState.READY
            true
        } catch (e: Exception) {
            _modelState.value = LocalModelState.ERROR
            false
        }
    }

    private fun registerInRegistry(config: LocalModelConfig) {
        val registeredModel = ModelInfo(
            id = "selected-local-gguf",
            displayName = config.name,
            provider = "Local User",
            type = ModelType.CODING,
            capabilities = listOf(ModelCapability.TEXT_GENERATION, ModelCapability.CODE_EXPLANATION, ModelCapability.CODE_COMPLETION),
            contextWindow = config.contextLength,
            parameterSize = "UNKNOWN",
            quantization = config.quantization,
            isLocal = true,
            isEnabled = true,
            isConfigured = true,
            runtimeType = RuntimeType.LOCAL
        )
        ModelRegistry.registerModel(registeredModel)
    }
}
