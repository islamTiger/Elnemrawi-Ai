package com.example.core.di

import android.content.Context
import com.example.data.ai.MockAiProvider
import com.example.data.cloudflare.MockCloudflareService
import com.example.data.files.MockProjectFileService
import com.example.data.github.MockGitService
import com.example.data.projects.MockProjectRepository
import com.example.data.supabase.MockSupabaseService
import com.example.data.terminal.MockBuildService
import com.example.data.terminal.MockCommandExecutor
import com.example.data.workspace.WorkspaceManagerImpl
import com.example.data.build.BuildSystemImpl
import com.example.data.agent.AgentPlannerImpl
import com.example.data.agent.AgentExecutorImpl
import com.example.data.agent.AgentEngineImpl
import com.example.data.tools.FileToolImpl
import com.example.data.tools.ProjectToolImpl
import com.example.data.tools.BuildToolImpl
import com.example.data.tools.GitToolImpl
import com.example.data.ai.runtime.*
import com.example.domain.ai.*
import com.example.domain.repository.*
import com.example.domain.workspace.Workspace
import com.example.domain.workspace.WorkspaceManager
import com.example.domain.build.BuildSystem
import com.example.domain.agent.AgentEngine
import com.example.domain.model.Project

object ServiceLocator {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    val context: Context
        get() = applicationContext ?: throw IllegalStateException("ServiceLocator context is not initialized. Please call init(context) first.")

    val aiProvider: AiProvider by lazy { MockAiProvider() }
    val projectRepository: ProjectRepository by lazy { MockProjectRepository() }
    val projectFileService: ProjectFileService by lazy { MockProjectFileService() }
    val gitService: GitService by lazy { MockGitService() }
    val commandExecutor: CommandExecutor by lazy { MockCommandExecutor() }
    val buildService: BuildService by lazy { MockBuildService() }
    val supabaseService: SupabaseService by lazy { MockSupabaseService() }
    val cloudflareService: CloudflareService by lazy { MockCloudflareService() }

    // Real Local Model Manager, Repos, Engines, and Runtime
    val localModelRepository: LocalModelRepository by lazy {
        SharedPreferencesLocalModelRepository(context)
    }

    val localModelManager: LocalModelManager by lazy {
        LocalModelManagerImpl(context, localModelRepository)
    }

    val llamaCppEngine: LlamaCppEngine by lazy {
        LlamaCppEngineImpl()
    }

    val openAiCompatibleConfig by lazy { OpenAiCompatibleConfig(baseUrl = null) }
    val openAiCompatibleRuntime by lazy { OpenAiCompatibleRuntime(openAiCompatibleConfig) }

    val localModelRuntime by lazy { LocalModelRuntime() }

    val routingAiRuntime: AiRuntime by lazy {
        RoutingAiRuntime(localModelRuntime, openAiCompatibleRuntime)
    }

    val modelSelector: ModelSelector by lazy {
        ModelSelectorImpl()
    }

    // Modern Workspace, Tools and Build Systems Core
    val workspaceManager: WorkspaceManager by lazy {
        WorkspaceManagerImpl(projectFileService)
    }

    val buildSystem: BuildSystem by lazy {
        BuildSystemImpl(buildService)
    }

    // Active tools wrapping project contexts
    val fileTool: com.example.domain.tools.FileTool by lazy {
        FileToolImpl(projectFileService, projectRepository)
    }

    val projectTool: com.example.domain.tools.ProjectTool by lazy {
        ProjectToolImpl(projectRepository)
    }

    val buildTool: com.example.domain.tools.BuildTool by lazy {
        BuildToolImpl(buildSystem)
    }

    val gitTool: com.example.domain.tools.GitTool by lazy {
        GitToolImpl(gitService)
    }

    private val agentPlanner by lazy { AgentPlannerImpl() }
    
    private val agentExecutor by lazy {
        AgentExecutorImpl(
            fileTool,
            projectTool,
            buildTool,
            gitTool,
            modelSelector,
            routingAiRuntime,
            devFallbackEnabled = true // Allow mock dev fallback simulation on agent workflow
        )
    }

    private val defaultProject = Project(
        id = "proj-1",
        name = "Nemrawy Code AI Core",
        localStatus = true,
        gitHubStatus = true,
        lastModified = "Just now",
        language = "Kotlin",
        description = "Main codebase of the Nemrawy autonomous coding agent and developer environment.",
        repositoryUrl = "https://github.com/nemrawy/codeai-core"
    )

    private val defaultWorkspace = Workspace(
        project = defaultProject,
        rootPath = "https://github.com/nemrawy/codeai-core",
        files = emptyList(),
        activeFile = null
    )

    val agentEngine: AgentEngine by lazy {
        AgentEngineImpl(agentPlanner, agentExecutor, defaultWorkspace, modelSelector)
    }
}
