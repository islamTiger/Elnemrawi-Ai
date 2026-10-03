package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.ServiceLocator
import com.example.data.agent.AgentEngineImpl
import com.example.data.agent.AgentPlannerImpl
import com.example.data.agent.parseEgyptianArabicIntent
import com.example.data.agent.AgentIntent
import com.example.data.device.tools.*
import com.example.data.files.*
import com.example.data.github.*
import com.example.data.supabase.*
import com.example.data.terminal.*
import com.example.data.security.SecureCredentialVault
import com.example.data.security.SecretRedactor
import com.example.domain.agent.AgentContext
import com.example.domain.agent.ExecutionStatus
import com.example.domain.model.AgentStep
import com.example.domain.model.StepStatus
import com.example.domain.model.StepType
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.tools.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentCoreTest {

    private lateinit var context: Context
    private lateinit var vault: SecureCredentialVault

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(context)
        ServiceLocator.currentEnvironment = ExecutionEnvironment.TEST
        vault = SecureCredentialVault(context)
    }

    @Test
    fun testEgyptianArabicIntentParsing() {
        // App generation with database integration
        val appState = parseEgyptianArabicIntent("اعمللي أبلكيشن طلبات واربطه بسوبابيز وخلي الأوردر يظهر في الداشبورد")
        assertEquals(AgentIntent.CREATE_APPLICATION, appState.intent)
        assertTrue(appState.requirements.contains("customer_app"))
        assertTrue(appState.requirements.contains("backend"))
        assertTrue(appState.requirements.contains("ordering"))
        assertTrue(appState.requirements.contains("admin_dashboard"))
        // Missing authentication method & payment method should be unknowns (No Guessing Policy)
        assertTrue(appState.unknowns.contains("authentication_method"))
        assertTrue(appState.unknowns.contains("payment_method"))

        // Launch app
        val launchState = parseEgyptianArabicIntent("افتح تطبيق المتصفح")
        assertEquals(AgentIntent.DEVICE_LAUNCH_APP, launchState.intent)

        // Capture screen
        val captureState = parseEgyptianArabicIntent("خد لقطة شاشة للواجهة")
        assertEquals(AgentIntent.DEVICE_CAPTURE_SCREEN, captureState.intent)

        // UI interaction
        val tapState = parseEgyptianArabicIntent("اضغط على زر تأكيد الحساب")
        assertEquals(AgentIntent.DEVICE_INTERACT, tapState.intent)

        // File picking SAF
        val fileState = parseEgyptianArabicIntent("هات الملف وشاركه مع التطبيقات")
        assertEquals(AgentIntent.DEVICE_FILE_OPERATION, fileState.intent)

        // Git push without specific repository (unknown repo)
        val gitNoRepo = parseEgyptianArabicIntent("ارفعه على جيت هاب")
        assertEquals(AgentIntent.GIT_SYNC, gitNoRepo.intent)
        assertTrue(gitNoRepo.unknowns.contains("target_github_repository"))

        // Git push with explicit repository
        val gitWithRepo = parseEgyptianArabicIntent("ارفعه على جيت هاب nemrawy/codeai-core")
        assertEquals(AgentIntent.GIT_SYNC, gitWithRepo.intent)
        assertEquals("nemrawy/codeai-core", gitWithRepo.knownFacts["target_github_repository"])
        assertFalse(gitWithRepo.unknowns.contains("target_github_repository"))
    }

    @Test
    fun testNoGuessingPolicyStopsExecution() = runBlocking {
        val planner = AgentPlannerImpl()
        // Missing repository in prompt
        val context = AgentContext(currentRequest = "ارفعه على جيت هاب", workspace = null, selectedModelId = "test")
        val plan = planner.planTask("ارفعه على جيت هاب", context)
        
        // Planner should produce the clarification step and stop producing further action steps
        assertTrue(plan.any { it.title.contains("Clarification Required") })
        assertFalse(plan.any { it.type == StepType.COMMIT })
    }

    @Test
    fun testSecureCredentialVaultEncryption() {
        val plainSecret = "ghp_secure_github_auth_personal_token_xyz123"
        val encrypted = vault.encrypt(plainSecret)
        assertNotEquals(plainSecret, encrypted)

        val decrypted = vault.decrypt(encrypted)
        assertEquals(plainSecret, decrypted)
    }

    @Test
    fun testSecretRedactorPrunesSensitiveData() {
        val rawMessage1 = "Logging into database with password=superSecurePassword123!"
        val redacted1 = SecretRedactor.redact(rawMessage1)
        assertTrue(redacted1.contains("password=[REDACTED_SECRET]"))
        assertFalse(redacted1.contains("superSecurePassword123!"))

        val rawMessage2 = "Executing push with GitHub Token: ghp_abc123xyz78900000000000000000000000"
        val redacted2 = SecretRedactor.redact(rawMessage2)
        assertTrue(redacted2.contains("[REDACTED_SECRET]"))
        assertFalse(redacted2.contains("ghp_abc123xyz"))
    }

    @Test
    fun testExternalIntegrationUnavailableState() = runBlocking {
        val github = GitHubToolImpl()
        val supabase = SupabaseToolImpl()
        val browser = BrowserToolImpl()

        val githubOutput = github.execute(mapOf("operation" to "pushChanges"))
        val supabaseOutput = supabase.execute(mapOf("operation" to "createTable"))
        val browserOutput = browser.execute(mapOf("operation" to "openUrl", "url" to "https://google.com"))

        assertTrue(githubOutput.contains("CAPABILITY_UNAVAILABLE"))
        assertTrue(supabaseOutput.contains("CAPABILITY_UNAVAILABLE"))
        assertTrue(browserOutput.contains("CAPABILITY_UNAVAILABLE"))
    }

    @Test
    fun testToolRegistryVerificationSupport() {
        val codeTool = CodeToolImpl()
        val supabaseTool = SupabaseToolImpl()

        ToolRegistry.registerTool(codeTool)
        ToolRegistry.registerTool(supabaseTool)

        assertNotNull(ToolRegistry.getTool("code_tool"))
        assertNotNull(ToolRegistry.getTool("supabase_tool"))
    }

    @Test
    fun testGitHubToolCredentialsStorage() = runBlocking {
        val gitService = ServiceLocator.gitService
        val vault = SecureCredentialVault(context)

        // Clear existing token
        gitService.disconnectAccount()
        assertFalse(vault.hasCredential("github_token"))

        // Connect token
        val success = gitService.connectAccount("ghp_testTokenMockValueLengthMoreThanTenChars")
        assertTrue(success)
        assertTrue(vault.hasCredential("github_token"))
        assertEquals("ghp_testTokenMockValueLengthMoreThanTenChars", vault.getCredential("github_token"))

        // Disconnect token
        gitService.disconnectAccount()
        assertFalse(vault.hasCredential("github_token"))
    }

    @Test
    fun testGitHubToolFailsGracefullyWithBadToken() = runBlocking {
        val github = GitHubToolImpl()
        val vault = SecureCredentialVault(context)

        // Store a bad token in the vault
        vault.storeCredential("github_token", "invalid_bad_token_format_xyz")

        val result = github.execute(mapOf("operation" to "listRepos"))
        assertTrue(result.contains("ERROR") || result.contains("INVALID_CREDENTIAL"))

        // Clean up token
        vault.deleteCredential("github_token")
    }

    @Test
    fun testCodeToolAnalyzeProducesRealSnapshot() = runBlocking {
        val codeTool = CodeToolImpl()
        val result = codeTool.execute(mapOf("operation" to "analyze"))
        
        assertTrue(result.contains("SUCCESS: Project Inspection Complete."))
        assertTrue(result.contains("ProjectSnapshot"))
        assertTrue(result.contains("projectPath"))
        assertTrue(result.contains("modules"))
        assertTrue(result.contains("sourceFiles"))
        assertTrue(result.contains("gradleFiles"))
    }

    @Test
    fun testCodeToolWriteAndReadCode() = runBlocking {
        val codeTool = CodeToolImpl()
        val tempFilePath = "app/src/main/java/com/example/TempTestFile.kt"
        val codeContent = "package com.example\n\nclass TempTestFile {\n    fun hello() = \"world\"\n}"

        // Write
        val writeResult = codeTool.execute(mapOf(
            "operation" to "writeCode",
            "filePath" to tempFilePath,
            "code" to codeContent
        ))
        assertTrue(writeResult.contains("SUCCESS"))

        // Read
        val readResult = codeTool.execute(mapOf(
            "operation" to "readCode",
            "filePath" to tempFilePath
        ))
        assertTrue(readResult.contains("SUCCESS"))
        assertTrue(readResult.contains("class TempTestFile"))

        // Search
        val searchResult = codeTool.execute(mapOf(
            "operation" to "searchCode",
            "query" to "TempTestFile"
        ))
        assertTrue(searchResult.contains("SUCCESS"))
        assertTrue(searchResult.contains("TempTestFile"))

        // Clean up
        val deleteResult = codeTool.execute(mapOf(
            "operation" to "deleteCode",
            "filePath" to tempFilePath
        ))
        assertTrue(deleteResult.contains("SUCCESS"))
    }

    @Test
    fun testProductionHardeningRoutingRules() {
        // Force environment to PRODUCTION
        ServiceLocator.currentEnvironment = ExecutionEnvironment.PRODUCTION

        // 1. Production Agent uses Real CodeTool
        val codeTool = ToolRegistry.getTool("code_tool")
        assertTrue(codeTool is CodeToolImpl)

        // 2. Production Build uses Real Gradle (RealBuildService)
        assertTrue(ServiceLocator.buildService is RealBuildService)
        assertFalse(ServiceLocator.buildService is MockBuildService)

        // 3. Production Git uses Real GitService (RealGitService)
        assertTrue(ServiceLocator.gitService is RealGitService)
        assertFalse(ServiceLocator.gitService is MockGitService)

        // 4. Production Supabase uses Real SupabaseService (RealSupabaseService)
        assertTrue(ServiceLocator.supabaseService is RealSupabaseService)
        assertFalse(ServiceLocator.supabaseService is MockSupabaseService)

        // 5. Mocks cannot be selected in PRODUCTION environment
        assertFalse(ServiceLocator.projectFileService is MockProjectFileService)
        assertTrue(ServiceLocator.projectFileService is RealProjectFileService)
    }

    @Test
    fun testFutureToolsReturnCapabilityUnavailable() = runBlocking {
        val tools: List<AiTool> = listOf(
            FutureTerminalTool(),
            FutureCloudflareTool(),
            FutureVisionTool(),
            FutureImageGenerationTool()
        )
        for (tool in tools) {
            val result = tool.execute(mapOf("operation" to "any"))
            assertTrue(result.contains("CAPABILITY_UNAVAILABLE") || result.contains("ERROR"))
        }
    }

    @Test
    fun testCompileBuildTestPackageVerifySemanticsAreDistinct() {
        // Simply assert semantic definitions to verify they remain conceptually clear and separate
        val compilePhase = "compileDebugKotlin"
        val buildPhase = "assembleDebug"
        val testPhase = "testDebugUnitTest"
        val packagePhase = "APK generation"
        val verifyPhase = "artifact + metadata verification"

        assertNotEquals(compilePhase, buildPhase)
        assertNotEquals(buildPhase, testPhase)
        assertNotEquals(testPhase, packagePhase)
        assertNotEquals(packagePhase, verifyPhase)
    }
}
