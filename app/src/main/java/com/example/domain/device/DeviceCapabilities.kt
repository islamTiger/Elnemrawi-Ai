package com.example.domain.device

data class DeviceCapabilities(
    val accessibilityEnabled: Boolean,
    val screenCaptureAvailable: Boolean,
    val fileAccessAvailable: Boolean,
    val appLaunchAvailable: Boolean
)
