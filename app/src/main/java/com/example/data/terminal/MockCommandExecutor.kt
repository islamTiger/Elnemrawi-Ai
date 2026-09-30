package com.example.data.terminal

import com.example.domain.repository.CommandExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockCommandExecutor : CommandExecutor {
    override fun executeCommand(command: String): Flow<String> = flow {
        emit("$ ncode-agent run: $command\n")
        delay(300)
        
        when {
            command.contains("git status", ignoreCase = true) -> {
                emit("On branch main\nYour branch is up to date with 'origin/main'.\n\nChanges not staged for commit:\n  (use \"git add <file>...\" to update what will be committed)\n")
                emit("  modified:   app/src/main/java/com/example/MainActivity.kt\n")
                emit("\nno changes added to commit (use \"git add\" and/or \"git commit -a\")\n")
            }
            command.contains("gradle build", ignoreCase = true) || command.contains("compile", ignoreCase = true) -> {
                emit("> Configure project :app\n")
                delay(400)
                emit("> Task :app:preBuild UP-TO-DATE\n")
                emit("> Task :app:compileDebugKotlin\n")
                delay(600)
                emit("> Task :app:compileDebugJavaWithJavac\n")
                emit("> Task :app:assembleDebug\n")
                delay(300)
                emit("\nBUILD SUCCESSFUL in 1s\n7 actionable tasks: 3 executed, 4 up-to-date\n")
            }
            command.contains("lint", ignoreCase = true) -> {
                emit("Scanning app source code for guidelines compliance...\n")
                delay(500)
                emit("No high-severity syntax errors found.\n")
                emit("Warning: unused import in Color.kt (line 4)\n")
                emit("SUCCESS: Codebase matches Nemrawy M3 design standards.\n")
            }
            else -> {
                emit("Executing standard action...\n")
                delay(300)
                emit("Command executed successfully.\n")
            }
        }
    }
}
