package com.example.domain.device

enum class DeviceErrorCode {
    CAPABILITY_NOT_AVAILABLE,
    PERMISSION_REQUIRED,
    ACCESSIBILITY_NOT_ENABLED,
    SCREEN_CAPTURE_NOT_AVAILABLE,
    ELEMENT_NOT_FOUND,
    APP_NOT_FOUND,
    ACTION_FAILED,
    NOT_IMPLEMENTED
}

data class DeviceActionResult(
    val success: Boolean,
    val actionName: String,
    val message: String,
    val errorCode: DeviceErrorCode? = null,
    val screenshot: DeviceScreenshot? = null,
    val elements: List<DeviceElement> = emptyList(),
    val data: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(
            actionName: String,
            message: String,
            screenshot: DeviceScreenshot? = null,
            elements: List<DeviceElement> = emptyList(),
            data: Map<String, Any> = emptyMap()
        ): DeviceActionResult = DeviceActionResult(
            success = true,
            actionName = actionName,
            message = message,
            errorCode = null,
            screenshot = screenshot,
            elements = elements,
            data = data
        )

        fun failure(
            actionName: String,
            errorCode: DeviceErrorCode,
            message: String,
            data: Map<String, Any> = emptyMap()
        ): DeviceActionResult = DeviceActionResult(
            success = false,
            actionName = actionName,
            message = message,
            errorCode = errorCode,
            data = data
        )
    }
}
