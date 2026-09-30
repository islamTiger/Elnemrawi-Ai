package com.example.data.ai.runtime

import com.example.core.registry.ModelRegistry
import com.example.domain.ai.ModelCategory
import com.example.domain.ai.ModelSelector
import com.example.domain.model.ModelCapability
import com.example.domain.model.ModelInfo
import com.example.domain.model.ModelType

class ModelSelectorImpl : ModelSelector {
    override fun selectModel(category: ModelCategory): ModelInfo? {
        val models = ModelRegistry.getModels()
        return when (category) {
            ModelCategory.GENERAL_TEXT -> {
                models.find { it.type == ModelType.TEXT && it.capabilities.contains(ModelCapability.TEXT_GENERATION) && it.isEnabled }
                    ?: models.firstOrNull { it.isEnabled }
            }
            ModelCategory.CODING -> {
                models.find { it.type == ModelType.CODING && it.capabilities.contains(ModelCapability.CODE_COMPLETION) && it.isEnabled }
                    ?: models.find { it.capabilities.contains(ModelCapability.CODE_EXPLANATION) && it.isEnabled }
            }
            ModelCategory.VISION -> {
                models.find { it.type == ModelType.VISION && it.capabilities.contains(ModelCapability.IMAGE_TO_TEXT) && it.isEnabled }
            }
            ModelCategory.IMAGE_GENERATION -> {
                models.find { it.type == ModelType.IMAGE_GENERATION && it.capabilities.contains(ModelCapability.IMAGE_GENERATION) && it.isEnabled }
            }
            ModelCategory.IMAGE_EDITING -> {
                models.find { it.type == ModelType.IMAGE_EDITING && it.capabilities.contains(ModelCapability.IMAGE_EDITING) && it.isEnabled }
            }
        }
    }
}
