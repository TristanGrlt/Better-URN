package org.better.urn.data.auth

/**
 * Data payload parsed from a Moodle Mobile authentication response.
 */
data class MoodleAuthPayload(
    val token: String,
    val privateToken: String? = null,
    val passport: String? = null,
    val urlHash: String? = null
)
