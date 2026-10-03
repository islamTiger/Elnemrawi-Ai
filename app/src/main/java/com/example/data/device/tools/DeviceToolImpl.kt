package com.example.data.device.tools

import com.example.domain.device.*
import com.example.domain.tools.DeviceTool

class DeviceToolImpl(private val deviceAgent: DeviceAgent) : DeviceTool {
    override val id: String = "device_tool"
    override val name: String = "Android Device Control Agent"
    override val description: String = "Perform real Android device UI automation: inspect active screen nodes, find elements, tap, type text, scroll, press back/home, take screenshot, and launch apps."
    override val inputSchema: Map<String, String> = mapOf(
        "action" to "String (inspectScreen | findElement | tap | tapElement | typeText | scroll | pressBack | pressHome | takeScreenshot | launchApp | longPress | swipe | wait | getCapabilities)",
        "packageName" to "String (Target application package name for launchApp, optional)",
        "text" to "String (Text to type or search on screen, optional)",
        "viewId" to "String (View resource ID to find or click, optional)",
        "contentDescription" to "String (Accessibility label/content description to search, optional)",
        "x" to "Float (Screen X coordinate for tap/longPress/swipe, optional)",
        "y" to "Float (Screen Y coordinate for tap/longPress/swipe, optional)",
        "endX" to "Float (End X coordinate for swipe, optional)",
        "endY" to "Float (End Y coordinate for swipe, optional)",
        "direction" to "String (forward | backward | up | down for scroll, optional)",
        "durationMs" to "Long (Duration in milliseconds for wait/longPress/swipe, optional)"
    )

    override suspend fun execute(arguments: Map<String, Any>): String {
        val action = arguments["action"]?.toString()?.lowercase() ?: "inspectscreen"

        return when (action) {
            "getcapabilities" -> {
                val caps = deviceAgent.getCapabilities()
                "Device Capabilities:\n" +
                "• Accessibility Enabled: ${caps.accessibilityEnabled}\n" +
                "• Screen Capture Available: ${caps.screenCaptureAvailable}\n" +
                "• App Launching Available: ${caps.appLaunchAvailable}\n" +
                "• File Access Available: ${caps.fileAccessAvailable}"
            }
            "inspectscreen" -> {
                formatResult(deviceAgent.inspectScreen())
            }
            "findelement" -> {
                val text = arguments["text"]?.toString()
                val viewId = arguments["viewId"]?.toString()
                val desc = arguments["contentDescription"]?.toString()
                if (text.isNullOrBlank() && viewId.isNullOrBlank() && desc.isNullOrBlank()) {
                    return "ERROR: At least one of 'text', 'viewId', or 'contentDescription' is required to find an element."
                }
                formatResult(deviceAgent.findElement(text, viewId, desc))
            }
            "tap" -> {
                val x = arguments["x"]?.toString()?.toFloatOrNull()
                val y = arguments["y"]?.toString()?.toFloatOrNull()
                if (x == null || y == null) {
                    return "ERROR: Missing 'x' and 'y' float coordinates for tap action"
                }
                formatResult(deviceAgent.tap(x, y))
            }
            "tapelement" -> {
                val text = arguments["text"]?.toString()
                val viewId = arguments["viewId"]?.toString()
                if (text.isNullOrBlank() && viewId.isNullOrBlank()) {
                    return "ERROR: Missing 'text' or 'viewId' to locate target element for tap"
                }
                formatResult(deviceAgent.executeAction(DeviceAction.TapElement(elementId = viewId, text = text)))
            }
            "typetext" -> {
                val text = arguments["text"]?.toString()
                if (text == null) {
                    return "ERROR: Missing 'text' parameter to type"
                }
                val viewId = arguments["viewId"]?.toString()
                formatResult(deviceAgent.typeText(text, viewId))
            }
            "pressback" -> {
                formatResult(deviceAgent.pressBack())
            }
            "presshome" -> {
                formatResult(deviceAgent.pressHome())
            }
            "scroll" -> {
                val dirStr = arguments["direction"]?.toString()?.lowercase() ?: "forward"
                val dir = when (dirStr) {
                    "backward", "up" -> ScrollDirection.BACKWARD
                    "left" -> ScrollDirection.LEFT
                    "right" -> ScrollDirection.RIGHT
                    else -> ScrollDirection.FORWARD
                }
                formatResult(deviceAgent.scroll(dir))
            }
            "takescreenshot" -> {
                formatResult(deviceAgent.takeScreenshot())
            }
            "launchapp" -> {
                val pkg = arguments["packageName"]?.toString()
                if (pkg.isNullOrBlank()) {
                    return "ERROR: Missing 'packageName' parameter to launch app"
                }
                formatResult(deviceAgent.launchApp(pkg))
            }
            "longpress" -> {
                val x = arguments["x"]?.toString()?.toFloatOrNull()
                val y = arguments["y"]?.toString()?.toFloatOrNull()
                val duration = arguments["durationMs"]?.toString()?.toLongOrNull() ?: 1000L
                if (x == null || y == null) {
                    return "ERROR: Missing 'x' and 'y' coordinates for long press"
                }
                formatResult(deviceAgent.longPress(x, y, duration))
            }
            "swipe" -> {
                val startX = arguments["startX"]?.toString()?.toFloatOrNull() ?: arguments["x"]?.toString()?.toFloatOrNull()
                val startY = arguments["startY"]?.toString()?.toFloatOrNull() ?: arguments["y"]?.toString()?.toFloatOrNull()
                val endX = arguments["endX"]?.toString()?.toFloatOrNull()
                val endY = arguments["endY"]?.toString()?.toFloatOrNull()
                val duration = arguments["durationMs"]?.toString()?.toLongOrNull() ?: 300L
                if (startX == null || startY == null || endX == null || endY == null) {
                    return "ERROR: Missing 'startX', 'startY', 'endX', or 'endY' coordinates for swipe"
                }
                formatResult(deviceAgent.swipe(startX, startY, endX, endY, duration))
            }
            "wait" -> {
                val duration = arguments["durationMs"]?.toString()?.toLongOrNull() ?: 1000L
                formatResult(deviceAgent.wait(duration))
            }
            else -> "ERROR: Unknown device action '$action'"
        }
    }

    private fun formatResult(result: DeviceActionResult): String {
        return if (result.success) {
            val sb = StringBuilder("SUCCESS: [${result.actionName}] ${result.message}\n")
            if (result.elements.isNotEmpty()) {
                sb.append("Discovered Elements (${result.elements.size}):\n")
                result.elements.take(20).forEach { elem ->
                    val label = elem.text ?: elem.contentDescription ?: elem.viewIdResourceName ?: elem.className ?: "element"
                    sb.append("• $label (bounds: ${elem.bounds.left},${elem.bounds.top} to ${elem.bounds.right},${elem.bounds.bottom}, clickable=${elem.isClickable})\n")
                }
            }
            if (result.screenshot != null) {
                sb.append("Screenshot captured: ${result.screenshot.width}x${result.screenshot.height} ${result.screenshot.format}\n")
            }
            sb.toString().trimEnd()
        } else {
            "ERROR: ${result.errorCode ?: DeviceErrorCode.ACTION_FAILED} - ${result.message}"
        }
    }
}
