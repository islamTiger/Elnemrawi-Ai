package com.example.domain.device

enum class ScrollDirection {
    FORWARD,
    BACKWARD,
    UP,
    DOWN,
    LEFT,
    RIGHT
}

sealed class DeviceAction(val name: String) {
    data class LaunchApp(val packageName: String, val appName: String? = null) : DeviceAction("launchApp")
    object PressBack : DeviceAction("pressBack")
    object PressHome : DeviceAction("pressHome")
    data class Tap(val x: Float, val y: Float) : DeviceAction("tap")
    data class TapElement(val elementId: String? = null, val text: String? = null) : DeviceAction("tapElement")
    data class LongPress(val x: Float, val y: Float, val durationMs: Long = 1000L) : DeviceAction("longPress")
    data class Swipe(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val durationMs: Long = 300L
    ) : DeviceAction("swipe")
    data class TypeText(val text: String, val elementId: String? = null) : DeviceAction("typeText")
    data class Scroll(val direction: ScrollDirection = ScrollDirection.FORWARD) : DeviceAction("scroll")
    data class Wait(val durationMs: Long) : DeviceAction("wait")
    object InspectScreen : DeviceAction("inspectScreen")
    data class FindElement(
        val text: String? = null,
        val viewId: String? = null,
        val contentDescription: String? = null
    ) : DeviceAction("findElement")
    object TakeScreenshot : DeviceAction("takeScreenshot")
}
