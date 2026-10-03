package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.ServiceLocator
import com.example.data.agent.AgentPlannerImpl
import com.example.data.device.android.AndroidDeviceAgent
import com.example.data.device.android.AndroidDeviceCapabilityDetector
import com.example.data.device.android.ScreenCaptureManager
import com.example.data.device.tools.AppToolImpl
import com.example.data.device.tools.DeviceToolImpl
import com.example.data.device.tools.RealDeviceFileTool
import com.example.domain.agent.AgentContext
import com.example.domain.model.*
import com.example.domain.device.DeviceAction
import com.example.domain.device.DeviceErrorCode
import com.example.domain.device.ScrollDirection
import com.example.domain.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeviceAgentTest {

    private lateinit var context: Context
    private lateinit var capabilityDetector: AndroidDeviceCapabilityDetector
    private lateinit var deviceAgent: AndroidDeviceAgent
    private lateinit var deviceTool: DeviceToolImpl
    private lateinit var appTool: AppToolImpl
    private lateinit var fileTool: RealDeviceFileTool
    private lateinit var planner: AgentPlannerImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(context)

        capabilityDetector = AndroidDeviceCapabilityDetector(context)
        deviceAgent = AndroidDeviceAgent(context, capabilityDetector, ScreenCaptureManager.instance)
        deviceTool = DeviceToolImpl(deviceAgent)
        appTool = AppToolImpl(context)
        fileTool = RealDeviceFileTool(context)
        planner = AgentPlannerImpl()

        // Clear screen capture consent
        ScreenCaptureManager.instance.clearConsent()
    }

    @Test
    fun testDeviceCapabilityDetection() {
        val capabilities = capabilityDetector.detectCapabilities()
        assertNotNull(capabilities)
        // Accessibility is not enabled in standard unit test environment
        assertFalse(capabilities.accessibilityEnabled)
        // Screen capture consent is not yet granted
        assertFalse(capabilities.screenCaptureAvailable)

        val permissionState = capabilityDetector.getPermissionState()
        assertNotNull(permissionState)
        assertFalse(permissionState.accessibilityGranted)
        assertFalse(permissionState.screenCaptureGranted)
        assertTrue(permissionState.missingPermissions.contains("ACCESSIBILITY_SERVICE"))
        assertTrue(permissionState.missingPermissions.contains("SCREEN_CAPTURE_CONSENT"))

        // Storage and share intents resolution
        val shareAvailable = capabilityDetector.isShareAvailable()
        val safAvailable = capabilityDetector.isStorageAccessFrameworkAvailable()
        // SAF or Share should return boolean without throwing exceptions
        assertNotNull(shareAvailable)
        assertNotNull(safAvailable)
    }

    @Test
    fun testDeviceToolRegistration() {
        ToolRegistry.registerTool(deviceTool)
        ToolRegistry.registerTool(appTool)
        ToolRegistry.registerTool(fileTool)

        assertEquals("device_tool", deviceTool.id)
        assertEquals("app_tool", appTool.id)
        assertEquals("real_device_file_tool", fileTool.id)

        assertNotNull(ToolRegistry.getTool("device_tool"))
        assertNotNull(ToolRegistry.getTool("app_tool"))
        assertNotNull(ToolRegistry.getTool("real_device_file_tool"))

        // Check input schemas exist and contain expected arguments
        assertTrue(deviceTool.inputSchema.containsKey("action"))
        assertTrue(appTool.inputSchema.containsKey("operation"))
        assertTrue(fileTool.inputSchema.containsKey("operation"))
    }

    @Test
    fun testAppToolOperations() = runBlocking {
        val apps = appTool.listInstalledApps()
        assertNotNull(apps)

        val notFoundResult = appTool.execute(mapOf("operation" to "find", "query" to "NonExistentApplication123XYZ"))
        assertTrue(notFoundResult.contains("APP_NOT_FOUND"))

        val notFoundLaunch = appTool.execute(mapOf("operation" to "launch", "packageName" to "com.example.nonexistent.app"))
        assertTrue(notFoundLaunch.contains("APP_NOT_FOUND"))

        val unknownOp = appTool.execute(mapOf("operation" to "invalidOperation"))
        assertTrue(unknownOp.contains("Unknown operation"))

        val missingQuery = appTool.execute(mapOf("operation" to "find"))
        assertTrue(missingQuery.contains("Missing 'query'"))
    }

    @Test
    fun testDeviceAgentMissingPermissionsErrorHandling() = runBlocking {
        // Without AccessibilityService connected, all UI actions must fail with ACCESSIBILITY_NOT_ENABLED
        val backResult = deviceAgent.pressBack()
        assertFalse(backResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, backResult.errorCode)
        assertTrue(backResult.message.contains("Accessibility"))

        val homeResult = deviceAgent.pressHome()
        assertFalse(homeResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, homeResult.errorCode)

        val tapResult = deviceAgent.tap(100f, 200f)
        assertFalse(tapResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, tapResult.errorCode)

        val typeResult = deviceAgent.typeText("Hello world")
        assertFalse(typeResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, typeResult.errorCode)

        val scrollResult = deviceAgent.scroll(ScrollDirection.FORWARD)
        assertFalse(scrollResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, scrollResult.errorCode)

        val inspectResult = deviceAgent.inspectScreen()
        assertFalse(inspectResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, inspectResult.errorCode)

        val findResult = deviceAgent.findElement("Submit")
        assertFalse(findResult.success)
        assertEquals(DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED, findResult.errorCode)

        // Screen capture without consent must return SCREEN_CAPTURE_NOT_AVAILABLE
        val screenshotResult = deviceAgent.takeScreenshot()
        assertFalse(screenshotResult.success)
        assertEquals(DeviceErrorCode.SCREEN_CAPTURE_NOT_AVAILABLE, screenshotResult.errorCode)

        // Launching non-existent app must return APP_NOT_FOUND
        val launchResult = deviceAgent.launchApp("com.fake.nonexistent")
        assertFalse(launchResult.success)
        assertEquals(DeviceErrorCode.APP_NOT_FOUND, launchResult.errorCode)
    }

    @Test
    fun testDeviceToolExecuteFormatsErrorsProperly() = runBlocking {
        val tapErrorOutput = deviceTool.execute(mapOf("action" to "tap", "x" to 50f, "y" to 50f))
        assertTrue(tapErrorOutput.startsWith("ERROR: ACCESSIBILITY_NOT_ENABLED"))

        val missingCoordinates = deviceTool.execute(mapOf("action" to "tap"))
        assertTrue(missingCoordinates.contains("Missing 'x' and 'y'"))

        val screenshotErrorOutput = deviceTool.execute(mapOf("action" to "takeScreenshot"))
        assertTrue(screenshotErrorOutput.startsWith("ERROR: SCREEN_CAPTURE_NOT_AVAILABLE"))

        val capabilitiesOutput = deviceTool.execute(mapOf("action" to "getCapabilities"))
        assertTrue(capabilitiesOutput.contains("Device Capabilities"))
        assertTrue(capabilitiesOutput.contains("Accessibility Enabled"))
    }

    @Test
    fun testDeviceActionWaitExecutes() = runBlocking {
        val waitResult = deviceAgent.wait(50L)
        assertTrue(waitResult.success)
        assertEquals("wait", waitResult.actionName)
        assertTrue(waitResult.message.contains("50ms"))
    }

    @Test
    fun testAgentPlannerSelectsDeviceTools() = runBlocking {
        val context = AgentContext(currentRequest = "افتح تطبيق المتصفح", workspace = null, selectedModelId = "test")

        // 1. App Launch Prompt
        val appSteps = planner.planTask("افتح تطبيق المتصفح", context)
        assertTrue(appSteps.any { it.type == StepType.DEVICE_CHECK_PERMISSIONS })
        assertTrue(appSteps.any { it.type == StepType.DEVICE_LAUNCH_APP })

        // 2. Screen Capture Prompt
        val screenshotSteps = planner.planTask("خذ لقطة شاشة للواجهة", context)
        assertTrue(screenshotSteps.any { it.type == StepType.DEVICE_CHECK_PERMISSIONS })
        assertTrue(screenshotSteps.any { it.type == StepType.DEVICE_CAPTURE_SCREEN })

        // 3. UI Interaction Prompt
        val tapSteps = planner.planTask("اضغط على زر تأكيد الحساب", context)
        assertTrue(tapSteps.any { it.type == StepType.DEVICE_CHECK_PERMISSIONS })
        assertTrue(tapSteps.any { it.type == StepType.DEVICE_INSPECT_UI })
        assertTrue(tapSteps.any { it.type == StepType.DEVICE_INTERACT })

        // 4. File Sharing Prompt
        val fileSteps = planner.planTask("شارك الملف مع التطبيقات", context)
        assertTrue(fileSteps.any { it.type == StepType.DEVICE_CHECK_PERMISSIONS })
        assertTrue(fileSteps.any { it.type == StepType.DEVICE_FILE_OPERATION })
    }

    @Test
    fun testRealDeviceFileTool() = runBlocking {
        // Save file
        val saveOutput = fileTool.execute(
            mapOf(
                "operation" to "saveFile",
                "fileName" to "agent_log.txt",
                "content" to "Autonomous Android Agent Test"
            )
        )
        assertTrue(saveOutput.startsWith("SUCCESS: File saved"))

        // List files
        val listOutput = fileTool.execute(mapOf("operation" to "listFiles"))
        assertTrue(listOutput.contains("agent_log.txt"))

        // SAF Pick intent creation
        val pickOutput = fileTool.execute(mapOf("operation" to "pickFile", "mimeType" to "text/plain"))
        assertTrue(pickOutput.contains("ACTION_PREPARED"))
        assertTrue(pickOutput.contains("android.intent.action.OPEN_DOCUMENT"))
    }
}
