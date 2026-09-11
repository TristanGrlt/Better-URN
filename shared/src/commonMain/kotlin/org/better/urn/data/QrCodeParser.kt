package org.better.urn.data

/**
 * Result of parsing a raw text payload from a scanned QR Code.
 */
sealed interface QrScanResult {
    data class Success(
        val url: String,
        val suggestedName: String,
    ) : QrScanResult

    data class InvalidFormat(
        val rawText: String,
        val reason: String,
    ) : QrScanResult
}

/**
 * Utility for analyzing raw QR code string contents and extracting valid ICS calendar URLs.
 */
object QrCodeParser {

    /**
     * Parses a raw text payload extracted from a QR code into a [QrScanResult].
     */
    fun parse(rawText: String): QrScanResult {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return QrScanResult.InvalidFormat(rawText, "Le QR code est vide.")
        }

        val isUrlScheme = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("webcal://", ignoreCase = true) ||
                trimmed.startsWith("webcals://", ignoreCase = true)

        if (!isUrlScheme && !trimmed.contains("://")) {
            // Check if it's a domain-like URL without scheme (e.g. "ade.univ-rouen.fr/...")
            if (!trimmed.contains(".")) {
                return QrScanResult.InvalidFormat(
                    rawText,
                    "Le contenu scanné ne semble pas être un lien ou une URL d'emploi du temps."
                )
            }
        }

        val normalized = normalizeUrl(trimmed)

        if (!isValidCalendarUrl(normalized)) {
            return QrScanResult.InvalidFormat(
                rawText,
                "L'URL scannée ne correspond pas à un format de calendrier supporté."
            )
        }

        val suggestedName = extractSuggestedName(normalized)

        return QrScanResult.Success(
            url = normalized,
            suggestedName = suggestedName
        )
    }

    private fun isValidCalendarUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".ics") ||
                lower.contains("caltype=ical") ||
                lower.contains("anonymous_cal") ||
                lower.contains("planning") ||
                lower.contains("calendar") ||
                lower.contains("agenda") ||
                lower.startsWith("http://") ||
                lower.startsWith("https://")
    }

    /**
     * Attempts to infer a friendly calendar display name from query parameters or URL patterns.
     */
    fun extractSuggestedName(url: String): String {
        return try {
            val queryStart = url.indexOf('?')
            val queryParams = if ((queryStart != -1) && (queryStart < url.length - 1)) {
                url.substring(queryStart + 1)
                    .split('&')
                    .mapNotNull { param ->
                        val parts = param.split('=', limit = 2)
                        if (parts.size == 2) parts[0].lowercase() to parts[1] else null
                    }.toMap()
            } else {
                emptyMap()
            }

            val resourceId = queryParams["resources"]
            val nameParam = queryParams["name"] ?: queryParams["title"]

            when {
                !nameParam.isNullOrBlank() -> nameParam.replace("+", " ").trim()
                !resourceId.isNullOrBlank() -> "Calendrier ADE ($resourceId)"
                url.contains("ade", ignoreCase = true) -> "Calendrier ADE"
                else -> "Emploi du temps"
            }
        } catch (_: Exception) {
            "Emploi du temps"
        }
    }
}
