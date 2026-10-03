package com.example.data.device.tools

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.tools.AppTool

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val versionName: String? = null
)

class AppToolImpl(private val context: Context) : AppTool {
    override val id: String = "app_tool"
    override val name: String = "Android Application Manager"
    override val description: String = "List, find, inspect, and launch installed applications on the Android device."
    override val inputSchema: Map<String, String> = mapOf(
        "operation" to "String (list | find | info | launch)",
        "query" to "String (App name or search keyword, optional)",
        "packageName" to "String (Package name, required for launch or specific info, optional)"
    )

    override suspend fun execute(arguments: Map<String, Any>): String {
        val operation = arguments["operation"]?.toString()?.lowercase() ?: "list"
        val query = arguments["query"]?.toString()
        val packageName = arguments["packageName"]?.toString()

        return when (operation) {
            "list" -> {
                val apps = listInstalledApps()
                val sb = StringBuilder("Installed Applications (${apps.size}):\n")
                apps.take(50).forEach { app ->
                    sb.append("• ${app.appName} (${app.packageName})\n")
                }
                if (apps.size > 50) {
                    sb.append("... and ${apps.size - 50} more. Use operation='find' to search.\n")
                }
                sb.toString()
            }
            "find" -> {
                if (query.isNullOrBlank()) {
                    return "ERROR: Missing 'query' parameter to search for applications"
                }
                val matches = findApp(query)
                if (matches.isEmpty()) {
                    return "ERROR: APP_NOT_FOUND - No installed application found matching '$query'"
                }
                val sb = StringBuilder("Matching Applications for '$query':\n")
                matches.forEach { app ->
                    sb.append("• ${app.appName} [${app.packageName}]\n")
                }
                sb.toString()
            }
            "info" -> {
                val targetPkg = packageName ?: query
                if (targetPkg.isNullOrBlank()) {
                    return "ERROR: Missing 'packageName' parameter for app info"
                }
                val info = getAppInfo(targetPkg)
                    ?: return "ERROR: APP_NOT_FOUND - Package '$targetPkg' not found on device"
                "Application Details:\n• Name: ${info.appName}\n• Package: ${info.packageName}\n• Version: ${info.versionName ?: "N/A"}\n• System App: ${info.isSystemApp}"
            }
            "launch" -> {
                var targetPkg = packageName
                if (targetPkg.isNullOrBlank() && !query.isNullOrBlank()) {
                    val found = findApp(query)
                    if (found.isNotEmpty()) {
                        targetPkg = found.first().packageName
                    }
                }
                if (targetPkg.isNullOrBlank()) {
                    return "ERROR: APP_NOT_FOUND - Target package name or app name is required to launch"
                }
                launchApp(targetPkg)
            }
            else -> "ERROR: Unknown operation '$operation'. Supported: list, find, info, launch"
        }
    }

    fun listInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager ?: return emptyList()
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
            val label = resolveInfo.loadLabel(pm).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppInfo(
                appName = label,
                packageName = pkg,
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    fun findApp(query: String): List<InstalledAppInfo> {
        val allApps = listInstalledApps()
        val q = query.trim().lowercase()
        return allApps.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    fun getAppInfo(packageName: String): InstalledAppInfo? {
        val pm = context.packageManager ?: return null
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val pkgInfo = try { pm.getPackageInfo(packageName, 0) } catch (e: Exception) { null }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppInfo(
                appName = label,
                packageName = packageName,
                isSystemApp = isSystem,
                versionName = pkgInfo?.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Also check by searching launcher apps
            findApp(packageName).firstOrNull()
        }
    }

    fun launchApp(packageName: String): String {
        val pm = context.packageManager ?: return "ERROR: CAPABILITY_NOT_AVAILABLE - PackageManager not available"
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return "ERROR: APP_NOT_FOUND - Application '$packageName' is not installed or has no launchable Activity"

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "SUCCESS: Launched application '$packageName'"
        } catch (e: Exception) {
            "ERROR: ACTION_FAILED - Failed to launch '$packageName': ${e.message ?: "Unknown error"}"
        }
    }
}
