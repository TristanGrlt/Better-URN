package org.better.urn.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

const val DEFAULT_ADE_URL = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1&resources=1234&caller=moodle"

open class EdtRepository(
    private val client: HttpClient = defaultClient
) {
    companion object {
        private val defaultClient by lazy {
            HttpClient(CIO)
        }
    }

    open suspend fun fetchAndParseIcs(
        url: String = DEFAULT_ADE_URL,
        isDarkTheme: Boolean = false
    ): List<EdtEvent> {
        val targetUrl = normalizeUrl(url)
        return try {
            val responseText = client.get(targetUrl) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (BetterURN)")
            }.bodyAsText()

            IcsParser.parseIcs(
                icsContent = responseText,
                timetableId = "default",
                isDarkTheme = isDarkTheme
            )
        } catch (e: Exception) {
            println("EDT_ERROR: ${e.stackTraceToString()}")
            val rawMsg = e.message?.takeIf { it.isNotBlank() }
            val errorDetails = rawMsg ?: e::class.simpleName ?: "Exception"
            throw Exception("Erreur réseau: $errorDetails", e)
        }
    }
}
