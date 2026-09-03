package org.better.urn.data

import com.russhwolf.settings.Settings

class UserPreferences {
    private val settings: Settings = Settings()

    var moodleUrl: String
        get() = settings.getString("moodle_url", "https://universitice.univ-rouen.fr")
        set(value) = settings.putString("moodle_url", value)

    var moodleToken: String
        get() = settings.getString("moodle_token", "")
        set(value) = settings.putString("moodle_token", value)
}
