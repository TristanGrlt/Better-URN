package org.better.urn.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object SecureStorage {
    private const val KEY_ALIAS = "betterurn_secure_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "betterurn_secure_prefs"

    // In-memory fallback used when Android Context or KeyStore is unavailable (e.g., host unit tests)
    private val fallbackStore = mutableMapOf<String, String>()

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun getPrefs(): SharedPreferences? {
        val context = AndroidContextProvider.context ?: return null
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun saveSecureString(key: String, value: String) {
        val prefs = getPrefs()
        if (prefs == null) {
            fallbackStore[key] = value
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encodedEncrypted = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            prefs.edit().putString(key, "$encodedIv:$encodedEncrypted").apply()
        } catch (_: Exception) {
            fallbackStore[key] = value
        }
    }

    actual fun getSecureString(key: String): String? {
        val prefs = getPrefs()
        if (prefs == null) {
            return fallbackStore[key]
        }
        val raw = prefs.getString(key, null) ?: return fallbackStore[key]
        return try {
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            fallbackStore[key]
        }
    }

    actual fun removeSecureString(key: String) {
        fallbackStore.remove(key)
        getPrefs()?.edit()?.remove(key)?.apply()
    }

    actual fun clear() {
        fallbackStore.clear()
        getPrefs()?.edit()?.clear()?.apply()
    }
}
