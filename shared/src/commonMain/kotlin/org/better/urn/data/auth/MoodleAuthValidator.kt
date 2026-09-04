package org.better.urn.data.auth

/**
 * Validates parsed authentication payloads against security and format rules.
 */
object MoodleAuthValidator {

    /**
     * Verifies the extracted payload against the saved passport and token format requirements.
     */
    fun validate(payload: MoodleAuthPayload?, storedPassport: String?): Boolean {
        if (payload == null) return false
        val token = payload.token.trim()

        if (token.isEmpty()) return false

        // Passport validation (anti-CSRF / replay attack prevention)
        if (!payload.passport.isNullOrBlank()) {
            if (storedPassport.isNullOrBlank() || payload.passport != storedPassport) {
                return false
            }
        }

        // Token format validation: printable ASCII characters only (codes 32 to 126)
        for (ch in token) {
            val code = ch.code
            if (code !in 32..126) {
                return false
            }
        }

        // Explicitly reject JSON/JWT structures to prevent double-parsing
        val trimmedLower = token.lowercase()
        if (trimmedLower.startsWith("{") ||
            trimmedLower.startsWith("[") ||
            trimmedLower.contains("{\"alg\":") ||
            trimmedLower.contains("\"alg\":")
        ) {
            return false
        }

        return true
    }
}
