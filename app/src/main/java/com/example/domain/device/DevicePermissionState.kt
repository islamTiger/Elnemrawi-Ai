package com.example.domain.device

data class DevicePermissionState(
    val accessibilityGranted: Boolean = false,
    val screenCaptureGranted: Boolean = false,
    val queryPackagesGranted: Boolean = false,
    val fileAccessGranted: Boolean = false,
    val missingPermissions: List<String> = emptyList()
)
