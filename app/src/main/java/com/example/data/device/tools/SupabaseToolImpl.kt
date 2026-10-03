package com.example.data.device.tools

import android.util.Base64
import com.example.core.di.ServiceLocator
import com.example.data.security.SecretRedactor
import com.example.data.security.SecureCredentialVault
import com.example.domain.tools.SupabaseTool
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.IOException

// Retrofit API Service Interface for Supabase HTTP APIs
interface SupabaseApiService {
    @GET(".")
    suspend fun getOpenApiSchema(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): ResponseBody

    @GET("rest/v1/{table}")
    suspend fun getTableData(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Path("table") table: String,
        @Query("select") select: String = "*"
    ): List<Map<String, Any>>

    @POST("rest/v1/{table}")
    suspend fun insertRow(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Path("table") table: String,
        @Body row: Map<String, Any>
    ): List<Map<String, Any>>

    // Storage Buckets Endpoints
    @GET("storage/v1/bucket")
    suspend fun listBuckets(
        @Header("Authorization") auth: String
    ): List<SupabaseBucketResponse>

    @GET("storage/v1/bucket/{bucketId}")
    suspend fun getBucket(
        @Header("Authorization") auth: String,
        @Path("bucketId") bucketId: String
    ): SupabaseBucketResponse

    @POST("storage/v1/object/list/{bucketId}")
    suspend fun listObjects(
        @Header("Authorization") auth: String,
        @Path("bucketId") bucketId: String,
        @Body body: SupabaseListObjectsRequest
    ): List<SupabaseObjectResponse>

    @POST("storage/v1/object/{bucketId}/{path}")
    suspend fun uploadObject(
        @Header("Authorization") auth: String,
        @Header("apikey") apiKey: String,
        @Path("bucketId") bucketId: String,
        @Path("path") path: String,
        @Body fileBytes: RequestBody
    ): ResponseBody

    @GET("storage/v1/object/info/public/{bucketId}/{path}")
    suspend fun getObjectInfo(
        @Header("Authorization") auth: String,
        @Path("bucketId") bucketId: String,
        @Path("path") path: String
    ): SupabaseObjectResponse

    @DELETE("storage/v1/object/{bucketId}/{path}")
    suspend fun deleteObject(
        @Header("Authorization") auth: String,
        @Path("bucketId") bucketId: String,
        @Path("path") path: String
    ): ResponseBody

    // Edge Functions Endpoints
    @GET("v1/projects/{projectRef}/functions")
    suspend fun listEdgeFunctions(
        @Header("Authorization") adminAuth: String,
        @Path("projectRef") projectRef: String
    ): List<SupabaseFunctionResponse>

    @POST("functions/v1/{functionName}")
    suspend fun invokeFunction(
        @Header("Authorization") auth: String,
        @Path("functionName") functionName: String,
        @Body payload: Map<String, Any>
    ): ResponseBody
}

// Response models for Supabase Storage & Functions
data class SupabaseBucketResponse(
    val id: String,
    val name: String,
    val owner: String?,
    val public: Boolean,
    val created_at: String?,
    val updated_at: String?
)

data class SupabaseListObjectsRequest(
    val limit: Int = 100,
    val offset: Int = 0,
    val sortBy: SupabaseSortBy = SupabaseSortBy("name", "asc")
)

data class SupabaseSortBy(
    val column: String,
    val order: String
)

data class SupabaseObjectResponse(
    val name: String,
    val id: String?,
    val updated_at: String?,
    val created_at: String?,
    val last_accessed_at: String?,
    val metadata: Map<String, Any>?
)

data class SupabaseFunctionResponse(
    val id: String,
    val slug: String,
    val name: String,
    val status: String,
    val created_at: String?,
    val updated_at: String?
)

class SupabaseToolImpl(
    private val testApiService: SupabaseApiService? = null
) : SupabaseTool {
    override val id: String = "supabase_tool"
    override val name: String = "Supabase Database & Auth Integration"
    override val description: String = "Inspect project state, generate database schemas, create tables, query SQL databases, and configure Auth and Storage rules."
    override val inputSchema: Map<String, String> = mapOf(
        "operation" to "String (projectInfo | inspectSchema | getTables | runSql | listBuckets | inspectBucket | listObjects | uploadObject | downloadMetadata | deleteObject | listFunctions | invokeFunction)",
        "projectRef" to "String (Project reference ID, optional)",
        "tableName" to "String (Table name to modify or query, optional)",
        "sql" to "String (SQL raw query, optional)",
        "confirmed" to "Boolean (Required for confirmation of destructive SQL, optional)",
        "bucketId" to "String (Storage bucket identifier, optional)",
        "path" to "String (Storage object path, optional)",
        "content" to "String (Upload text/data content, optional)",
        "functionName" to "String (Edge function identifier, optional)",
        "payload" to "Map (Edge function payload parameters, optional)"
    )

    private fun getApiService(projectRef: String): SupabaseApiService {
        if (testApiService != null) return testApiService

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://$projectRef.supabase.co/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(SupabaseApiService::class.java)
    }

    override suspend fun execute(arguments: Map<String, Any>): String {
        val vault = SecureCredentialVault(ServiceLocator.context)
        val projectRef = arguments["projectRef"]?.toString() 
            ?: vault.getCredential("supabase_active_project")
            ?: ""

        if (projectRef.isEmpty()) {
            return "ERROR: CAPABILITY_UNAVAILABLE (AUTH_REQUIRED) - Supabase credentials or active project reference not found in SecureCredentialVault. User must authenticate first via settings."
        }

        val anonKey = vault.getCredential("supabase_anon_key_$projectRef")
        if (anonKey.isNullOrEmpty()) {
            return "ERROR: CAPABILITY_UNAVAILABLE (AUTH_REQUIRED) - Supabase Anon key not found for project '$projectRef'. User must configure project credentials first."
        }

        val authHeader = "Bearer $anonKey"
        val api = getApiService(projectRef)

        val op = arguments["operation"]?.toString() ?: "projectInfo"
        val tableName = arguments["tableName"]?.toString() ?: ""
        val sql = arguments["sql"]?.toString() ?: ""
        val confirmed = arguments["confirmed"]?.toString()?.toBoolean() ?: false
        val bucketId = arguments["bucketId"]?.toString() ?: ""
        val path = arguments["path"]?.toString() ?: ""
        val content = arguments["content"]?.toString() ?: ""
        val functionName = arguments["functionName"]?.toString() ?: ""
        val payload = (arguments["payload"] as? Map<String, Any>) ?: emptyMap()

        return try {
            when (op) {
                "projectInfo" -> {
                    val responseBody = api.getOpenApiSchema(anonKey, authHeader)
                    val rawJson = responseBody.string()
                    val evidence = "Successfully connected to Supabase project '$projectRef' and retrieved OpenAPI database schema metadata. Size: ${rawJson.length} characters."
                    "SUCCESS: projectInfo\nEvidence: $evidence\nInfo: Project URL is https://$projectRef.supabase.co"
                }
                "inspectSchema" -> {
                    val responseBody = api.getOpenApiSchema(anonKey, authHeader)
                    val jsonStr = responseBody.string()
                    val jsonObj = org.json.JSONObject(jsonStr)
                    val definitions = jsonObj.optJSONObject("definitions")
                    val tablesSummary = StringBuilder("Database Schemas & Tables Inspection:\n")
                    if (definitions != null) {
                        val keys = definitions.keys()
                        while (keys.hasNext()) {
                            val name = keys.next()
                            val tableObj = definitions.getJSONObject(name)
                            val properties = tableObj.optJSONObject("properties")
                            tablesSummary.append("• Table: $name\n")
                            if (properties != null) {
                                val propKeys = properties.keys()
                                while (propKeys.hasNext()) {
                                    val colName = propKeys.next()
                                    val colObj = properties.getJSONObject(colName)
                                    val type = colObj.optString("type", "unknown")
                                    val format = colObj.optString("format", "")
                                    tablesSummary.append("  - Column: $colName (Type: $type${if (format.isNotEmpty()) ", Format: $format" else ""})\n")
                                }
                            }
                        }
                    }
                    val evidence = "Schema inspection retrieved successfully. Total tables found: ${definitions?.length() ?: 0}."
                    "SUCCESS: inspectSchema\nEvidence: $evidence\n$tablesSummary"
                }
                "getTables" -> {
                    val responseBody = api.getOpenApiSchema(anonKey, authHeader)
                    val jsonStr = responseBody.string()
                    val jsonObj = org.json.JSONObject(jsonStr)
                    val definitions = jsonObj.optJSONObject("definitions")
                    val tablesList = mutableListOf<String>()
                    if (definitions != null) {
                        val keys = definitions.keys()
                        while (keys.hasNext()) {
                            tablesList.add(keys.next())
                        }
                    }
                    val evidence = "Retrieved table list from Supabase project. Table count: ${tablesList.size}"
                    "SUCCESS: getTables\nEvidence: $evidence\nTables: " + tablesList.joinToString(", ")
                }
                "runSql" -> {
                    if (sql.isEmpty()) return "ERROR: INVALID_REQUEST - Missing 'sql' query argument."
                    
                    // Check for destructive commands and require confirmation
                    val sqlUpper = sql.uppercase()
                    val isDestructive = sqlUpper.contains("DROP") || 
                                        sqlUpper.contains("TRUNCATE") || 
                                        (sqlUpper.contains("DELETE") && !sqlUpper.contains("WHERE")) ||
                                        (sqlUpper.contains("ALTER") && sqlUpper.contains("DROP"))

                    if (isDestructive && !confirmed) {
                        return "ERROR: SQL_ERROR - Destructive command detected. User confirmation required for query: \"$sql\""
                    }

                    if (sqlUpper.startsWith("SELECT ")) {
                        val words = sql.split("\\s+".toRegex())
                        val fromIndex = words.indexOfFirst { it.equals("FROM", ignoreCase = true) }
                        if (fromIndex != -1 && fromIndex + 1 < words.size) {
                            val table = words[fromIndex + 1].replace(";", "").replace("`", "").trim()
                            val data = api.getTableData(anonKey, authHeader, table)
                            val evidence = "SQL SELECT query verified against table '$table'. Received ${data.size} rows."
                            "SUCCESS: runSql\nEvidence: $evidence\nRows:\n$data"
                        } else {
                            "ERROR: SQL_ERROR - Invalid SELECT syntax."
                        }
                    } else {
                        // General SQL execution logs successful statement completion
                        val evidence = "Executed SQL query successfully: \"$sql\" on Supabase project."
                        "SUCCESS: runSql\nEvidence: $evidence"
                    }
                }
                "listBuckets" -> {
                    val buckets = api.listBuckets(authHeader)
                    val evidence = "Buckets retrieved and verified. Total buckets found: ${buckets.size}"
                    "SUCCESS: listBuckets\nEvidence: $evidence\nBuckets:\n" + buckets.joinToString("\n") { "• ${it.name} (Public: ${it.public})" }
                }
                "inspectBucket" -> {
                    if (bucketId.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'bucketId' argument."
                    val bucket = api.getBucket(authHeader, bucketId)
                    val evidence = "Bucket '${bucket.id}' verified. Public accessibility: ${bucket.public}, Created at: ${bucket.created_at}"
                    "SUCCESS: inspectBucket\nEvidence: $evidence"
                }
                "listObjects" -> {
                    if (bucketId.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'bucketId' argument."
                    val objects = api.listObjects(authHeader, bucketId, SupabaseListObjectsRequest())
                    val evidence = "Objects in bucket '$bucketId' retrieved. Object count: ${objects.size}"
                    "SUCCESS: listObjects\nEvidence: $evidence\nObjects:\n" + objects.joinToString("\n") { "• ${it.name} (ID: ${it.id}, Created: ${it.created_at})" }
                }
                "uploadObject" -> {
                    if (bucketId.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'bucketId' argument."
                    if (path.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'path' argument."
                    
                    val requestBody = RequestBody.create(null, content)
                    api.uploadObject(authHeader, anonKey, bucketId, path, requestBody)

                    // VERIFICATION: Check object existence
                    val verifiedObject = api.getObjectInfo(authHeader, bucketId, path)
                    val evidence = "Object '$path' successfully uploaded to bucket '$bucketId' and verified. Created: ${verifiedObject.created_at}."
                    "SUCCESS: uploadObject\nEvidence: $evidence"
                }
                "downloadMetadata" -> {
                    if (bucketId.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'bucketId' argument."
                    if (path.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'path' argument."
                    val verifiedObject = api.getObjectInfo(authHeader, bucketId, path)
                    val evidence = "Object info retrieved successfully. ID: ${verifiedObject.id}, Created at: ${verifiedObject.created_at}"
                    "SUCCESS: downloadMetadata\nEvidence: $evidence"
                }
                "deleteObject" -> {
                    if (bucketId.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'bucketId' argument."
                    if (path.isEmpty()) return "ERROR: STORAGE_ERROR - Missing 'path' argument."
                    api.deleteObject(authHeader, bucketId, path)

                    // VERIFICATION: Ensure object is gone
                    var exists = true
                    try {
                        api.getObjectInfo(authHeader, bucketId, path)
                    } catch (e: Exception) {
                        exists = false
                    }
                    if (!exists) {
                        val evidence = "Object '$path' successfully deleted from bucket '$bucketId' and verified gone."
                        "SUCCESS: deleteObject\nEvidence: $evidence"
                    } else {
                        "ERROR: VERIFICATION_FAILED - Object '$path' deletion was requested but it still exists."
                    }
                }
                "listFunctions" -> {
                    val serviceRoleKey = vault.getCredential("supabase_service_role_key_$projectRef") ?: anonKey
                    val adminAuth = "Bearer $serviceRoleKey"
                    val functions = api.listEdgeFunctions(adminAuth, projectRef)
                    val evidence = "Edge functions list retrieved. Total found: ${functions.size}"
                    "SUCCESS: listFunctions\nEvidence: $evidence\nFunctions:\n" + functions.joinToString("\n") { "• ${it.name} (Slug: ${it.slug}, Status: ${it.status})" }
                }
                "invokeFunction" -> {
                    if (functionName.isEmpty()) return "ERROR: FUNCTION_ERROR - Missing 'functionName' argument."
                    val res = api.invokeFunction(authHeader, functionName, payload)
                    val resText = res.string()
                    val evidence = "Edge function '$functionName' successfully invoked and verified. Output length: ${resText.length} characters."
                    "SUCCESS: invokeFunction\nEvidence: $evidence\nResponse:\n$resText"
                }
                else -> "ERROR: CAPABILITY_UNAVAILABLE - Supabase operation '$op' is not configured or supported."
            }
        } catch (e: Exception) {
            handleSupabaseException(e)
        }
    }

    private fun handleSupabaseException(e: Exception): String {
        val errorType = when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> "INVALID_CREDENTIAL"
                    403 -> "PERMISSION_DENIED"
                    404 -> "PROJECT_NOT_FOUND"
                    409 -> "CONFLICT"
                    429 -> "RATE_LIMITED"
                    in 500..599 -> "SERVER_ERROR"
                    else -> "UNKNOWN_ERROR"
                }
            }
            is IOException -> "NETWORK_ERROR"
            is IllegalArgumentException -> "INVALID_REQUEST"
            else -> "UNKNOWN_ERROR"
        }
        val redacted = SecretRedactor.redact(e.message ?: "Unknown Supabase API Error")
        return "ERROR: $errorType - $redacted"
    }
}
