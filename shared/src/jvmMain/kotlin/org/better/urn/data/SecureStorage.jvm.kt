package org.better.urn.data

import java.io.File
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual object SecureStorage {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val storageFile: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".betterurn")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, "secure_store.properties")
    }

    private val secretKey by lazy {
        val user = System.getProperty("user.name") ?: "default_user"
        val os = System.getProperty("os.name") ?: "unknown_os"
        val pass = "betterurn_jvm_salt_${user}_$os".toCharArray()
        val salt = "secure_salt_betterurn_2026".toByteArray(Charsets.UTF_8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pass, salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        SecretKeySpec(tmp.encoded, "AES")
    }

    private fun loadProperties(): Properties {
        val props = Properties()
        if (storageFile.exists()) {
            try {
                storageFile.inputStream().use { props.load(it) }
            } catch (e: Exception) {
                println("SecureStorage JVM load Error: ${e.message}")
            }
        }
        return props
    }

    private fun saveProperties(props: Properties) {
        try {
            val dir = storageFile.parentFile
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }
            storageFile.outputStream().use { props.store(it, "Secure Storage") }
        } catch (e: Exception) {
            println("SecureStorage JVM save Error: ${e.message}")
        }
    }

    actual fun saveSecureString(key: String, value: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val encodedIv = Base64.getEncoder().encodeToString(iv)
            val encodedEncrypted = Base64.getEncoder().encodeToString(encryptedBytes)
            val props = loadProperties()
            props.setProperty(key, "$encodedIv:$encodedEncrypted")
            saveProperties(props)
        } catch (e: Exception) {
            println("SecureStorage JVM saveSecureString Error: ${e.message}")
        }
    }

    actual fun getSecureString(key: String): String? {
        val props = loadProperties()
        val raw = props.getProperty(key) ?: return null
        return try {
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val iv = Base64.getDecoder().decode(parts[0])
            val encryptedBytes = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            println("SecureStorage JVM getSecureString Error: ${e.message}")
            null
        }
    }

    actual fun removeSecureString(key: String) {
        val props = loadProperties()
        if (props.containsKey(key)) {
            props.remove(key)
            saveProperties(props)
        }
    }

    actual fun clear() {
        if (storageFile.exists()) {
            storageFile.delete()
        }
    }
}
