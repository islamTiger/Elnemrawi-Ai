package com.example.data.supabase

import com.example.domain.repository.SupabaseService

class MockSupabaseService : SupabaseService {
    // TODO: Connect credentials and use actual supabase-kt client libraries
    override val isConfigured: Boolean = false

    override suspend fun syncDatabase() {
        if (!isConfigured) {
            throw IllegalStateException("Supabase is not configured. Please supply your API_KEY and SUPABASE_URL in the Secrets panel.")
        }
    }

    override suspend fun getAuthSession() {
        if (!isConfigured) {
            throw IllegalStateException("Supabase is not configured.")
        }
    }

    override suspend fun uploadToStorage(bucket: String, path: String, bytes: ByteArray) {
        if (!isConfigured) {
            throw IllegalStateException("Supabase is not configured.")
        }
    }

    override suspend fun invokeEdgeFunction(functionName: String, payload: Map<String, Any>): String {
        if (!isConfigured) {
            throw IllegalStateException("Supabase is not configured.")
        }
        return "Stub response"
    }
}
