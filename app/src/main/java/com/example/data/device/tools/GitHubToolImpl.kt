package com.example.data.device.tools

import android.util.Base64
import com.example.core.di.ServiceLocator
import com.example.data.security.SecretRedactor
import com.example.data.security.SecureCredentialVault
import com.example.domain.tools.GitHubTool
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.IOException

// Retrofit API Service Interface for GitHub REST API
interface GitHubApiService {
    @GET("user/repos")
    suspend fun listRepositories(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubRepoResponse>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepoResponse

    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<GitHubBranchResponse>

    @GET("repos/{owner}/{repo}/git/ref/{ref}")
    suspend fun getRef(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): GitHubRefResponse

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateRefRequest
    ): GitHubRefResponse

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): GitHubContentResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: CreateOrUpdateFileRequest
    ): CreateOrUpdateFileResponse

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePRRequest
    ): GitHubPRResponse

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubWorkflowRunsResponse

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): GitHubWorkflowRun

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun listWorkflowJobs(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): GitHubJobsResponse

    @GET("repos/{owner}/{repo}/actions/jobs/{job_id}/logs")
    suspend fun getJobLogs(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("job_id") jobId: Long
    ): ResponseBody

    @GET("user")
    suspend fun getCurrentUser(
        @Header("Authorization") auth: String
    ): GitHubUserResponse
}

// Structured Response/Request Models for Moshi
data class GitHubRepoResponse(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val html_url: String,
    val default_branch: String?,
    val owner: GitHubUserResponse
)

data class GitHubUserResponse(
    val login: String,
    val id: Long,
    val avatar_url: String?
)

data class GitHubBranchResponse(
    val name: String,
    val commit: GitHubBranchCommit
)

data class GitHubBranchCommit(
    val sha: String,
    val url: String
)

data class GitHubRefResponse(
    val ref: String,
    val node_id: String,
    val url: String,
    val `object`: GitHubRefObject
)

data class GitHubRefObject(
    val sha: String,
    val type: String,
    val url: String
)

data class CreateRefRequest(
    val ref: String,
    val sha: String
)

data class GitHubContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String,
    val content: String?,
    val encoding: String?,
    val download_url: String?
)

data class CreateOrUpdateFileRequest(
    val message: String,
    val content: String, // Base64 encoded
    val branch: String?,
    val sha: String? = null
)

data class CreateOrUpdateFileResponse(
    val content: GitHubContentResponse?,
    val commit: GitHubCommitInfo
)

data class GitHubCommitInfo(
    val sha: String,
    val html_url: String,
    val message: String
)

data class CreatePRRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String?
)

data class GitHubPRResponse(
    val id: Long,
    val number: Int,
    val html_url: String,
    val state: String,
    val title: String,
    val body: String?,
    val head: GitHubBranchInfo,
    val base: GitHubBranchInfo
)

data class GitHubBranchInfo(
    val label: String,
    val ref: String,
    val sha: String
)

data class GitHubWorkflowRunsResponse(
    val total_count: Int,
    val workflow_runs: List<GitHubWorkflowRun>
)

data class GitHubWorkflowRun(
    val id: Long,
    val name: String?,
    val run_number: Int,
    val status: String?,
    val conclusion: String?,
    val html_url: String,
    val created_at: String?
)

data class GitHubJobsResponse(
    val total_count: Int,
    val jobs: List<GitHubJob>
)

data class GitHubJob(
    val id: Long,
    val run_id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val html_url: String
)

class GitHubToolImpl : GitHubTool {
    override val id: String = "github_tool"
    override val name: String = "GitHub Autonomous Integration"
    override val description: String = "Manage branch creations, file staging, commits, pushes, and Pull Requests on GitHub repositories."
    override val inputSchema: Map<String, String> = mapOf(
        "operation" to "String (listRepos | getRepository | listBranches | createBranch | getFile | createOrUpdateFile | createPR | listWorkflowRuns | getWorkflowRun | getWorkflowLogs)",
        "repository" to "String (Full repo name, optional, e.g., owner/repo)",
        "owner" to "String (Owner name, optional)",
        "repo" to "String (Repo name, optional)",
        "branch" to "String (Branch name, optional)",
        "path" to "String (File/folder path, optional)",
        "content" to "String (File content, optional)",
        "commitMessage" to "String (Commit message, optional)",
        "title" to "String (PR title, optional)",
        "body" to "String (PR body, optional)",
        "head" to "String (PR head branch, optional)",
        "base" to "String (PR/Branch base branch, optional)",
        "runId" to "String (Workflow run ID, optional)"
    )

    private fun getApiService(): GitHubApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(GitHubApiService::class.java)
    }

    override suspend fun execute(arguments: Map<String, Any>): String {
        val vault = SecureCredentialVault(ServiceLocator.context)
        val token = vault.getCredential("github_token")

        if (token.isNullOrEmpty()) {
            return "ERROR: CAPABILITY_UNAVAILABLE (AUTH_REQUIRED) - GitHub API token is not configured in SecureCredentialVault. User must authenticate first via settings."
        }

        val authHeader = "token $token"
        val api = getApiService()

        val op = arguments["operation"]?.toString() ?: "listRepos"
        val repository = arguments["repository"]?.toString() ?: ""
        val ownerParam = arguments["owner"]?.toString() ?: ""
        val repoParam = arguments["repo"]?.toString() ?: ""
        val branch = arguments["branch"]?.toString() ?: ""
        val path = arguments["path"]?.toString() ?: ""
        val content = arguments["content"]?.toString() ?: ""
        val commitMessage = arguments["commitMessage"]?.toString() ?: arguments["message"]?.toString() ?: "feat: updated file content"
        val title = arguments["title"]?.toString() ?: ""
        val body = arguments["body"]?.toString() ?: ""
        val head = arguments["head"]?.toString() ?: ""
        val base = arguments["base"]?.toString() ?: "main"
        val runIdStr = arguments["runId"]?.toString() ?: ""

        return try {
            when (op) {
                "listRepos" -> {
                    val repos = api.listRepositories(authHeader)
                    val evidence = "Fetched ${repos.size} repositories. First few: ${repos.take(5).map { it.full_name }}"
                    val listText = repos.joinToString("\n") { "• ${it.full_name}: ${it.description ?: "No description"}" }
                    "SUCCESS: listRepositories\nEvidence: $evidence\nRepos:\n$listText"
                }
                "getRepository" -> {
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val repoRes = api.getRepository(authHeader, owner, repo)
                    val evidence = "Repository verified: ${repoRes.full_name} exists, default branch is ${repoRes.default_branch}"
                    "SUCCESS: getRepository\nEvidence: $evidence\nDetails: ID: ${repoRes.id}, HTML URL: ${repoRes.html_url}"
                }
                "listBranches" -> {
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val branches = api.listBranches(authHeader, owner, repo)
                    val evidence = "Branches verified: Total ${branches.size} branches found: ${branches.map { it.name }}"
                    val listText = branches.joinToString("\n") { "• ${it.name} (SHA: ${it.commit.sha})" }
                    "SUCCESS: listBranches\nEvidence: $evidence\nBranches:\n$listText"
                }
                "createBranch" -> {
                    if (branch.isEmpty()) return "ERROR: INVALID_REQUEST - Missing 'branch' argument."
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val baseBranch = base.ifEmpty { "main" }
                    
                    // 1. Get base branch SHA
                    val baseRef = api.getRef(authHeader, owner, repo, "heads/$baseBranch")
                    val baseSha = baseRef.`object`.sha

                    // 2. Create new branch ref
                    val newRefName = "refs/heads/$branch"
                    api.createRef(authHeader, owner, repo, CreateRefRequest(newRefName, baseSha))

                    // 3. VERIFICATION: Retrieve new ref and compare SHA
                    val verifiedRef = api.getRef(authHeader, owner, repo, "heads/$branch")
                    if (verifiedRef.`object`.sha == baseSha) {
                        val evidence = "Branch '$branch' successfully created from '$baseBranch' and verified on remote. SHA: ${verifiedRef.`object`.sha}"
                        "SUCCESS: createBranch\nEvidence: $evidence"
                    } else {
                        "ERROR: VERIFICATION_FAILED - Branch created but remote SHA mismatch. Expected $baseSha, got ${verifiedRef.`object`.sha}"
                    }
                }
                "getFile" -> {
                    if (path.isEmpty()) return "ERROR: INVALID_REQUEST - Missing 'path' argument."
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val refBranch = branch.ifEmpty { null }
                    val fileResponse = api.getFileContent(authHeader, owner, repo, path, refBranch)
                    val decodedContent = if (fileResponse.content != null) {
                        val cleanBase64 = fileResponse.content.replace("\n", "").replace("\r", "")
                        String(Base64.decode(cleanBase64, Base64.DEFAULT), Charsets.UTF_8)
                    } else {
                        ""
                    }
                    val evidence = "File '$path' successfully read from branch '${branch.ifEmpty { "default" }}'. Size: ${fileResponse.size} bytes, SHA: ${fileResponse.sha}"
                    "SUCCESS: getFile\nEvidence: $evidence\nContent:\n$decodedContent"
                }
                "createOrUpdateFile" -> {
                    if (path.isEmpty()) return "ERROR: INVALID_REQUEST - Missing 'path' argument."
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val refBranch = branch.ifEmpty { null }

                    // 1. Get existing file SHA if it exists
                    var existingSha: String? = null
                    try {
                        val existingFile = api.getFileContent(authHeader, owner, repo, path, refBranch)
                        existingSha = existingFile.sha
                    } catch (e: Exception) {
                        // If 404, file doesn't exist yet, which is fine
                    }

                    // 2. Base64 encode the new content
                    val base64Content = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

                    // 3. Create/update the file on GitHub
                    val request = CreateOrUpdateFileRequest(
                        message = commitMessage,
                        content = base64Content,
                        branch = refBranch,
                        sha = existingSha
                    )
                    val response = api.createOrUpdateFile(authHeader, owner, repo, path, request)

                    // 4. VERIFICATION: Read back file and verify contents & SHA
                    val verifiedFile = api.getFileContent(authHeader, owner, repo, path, refBranch)
                    val decodedVerifiedContent = if (verifiedFile.content != null) {
                        val cleanBase64 = verifiedFile.content.replace("\n", "").replace("\r", "")
                        String(Base64.decode(cleanBase64, Base64.DEFAULT), Charsets.UTF_8)
                    } else {
                        ""
                    }

                    if (verifiedFile.sha == response.content?.sha && decodedVerifiedContent == content) {
                        val evidence = "File '$path' successfully committed and pushed to remote branch '${branch.ifEmpty { "default" }}'. verified SHA: ${verifiedFile.sha}, commit SHA: ${response.commit.sha}, commit message: \"${response.commit.message}\""
                        "SUCCESS: createOrUpdateFile\nEvidence: $evidence\nCommit HTML URL: ${response.commit.html_url}"
                    } else {
                        "ERROR: VERIFICATION_FAILED - File committed but read-back check failed. SHA expected: ${response.content?.sha}, got: ${verifiedFile.sha}"
                    }
                }
                "createPR" -> {
                    if (head.isEmpty()) return "ERROR: INVALID_REQUEST - Missing 'head' branch argument."
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val request = CreatePRRequest(
                        title = title.ifEmpty { "Autonomous changes from Nemrawy AI" },
                        head = head,
                        base = base.ifEmpty { "main" },
                        body = body.ifEmpty { "Autonomous Pull Request generated by Nemrawy AI agent." }
                    )
                    val response = api.createPullRequest(authHeader, owner, repo, request)

                    // VERIFICATION: Check response status and details
                    if (response.id > 0 && response.state == "open") {
                        val evidence = "Pull Request successfully opened and verified on remote. PR Number: #${response.number}, State: ${response.state}, Head: ${response.head.ref}, Base: ${response.base.ref}"
                        "SUCCESS: createPR\nEvidence: $evidence\nPR HTML URL: ${response.html_url}"
                    } else {
                        "ERROR: VERIFICATION_FAILED - Pull Request was created but state is not open (State: ${response.state})."
                    }
                }
                "listWorkflowRuns" -> {
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val response = api.listWorkflowRuns(authHeader, owner, repo)
                    val runs = response.workflow_runs
                    val evidence = "Workflow runs fetched and verified on remote. Total found: ${response.total_count}. Active/Recent count: ${runs.size}"
                    val listText = runs.joinToString("\n") {
                        "• Run #${it.run_number} (${it.name ?: "Unknown"}): Status: ${it.status}, Conclusion: ${it.conclusion ?: "In progress"}, Created: ${it.created_at}"
                    }
                    "SUCCESS: listWorkflowRuns\nEvidence: $evidence\nRuns:\n$listText"
                }
                "getWorkflowRun" -> {
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val runId = runIdStr.toLongOrNull() ?: return "ERROR: INVALID_REQUEST - Missing or invalid runId"
                    val run = api.getWorkflowRun(authHeader, owner, repo, runId)
                    val evidence = "Workflow run #${run.run_number} retrieved and verified. Status: ${run.status}, Conclusion: ${run.conclusion ?: "Running"}"
                    "SUCCESS: getWorkflowRun\nEvidence: $evidence\nDetails: Name: ${run.name}, ID: ${run.id}, URL: ${run.html_url}"
                }
                "getWorkflowLogs" -> {
                    val (owner, repo) = resolveOwnerAndRepo(api, authHeader, repository, ownerParam, repoParam)
                    val runId = runIdStr.toLongOrNull() ?: return "ERROR: INVALID_REQUEST - Missing or invalid runId"

                    // 1. Get jobs for this run
                    val jobsResponse = api.listWorkflowJobs(authHeader, owner, repo, runId)
                    val firstJob = jobsResponse.jobs.firstOrNull() ?: return "ERROR: NOT_FOUND - No jobs found for workflow run $runId"

                    // 2. Fetch raw plain-text logs for the first job
                    val responseBody = api.getJobLogs(authHeader, owner, repo, firstJob.id)
                    val logText = responseBody.string()

                    val evidence = "Successfully fetched logs for job '${firstJob.name}' (ID: ${firstJob.id}) under run $runId. Log length: ${logText.length} characters."
                    "SUCCESS: getWorkflowLogs\nEvidence: $evidence\nLogs:\n$logText"
                }
                else -> "ERROR: INVALID_REQUEST - Unknown GitHub operation: $op"
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    private suspend fun resolveOwnerAndRepo(
        api: GitHubApiService,
        authHeader: String,
        repository: String,
        ownerParam: String,
        repoParam: String
    ): Pair<String, String> {
        if (repository.contains("/")) {
            val parts = repository.split("/")
            return Pair(parts[0].trim(), parts[1].trim())
        }
        val repo = if (repository.isNotEmpty()) repository else repoParam
        val owner = if (ownerParam.isNotEmpty()) {
            ownerParam
        } else {
            try {
                val user = api.getCurrentUser(authHeader)
                user.login
            } catch (e: Exception) {
                "user"
            }
        }
        return Pair(owner, repo)
    }

    private fun handleException(e: Exception): String {
        val errorType = when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> "INVALID_CREDENTIAL"
                    403 -> {
                        val rateLimitRemaining = e.response()?.headers()?.get("X-RateLimit-Remaining")
                        if (rateLimitRemaining == "0") "RATE_LIMITED" else "FORBIDDEN"
                    }
                    404 -> "NOT_FOUND"
                    409 -> "CONFLICT"
                    422 -> "INVALID_REQUEST"
                    in 500..599 -> "SERVER_ERROR"
                    else -> "UNKNOWN_ERROR"
                }
            }
            is IOException -> "NETWORK_ERROR"
            else -> "UNKNOWN_ERROR"
        }
        val rawMessage = e.message ?: "No error message"
        val redactedMessage = SecretRedactor.redact(rawMessage)
        return "ERROR: $errorType - $redactedMessage"
    }
}
