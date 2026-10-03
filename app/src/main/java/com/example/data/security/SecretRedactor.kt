package com.example.data.security

object SecretRedactor {
    private val patterns = listOf(
        Regex("(?i)(password|passwd|pwd|secret|token|api_key|apikey|private_key|key|auth|credential)\\s*[:=]\\s*\"?[a-zA-Z0-9_\\-+=/]{8,}\"?"),
        Regex("ghp_[a-zA-Z0-9]{36,255}"), // GitHub personal access token
        Regex("sbp_[a-zA-Z0-9]{36,255}"), // Supabase project API key
        Regex("eyJhbGciOi[a-zA-Z0-9_\\-+=/.]+"), // JWT/Supabase keys
        Regex("(?i)(service_role|service-role|anon_key|anon-key)\\s*[:=]?\\s*\"?[a-zA-Z0-9_\\-+=/]{8,}\"?")
    )
    
    fun redact(message: String): String {
        var redacted = message
        for (pattern in patterns) {
            redacted = pattern.replace(redacted) { matchResult ->
                val text = matchResult.value
                val prefix = text.takeWhile { it != ':' && it != '=' }
                if (prefix.isNotEmpty() && prefix.length < text.length) {
                    val operator = text[prefix.length]
                    "$prefix$operator[REDACTED_SECRET]"
                } else {
                    "[REDACTED_SECRET]"
                }
            }
        }
        return redacted
    }
}
