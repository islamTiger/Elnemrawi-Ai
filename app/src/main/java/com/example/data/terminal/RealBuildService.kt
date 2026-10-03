package com.example.data.terminal

import com.example.domain.model.Project
import com.example.domain.repository.BuildError
import com.example.domain.repository.BuildResult
import com.example.domain.repository.BuildService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RealBuildService : BuildService {
    private val workspaceRoot = File("/")

    override suspend fun runBuild(project: Project): Flow<BuildResult> = flow {
        emit(BuildResult(false, "Starting real Gradle build pipeline...\n> Executing :app:compileDebugKotlin\n"))
        
        try {
            val pb = ProcessBuilder("gradle", ":app:compileDebugKotlin")
            pb.directory(workspaceRoot)
            pb.redirectErrorStream(true)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logsBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logsBuilder.append(line).append("\n")
                emit(BuildResult(false, logsBuilder.toString()))
            }

            val exited = process.waitFor(60, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                emit(BuildResult(false, "ERROR: Gradle process timed out.", emptyList()))
                return@flow
            }

            val exitCode = process.exitValue()
            val logs = logsBuilder.toString()

            if (exitCode == 0) {
                emit(BuildResult(true, "BUILD SUCCESSFUL\n\nLogs:\n$logs", emptyList()))
            } else {
                val errors = parseCompilerErrors(logs)
                emit(BuildResult(false, "BUILD FAILED (Exit Code: $exitCode)\n\nLogs:\n$logs", errors))
            }
        } catch (e: Exception) {
            emit(BuildResult(false, "ERROR: Exception starting Gradle process: ${e.message}", emptyList()))
        }
    }

    private fun parseCompilerErrors(logs: String): List<BuildError> {
        val list = mutableListOf<BuildError>()
        val regex = Regex("""e: (.*?): \((.*?),(.*?)\): (.*)""")
        regex.findAll(logs).forEach { match ->
            val path = match.groupValues[1]
            val lineNum = match.groupValues[2].toIntOrNull() ?: 1
            val errMsg = match.groupValues[4]
            list.add(BuildError(path, lineNum, errMsg, ""))
        }
        return list
    }
}
