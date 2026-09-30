package com.example.data.terminal

import com.example.domain.model.Project
import com.example.domain.repository.BuildError
import com.example.domain.repository.BuildResult
import com.example.domain.repository.BuildService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockBuildService : BuildService {
    override suspend fun runBuild(project: Project): Flow<BuildResult> = flow {
        emit(BuildResult(false, "Starting Gradle daemon...\n> Configuring project :app\n"))
        delay(600)
        
        emit(BuildResult(false, "Starting Gradle daemon...\n> Configuring project :app\n> Task :app:compileDebugKotlin\n"))
        delay(1000)

        // Return a realistic compiler error that our autonomous coding agent could later inspect and fix!
        val errors = listOf(
            BuildError(
                file = "app/src/main/java/com/example/MainActivity.kt",
                line = 24,
                message = "Unresolved reference: enableEdgeToEdge. Ensure import statement is complete.",
                contextCode = "enableEdgeToEdge()\nsetContent {"
            )
        )
        
        val logs = """
            Starting Gradle daemon...
            > Configuring project :app
            > Task :app:compileDebugKotlin
            e: app/src/main/java/com/example/MainActivity.kt: (24, 9): Unresolved reference: enableEdgeToEdge.
            
            FAILURE: Build failed with an exception.
            * What went wrong:
            Execution failed for task ':app:compileDebugKotlin'.
            > A failure occurred while executing kotlin compiler execution service.
            
            BUILD FAILED in 2s
        """.trimIndent()
        
        emit(BuildResult(false, logs, errors))
    }
}
