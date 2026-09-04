package org.better.urn.data.auth

/**
 * Normalizes input strings received from deep links or manual token entry.
 */
object MoodleAuthNormalizer {

    /**
     * Sanitizes and extracts the raw candidate token/payload string.
     */
    fun normalize(input: String): String {
        var raw = input.trim()
        if (raw.isEmpty()) return ""

        // Extract value following token= if present
        if (raw.contains("token=")) {
            val tokenPart = raw.substringAfter("token=")
            raw = tokenPart.substringBefore("&").substringBefore("#")
        } else if (raw.contains("://")) {
            // Naked URI scheme (e.g. betterurn://<payload> or betterurn://?payload)
            val pathAndQuery = raw.substringAfter("://")
            raw = if (pathAndQuery.startsWith("?")) pathAndQuery.removePrefix("?") else pathAndQuery
            raw = raw.substringBefore("&").substringBefore("#")
        } else if (raw.startsWith("betterurn:")) {
            val path = raw.removePrefix("betterurn:")
            val cleanPath = path.removePrefix("//").removePrefix("?")
            raw = cleanPath.substringBefore("&").substringBefore("#")
        }

        // Base64 sanitization: replace percent-encoded Base64 characters
        raw = raw
            .replace("%3D", "=", ignoreCase = true)
            .replace("%2B", "+", ignoreCase = true)
            .replace("%2F", "/", ignoreCase = true)

        // Replace URL-safe Base64 characters with standard characters
        raw = raw.replace("-", "+").replace("_", "/")

        return raw.trim()
    }
}
