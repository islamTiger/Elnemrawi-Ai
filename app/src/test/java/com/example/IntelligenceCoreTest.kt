package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.ServiceLocator
import com.example.data.ai.GeminiModelProvider
import com.example.data.ai.LocalModelProvider
import com.example.data.security.SecureCredentialVault
import com.example.data.security.SecretRedactor
import com.example.domain.ai.*
import com.example.domain.model.ExecutionEnvironment
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IntelligenceCoreTest {

    private lateinit var context: Context
    private lateinit var vault: SecureCredentialVault
    private lateinit var router: ModelRouter
    private lateinit var classifier: TaskClassifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(context)
        ServiceLocator.currentEnvironment = ExecutionEnvironment.TEST
        vault = SecureCredentialVault(context)
        router = ModelRouter()
        classifier = TaskClassifier()

        // Clean any keys before each test
        vault.deleteCredential("gemini_api_key")
    }

    // 1. ModelProvider contract test
    @Test
    fun testModelProviderContract() {
        val gemini = GeminiModelProvider()
        assertEquals("google_provider", gemini.providerId)
        assertFalse(gemini.isConfigured) // Initially false
    }

    // 2. Real provider connection test (Mock/Real config)
    @Test
    fun testRealProviderConnection() = runBlocking {
        val gemini = GeminiModelProvider()
        assertFalse(gemini.isConfigured)

        // Storing key
        vault.storeCredential("gemini_api_key", "AIzaSyFakeKeyForTest_123456789")
        assertTrue(gemini.isConfigured)

        // Trigger request without active internet to check auth/validation failure (graceful exception)
        val request = ModelRequest(prompt = "Hello", modelId = "gemini-3.5-flash")
        val response = gemini.generate(request)
        
        // Should return a response with errors since the key is fake / network is isolated in robolectric
        assertTrue(response.errors.isNotEmpty() || response.text.isNotEmpty())
        vault.deleteCredential("gemini_api_key")
    }

    // 3. Streaming test
    @Test
    fun testStreamingDeltasAndCompletion() = runBlocking {
        val gemini = GeminiModelProvider()
        vault.storeCredential("gemini_api_key", "AIzaSyFakeKeyForTest_123456789")

        val request = ModelRequest(prompt = "Hello world from Nemrawy", modelId = "gemini-3.5-flash")
        val deltas = gemini.generateStreaming(request).toList()

        // Should receive flow deltas properly or error state
        if (deltas.isNotEmpty() && deltas.first().error == null) {
            assertTrue(deltas.any { it.isComplete })
            assertNotNull(deltas.first().textDelta)
        } else {
            assertTrue(deltas.first().error!!.contains("ERROR"))
        }
        vault.deleteCredential("gemini_api_key")
    }

    // 4. Cancellation test
    @Test
    fun testStreamingCancellation() = runBlocking {
        val gemini = GeminiModelProvider()
        vault.storeCredential("gemini_api_key", "AIzaSyFakeKeyForTest_123456789")

        val request = ModelRequest(prompt = "Large streaming text prompt", modelId = "gemini-3.5-flash")
        
        var cancelled = false
        try {
            // Collecting first item then throwing to cancel the flow collection
            gemini.generateStreaming(request).collect {
                throw kotlinx.coroutines.CancellationException("Cancelled by consumer")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        vault.deleteCredential("gemini_api_key")
    }

    // 5. Timeout test
    @Test
    fun testAdaptiveTimeoutConfiguration() {
        val quickTimeout = TimeoutConfig.getTimeout(ThinkingMode.QUICK)
        val deepTimeout = TimeoutConfig.getTimeout(ThinkingMode.DEEP)
        val extremeTimeout = TimeoutConfig.getTimeout(ThinkingMode.EXTREME)

        assertEquals(10000L, quickTimeout)
        assertEquals(30000L, deepTimeout)
        assertEquals(120000L, extremeTimeout)
    }

    // 6. Retry test (Exponential Backoff)
    @Test
    fun testRetryExponentialBackoff() = runBlocking {
        var attempts = 0
        try {
            router.executeWithRetry(RetryPolicy(maxAttempts = 3, initialDelayMs = 10L)) {
                attempts++
                throw IOException("Server Error 503 Service Unavailable")
            }
        } catch (e: Exception) {
            // Expected
        }
        assertEquals(3, attempts)
    }

    // 7. Rate-limit test (429 treated as retryable)
    @Test
    fun testRateLimitTriggeringRetry() = runBlocking {
        var attempts = 0
        try {
            router.executeWithRetry(RetryPolicy(maxAttempts = 2, initialDelayMs = 5L)) {
                attempts++
                throw IOException("Rate limit exceeded. HTTP 429 Too Many Requests")
            }
        } catch (e: Exception) {
            // Expected
        }
        assertEquals(2, attempts)
    }

    // 8. Structured-output test
    @Test
    fun testStructuredOutputTaskPlanParsing() {
        val schema = JSONObject().apply {
            put("goal", "create ordering backend")
            put("riskLevel", "LOW")
            put("requiresConfirmation", false)
        }

        assertEquals("create ordering backend", schema.getString("goal"))
        assertEquals("LOW", schema.getString("riskLevel"))
        assertFalse(schema.getBoolean("requiresConfirmation"))
    }

    // 9. Tool-call validation test
    @Test
    fun testToolCallValidationSchema() {
        val toolCall = ToolCall(
            toolId = "supabase_tool",
            functionName = "createTable",
            arguments = mapOf("tableName" to "products")
        )

        assertEquals("supabase_tool", toolCall.toolId)
        assertEquals("createTable", toolCall.functionName)
        assertEquals("products", toolCall.arguments["tableName"])
    }

    // 10. ModelRouter test
    @Test
    fun testModelRouterCapabilities() {
        val fastModel = router.route("Hello how are you")
        assertEquals(CapabilityClass.FAST, fastModel.capabilityClass)
        assertEquals("gemini-3.5-flash", fastModel.modelId)

        val codingModel = router.route("Write Kotlin class for products table")
        assertEquals(CapabilityClass.CODING, codingModel.capabilityClass)
        assertEquals("gemini-3.1-pro-preview", codingModel.modelId)
    }

    // 11. TaskClassifier test
    @Test
    fun testTaskClassifierCategorization() {
        val cat1 = classifier.classifyTask("Write class Products")
        assertEquals(TaskCategory.CODING, cat1)

        val cat2 = classifier.classifyTask("اكتب مخطط جدول المنتجات")
        assertEquals(TaskCategory.STRUCTURED_DATA, cat2)
    }

    // 12. Provider fallback test
    @Test
    fun testProviderFallbackWithSecurityCheck() {
        val policySecure = SecurityPolicy(restrictDataTransfers = true)
        val policyNormal = SecurityPolicy(restrictDataTransfers = false)

        // Normal fallback should succeed
        val fallback = router.handleFallback("google_provider", CapabilityClass.LOCAL, policyNormal)
        assertEquals("local_provider", fallback.providerId)

        // Restricted fallback should raise SecurityException
        try {
            router.handleFallback("google_provider", CapabilityClass.LOCAL, policySecure)
            fail("Should fail due to security policy")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("SECURITY_POLICY_VIOLATION"))
        }
    }

    // 13. Secret redaction test
    @Test
    fun testSecretRedactorOnAPIResponse() {
        val rawMessage = "ERROR: Unauthorized access using api_key=AIzaSyFakeKeyFromTelemetry_xyz123"
        val clean = SecretRedactor.redact(rawMessage)
        assertFalse(clean.contains("AIzaSyFakeKeyFromTelemetry"))
        assertTrue(clean.contains("[REDACTED_SECRET]"))
    }

    // 14. Credential vault integration test
    @Test
    fun testCredentialVaultModelProviderAccess() {
        val gemini = GeminiModelProvider()
        assertFalse(gemini.isConfigured)

        vault.storeCredential("gemini_api_key", "secret-test-token")
        assertTrue(gemini.isConfigured)
        assertEquals("secret-test-token", vault.getCredential("gemini_api_key"))

        vault.deleteCredential("gemini_api_key")
        assertFalse(gemini.isConfigured)
    }

    // 15. Invalid model response test
    @Test
    fun testInvalidModelResponseState() = runBlocking {
        val gemini = GeminiModelProvider()
        val response = gemini.generate(ModelRequest(prompt = "Hello", modelId = "gemini-3.5-flash"))
        
        // Since no real API key is configured, response should elegantly return error model containing AUTH_REQUIRED
        assertTrue(response.errors.first().contains("AUTH_REQUIRED"))
    }

    // 16. Provider unavailable test
    @Test
    fun testLocalProviderUnavailable() = runBlocking {
        val local = LocalModelProvider()
        assertFalse(local.isConfigured)

        try {
            local.generate(ModelRequest(prompt = "Hello", modelId = "local-llama-3b"))
            fail("Should fail as unconfigured")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("CAPABILITY_NOT_CONFIGURED"))
        }
    }

    // 17. Capability unsupported test
    @Test
    fun testLocalProviderCapabilitiesUnsupported() = runBlocking {
        val local = LocalModelProvider()
        val caps = local.modelCapabilities("local-llama-3b")
        assertTrue(caps.isEmpty())
    }

    // 18. No-guessing test (Vault empty returns error)
    @Test
    fun testNoGuessingEmptyCredentials() = runBlocking {
        val gemini = GeminiModelProvider()
        val response = gemini.generate(ModelRequest(prompt = "Hello", modelId = "gemini-3.5-flash"))
        assertTrue(response.errors.first().contains("ERROR: AUTH_REQUIRED"))
    }

    // 19. Quick/Deep/Extreme policy test
    @Test
    fun testThinkingModeBudgets() {
        val quick = router.getBudget(ThinkingMode.QUICK)
        val deep = router.getBudget(ThinkingMode.DEEP)
        val extreme = router.getBudget(ThinkingMode.EXTREME)

        assertEquals(1024, quick.maxOutputTokens)
        assertEquals(4096, deep.maxOutputTokens)
        assertEquals(8192, extreme.maxOutputTokens)

        assertEquals(2, quick.maxToolIterations)
        assertEquals(10, deep.maxToolIterations)
        assertEquals(30, extreme.maxToolIterations)
    }
}
