package com.example.domain.ai

import com.example.domain.model.ModelInfo

enum class ModelCategory {
    GENERAL_TEXT,
    CODING,
    VISION,
    IMAGE_GENERATION,
    IMAGE_EDITING
}

interface ModelSelector {
    fun selectModel(category: ModelCategory): ModelInfo?
}
