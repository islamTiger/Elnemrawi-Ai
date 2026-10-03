package com.example.domain.device

data class DeviceScreenshot(
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val format: String = "JPEG",
    val base64Data: String? = null,
    val uriString: String? = null
)
