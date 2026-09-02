package org.better.urn.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class MoodleClient(private val baseUrl: String, private val token: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                coerceInputValues = true
            })
        }
    }

    suspend fun getUserProfile(): MoodleUser {
        return client.get("$baseUrl/webservice/rest/server.php") {
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_webservice_get_site_info")
                parameters.append("moodlewsrestformat", "json")
            }
        }.body()
    }

    suspend fun getEnrolledCourses(userId: Int): List<Course> {
        return client.get("$baseUrl/webservice/rest/server.php") {
            url {
                parameters.append("wstoken", token)
                parameters.append("wsfunction", "core_enrol_get_users_courses")
                parameters.append("moodlewsrestformat", "json")
                parameters.append("userid", userId.toString())
            }
        }.body()
    }
}
