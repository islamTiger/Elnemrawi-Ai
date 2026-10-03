package com.example.data.files

import com.example.domain.model.Project
import com.example.domain.model.ProjectFile
import com.example.domain.repository.ProjectFileService
import java.io.File

class RealProjectFileService : ProjectFileService {
    private val workspaceRoot = File("/")

    override suspend fun getProjectFiles(project: Project): List<ProjectFile> {
        val appDir = File(workspaceRoot, "app")
        if (!appDir.exists()) return emptyList()
        return scanDir(appDir)
    }

    private fun scanDir(dir: File): List<ProjectFile> {
        val list = mutableListOf<ProjectFile>()
        dir.listFiles()?.forEach { file ->
            val name = file.name
            if (file.isDirectory) {
                if (name == "build" || name == ".gradle" || name == ".git" || name == "node_modules") return@forEach
                list.add(
                    ProjectFile(
                        path = file.relativeTo(workspaceRoot).path,
                        name = name,
                        isDirectory = true,
                        children = scanDir(file)
                    )
                )
            } else {
                list.add(
                    ProjectFile(
                        path = file.relativeTo(workspaceRoot).path,
                        name = name,
                        isDirectory = false,
                        content = ""
                    )
                )
            }
        }
        return list
    }

    override suspend fun getFileContent(project: Project, path: String): String {
        val file = File(workspaceRoot, path)
        if (!file.exists() || !file.isFile) return ""
        return file.readText()
    }

    override suspend fun saveFileContent(project: Project, path: String, content: String) {
        val file = File(workspaceRoot, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    override suspend fun createNewFile(project: Project, parentPath: String, name: String, isDirectory: Boolean) {
        val parent = if (parentPath.isEmpty()) workspaceRoot else File(workspaceRoot, parentPath)
        val file = File(parent, name)
        if (isDirectory) {
            file.mkdirs()
        } else {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    override suspend fun deleteFile(project: Project, path: String) {
        val file = File(workspaceRoot, path)
        if (file.exists()) {
            file.deleteRecursively()
        }
    }
}
