package com.example.data.device.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.tools.AiTool
import java.io.File

class RealDeviceFileTool(private val context: Context) : AiTool {
    override val id: String = "real_device_file_tool"
    override val name: String = "Android Device File Operations"
    override val description: String = "Perform official Android Storage Access Framework file picking, saving to app workspace, and sharing files via Android Sharesheet."
    override val inputSchema: Map<String, String> = mapOf(
        "operation" to "String (pickFile | pickImage | saveFile | shareFile | listFiles)",
        "fileName" to "String (Name of file to save or share, optional)",
        "content" to "String (Text or data to save into file, optional)",
        "mimeType" to "String (MIME type for picker or share, default */*)",
        "shareText" to "String (Text caption or message to share, optional)"
    )

    override suspend fun execute(arguments: Map<String, Any>): String {
        val operation = arguments["operation"]?.toString()?.lowercase() ?: "listfiles"
        val fileName = arguments["fileName"]?.toString()
        val content = arguments["content"]?.toString()
        val mimeType = arguments["mimeType"]?.toString() ?: "*/*"
        val shareText = arguments["shareText"]?.toString()

        return when (operation) {
            "pickfile" -> {
                val intent = createPickFileIntent(mimeType)
                "ACTION_PREPARED: Storage Access Framework picker intent created for MIME '$mimeType'. Launch intent: ${intent.action}"
            }
            "pickimage" -> {
                val intent = createPickFileIntent("image/*")
                "ACTION_PREPARED: Photo / Media picker intent created. Launch intent: ${intent.action}"
            }
            "savefile" -> {
                if (fileName.isNullOrBlank()) {
                    return "ERROR: Missing 'fileName' parameter to save file"
                }
                saveFile(fileName, content ?: "")
            }
            "sharefile" -> {
                shareFile(fileName, shareText, mimeType)
            }
            "listfiles" -> {
                val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val files = filesDir.listFiles() ?: emptyArray()
                val sb = StringBuilder("App Document Storage (${files.size} files):\n")
                files.forEach { file ->
                    sb.append("• ${file.name} (${file.length()} bytes)\n")
                }
                sb.toString()
            }
            else -> "ERROR: Unknown operation '$operation'. Supported: pickFile, pickImage, saveFile, shareFile, listFiles"
        }
    }

    fun createPickFileIntent(mimeType: String = "*/*"): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    fun saveFile(fileName: String, content: String): String {
        return try {
            val targetDir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(targetDir, fileName)
            file.writeText(content)
            "SUCCESS: File saved to '${file.absolutePath}' (${file.length()} bytes)"
        } catch (e: Exception) {
            "ERROR: ACTION_FAILED - Failed to save file '$fileName': ${e.message ?: "Unknown error"}"
        }
    }

    fun shareFile(fileName: String?, shareText: String?, mimeType: String): String {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                if (!shareText.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                if (!fileName.isNullOrBlank()) {
                    val targetDir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(targetDir, fileName)
                    if (file.exists()) {
                        val uri: Uri = try {
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        } catch (e: Exception) {
                            Uri.fromFile(file)
                        }
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            "SUCCESS: Opened Android Sharesheet for ${fileName ?: shareText ?: "content"}"
        } catch (e: Exception) {
            "ERROR: ACTION_FAILED - Failed to launch share dialog: ${e.message ?: "Unknown error"}"
        }
    }
}
