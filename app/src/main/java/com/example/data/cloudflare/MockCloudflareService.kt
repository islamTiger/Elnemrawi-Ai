package com.example.data.cloudflare

import com.example.domain.repository.CloudflareService

class MockCloudflareService : CloudflareService {
    // TODO: Connect credentials and use Cloudflare Worker/KV API routes
    override val isConfigured: Boolean = false

    override suspend fun runWorker(workerName: String, params: Map<String, Any>): String {
        if (!isConfigured) {
            throw IllegalStateException("Cloudflare integrations require Account ID and API Token configured in Secrets.")
        }
        return "Stub response"
    }

    override suspend fun cachePut(key: String, value: String) {
        if (!isConfigured) {
            throw IllegalStateException("Cloudflare is not configured.")
        }
    }

    override suspend fun cacheGet(key: String): String? {
        if (!isConfigured) {
            throw IllegalStateException("Cloudflare is not configured.")
        }
        return null
    }
}
