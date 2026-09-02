package org.better.urn.data

import kotlinx.serialization.Serializable

@Serializable
data class MoodleUser(
    val userid: Int,
    val fullname: String,
    val userpictureurl: String
)

@Serializable
data class Course(
    val id: Int,
    val fullname: String,
    val shortname: String,
    val overviewfiles: List<MoodleFile> = emptyList()
) {
    fun getImageUrl(token: String): String? {
        val fileUrl = overviewfiles.firstOrNull()?.fileurl ?: return null
        return "$fileUrl?token=$token"
    }
}

@Serializable
data class MoodleFile(val fileurl: String)
