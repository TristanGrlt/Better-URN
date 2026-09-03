package org.better.urn.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreferences {
    private val settings: Settings = Settings()
    
    private val json = Json { ignoreUnknownKeys = true }

    var moodleUrl: String
        get() = settings.getString("moodle_url", "https://universitice.univ-rouen.fr")
        set(value) = settings.putString("moodle_url", value)

    var moodleToken: String
        get() = settings.getString("moodle_token", "")
        set(value) = settings.putString("moodle_token", value)

    // --- Caching ---
    
    var cachedUser: MoodleUser?
        get() = settings.getStringOrNull("cached_user")?.let { 
            try { json.decodeFromString(it) } catch (e: Exception) { null } 
        }
        set(value) {
            if (value != null) settings.putString("cached_user", json.encodeToString(value))
            else settings.remove("cached_user")
        }

    var cachedCourses: List<Course>
        get() = settings.getStringOrNull("cached_courses")?.let { 
            try { json.decodeFromString(it) } catch (e: Exception) { emptyList() } 
        } ?: emptyList()
        set(value) = settings.putString("cached_courses", json.encodeToString(value))
}
