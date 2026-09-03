package org.better.urn.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreferences {
    private val settings: Settings = Settings()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

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

    fun getCachedCourseSections(courseId: Int): List<CourseSection> {
        val key = "cached_course_sections_$courseId"
        return settings.getStringOrNull(key)?.let {
            try { json.decodeFromString(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    fun setCachedCourseSections(courseId: Int, sections: List<CourseSection>) {
        val key = "cached_course_sections_$courseId"
        try {
            settings.putString(key, json.encodeToString(sections))
        } catch (_: Exception) {
            // Ignore cache storage overflow errors on Desktop/JVM
        }
    }

    fun getCollapsedSectionIds(courseId: Int): Set<Int> {
        val key = "collapsed_sections_$courseId"
        return settings.getStringOrNull(key)?.let {
            try { json.decodeFromString(it) } catch (e: Exception) { emptySet() }
        } ?: emptySet()
    }

    fun setCollapsedSectionIds(courseId: Int, sectionIds: Set<Int>) {
        val key = "collapsed_sections_$courseId"
        settings.putString(key, json.encodeToString(sectionIds))
    }
}
