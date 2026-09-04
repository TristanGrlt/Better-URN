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

    // --- Caching (File-based via CacheStorage) ---
    
    var cachedUser: MoodleUser?
        get() = CacheStorage.getString("cached_user")?.let { 
            try { json.decodeFromString(it) } catch (e: Exception) { null } 
        }
        set(value) {
            if (value != null) {
                CacheStorage.saveString("cached_user", json.encodeToString(value))
            } else {
                CacheStorage.remove("cached_user")
            }
        }

    var cachedCourses: List<Course>
        get() = CacheStorage.getString("cached_courses")?.let { 
            try { json.decodeFromString(it) } catch (e: Exception) { emptyList() } 
        } ?: emptyList()
        set(value) {
            CacheStorage.saveString("cached_courses", json.encodeToString(value))
        }

    fun getCachedCourseSections(courseId: Int): List<CourseSection> {
        val key = "cached_course_sections_$courseId"
        return CacheStorage.getString(key)?.let {
            try { json.decodeFromString(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    fun setCachedCourseSections(courseId: Int, sections: List<CourseSection>) {
        val key = "cached_course_sections_$courseId"
        try {
            CacheStorage.saveString(key, json.encodeToString(sections))
        } catch (_: Exception) {
            // Ignore cache write error
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
