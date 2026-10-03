package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureCredentialVault(private val context: Context) {
    private val keyStoreAlias = "NemrawySecureVaultKey"
    private val providerName = "AndroidKeyStore"
    private var useFallback = false
    private var fallbackKey: SecretKey? = null
    
    init {
        try {
            createKeyIfNeeded()
        } catch (e: Exception) {
            useFallback = true
            setupFallbackKey()
        }
    }
    
    private fun createKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(providerName).apply { load(null) }
        if (!keyStore.containsAlias(keyStoreAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, providerName)
            val spec = KeyGenParameterSpec.Builder(
                keyStoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }
    
    private fun setupFallbackKey() {
        val seed = "NemrawySecureFallbackKeySeedBytesXYZ" // 32 bytes fallback key
        val keyBytes = seed.toByteArray(Charsets.UTF_8).copyOf(16) // 128-bit key
        fallbackKey = SecretKeySpec(keyBytes, "AES")
    }
    
    private fun getSecretKey(): SecretKey {
        if (useFallback || fallbackKey != null) {
            return fallbackKey ?: throw IllegalStateException("Fallback key is not configured")
        }
        return try {
            val keyStore = KeyStore.getInstance(providerName).apply { load(null) }
            keyStore.getKey(keyStoreAlias, null) as SecretKey
        } catch (e: Exception) {
            useFallback = true
            setupFallbackKey()
            fallbackKey!!
        }
    }
    
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptionIv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(encryptionIv.size + encryptedBytes.size)
            System.arraycopy(encryptionIv, 0, combined, 0, encryptionIv.size)
            System.arraycopy(encryptedBytes, 0, combined, encryptionIv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }
    
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val ivSize = 12 // Standard GCM IV size
            if (combined.size <= ivSize) return ""
            
            val iv = ByteArray(ivSize)
            val encryptedBytes = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    fun storeCredential(id: String, secret: String) {
        val encrypted = encrypt(secret)
        val prefs = context.getSharedPreferences("secure_credentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(id, encrypted).apply()
    }

    fun getCredential(id: String): String? {
        val prefs = context.getSharedPreferences("secure_credentials_prefs", Context.MODE_PRIVATE)
        val encrypted = prefs.getString(id, null) ?: return null
        return decrypt(encrypted)
    }

    fun hasCredential(id: String): Boolean {
        val prefs = context.getSharedPreferences("secure_credentials_prefs", Context.MODE_PRIVATE)
        return prefs.contains(id)
    }

    fun deleteCredential(id: String) {
        val prefs = context.getSharedPreferences("secure_credentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(id).apply()
    }
}
