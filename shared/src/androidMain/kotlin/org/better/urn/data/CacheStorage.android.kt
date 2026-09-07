package org.better.urn.data

import java.io.File

actual object CacheStorage {
    private fun getCacheDir(): File {
        val context = AndroidContextProvider.context
        val baseDir = if (context != null) {
            context.cacheDir
        } else {
            val tmp = System.getProperty("java.io.tmpdir") ?: "."
            File(tmp, "betterurn_cache")
        }
        val dir = File(baseDir, "json_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getImageCacheDir(): File {
        val context = AndroidContextProvider.context
        val baseDir = if (context != null) {
            context.cacheDir
        } else {
            val tmp = System.getProperty("java.io.tmpdir") ?: "."
            File(tmp, "betterurn_cache")
        }
        val dir = File(baseDir, "image_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getFileForKey(key: String): File {
        val safeFileName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".json"
        return File(getCacheDir(), safeFileName)
    }

    private fun getFileForImageKey(key: String): File {
        val safeFileName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getImageCacheDir(), safeFileName)
    }

    actual fun saveString(key: String, content: String) {
        try {
            val file = getFileForKey(key)
            file.writeText(content, Charsets.UTF_8)
        } catch (e: Exception) {
            println("CacheStorage Android save Error: ${e.message}")
        }
    }

    actual fun getString(key: String): String? {
        return try {
            val file = getFileForKey(key)
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            println("CacheStorage Android get Error: ${e.message}")
            null
        }
    }

    actual fun saveBytes(key: String, bytes: ByteArray): String? {
        return try {
            val file = getFileForImageKey(key)
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            println("CacheStorage Android saveBytes Error: ${e.message}")
            null
        }
    }

    actual fun getFilePath(key: String): String? {
        return try {
            val file = getFileForImageKey(key)
            if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            println("CacheStorage Android getFilePath Error: ${e.message}")
            null
        }
    }

    actual fun remove(key: String) {
        try {
            val file = getFileForKey(key)
            if (file.exists()) {
                file.delete()
            }
            val imgFile = getFileForImageKey(key)
            if (imgFile.exists()) {
                imgFile.delete()
            }
        } catch (e: Exception) {
            println("CacheStorage Android remove Error: ${e.message}")
        }
    }

    actual fun clear() {
        try {
            getCacheDir().listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
            getImageCacheDir().listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
        } catch (e: Exception) {
            println("CacheStorage Android clear Error: ${e.message}")
        }
    }
}

