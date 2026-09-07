package org.better.urn.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class MoodleTokenExpiredException(message: String) : Exception(message)

class MoodleClient(baseUrl: String, private val token: String) {
    private val cleanBaseUrl = baseUrl.trimEnd('/')

    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true
            isLenient = true
        }

        private val sharedClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        fun checkMoodleError(responseText: String) {
            val trimmed = responseText.trimStart()
            if (!trimmed.startsWith("{")) return

            val jsonObject = try {
                json.parseToJsonElement(trimmed) as? JsonObject
            } catch (_: Exception) {
                null
            } ?: return

            val exception = jsonObject["exception"]?.jsonPrimitive?.contentOrNull
            val errorCode = jsonObject["errorcode"]?.jsonPrimitive?.contentOrNull
            val errorMessage = jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["error"]?.jsonPrimitive?.contentOrNull

            if ((exception != null) || (errorCode != null)) {
                val msg = errorMessage ?: "Erreur Moodle inconnue"
                val codeLower = errorCode?.lowercase().orEmpty()
                val msgLower = msg.lowercase()

                val isTokenExpired = (codeLower in setOf("invalidtoken", "tokenexpired", "accessexception")) ||
                        ("token" in codeLower) ||
                        ("jeton" in msgLower) ||
                        (("token" in msgLower) && (("invalid" in msgLower) || ("expired" in msgLower) || ("not found" in msgLower)))

                if (isTokenExpired) {
                    throw MoodleTokenExpiredException(msg)
                } else {
                    throw IllegalStateException("Moodle : $msg")
                }
            }
        }
    }

    suspend fun getUserProfile(): MoodleUser {
        val responseText: String = sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_webservice_get_site_info")
                parameters.append("moodlewsrestformat", "json")
            }
        }.body()

        checkMoodleError(responseText)
        return json.decodeFromString(responseText)
    }

    suspend fun getEnrolledCourses(userId: Int): List<Course> {
        val responseText: String = sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_enrol_get_users_courses")
                parameters.append("moodlewsrestformat", "json")
                parameters.append("userid", userId.toString())
            }
        }.body()

        checkMoodleError(responseText)
        return json.decodeFromString(responseText)
    }

    suspend fun getCourseContents(courseId: Int): List<CourseSection> {
        val responseText: String = sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_course_get_contents")
                parameters.append("moodlewsrestformat", "json")
                parameters.append("courseid", courseId.toString())
            }
        }.body()

        checkMoodleError(responseText)
        return json.decodeFromString(responseText)
    }

    /**
     * Updates user course visibility preference on Moodle server.
     */
    suspend fun setCourseHidden(courseId: Int, isHidden: Boolean, userId: Int? = null) {
        val responseText: String = sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_user_update_user_preferences")
                parameters.append("moodlewsrestformat", "json")
                parameters.append("preferences[0][name]", "block_myoverview_hidden_course_$courseId")
                parameters.append("preferences[0][value]", if (isHidden) "1" else "0")
                if (userId != null) {
                    parameters.append("preferences[0][userid]", userId.toString())
                }
            }
        }.body()

        checkMoodleError(responseText)
    }

    /**
     * Downloads raw bytes from an image or resource URL.
     */
    suspend fun downloadBytes(url: String): ByteArray? {
        return try {
            val response = sharedClient.get(url) {
                headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            }
            if (response.status.isSuccess()) {
                val contentType = response.contentType()
                if (contentType?.match(ContentType.Text.Html) == true) {
                    null
                } else {
                    response.body<ByteArray>()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("MoodleClient downloadBytes error: ${e.message}")
            null
        }
    }
}


