package com.example.data.files

import com.example.domain.model.Project
import com.example.domain.model.ProjectFile
import com.example.domain.repository.ProjectFileService
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class MockProjectFileService : ProjectFileService {
    // Local in-memory file system for interactive editing in Code Editor
    private val projectFilesMap = MutableStateFlow<Map<String, List<ProjectFile>>>(
        mapOf(
            "proj-1" to listOf(
                ProjectFile(
                    path = "app",
                    name = "app",
                    isDirectory = true,
                    children = listOf(
                        ProjectFile(
                            path = "app/src",
                            name = "src",
                            isDirectory = true,
                            children = listOf(
                                ProjectFile(
                                    path = "app/src/main",
                                    name = "main",
                                    isDirectory = true,
                                    children = listOf(
                                        ProjectFile(
                                            path = "app/src/main/AndroidManifest.xml",
                                            name = "AndroidManifest.xml",
                                            isDirectory = false,
                                            content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.MyApplication">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
                                        ),
                                        ProjectFile(
                                            path = "app/src/main/java",
                                            name = "java",
                                            isDirectory = true,
                                            children = listOf(
                                                ProjectFile(
                                                    path = "app/src/main/java/com",
                                                    name = "com",
                                                    isDirectory = true,
                                                    children = listOf(
                                                        ProjectFile(
                                                            path = "app/src/main/java/com/example",
                                                            name = "example",
                                                            isDirectory = true,
                                                            children = listOf(
                                                                ProjectFile(
                                                                    path = "app/src/main/java/com/example/MainActivity.kt",
                                                                    name = "MainActivity.kt",
                                                                    isDirectory = false,
                                                                    content = """package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Autonomous App workspace shell
            }
        }
    }
}"""
                                                                ),
                                                                ProjectFile(
                                                                    path = "app/src/main/java/com/example/ui",
                                                                    name = "ui",
                                                                    isDirectory = true,
                                                                    children = listOf(
                                                                        ProjectFile(
                                                                            path = "app/src/main/java/com/example/ui/theme",
                                                                            name = "theme",
                                                                            isDirectory = true,
                                                                            children = listOf(
                                                                                ProjectFile(
                                                                                    path = "app/src/main/java/com/example/ui/theme/Color.kt",
                                                                                    name = "Color.kt",
                                                                                    isDirectory = false,
                                                                                    content = """package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val FuchsiaPrimary = Color(0xFFFF2A85)
val MauveSecondary = Color(0xFF7E57C2)"""
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ProjectFile(
                            path = "app/build.gradle.kts",
                            name = "build.gradle.kts",
                            isDirectory = false,
                            content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example"
    compileSdk = 36
}"""
                        )
                    )
                )
            ),
            "proj-2" to listOf(
                ProjectFile(
                    path = "src",
                    name = "src",
                    isDirectory = true,
                    children = listOf(
                        ProjectFile(
                            path = "src/main",
                            name = "main",
                            isDirectory = true,
                            children = listOf(
                                ProjectFile(
                                    path = "src/main/kotlin",
                                    name = "kotlin",
                                    isDirectory = true,
                                    children = listOf(
                                        ProjectFile(
                                            path = "src/main/kotlin/Application.kt",
                                            name = "Application.kt",
                                            isDirectory = false,
                                            content = """package com.commerce

fun main() {
    println("Starting high-throughput microservices gateway...")
}"""
                                        ),
                                        ProjectFile(
                                            path = "src/main/kotlin/OrderController.kt",
                                            name = "OrderController.kt",
                                            isDirectory = false,
                                            content = """package com.commerce.controller

class OrderController {
    fun createOrder(orderId: String) {
        println("Processing transaction for ID: ${'$'}orderId")
    }
}"""
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                ProjectFile(
                    path = "build.gradle.kts",
                    name = "build.gradle.kts",
                    isDirectory = false,
                    content = """plugins {
    kotlin("jvm") version "2.2.10"
}"""
                )
            ),
            "proj-3" to listOf(
                ProjectFile(
                    path = "src",
                    name = "src",
                    isDirectory = true,
                    children = listOf(
                        ProjectFile(
                            path = "src/pages",
                            name = "pages",
                            isDirectory = true,
                            children = listOf(
                                ProjectFile(
                                    path = "src/pages/index.tsx",
                                    name = "index.tsx",
                                    isDirectory = false,
                                    content = """import React from 'react';

export default function Dashboard() {
  return (
    <div className="min-h-screen bg-slate-900 text-white p-8">
      <h1 className="text-3xl font-bold">Nemrawy Dashboard</h1>
    </div>
  );
}"""
                                )
                            )
                        )
                    )
                ),
                ProjectFile(
                    path = "package.json",
                    name = "package.json",
                    isDirectory = false,
                    content = """{
  "name": "dashboard-client",
  "version": "1.0.0",
  "dependencies": {
    "react": "^18.2.0",
    "tailwindcss": "^3.3.0"
  }
}"""
                )
            )
        )
    )

    override suspend fun getProjectFiles(project: Project): List<ProjectFile> {
        return projectFilesMap.value[project.id] ?: emptyList()
    }

    override suspend fun getFileContent(project: Project, path: String): String {
        return findFileByPath(projectFilesMap.value[project.id] ?: emptyList(), path)?.content ?: ""
    }

    override suspend fun saveFileContent(project: Project, path: String, content: String) {
        val projectList = projectFilesMap.value[project.id] ?: return
        val updatedList = updateFileContentInList(projectList, path, content)
        val newMap = projectFilesMap.value.toMutableMap()
        newMap[project.id] = updatedList
        projectFilesMap.value = newMap
    }

    override suspend fun createNewFile(project: Project, parentPath: String, name: String, isDirectory: Boolean) {
        val projectList = projectFilesMap.value[project.id] ?: emptyList()
        val newPath = if (parentPath.isEmpty()) name else "$parentPath/$name"
        val newFile = ProjectFile(
            path = newPath,
            name = name,
            isDirectory = isDirectory,
            content = if (isDirectory) "" else "// New file created under $newPath"
        )
        val updatedList = if (parentPath.isEmpty()) {
            projectList + newFile
        } else {
            insertFileIntoParent(projectList, parentPath, newFile)
        }
        val newMap = projectFilesMap.value.toMutableMap()
        newMap[project.id] = updatedList
        projectFilesMap.value = newMap
    }

    override suspend fun deleteFile(project: Project, path: String) {
        val projectList = projectFilesMap.value[project.id] ?: return
        val updatedList = removeFileFromList(projectList, path)
        val newMap = projectFilesMap.value.toMutableMap()
        newMap[project.id] = updatedList
        projectFilesMap.value = newMap
    }

    // Helper functions to traverse recursive file list tree
    private fun findFileByPath(list: List<ProjectFile>, path: String): ProjectFile? {
        for (file in list) {
            if (file.path == path) return file
            if (file.isDirectory) {
                val found = findFileByPath(file.children, path)
                if (found != null) return found
            }
        }
        return null
    }

    private fun updateFileContentInList(list: List<ProjectFile>, path: String, content: String): List<ProjectFile> {
        return list.map { file ->
            if (file.path == path) {
                file.copy(content = content)
            } else if (file.isDirectory) {
                file.copy(children = updateFileContentInList(file.children, path, content))
            } else {
                file
            }
        }
    }

    private fun insertFileIntoParent(list: List<ProjectFile>, parentPath: String, newFile: ProjectFile): List<ProjectFile> {
        return list.map { file ->
            if (file.path == parentPath && file.isDirectory) {
                file.copy(children = file.children + newFile)
            } else if (file.isDirectory) {
                file.copy(children = insertFileIntoParent(file.children, parentPath, newFile))
            } else {
                file
            }
        }
    }

    private fun removeFileFromList(list: List<ProjectFile>, path: String): List<ProjectFile> {
        return list.filter { it.path != path }.map { file ->
            if (file.isDirectory) {
                file.copy(children = removeFileFromList(file.children, path))
            } else {
                file
            }
        }
    }
}
