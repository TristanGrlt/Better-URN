package org.better.urn.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MoodleUser(
    val userid: Int,
    val fullname: String,
    val userpictureurl: String
)

@Immutable
@Serializable
data class Course(
    val id: Int,
    val fullname: String,
    val shortname: String,
    val overviewfiles: List<MoodleFile> = emptyList(),
    val imageUrl: String? = null,
    @SerialName("hidden") val isHidden: Boolean = false
) {
    fun sanitized(): Course = copy(
        fullname = fullname.cleanHtml(),
        shortname = shortname.cleanHtml()
    )

    fun getImageUrl(token: String): String? {
        if (imageUrl != null) return imageUrl
        val fileUrl = overviewfiles.firstOrNull()?.fileurl ?: return null
        if (token.isBlank()) return fileUrl
        return "$fileUrl?token=$token"
    }

    fun withResolvedImageUrl(token: String): Course {
        val currentImg = imageUrl
        if (currentImg != null && !currentImg.startsWith("http://") && !currentImg.startsWith("https://")) {
            val cleanPath = currentImg.removePrefix("file:")
            if (java.io.File(cleanPath).exists()) {
                return copy(imageUrl = cleanPath)
            }
        }
        val fileUrl = overviewfiles.firstOrNull()?.fileurl ?: currentImg ?: return this
        val rawUrl = if (fileUrl.contains("?") || token.isBlank()) fileUrl else "$fileUrl?token=$token"
        val cachedLocalPath = CourseImageCache.getCachedImagePath(id, rawUrl)
        return copy(imageUrl = cachedLocalPath ?: rawUrl)
    }

    /**
     * Constructs the web URL to view this course in a browser.
     */
    fun getWebUrl(baseUrl: String): String {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        return "$cleanBaseUrl/course/view.php?id=$id"
    }


}

@Immutable
@Serializable
data class MoodleFile(val fileurl: String)

@Immutable
@Serializable
data class CourseSection(
    val id: Int,
    val name: String,
    val summary: String? = null,
    val visible: Int? = 1,
    val modules: List<CourseModule> = emptyList()
) {
    fun sanitized(): CourseSection = copy(
        name = name.cleanHtml(),
        summary = summary?.cleanHtml()?.takeIf { it.isNotBlank() },
        modules = modules.map { it.sanitized() }
    )
}

@Immutable
@Serializable
data class CourseModule(
    val id: Int,
    val name: String,
    val modname: String,
    val modicon: String? = null,
    val description: String? = null,
    val onclick: String? = null,
    val url: String? = null,
    val contents: List<ModuleContent>? = null,
    val completion: Int? = null
) {
    fun sanitized(): CourseModule = copy(
        name = name.cleanHtml(),
        description = description?.cleanHtml()?.takeIf { it.isNotBlank() },
        contents = contents?.map { it.sanitized() }
    )

    /**
     * Resolves the primary target URL for this module, prioritizing direct file URLs with webservice token authentication.
     */
    fun getPrimaryUrl(token: String): String? {
        val directFileUrl = contents?.firstOrNull()?.fileurl
        val rawUrl = directFileUrl ?: url ?: return null
        if (token.isBlank()) return rawUrl

        var targetUrl = rawUrl
        if (targetUrl.contains("/pluginfile.php/") && !targetUrl.contains("/webservice/pluginfile.php/")) {
            targetUrl = targetUrl.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
        }

        if (targetUrl.contains("forcedownload=1")) {
            targetUrl = targetUrl.replace("forcedownload=1", "forcedownload=0")
        }

        return if (targetUrl.contains("wstoken=") || targetUrl.contains("token=")) {
            targetUrl
        } else if (targetUrl.contains("?")) {
            "$targetUrl&token=$token"
        } else {
            "$targetUrl?token=$token"
        }
    }
}

@Immutable
@Serializable
data class ModuleContent(
    val type: String? = null,
    val filename: String? = null,
    val filepath: String? = null,
    val filesize: Long? = null,
    val fileurl: String? = null,
    val mimetype: String? = null,
    val timecreated: Long? = null,
    val timemodified: Long? = null
) {
    fun sanitized(): ModuleContent = copy(
        filename = filename?.cleanHtml()
    )

    /**
     * Returns a human-readable file size string.
     */
    fun getFormattedFileSize(): String? {
        val bytes = filesize ?: return null
        if (bytes <= 0) return null
        val kb = bytes / 1024.0
        if (kb < 1024) return "${(kb * 10).toInt() / 10.0} KB"
        val mb = kb / 1024.0
        return "${(mb * 10).toInt() / 10.0} MB"
    }
}
