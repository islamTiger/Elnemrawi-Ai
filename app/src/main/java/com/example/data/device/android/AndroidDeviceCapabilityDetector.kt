package com.example.data.device.android

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import com.example.domain.device.DeviceCapabilities
import com.example.domain.device.DevicePermissionState

class AndroidDeviceCapabilityDetector(private val context: Context) {

    fun detectCapabilities(): DeviceCapabilities {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val screenCaptureAvailable = ScreenCaptureManager.instance.isConsentGranted
        val fileAccessAvailable = isStorageAccessFrameworkAvailable()
        val appLaunchAvailable = isAppLaunchAvailable()

        return DeviceCapabilities(
            accessibilityEnabled = accessibilityEnabled,
            screenCaptureAvailable = screenCaptureAvailable,
            fileAccessAvailable = fileAccessAvailable,
            appLaunchAvailable = appLaunchAvailable
        )
    }

    fun getPermissionState(): DevicePermissionState {
        val accessibility = isAccessibilityServiceEnabled()
        val screenCapture = ScreenCaptureManager.instance.isConsentGranted
        val fileAccess = isStorageAccessFrameworkAvailable()

        val missing = mutableListOf<String>()
        if (!accessibility) missing.add("ACCESSIBILITY_SERVICE")
        if (!screenCapture) missing.add("SCREEN_CAPTURE_CONSENT")

        return DevicePermissionState(
            accessibilityGranted = accessibility,
            screenCaptureGranted = screenCapture,
            queryPackagesGranted = isAppLaunchAvailable(),
            fileAccessGranted = fileAccess,
            missingPermissions = missing
        )
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (DeviceAccessibilityService.isConnected) {
            return true
        }

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${context.packageName}/${DeviceAccessibilityService::class.java.name}"

        return enabledServices.any { service ->
            val id = service.id
            id != null && (id == expectedServiceName || id.contains(DeviceAccessibilityService::class.java.simpleName))
        }
    }

    fun isStorageAccessFrameworkAvailable(): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    fun isAppLaunchAvailable(): Boolean {
        val pm = context.packageManager ?: return false
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.isNotEmpty()
    }

    fun isShareAvailable(): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
        }
        return intent.resolveActivity(context.packageManager) != null
    }
}
