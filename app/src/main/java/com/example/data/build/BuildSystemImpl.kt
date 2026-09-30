package com.example.data.build

import com.example.domain.build.BuildSystem
import com.example.domain.build.BuildRequest
import com.example.domain.build.BuildResult
import com.example.domain.build.BuildStatus
import com.example.domain.build.BuildError
import com.example.domain.repository.BuildService
import com.example.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class BuildSystemImpl(
    private val buildService: BuildService
) : BuildSystem {
    override fun compile(request: BuildRequest): Flow<BuildResult> {
        val dummyProject = Project(
            id = "proj-1",
            name = "Nemrawy Code AI Core",
            localStatus = true,
            gitHubStatus = false,
            lastModified = "Just now",
            language = "Kotlin",
            description = "Main codebase"
        )
        return flow {
            buildService.runBuild(dummyProject).collect { repoResult ->
                val status = if (repoResult.success) BuildStatus.BUILD_SUCCESS else BuildStatus.BUILD_FAILED
                val errors = repoResult.errors.map { err ->
                    BuildError(
                        file = err.file,
                        line = err.line,
                        message = err.message,
                        contextCode = err.contextCode
                    )
                }
                emit(
                    BuildResult(
                        status = status,
                        logs = repoResult.logs,
                        errors = errors
                    )
                )
            }
        }
    }
}
