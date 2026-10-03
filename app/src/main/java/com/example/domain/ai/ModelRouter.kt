package com.example.domain.ai

import com.example.core.di.ServiceLocator
import com.example.data.security.SecureCredentialVault
import com.example.data.security.SecretRedactor
import kotlinx.coroutines.delay
import org.json.JSONObject

// Capability Classes for Model routing
enum class CapabilityClass {
    FAST,
    DEEP,
    CODING,
    VISION,
    LOCAL
}

// Task categories
enum class TaskCategory {
    SIMPLE_QUESTION,
    COMPLEX_REASONING,
    CODING,
    IMAGE_UNDERSTANDING,
    STRUCTURED_DATA,
    PLANNING,
    TOOL_USE,
    RESEARCH,
    AUTOMATION
}

// Adaptive Thinking Budget Modes
enum class ThinkingMode {
    QUICK,
    DEEP,
    EXTREME
}

// Configurable budget parameters
data class ThinkingBudget(
    val mode: ThinkingMode,
    val maxOutputTokens: Int,
    val maxToolIterations: Int,
    val maxResearchIterations: Int,
    val verificationDepth: String,
    val timeoutMs: Long
)

// Object-oriented Timeout configuration constants
object TimeoutConfig {
    val QUICK_TIMEOUT = 10000L
    val DEEP_TIMEOUT = 30000L
    val EXTREME_TIMEOUT = 120000L

    fun getTimeout(mode: ThinkingMode): Long = when (mode) {
        ThinkingMode.QUICK -> QUICK_TIMEOUT
        ThinkingMode.DEEP -> DEEP_TIMEOUT
        ThinkingMode.EXTREME -> EXTREME_TIMEOUT
    }
}

// Retry Strategy parameters
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000L,
    val backoffFactor: Double = 2.0
)

// Task Classifier
class TaskClassifier {
    fun classifyTask(prompt: String): TaskCategory {
        val p = prompt.lowercase()
        return when {
            p.contains("جدول") || p.contains("مخطط") || p.contains("schema") -> TaskCategory.STRUCTURED_DATA
            p.contains("كود") || p.contains("اكتب كلاس") || p.contains("برمج") || p.contains("code") || p.contains("kotlin") || p.contains("class") -> TaskCategory.CODING
            p.contains("صورة") || p.contains("اسكرين") || p.contains("شاشة") || p.contains("image") -> TaskCategory.IMAGE_UNDERSTANDING
            p.contains("خطط") || p.contains("plan") -> TaskCategory.PLANNING
            p.contains("ابحث") || p.contains("search") -> TaskCategory.RESEARCH
            p.contains("افتح") || p.contains("شغل") || p.contains("اضغط") -> TaskCategory.AUTOMATION
            p.contains("سوبابيز") || p.contains("جيت") -> TaskCategory.TOOL_USE
            p.contains("لماذا") || p.contains("اشرح") || p.contains("فكر") || p.contains("reason") -> TaskCategory.COMPLEX_REASONING
            else -> TaskCategory.SIMPLE_QUESTION
        }
    }

    fun recommendCapability(category: TaskCategory): CapabilityClass {
        return when (category) {
            TaskCategory.SIMPLE_QUESTION -> CapabilityClass.FAST
            TaskCategory.COMPLEX_REASONING -> CapabilityClass.DEEP
            TaskCategory.CODING -> CapabilityClass.CODING
            TaskCategory.IMAGE_UNDERSTANDING -> CapabilityClass.VISION
            TaskCategory.STRUCTURED_DATA -> CapabilityClass.FAST
            TaskCategory.PLANNING -> CapabilityClass.DEEP
            TaskCategory.TOOL_USE -> CapabilityClass.CODING
            TaskCategory.RESEARCH -> CapabilityClass.DEEP
            TaskCategory.AUTOMATION -> CapabilityClass.LOCAL
        }
    }
}

// Model Router
class ModelRouter {
    private val classifier = TaskClassifier()

    fun route(prompt: String, forceLocal: Boolean = false): RoutedModel {
        if (forceLocal) {
            return RoutedModel(CapabilityClass.LOCAL, "local-llama-3b", "local_provider")
        }

        val category = classifier.classifyTask(prompt)
        val cap = classifier.recommendCapability(category)

        return when (cap) {
            CapabilityClass.FAST -> RoutedModel(CapabilityClass.FAST, "gemini-3.5-flash", "google_provider")
            CapabilityClass.DEEP -> RoutedModel(CapabilityClass.DEEP, "gemini-3.1-pro-preview", "google_provider")
            CapabilityClass.CODING -> RoutedModel(CapabilityClass.CODING, "gemini-3.1-pro-preview", "google_provider")
            CapabilityClass.VISION -> RoutedModel(CapabilityClass.VISION, "gemini-2.5-flash-image", "google_provider")
            CapabilityClass.LOCAL -> RoutedModel(CapabilityClass.LOCAL, "local-llama-3b", "local_provider")
        }
    }

    fun getBudget(mode: ThinkingMode): ThinkingBudget {
        return when (mode) {
            ThinkingMode.QUICK -> ThinkingBudget(
                mode = mode,
                maxOutputTokens = 1024,
                maxToolIterations = 2,
                maxResearchIterations = 1,
                verificationDepth = "BASIC",
                timeoutMs = TimeoutConfig.getTimeout(mode)
            )
            ThinkingMode.DEEP -> ThinkingBudget(
                mode = mode,
                maxOutputTokens = 4096,
                maxToolIterations = 10,
                maxResearchIterations = 5,
                verificationDepth = "NORMAL",
                timeoutMs = TimeoutConfig.getTimeout(mode)
            )
            ThinkingMode.EXTREME -> ThinkingBudget(
                mode = mode,
                maxOutputTokens = 8192,
                maxToolIterations = 30,
                maxResearchIterations = 15,
                verificationDepth = "COMPREHENSIVE",
                timeoutMs = TimeoutConfig.getTimeout(mode)
            )
        }
    }

    suspend fun <T> executeWithRetry(
        policy: RetryPolicy = RetryPolicy(),
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = policy.initialDelayMs

        for (attempt in 1..policy.maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isRetryable = isExceptionRetryable(e)
                if (!isRetryable || attempt == policy.maxAttempts) {
                    throw e
                }
                delay(delayMs)
                delayMs = (delayMs * policy.backoffFactor).toLong()
            }
        }
        throw lastException ?: IllegalStateException("Retry failed.")
    }

    private fun isExceptionRetryable(e: Exception): Boolean {
        val msg = e.message ?: ""
        return msg.contains("timeout", ignoreCase = true) || 
               msg.contains("rate limit", ignoreCase = true) || 
               msg.contains("server error", ignoreCase = true) || 
               msg.contains("429") || 
               msg.contains("500") || 
               msg.contains("503")
    }

    fun handleFallback(
        originalProviderId: String,
        requiredCapability: CapabilityClass,
        securityPolicy: SecurityPolicy
    ): RoutedModel {
        // Strict fallback security verification
        if (securityPolicy.restrictDataTransfers) {
            throw SecurityException("SECURITY_POLICY_VIOLATION - Data transfer restriction blocks fallback.")
        }
        
        return if (originalProviderId == "google_provider") {
            RoutedModel(requiredCapability, "local-llama-3b", "local_provider")
        } else {
            RoutedModel(requiredCapability, "gemini-3.5-flash", "google_provider")
        }
    }
}

data class RoutedModel(
    val capabilityClass: CapabilityClass,
    val modelId: String,
    val providerId: String
)

data class SecurityPolicy(
    val restrictDataTransfers: Boolean = false,
    val allowedProviders: List<String> = listOf("google_provider", "local_provider")
)
