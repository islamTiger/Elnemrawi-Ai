package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.di.ServiceLocator
import com.example.data.agent.AgentPlannerImpl
import com.example.data.agent.AgentExecutorImpl
import com.example.data.agent.AgentIntent
import com.example.data.agent.parseEgyptianArabicIntent
import com.example.data.device.tools.*
import com.example.data.security.SecureCredentialVault
import com.example.data.security.SecretRedactor
import com.example.domain.agent.AgentContext
import com.example.domain.agent.ExecutionStatus
import com.example.domain.model.AgentStep
import com.example.domain.model.StepStatus
import com.example.domain.model.StepType
import com.example.domain.model.ExecutionEnvironment
import com.example.domain.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

class FakeSupabaseApiService : SupabaseApiService {
    var shouldFailConnection = false
    var shouldFailAuth = false
    var shouldReturnEmptySchema = false
    var shouldFailStorageUpload = false
    var shouldFailFunction = false
    var listBucketsCalled = false
    var uploadObjectCalled = false
    var getObjectInfoCalledCount = 0

    override suspend fun getOpenApiSchema(apiKey: String, auth: String): ResponseBody {
        if (shouldFailAuth) throw HttpException(
            Response.error<Any>(401, "Unauthorized".toResponseBody(null))
        )
        if (shouldFailConnection) throw java.io.IOException("Network unreachable")
        
        val json = if (shouldReturnEmptySchema) "{}" else """
            {
                "definitions": {
                    "products": {
                        "properties": {
                            "id": { "type": "integer", "format": "int8" },
                            "title": { "type": "string" },
                            "price": { "type": "number", "format": "float" }
                        }
                    }
                }
            }
        """.trimIndent()
        return json.toResponseBody(null)
    }

    override suspend fun getTableData(apiKey: String, auth: String, table: String, select: String): List<Map<String, Any>> {
        if (table == "nonexistent") throw HttpException(
            Response.error<Any>(404, "Table not found".toResponseBody(null))
        )
        return listOf(mapOf("id" to 1, "title" to "Product 1", "price" to 12.99))
    }

    override suspend fun insertRow(apiKey: String, auth: String, prefer: String, table: String, row: Map<String, Any>): List<Map<String, Any>> {
        return listOf(row)
    }

    override suspend fun listBuckets(auth: String): List<SupabaseBucketResponse> {
        listBucketsCalled = true
        return listOf(SupabaseBucketResponse("bucket-1", "assets", null, true, "2026-10-03", "2026-10-03"))
    }

    override suspend fun getBucket(auth: String, bucketId: String): SupabaseBucketResponse {
        return SupabaseBucketResponse(bucketId, "assets", null, true, "2026-10-03", "2026-10-03")
    }

    override suspend fun listObjects(auth: String, bucketId: String, body: SupabaseListObjectsRequest): List<SupabaseObjectResponse> {
        return listOf(SupabaseObjectResponse("image1.png", "obj-1", "2026-10-03", "2026-10-03", "2026-10-03", emptyMap()))
    }

    override suspend fun uploadObject(auth: String, apiKey: String, bucketId: String, path: String, fileBytes: okhttp3.RequestBody): ResponseBody {
        uploadObjectCalled = true
        if (shouldFailStorageUpload) throw java.io.IOException("Upload failed")
        return "success".toResponseBody(null)
    }

    override suspend fun getObjectInfo(auth: String, bucketId: String, path: String): SupabaseObjectResponse {
        getObjectInfoCalledCount++
        if (shouldFailStorageUpload) throw HttpException(
            Response.error<Any>(404, "Object not found".toResponseBody(null))
        )
        return SupabaseObjectResponse(path, "obj-1", "2026-10-03", "2026-10-03", "2026-10-03", mapOf("size" to 1024))
    }

    override suspend fun deleteObject(auth: String, bucketId: String, path: String): ResponseBody {
        return "deleted".toResponseBody(null)
    }

    override suspend fun listEdgeFunctions(adminAuth: String, projectRef: String): List<SupabaseFunctionResponse> {
        if (shouldFailFunction) throw HttpException(
            Response.error<Any>(500, "Internal function error".toResponseBody(null))
        )
        return listOf(SupabaseFunctionResponse("func-1", "hello", "Hello Function", "active", "2026-10-03", "2026-10-03"))
    }

    override suspend fun invokeFunction(auth: String, functionName: String, payload: Map<String, Any>): ResponseBody {
        return "Hello Output".toResponseBody(null)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseIntegrationTest {

    private lateinit var context: Context
    private lateinit var vault: SecureCredentialVault
    private lateinit var fakeApi: FakeSupabaseApiService
    private lateinit var supabaseTool: SupabaseToolImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ServiceLocator.init(context)
        ServiceLocator.currentEnvironment = ExecutionEnvironment.TEST
        vault = SecureCredentialVault(context)
        fakeApi = FakeSupabaseApiService()
        supabaseTool = SupabaseToolImpl(testApiService = fakeApi)

        // Reset credential keys
        vault.deleteCredential("supabase_active_project")
    }

    @Test
    fun testMissingCredentialsTriggersAuthRequired() = runBlocking {
        val result = supabaseTool.execute(mapOf("operation" to "projectInfo"))
        assertTrue(result.contains("ERROR") && result.contains("AUTH_REQUIRED"))
    }

    @Test
    fun testInvalidCredentialsErrorClassification() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")
        fakeApi.shouldFailAuth = true

        val result = supabaseTool.execute(mapOf("operation" to "projectInfo", "projectRef" to "test-ref"))
        assertTrue(result.contains("ERROR"))
        assertTrue(result.contains("INVALID_CREDENTIAL"))
    }

    @Test
    fun testProjectUnavailableClassification() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")
        fakeApi.shouldFailConnection = true

        val result = supabaseTool.execute(mapOf("operation" to "projectInfo", "projectRef" to "test-ref"))
        assertTrue(result.contains("ERROR"))
        assertTrue(result.contains("NETWORK_ERROR"))
    }

    @Test
    fun testSchemaInspectionAndGetTables() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")

        // 1. Test inspectSchema
        val inspectResult = supabaseTool.execute(mapOf("operation" to "inspectSchema", "projectRef" to "test-ref"))
        assertTrue(inspectResult.contains("SUCCESS: inspectSchema"))
        assertTrue(inspectResult.contains("Table: products"))
        assertTrue(inspectResult.contains("Column: title (Type: string)"))

        // 2. Test getTables
        val tablesResult = supabaseTool.execute(mapOf("operation" to "getTables", "projectRef" to "test-ref"))
        assertTrue(tablesResult.contains("SUCCESS: getTables"))
        assertTrue(tablesResult.contains("products"))
    }

    @Test
    fun testTableNotFoundHandling() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")

        val result = supabaseTool.execute(mapOf(
            "operation" to "runSql",
            "sql" to "SELECT * FROM nonexistent",
            "projectRef" to "test-ref"
        ))
        assertTrue(result.contains("ERROR"))
        assertTrue(result.contains("PROJECT_NOT_FOUND") || result.contains("SQL_ERROR") || result.contains("NOT_FOUND"))
    }

    @Test
    fun testDestructiveSqlRequiresConfirmation() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")

        // Non-confirmed destructive SQL should fail
        val resultUnconfirmed = supabaseTool.execute(mapOf(
            "operation" to "runSql",
            "sql" to "DROP TABLE products",
            "projectRef" to "test-ref"
        ))
        assertTrue(resultUnconfirmed.contains("ERROR") && resultUnconfirmed.contains("SQL_ERROR"))
        assertTrue(resultUnconfirmed.contains("confirmation required"))

        // Confirmed destructive SQL should succeed
        val resultConfirmed = supabaseTool.execute(mapOf(
            "operation" to "runSql",
            "sql" to "DROP TABLE products",
            "confirmed" to true,
            "projectRef" to "test-ref"
        ))
        assertTrue(resultConfirmed.contains("SUCCESS: runSql"))
    }

    @Test
    fun testStorageUploadAndPostUploadVerification() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")

        // 1. Success upload and verify
        val successResult = supabaseTool.execute(mapOf(
            "operation" to "uploadObject",
            "bucketId" to "assets",
            "path" to "logo.png",
            "content" to "logo bytes",
            "projectRef" to "test-ref"
        ))
        assertTrue(successResult.contains("SUCCESS: uploadObject"))
        assertTrue(successResult.contains("verified"))
        assertTrue(fakeApi.uploadObjectCalled)
        assertEquals(1, fakeApi.getObjectInfoCalledCount)

        // 2. Failure storage upload
        fakeApi.shouldFailStorageUpload = true
        val failureResult = supabaseTool.execute(mapOf(
            "operation" to "uploadObject",
            "bucketId" to "assets",
            "path" to "logo.png",
            "content" to "logo bytes",
            "projectRef" to "test-ref"
        ))
        assertTrue(failureResult.contains("ERROR") && (failureResult.contains("NETWORK_ERROR") || failureResult.contains("VERIFICATION_FAILED")))
    }

    @Test
    fun testEdgeFunctionListAndInvokeFailure() = runBlocking {
        vault.storeCredential("supabase_active_project", "test-ref")
        vault.storeCredential("supabase_anon_key_test-ref", "test-key")

        // 1. List Functions Success
        val listResult = supabaseTool.execute(mapOf("operation" to "listFunctions", "projectRef" to "test-ref"))
        assertTrue(listResult.contains("SUCCESS: listFunctions"))
        assertTrue(listResult.contains("hello"))

        // 2. List Functions Failure
        fakeApi.shouldFailFunction = true
        val listFailure = supabaseTool.execute(mapOf("operation" to "listFunctions", "projectRef" to "test-ref"))
        assertTrue(listFailure.contains("ERROR") && listFailure.contains("SERVER_ERROR"))
    }

    @Test
    fun testSecretRedactionSafeguard() {
        val rawMessage = "ERROR: Connection failed with key eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp4cHFtciIsInJvbGUiOiJzZXJ2aWNlX3JvbGUiLCJpYXQiOjE2ODAxNTY3Mjl9"
        val redacted = SecretRedactor.redact(rawMessage)
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1Ni"))
        assertTrue(redacted.contains("[REDACTED_SECRET]"))
    }

    @Test
    fun testAgentPlannerSupabaseSetupWorkflow() = runBlocking {
        val planner = AgentPlannerImpl()
        
        // 1. Supabase Setup Prompt Parsing
        val state = parseEgyptianArabicIntent("اعمل جدول products في Supabase")
        assertEquals(AgentIntent.SUPABASE_DB_SETUP, state.intent)
        
        // 2. Planning sequence
        val context = AgentContext(
            currentRequest = "اعمل جدول products في Supabase",
            workspace = null,
            selectedModelId = "test-model",
            currentIntent = state.intent.name,
            requirements = state.requirements,
            unknownRequirements = state.unknowns,
            knownFacts = state.knownFacts
        )
        val steps = planner.planTask("اعمل جدول products في Supabase", context)
        
        // Ensure steps include connection initialization & schema check
        assertTrue(steps.any { it.title.contains("Supabase Connection") })
        assertTrue(steps.any { it.title.contains("Verify Database Schemas") })
    }
}
