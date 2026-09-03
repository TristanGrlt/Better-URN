package org.better.urn.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
    }

    suspend fun getUserProfile(): MoodleUser {
        return sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_webservice_get_site_info")
                parameters.append("moodlewsrestformat", "json")
            }
        }.body()
    }

    suspend fun getEnrolledCourses(userId: Int): List<Course> {
        return sharedClient.get("$cleanBaseUrl/webservice/rest/server.php") {
            headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_enrol_get_users_courses")
                parameters.append("moodlewsrestformat", "json")
                parameters.append("userid", userId.toString())
            }
        }.body()
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

        if (responseText.trimStart().startsWith("{")) {
            val jsonObject = try {
                json.parseToJsonElement(responseText).jsonObject
            } catch (_: Exception) {
                null
            }
            val errorMessage = jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: jsonObject?.get("exception")?.jsonPrimitive?.contentOrNull
            if (errorMessage != null) {
                throw IllegalStateException("Moodle : $errorMessage")
            }
        }

        return json.decodeFromString(responseText)
    }
}
