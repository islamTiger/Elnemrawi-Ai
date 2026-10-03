package com.example.data.device.tools

import com.example.domain.tools.CodeTool
import com.example.domain.model.ProjectSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class CodeToolImpl : CodeTool {
    override val id: String = "code_tool"
    override val name: String = "Autonomous Code Generator & Intelligence"
    override val description: String = "Statically analyze codebase architecture, modify Kotlin & Compose files, trigger compile checks, and perform automated code self-healing."
    override val inputSchema: Map<String, String> = mapOf(
        "operation" to "String (analyze | readCode | writeCode | deleteCode | searchCode | findSymbols | analyzeImports | analyzeReferences | runBuild)",
        "filePath" to "String (File path relative to workspace, optional)",
        "code" to "String (Source code content to write, optional)",
        "query" to "String (Search query or symbol name, optional)",
        "symbolName" to "String (Symbol name to search references, optional)"
    )

    private val workspaceRoot = File("/")

    override suspend fun execute(arguments: Map<String, Any>): String {
        val op = arguments["operation"]?.toString() ?: "analyze"
        val filePathParam = arguments["filePath"]?.toString() ?: ""
        val codeParam = arguments["code"]?.toString() ?: ""
        val queryParam = arguments["query"]?.toString() ?: ""
        val symbolNameParam = arguments["symbolName"]?.toString() ?: ""

        return try {
            when (op) {
                "analyze" -> performAnalyze()
                "readCode" -> performReadCode(filePathParam)
                "writeCode" -> performWriteCode(filePathParam, codeParam)
                "deleteCode" -> performDeleteCode(filePathParam)
                "searchCode" -> performSearchCode(queryParam)
                "findSymbols" -> performFindSymbols(queryParam)
                "analyzeImports" -> performAnalyzeImports(filePathParam)
                "analyzeReferences" -> performAnalyzeReferences(symbolNameParam)
                "runBuild" -> performRunBuild()
                else -> "ERROR: Unknown operation '$op'"
            }
        } catch (e: Exception) {
            "ERROR: Exception executing operation '$op': ${e.message}"
        }
    }

    private fun performAnalyze(): String {
        val appDir = File(workspaceRoot, "app")
        val sourceFiles = mutableListOf<String>()
        val testFiles = mutableListOf<String>()
        val resourceFiles = mutableListOf<String>()
        val gradleFiles = mutableListOf<String>()
        val manifestFiles = mutableListOf<String>()
        val modules = mutableListOf<String>()

        if (appDir.exists() && appDir.isDirectory) {
            modules.add("app")
            scanFilesRecursively(appDir, sourceFiles, testFiles, resourceFiles, gradleFiles, manifestFiles)
        }

        // Check top-level gradle files
        val topBuildGradle = File(workspaceRoot, "build.gradle.kts")
        if (topBuildGradle.exists()) gradleFiles.add("build.gradle.kts")
        val settingsGradle = File(workspaceRoot, "settings.gradle.kts")
        if (settingsGradle.exists()) gradleFiles.add("settings.gradle.kts")

        // Parse dependencies from build.gradle.kts
        val dependencies = parseDependencies()

        // Check Git State
        val gitDir = File(workspaceRoot, ".git")
        val gitState = if (gitDir.exists()) "Initialized. Ready." else "Uninitialized."

        // Detect Architecture
        val detectedArch = detectArchitecture()

        val snapshot = ProjectSnapshot(
            projectPath = workspaceRoot.absolutePath,
            modules = modules,
            sourceFiles = sourceFiles,
            testFiles = testFiles,
            resourceFiles = resourceFiles,
            gradleFiles = gradleFiles,
            manifestFiles = manifestFiles,
            dependencies = dependencies,
            buildVariants = listOf("debug", "release"),
            gitState = gitState,
            detectedArchitecture = detectedArch,
            inspectionTimestamp = System.currentTimeMillis()
        )

        // Serialize to JSON using JSONObject to meet structured requirement cleanly
        val json = JSONObject().apply {
            put("projectPath", snapshot.projectPath)
            put("modules", JSONArray(snapshot.modules))
            put("sourceFiles", JSONArray(snapshot.sourceFiles))
            put("testFiles", JSONArray(snapshot.testFiles))
            put("resourceFiles", JSONArray(snapshot.resourceFiles))
            put("gradleFiles", JSONArray(snapshot.gradleFiles))
            put("manifestFiles", JSONArray(snapshot.manifestFiles))
            put("dependencies", JSONArray(snapshot.dependencies))
            put("buildVariants", JSONArray(snapshot.buildVariants))
            put("gitState", snapshot.gitState)
            put("detectedArchitecture", snapshot.detectedArchitecture)
            put("inspectionTimestamp", snapshot.inspectionTimestamp)
        }

        return "SUCCESS: Project Inspection Complete.\nProjectSnapshot:\n${json.toString(2)}"
    }

    private fun scanFilesRecursively(
        file: File,
        source: MutableList<String>,
        test: MutableList<String>,
        res: MutableList<String>,
        gradle: MutableList<String>,
        manifest: MutableList<String>
    ) {
        val name = file.name
        if (file.isDirectory) {
            if (name == "build" || name == ".gradle" || name == ".git" || name == "node_modules") return
            file.listFiles()?.forEach { scanFilesRecursively(it, source, test, res, gradle, manifest) }
        } else {
            val relativePath = file.relativeTo(workspaceRoot).path
            when {
                name == "AndroidManifest.xml" -> manifest.add(relativePath)
                name.endsWith(".gradle.kts") || name.endsWith(".gradle") -> gradle.add(relativePath)
                relativePath.contains("src/test/") || relativePath.contains("src/androidTest/") -> {
                    if (name.endsWith(".kt") || name.endsWith(".java")) {
                        test.add(relativePath)
                    }
                }
                relativePath.contains("src/main/res/") -> res.add(relativePath)
                relativePath.contains("src/main/java/") || relativePath.contains("src/main/kotlin/") -> {
                    if (name.endsWith(".kt") || name.endsWith(".java")) {
                        source.add(relativePath)
                    }
                }
            }
        }
    }

    private fun parseDependencies(): List<String> {
        val deps = mutableListOf<String>()
        val buildGradle = File(workspaceRoot, "app/build.gradle.kts")
        if (buildGradle.exists() && buildGradle.isFile) {
            buildGradle.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("implementation") || trimmed.startsWith("api") || trimmed.startsWith("ksp") || trimmed.startsWith("kapt")) {
                    val dep = trimmed.substringAfter("(").substringBefore(")").replace("\"", "").replace("'", "")
                    if (dep.isNotEmpty()) deps.add(dep)
                }
            }
        }
        return deps
    }

    private fun detectArchitecture(): String {
        val pkgDir = File(workspaceRoot, "app/src/main/java/com/example")
        if (pkgDir.exists() && pkgDir.isDirectory) {
            val files = pkgDir.listFiles() ?: emptyArray()
            val subDirs = files.filter { it.isDirectory }.map { it.name }
            if (subDirs.contains("domain") && subDirs.contains("data") && subDirs.contains("ui")) {
                return "Clean Architecture / MVVM with Jetpack Compose"
            }
        }
        return "Standard Android Project Structure"
    }

    private fun performReadCode(filePath: String): String {
        if (filePath.isEmpty()) return "ERROR: filePath is required."
        val file = resolveFile(filePath)
        if (!file.exists() || !file.isFile) {
            return "ERROR: File '$filePath' does not exist."
        }
        val content = file.readText()
        return "SUCCESS: Read $filePath (${content.length} bytes):\n$content"
    }

    private fun performWriteCode(filePath: String, code: String): String {
        if (filePath.isEmpty()) return "ERROR: filePath is required."
        val file = resolveFile(filePath)
        file.parentFile?.mkdirs()
        file.writeText(code)
        return "SUCCESS: Wrote code to '$filePath'."
    }

    private fun performDeleteCode(filePath: String): String {
        if (filePath.isEmpty()) return "ERROR: filePath is required."
        val file = resolveFile(filePath)
        if (!file.exists()) return "ERROR: File '$filePath' does not exist."
        if (file.delete()) {
            return "SUCCESS: Deleted '$filePath'."
        }
        return "ERROR: Failed to delete '$filePath'."
    }

    private fun performSearchCode(query: String): String {
        if (query.isEmpty()) return "ERROR: query parameter is required for searchCode."
        val matchedLines = mutableListOf<String>()
        val appDir = File(workspaceRoot, "app/src/main")
        if (appDir.exists() && appDir.isDirectory) {
            searchFilesText(appDir, query, matchedLines)
        }
        return if (matchedLines.isEmpty()) {
            "SUCCESS: No matches found for query '$query'."
        } else {
            "SUCCESS: Found matches for query '$query':\n" + matchedLines.joinToString("\n")
        }
    }

    private fun searchFilesText(file: File, query: String, results: MutableList<String>) {
        if (file.isDirectory) {
            val name = file.name
            if (name == "build" || name == ".gradle" || name == ".git" || name == "node_modules") return
            file.listFiles()?.forEach { searchFilesText(it, query, results) }
        } else {
            if (file.name.endsWith(".kt") || file.name.endsWith(".java") || file.name.endsWith(".xml") || file.name.endsWith(".kts")) {
                var lineNum = 1
                file.forEachLine { line ->
                    if (line.contains(query, ignoreCase = true)) {
                        val relPath = file.relativeTo(workspaceRoot).path
                        results.add("[$relPath:$lineNum]: ${line.trim()}")
                    }
                    lineNum++
                }
            }
        }
    }

    private fun performFindSymbols(query: String): String {
        val matchedSymbols = mutableListOf<String>()
        val appDir = File(workspaceRoot, "app/src/main")
        if (appDir.exists() && appDir.isDirectory) {
            findSymbolsInDir(appDir, query, matchedSymbols)
        }
        return if (matchedSymbols.isEmpty()) {
            "SUCCESS: No symbols found."
        } else {
            "SUCCESS: Found symbols:\n" + matchedSymbols.joinToString("\n")
        }
    }

    private fun findSymbolsInDir(file: File, query: String, results: MutableList<String>) {
        if (file.isDirectory) {
            val name = file.name
            if (name == "build" || name == ".gradle" || name == ".git") return
            file.listFiles()?.forEach { findSymbolsInDir(it, query, results) }
        } else {
            if (file.name.endsWith(".kt") || file.name.endsWith(".java")) {
                val relPath = file.relativeTo(workspaceRoot).path
                file.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("class ") || trimmed.startsWith("interface ") || trimmed.startsWith("fun ") || trimmed.startsWith("sealed class ") || trimmed.startsWith("data class ") || trimmed.startsWith("object ")) {
                        if (query.isEmpty() || trimmed.contains(query, ignoreCase = true)) {
                            results.add("[$relPath]: $trimmed")
                        }
                    }
                }
            }
        }
    }

    private fun performAnalyzeImports(filePath: String): String {
        if (filePath.isEmpty()) return "ERROR: filePath is required for analyzeImports."
        val file = resolveFile(filePath)
        if (!file.exists() || !file.isFile) return "ERROR: File '$filePath' does not exist."

        val imports = mutableListOf<String>()
        var packageName = ""
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("package ")) {
                packageName = trimmed
            } else if (trimmed.startsWith("import ")) {
                imports.add(trimmed)
            }
        }
        return "SUCCESS: Analyzed imports for '$filePath'.\nPackage: $packageName\nImports:\n" + imports.joinToString("\n")
    }

    private fun performAnalyzeReferences(symbolName: String): String {
        if (symbolName.isEmpty()) return "ERROR: symbolName is required for analyzeReferences."
        return performSearchCode(symbolName)
    }

    private fun performRunBuild(): String {
        return try {
            val pb = ProcessBuilder("gradle", ":app:compileDebugKotlin")
            pb.directory(workspaceRoot)
            pb.redirectErrorStream(true)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logsBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logsBuilder.append(line).append("\n")
            }

            val exited = process.waitFor(60, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                return "ERROR: Build timed out after 60 seconds."
            }

            val exitCode = process.exitValue()
            val logs = logsBuilder.toString()

            if (exitCode == 0) {
                "SUCCESS: gradle :app:compileDebugKotlin succeeded.\nLogs:\n$logs"
            } else {
                // Parse compilation errors
                val errors = parseCompilerErrors(logs)
                val jsonErrors = JSONArray()
                errors.forEach { err ->
                    jsonErrors.put(JSONObject().apply {
                        put("file", err.file)
                        put("line", err.line)
                        put("message", err.message)
                    })
                }
                "ERROR: Build failed with exit code $exitCode.\nParsed Compile Errors:\n${jsonErrors.toString(2)}\n\nLogs:\n$logs"
            }
        } catch (e: Exception) {
            "ERROR: Failed to trigger build: ${e.message}"
        }
    }

    private fun parseCompilerErrors(logs: String): List<ParsedCompileError> {
        val list = mutableListOf<ParsedCompileError>()
        // Match e: /path/to/file.kt: (line, col): error message
        val regex = Regex("""e: (.*?): \((.*?),(.*?)\): (.*)""")
        regex.findAll(logs).forEach { match ->
            val path = match.groupValues[1]
            val lineNum = match.groupValues[2].toIntOrNull() ?: 1
            val errMsg = match.groupValues[4]
            list.add(ParsedCompileError(path, lineNum, errMsg))
        }
        return list
    }

    private fun resolveFile(filePath: String): File {
        return if (filePath.startsWith("/")) {
            File(filePath)
        } else {
            File(workspaceRoot, filePath)
        }
    }

    data class ParsedCompileError(
        val file: String,
        val line: Int,
        val message: String
    )
}
