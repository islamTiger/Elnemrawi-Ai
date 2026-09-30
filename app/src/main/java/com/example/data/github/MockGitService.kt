package com.example.data.github

import com.example.domain.model.Project
import com.example.domain.repository.GitService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class MockGitService : GitService {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val _connectedAccountName = MutableStateFlow<String?>(null)
    override val connectedAccountName: Flow<String?> = _connectedAccountName.asStateFlow()

    private val mockGitRepos = listOf(
        Project(
            name = "android-calculator-agent",
            language = "Kotlin",
            description = "A standard material calculator illustrating voice agent actions.",
            repositoryUrl = "https://github.com/nemrawy/android-calculator-agent"
        ),
        Project(
            name = "cloudflare-kv-sync",
            language = "TypeScript",
            description = "Syncing local database stores directly with Cloudflare KV storage rules.",
            repositoryUrl = "https://github.com/nemrawy/cloudflare-kv-sync"
        ),
        Project(
            name = "supabase-compose-auth",
            language = "Kotlin",
            description = "An easy-to-use template wrapping Supabase Auth with Jetpack Compose.",
            repositoryUrl = "https://github.com/nemrawy/supabase-compose-auth"
        )
    )

    override fun getRepositories(): Flow<List<Project>> = flow {
        if (_isConnected.value) {
            emit(mockGitRepos)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun connectAccount(token: String): Boolean {
        // TODO: Real OAuth2 or Personal Access Token (PAT) authentication integration
        if (token.isNotBlank() && token.startsWith("ghp_")) {
            _isConnected.value = true
            _connectedAccountName.value = "nemrawy"
            return true
        }
        return false
    }

    override suspend fun disconnectAccount() {
        _isConnected.value = false
        _connectedAccountName.value = null
    }

    override suspend fun cloneRepository(url: String): Project {
        // TODO: Real Git clone command execution using SSH/HTTPS
        val name = url.substringAfterLast("/").substringBefore(".git")
        return Project(
            name = name,
            language = "Kotlin",
            description = "Cloned repository from $url",
            localStatus = true,
            gitHubStatus = true,
            lastModified = "Just now",
            repositoryUrl = url
        )
    }

    override suspend fun pull(project: Project): Boolean {
        // TODO: Real git pull execution
        return _isConnected.value
    }

    override suspend fun commitAndPush(project: Project, message: String): Boolean {
        // TODO: Real git commit and git push execution
        return _isConnected.value
    }
}
