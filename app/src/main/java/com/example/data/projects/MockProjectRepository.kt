package com.example.data.projects

import com.example.domain.model.Project
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MockProjectRepository : ProjectRepository {
    private val _projects = MutableStateFlow<List<Project>>(listOf(
        Project(
            id = "proj-1",
            name = "Nemrawy Code AI Core",
            localStatus = true,
            gitHubStatus = true,
            lastModified = "Just now",
            language = "Kotlin",
            description = "Main codebase of the Nemrawy autonomous coding agent and developer environment.",
            repositoryUrl = "https://github.com/nemrawy/codeai-core"
        ),
        Project(
            id = "proj-2",
            name = "Smart Commerce backend",
            localStatus = true,
            gitHubStatus = false,
            lastModified = "2 days ago",
            language = "Kotlin",
            description = "High-throughput microservices gateway implementing transactional APIs and database mappings."
        ),
        Project(
            id = "proj-3",
            name = "Responsive Dashboard Client",
            localStatus = false,
            gitHubStatus = true,
            lastModified = "1 week ago",
            language = "TypeScript",
            description = "Next.js visual client representing real-time telemetry pipelines and user permission portals.",
            repositoryUrl = "https://github.com/nemrawy/dashboard-nextjs"
        )
    ))

    override fun getProjects(): Flow<List<Project>> = _projects.asStateFlow()

    override suspend fun getProjectById(id: String): Project? {
        return _projects.value.find { it.id == id }
    }

    override suspend fun createProject(name: String, language: String, description: String): Project {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val newProject = Project(
            name = name,
            language = language,
            description = description,
            localStatus = true,
            gitHubStatus = false,
            lastModified = dateFormat.format(Date())
        )
        _projects.value = _projects.value + newProject
        return newProject
    }

    override suspend fun deleteProject(id: String) {
        _projects.value = _projects.value.filter { it.id != id }
    }
}
