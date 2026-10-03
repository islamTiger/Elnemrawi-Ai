package com.example.data.terminal

import com.example.domain.repository.CommandExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RealCommandExecutor : CommandExecutor {
    private val workspaceRoot = File("/")

    override fun executeCommand(command: String): Flow<String> = flow {
        val sanitized = command.trim()
        
        // Security restriction: allow only safe command prefixes
        val isSafe = sanitized.startsWith("ls") || 
                     sanitized.startsWith("grep") || 
                     sanitized.startsWith("git") || 
                     sanitized.startsWith("gradle")
        
        if (!isSafe) {
            emit("ERROR: SECURITY_VIOLATION - Command execution blocked. Command not allowed.")
            return@flow
        }

        try {
            val parts = sanitized.split("\\s+".toRegex())
            val pb = ProcessBuilder(parts)
            pb.directory(workspaceRoot)
            pb.redirectErrorStream(true)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
            }
            process.waitFor()
        } catch (e: Exception) {
            emit("ERROR: Exception executing command: ${e.message}")
        }
    }
}
