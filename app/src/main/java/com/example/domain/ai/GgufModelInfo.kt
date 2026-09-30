package com.example.domain.ai

import android.content.Context
import android.net.Uri
import java.io.InputStream

data class GgufModelInfo(
    val architecture: String = "UNKNOWN",
    val parameterCount: String = "UNKNOWN",
    val quantization: String = "UNKNOWN",
    val contextLength: String = "UNKNOWN",
    val vocabularyInfo: String = "UNKNOWN",
    val tensorInformation: String = "UNKNOWN",
    val fileSize: Long = 0L
)

class GgufMetadataReader {
    fun readMetadata(context: Context, uri: Uri): GgufModelInfo {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Cannot open stream for URI")
            
            // Read first 4 bytes for magic header 'GGUF'
            val magic = ByteArray(4)
            val bytesRead = inputStream.read(magic)
            if (bytesRead < 4) {
                throw IllegalArgumentException("File is too small to contain a GGUF header")
            }
            
            val isGguf = magic[0] == 'G'.code.toByte() &&
                         magic[1] == 'G'.code.toByte() &&
                         magic[2] == 'U'.code.toByte() &&
                         magic[3] == 'F'.code.toByte()
                         
            if (!isGguf) {
                throw IllegalArgumentException("Magic header does not match GGUF format")
            }
            
            // Valid GGUF file structure. Keep other specific nested model configs as UNKNOWN as instructed.
            return GgufModelInfo(
                architecture = "UNKNOWN",
                parameterCount = "UNKNOWN",
                quantization = "UNKNOWN",
                contextLength = "UNKNOWN",
                vocabularyInfo = "UNKNOWN",
                tensorInformation = "UNKNOWN"
            )
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                inputStream?.close()
            } catch (ignored: Exception) {}
        }
    }
}
