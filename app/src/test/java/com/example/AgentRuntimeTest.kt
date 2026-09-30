package com.example

import com.example.core.di.ServiceLocator
import com.example.core.registry.ModelRegistry
import com.example.domain.ai.*
import com.example.domain.model.*
import com.example.data.ai.runtime.LocalModelRuntime
import com.example.data.ai.runtime.OpenAiCompatibleConfig
import com.example.data.ai.runtime.OpenAiCompatibleRuntime
import com.example.data.ai.runtime.ModelSelectorImpl
import com.example.data.ai.runtime.RoutingAiRuntime
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentRuntimeTest {

    private val modelSelector: ModelSelector = ModelSelectorImpl()

    @Before
    fun setUp() {
        ServiceLocator.init(androidx.test.core.app.ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testModelSelectionByCapability() {
        val codingModel = modelSelector.selectModel(ModelCategory.CODING)
        assertNotNull(codingModel)
        assertTrue(codingModel!!.capabilities.contains(ModelCapability.CODE_EXPLANATION))
        assertEquals(ModelType.CODING, codingModel.type)

        val visionModel = modelSelector.selectModel(ModelCategory.VISION)
        assertNotNull(visionModel)
        assertTrue(visionModel!!.capabilities.contains(ModelCapability.IMAGE_TO_TEXT))
        assertEquals(ModelType.VISION, visionModel.type)
    }

    @Test
    fun testNoConfiguredModelThrows() {
        val runtime = LocalModelRuntime()
        // Unconfigure the model using ServiceLocator manager
        runBlocking {
            ServiceLocator.localModelManager.removeModel()
        }
        assertFalse(runtime.isConfigured)

        val modelInfo = ModelInfo(
            id = "test-model",
            displayName = "Test Model",
            provider = "Provider",
            type = ModelType.TEXT,
            capabilities = listOf(ModelCapability.TEXT_GENERATION),
            contextWindow = 2048,
            parameterSize = "7B",
            quantization = "None",
            isLocal = true,
            isEnabled = true,
            isConfigured = false,
            runtimeType = RuntimeType.LOCAL
        )

        try {
            runBlocking {
                runtime.generate(AiRuntimeRequest(prompt = "Hello", model = modelInfo))
            }
            fail("Expected AiRuntimeException")
        } catch (e: AiRuntimeException) {
            assertEquals(AiErrorCode.NOT_CONFIGURED, e.code)
        }
    }

    @Test
    fun testOpenAiCompatibleConfig() {
        val config = OpenAiCompatibleConfig(baseUrl = "https://localhost:8080/v1", modelId = "deepseek")
        val runtime = OpenAiCompatibleRuntime(config)
        assertTrue(runtime.isConfigured)
    }

    @Test
    fun testRoutingAiRuntimeResolvesCorrectly() {
        val localRuntime = LocalModelRuntime()
        val openAiRuntime = OpenAiCompatibleRuntime(OpenAiCompatibleConfig(baseUrl = "https://api.openai.com/v1"))
        val routing = RoutingAiRuntime(localRuntime, openAiRuntime)

        val localModel = ModelInfo(
            id = "test-local",
            displayName = "Test Local",
            provider = "Meta",
            type = ModelType.TEXT,
            capabilities = listOf(ModelCapability.TEXT_GENERATION),
            contextWindow = 2048,
            parameterSize = "8B",
            quantization = "Q4",
            isLocal = true,
            isEnabled = true,
            isConfigured = true,
            runtimeType = RuntimeType.LOCAL
        )

        runBlocking {
            try {
                routing.generate(AiRuntimeRequest(prompt = "test", model = localModel))
                fail("Expected unconfigured local engine throw")
            } catch (e: AiRuntimeException) {
                assertEquals(AiErrorCode.NOT_CONFIGURED, e.code)
            }
        }
    }

    @Test
    fun testStreamingEventsFlow() = runBlocking {
        val config = OpenAiCompatibleConfig(baseUrl = "https://localhost:8000/v1")
        val runtime = OpenAiCompatibleRuntime(config)
        
        val model = ModelInfo(
            id = "test-openai",
            displayName = "Test OpenAI",
            provider = "Self-hosted",
            type = ModelType.TEXT,
            capabilities = listOf(ModelCapability.TEXT_GENERATION),
            contextWindow = 2048,
            parameterSize = "Unknown",
            quantization = "None",
            isLocal = false,
            isEnabled = true,
            isConfigured = true,
            runtimeType = RuntimeType.OPENAI_COMPATIBLE
        )

        val events = runtime.generateStream(AiRuntimeRequest(prompt = "Hello", model = model)).toList()
        assertTrue(events.contains(AiRuntimeStreamEvent.STARTED))
        assertTrue(events.any { it is AiRuntimeStreamEvent.TOKEN })
        assertTrue(events.contains(AiRuntimeStreamEvent.COMPLETED))
    }
}
