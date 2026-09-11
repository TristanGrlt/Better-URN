package org.better.urn.data.auth

import kotlin.random.Random

/**
 * Initiates the Moodle Mobile Web SSO authentication flow.
 */
object MoodleAuthInitiator {
    private const val PASSPORT_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val PASSPORT_LENGTH = 32
    private const val DEFAULT_SERVICE = "moodle_mobile_app"
    private const val DEFAULT_URL_SCHEME = "betterurn"

    /**
     * Generates a random 32-character alphanumeric ephemeral passport.
     */
    fun generatePassport(): String {
        return (1..PASSPORT_LENGTH)
            .asSequence()
            .map { PASSPORT_CHARSET[Random.nextInt(PASSPORT_CHARSET.length)] }
            .joinToString("")
    }

    /**
     * Constructs the web authorization launch URL for Moodle Mobile authentication.
     */
    fun createLaunchUrl(
        baseUrl: String,
        passport: String,
        service: String = DEFAULT_SERVICE,
        urlScheme: String = DEFAULT_URL_SCHEME,
    ): String {
        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val launchEndpoint = "$cleanBaseUrl/admin/tool/mobile/launch.php"
        return "$launchEndpoint?service=$service&passport=$passport&urlscheme=$urlScheme"
    }
}
