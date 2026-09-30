package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val providerName: String
    val isConfigured: Boolean
    suspend fun getAvailableModels(): List<ModelInfo>
    suspend fun generateResponse(prompt: String, modelId: String): Flow<String>
    suspend fun generateImage(prompt: String, modelId: String): String
    suspend fun editImage(imagePath: String, prompt: String, modelId: String): String
}

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    suspend fun getProjectById(id: String): Project?
    suspend fun createProject(name: String, language: String, description: String): Project
    suspend fun deleteProject(id: String)
}

interface ProjectFileService {
    suspend fun getProjectFiles(project: Project): List<ProjectFile>
    suspend fun getFileContent(project: Project, path: String): String
    suspend fun saveFileContent(project: Project, path: String, content: String)
    suspend fun createNewFile(project: Project, parentPath: String, name: String, isDirectory: Boolean)
    suspend fun deleteFile(project: Project, path: String)
}

interface CommandExecutor {
    fun executeCommand(command: String): Flow<String>
}

interface GitService {
    val isConnected: Flow<Boolean>
    val connectedAccountName: Flow<String?>
    fun getRepositories(): Flow<List<Project>>
    suspend fun connectAccount(token: String): Boolean
    suspend fun disconnectAccount()
    suspend fun cloneRepository(url: String): Project
    suspend fun pull(project: Project): Boolean
    suspend fun commitAndPush(project: Project, message: String): Boolean
}

interface BuildService {
    suspend fun runBuild(project: Project): Flow<BuildResult>
}

data class BuildResult(
    val success: Boolean,
    val logs: String,
    val errors: List<BuildError> = emptyList()
)

data class BuildError(
    val file: String,
    val line: Int,
    val message: String,
    val contextCode: String
)



interface SupabaseService {
    val isConfigured: Boolean
    suspend fun syncDatabase()
    suspend fun getAuthSession()
    suspend fun uploadToStorage(bucket: String, path: String, bytes: ByteArray)
    suspend fun invokeEdgeFunction(functionName: String, payload: Map<String, Any>): String
}

interface CloudflareService {
    val isConfigured: Boolean
    suspend fun runWorker(workerName: String, params: Map<String, Any>): String
    suspend fun cachePut(key: String, value: String)
    suspend fun cacheGet(key: String): String?
}
