package com.example.data.device.android

import android.content.Context
import android.content.Intent
import com.example.domain.device.*
import kotlinx.coroutines.delay

class AndroidDeviceAgent(
    private val context: Context,
    private val capabilityDetector: AndroidDeviceCapabilityDetector = AndroidDeviceCapabilityDetector(context),
    private val screenCaptureManager: ScreenCaptureManager = ScreenCaptureManager.instance
) : DeviceAgent {

    override suspend fun getCapabilities(): DeviceCapabilities {
        return capabilityDetector.detectCapabilities()
    }

    override suspend fun executeAction(action: DeviceAction): DeviceActionResult {
        return when (action) {
            is DeviceAction.LaunchApp -> launchApp(action.packageName)
            is DeviceAction.PressBack -> pressBack()
            is DeviceAction.PressHome -> pressHome()
            is DeviceAction.Tap -> tap(action.x, action.y)
            is DeviceAction.TapElement -> tapElement(action.elementId, action.text)
            is DeviceAction.LongPress -> longPress(action.x, action.y, action.durationMs)
            is DeviceAction.Swipe -> swipe(action.startX, action.startY, action.endX, action.endY, action.durationMs)
            is DeviceAction.TypeText -> typeText(action.text, action.elementId)
            is DeviceAction.Scroll -> scroll(action.direction)
            is DeviceAction.Wait -> wait(action.durationMs)
            is DeviceAction.InspectScreen -> inspectScreen()
            is DeviceAction.FindElement -> findElement(action.text, action.viewId, action.contentDescription)
            is DeviceAction.TakeScreenshot -> takeScreenshot()
        }
    }

    override suspend fun launchApp(packageName: String): DeviceActionResult {
        val pm = context.packageManager
            ?: return DeviceActionResult.failure(
                actionName = "launchApp",
                errorCode = DeviceErrorCode.CAPABILITY_NOT_AVAILABLE,
                message = "PackageManager is not available"
            )

        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return DeviceActionResult.failure(
                actionName = "launchApp",
                errorCode = DeviceErrorCode.APP_NOT_FOUND,
                message = "Application '$packageName' is not installed or has no launchable Activity",
                data = mapOf("packageName" to packageName)
            )

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            DeviceActionResult.success(
                actionName = "launchApp",
                message = "Launched app '$packageName' successfully",
                data = mapOf("packageName" to packageName)
            )
        } catch (e: Exception) {
            DeviceActionResult.failure(
                actionName = "launchApp",
                errorCode = DeviceErrorCode.ACTION_FAILED,
                message = "Failed to launch '$packageName': ${e.message ?: "Unknown error"}"
            )
        }
    }

    override suspend fun pressBack(): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "pressBack",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot press back."
            )
        return service.pressBack()
    }

    override suspend fun pressHome(): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "pressHome",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot press home."
            )
        return service.pressHome()
    }

    override suspend fun tap(x: Float, y: Float): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "tap",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot perform tap gesture."
            )
        return service.tapCoordinates(x, y)
    }

    suspend fun tapElement(elementId: String?, text: String?): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "tapElement",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot click element."
            )
        return service.clickElement(elementId, text)
    }

    override suspend fun longPress(x: Float, y: Float, durationMs: Long): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "longPress",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot perform long press gesture."
            )
        return service.longPressCoordinates(x, y, durationMs)
    }

    override suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "swipe",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot perform swipe gesture."
            )
        return service.swipeCoordinates(startX, startY, endX, endY, durationMs)
    }

    override suspend fun typeText(text: String, elementId: String?): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "typeText",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot enter text."
            )
        return service.typeText(text, elementId)
    }

    override suspend fun scroll(direction: ScrollDirection): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "scroll",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot scroll."
            )
        return service.scroll(direction)
    }

    override suspend fun wait(durationMs: Long): DeviceActionResult {
        delay(durationMs)
        return DeviceActionResult.success(
            actionName = "wait",
            message = "Waited for ${durationMs}ms"
        )
    }

    override suspend fun inspectScreen(): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "inspectScreen",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot inspect screen UI nodes."
            )
        return service.inspectScreen()
    }

    override suspend fun findElement(
        text: String?,
        viewId: String?,
        contentDescription: String?
    ): DeviceActionResult {
        val service = DeviceAccessibilityService.instance
            ?: return DeviceActionResult.failure(
                actionName = "findElement",
                errorCode = DeviceErrorCode.ACCESSIBILITY_NOT_ENABLED,
                message = "Accessibility service is not enabled. Cannot search UI nodes."
            )
        return service.findElements(text, viewId, contentDescription)
    }

    override suspend fun takeScreenshot(): DeviceActionResult {
        return screenCaptureManager.captureScreenshot(context)
    }
}
