package com.example.domain.ai

object ModelProviderRegistry {
    private val providers = mutableMapOf<String, ModelProvider>()

    fun registerProvider(provider: ModelProvider) {
        providers[provider.providerId] = provider
    }

    fun getProvider(providerId: String): ModelProvider? {
        return providers[providerId]
    }

    fun getProviders(): List<ModelProvider> {
        return providers.values.toList()
    }

    fun clear() {
        providers.clear()
    }
}
