package org.better.urn.data.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Deterministically parses a normalized Moodle Mobile authentication string.
 */
object MoodleAuthParser {

    @OptIn(ExperimentalEncodingApi::class)
    fun parse(normalizedInput: String): MoodleAuthPayload? {
        if (normalizedInput.isBlank()) return null

        // Step 1: Base64 decoding attempt
        val decodedString: String = try {
            val decodedBytes = Base64.decode(normalizedInput)
            val decodedStr = decodedBytes.decodeToString()
            val hasNonPrintable = decodedStr.any { ch -> ch.code < 32 || ch.code > 126 }
            if (hasNonPrintable && !decodedStr.contains(":::")) {
                normalizedInput
            } else {
                decodedStr
            }
        } catch (_: Exception) {
            normalizedInput
        }

        // Step 2: Split strictly by ":::"
        val parts = decodedString.split(":::")

        // Rule 1: Key-Value extraction (if payload contains explicit key=value pairs)
        if (decodedString.contains("=") && parts.any { it.contains("=") }) {
            var extractedToken: String? = null
            var extractedPassport: String? = null

            for (part in parts) {
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim()
                    val value = kv[1].trim()
                    if (key.equals("token", ignoreCase = true) || key.equals("wstoken", ignoreCase = true)) {
                        extractedToken = value
                    } else if (key.equals("passport", ignoreCase = true)) {
                        extractedPassport = value
                    }
                }
            }

            if (!extractedToken.isNullOrBlank()) {
                return MoodleAuthPayload(
                    token = extractedToken,
                    passport = extractedPassport
                )
            }
        }

        // Rule 2: Positional extraction (Priority 1 for structured MD5:::TOKEN payloads)
        if (parts.size >= 2) {
            val urlHash = parts[0].trim()
            val token = parts[1].trim()
            val privateToken = if (parts.size > 2 && parts[2].isNotBlank()) parts[2].trim() else null
            val passport = if (parts.size > 3 && parts[3].isNotBlank()) parts[3].trim() else null

            if (token.isNotBlank()) {
                return MoodleAuthPayload(
                    token = token,
                    privateToken = privateToken,
                    passport = passport,
                    urlHash = urlHash
                )
            }
        }

        // Rule 3: Bare Token (Fallback)
        if (parts.size == 1 && !decodedString.contains(":::")) {
            val candidateToken = normalizedInput.trim()
            if (candidateToken.isNotBlank()) {
                return MoodleAuthPayload(token = candidateToken)
            }
        }

        return null
    }
}
