package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.ServiceLocator
import com.example.core.registry.ModelRegistry
import com.example.data.ai.runtime.LocalModelRuntime
import com.example.domain.ai.*
import com.example.domain.model.*
import com.example.domain.agent.AgentContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalModelRuntimeTest {

    private lateinit var context: Context
    private lateinit var manager: LocalModelManager
    private lateinit var runtime: LocalModelRuntime

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(context)
        
        manager = ServiceLocator.localModelManager
        runtime = ServiceLocator.localModelRuntime

        runBlocking {
            manager.removeModel()
        }
    }

    @Test
    fun testInitialStateNoModel() {
        assertEquals(LocalModelState.NO_MODEL, manager.modelState.value)
        assertNull(manager.currentConfig.value)
        assertFalse(runtime.isConfigured)
        assertEquals("NOT_CONFIGURED", runtime.getStatus())
    }

    @Test
    fun testValidationSucceeds() {
        runBlocking {
            val tempFile = File.createTempFile("valid_model", ".gguf")
            tempFile.writeBytes(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte(), 0, 0, 0, 1))
            val uri = Uri.fromFile(tempFile)

            manager.selectModel(uri.toString(), "llama-3-test.gguf", tempFile.length())

            assertEquals(LocalModelState.READY, manager.modelState.value)
            assertNotNull(manager.currentConfig.value)
            assertEquals("llama-3-test.gguf", manager.currentConfig.value?.name)
            assertTrue(runtime.isConfigured)
            
            // Native engine is unconfigured by default, so runtime status reflects native unavailable
            assertEquals("NATIVE_ENGINE_UNAVAILABLE", runtime.getStatus())

            // Verify it was dynamically registered in ModelRegistry
            val regModel = ModelRegistry.getModel("selected-local-gguf")
            assertNotNull(regModel)
            assertEquals("llama-3-test.gguf", regModel?.displayName)
            assertTrue(regModel?.isConfigured == true)

            tempFile.delete()
        }
    }

    @Test
    fun testValidationFails() {
        runBlocking {
            val tempFile = File.createTempFile("invalid_model", ".gguf")
            tempFile.writeBytes(byteArrayOf('B'.code.toByte(), 'A'.code.toByte(), 'D'.code.toByte(), 'G'.code.toByte(), 0, 0, 0, 1))
            val uri = Uri.fromFile(tempFile)

            try {
                manager.selectModel(uri.toString(), "llama-3-bad.gguf", tempFile.length())
                fail("Expected selectModel to throw due to invalid GGUF header")
            } catch (ignored: Exception) {
                // Expected
            }

            assertEquals(LocalModelState.INVALID, manager.modelState.value)
            assertNull(manager.currentConfig.value)
            assertFalse(runtime.isConfigured)

            tempFile.delete()
        }
    }

    @Test
    fun testModelRemovalResets() {
        runBlocking {
            val tempFile = File.createTempFile("valid_model", ".gguf")
            tempFile.writeBytes(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte(), 0, 0, 0, 1))
            val uri = Uri.fromFile(tempFile)

            manager.selectModel(uri.toString(), "llama-3-test.gguf", tempFile.length())
            assertTrue(runtime.isConfigured)

            manager.removeModel()
            assertEquals(LocalModelState.NO_MODEL, manager.modelState.value)
            assertNull(manager.currentConfig.value)
            assertFalse(runtime.isConfigured)

            // Verify registry reflects removal
            val regModel = ModelRegistry.getModel("selected-local-gguf")
            assertFalse(regModel?.isConfigured == true)

            tempFile.delete()
        }
    }

    @Test
    fun testNativeEngineUnavailableThrows() {
        runBlocking {
            val tempFile = File.createTempFile("valid_model", ".gguf")
            tempFile.writeBytes(byteArrayOf('G'.code.toByte(), 'G'.code.toByte(), 'U'.code.toByte(), 'F'.code.toByte(), 0, 0, 0, 1))
            val uri = Uri.fromFile(tempFile)
            manager.selectModel(uri.toString(), "llama-3-test.gguf", tempFile.length())

            // Ensure engine is reported unavailable
            assertEquals(LlamaCppEngineStatus.NATIVE_ENGINE_UNAVAILABLE, ServiceLocator.llamaCppEngine.status)

            try {
                runtime.generate("test prompt")
                fail("Expected generate to throw due to unavailable native engine")
            } catch (e: AiRuntimeException) {
                assertEquals(AiErrorCode.NOT_CONFIGURED, e.code)
                assertTrue(e.message?.contains("Local model selected, but the native inference engine is unavailable.") == true)
            }

            tempFile.delete()
        }
    }

    @Test
    fun testChatStopsCleanlyUnconfigured() {
        runBlocking {
            // Clear configuration
            manager.removeModel()
            
            val aiProvider = ServiceLocator.aiProvider
            val responseFlow = aiProvider.generateResponse("hello", "selected-local-gguf")
            
            val firstEmission = responseFlow.first()
            assertEquals("Local model runtime is not configured.", firstEmission)
        }
    }

    @Test
    fun testAgentStopsCleanlyUnconfigured() {
        runBlocking {
            manager.removeModel()
            
            val testStep = AgentStep(
                title = "Test Read",
                description = "Test Description",
                type = StepType.READ_PROJECT,
                status = StepStatus.PENDING
            )
            
            val testContext = AgentContext(
                currentRequest = "Analyze the workspace",
                workspace = null,
                selectedModelId = "selected-local-gguf"
            )
            
            try {
                // Execute agent sequence step directly using our shared singleton runtime
                val agentExecutor = com.example.data.agent.AgentExecutorImpl(
                    ServiceLocator.fileTool,
                    ServiceLocator.projectTool,
                    ServiceLocator.buildTool,
                    ServiceLocator.gitTool,
                    ServiceLocator.modelSelector,
                    runtime,
                    false
                )
                agentExecutor.executeStep(testStep, testContext)
                fail("Expected AgentExecutor to throw due to unconfigured local runtime")
            } catch (e: AiRuntimeException) {
                assertEquals(AiErrorCode.NOT_CONFIGURED, e.code)
                assertTrue(e.message?.contains("Local model runtime is not configured.") == true)
            }
        }
    }
}
