package com.example.domain.build

import kotlinx.coroutines.flow.Flow

enum class BuildTarget {
    ANDROID_APK,
    GRADLE_PROJECT,
    WEB_PROJECT,
    NODE_PROJECT
}

enum class BuildStatus {
    BUILD_SUCCESS,
    BUILD_FAILED,
    COMPILE_ERROR
}

data class BuildError(
    val file: String,
    val line: Int,
    val message: String,
    val contextCode: String
)

data class BuildResult(
    val status: BuildStatus,
    val logs: String,
    val errors: List<BuildError>
)

data class BuildRequest(
    val target: BuildTarget
)

interface BuildSystem {
    fun compile(request: BuildRequest): Flow<BuildResult>
}
