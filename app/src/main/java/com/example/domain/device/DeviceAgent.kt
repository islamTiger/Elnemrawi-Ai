package com.example.domain.device

interface DeviceAgent {
    suspend fun getCapabilities(): DeviceCapabilities
    suspend fun executeAction(action: DeviceAction): DeviceActionResult
    suspend fun launchApp(packageName: String): DeviceActionResult
    suspend fun pressBack(): DeviceActionResult
    suspend fun pressHome(): DeviceActionResult
    suspend fun tap(x: Float, y: Float): DeviceActionResult
    suspend fun longPress(x: Float, y: Float, durationMs: Long = 1000L): DeviceActionResult
    suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): DeviceActionResult
    suspend fun typeText(text: String, elementId: String? = null): DeviceActionResult
    suspend fun scroll(direction: ScrollDirection): DeviceActionResult
    suspend fun wait(durationMs: Long): DeviceActionResult
    suspend fun inspectScreen(): DeviceActionResult
    suspend fun findElement(
        text: String? = null,
        viewId: String? = null,
        contentDescription: String? = null
    ): DeviceActionResult
    suspend fun takeScreenshot(): DeviceActionResult
}
