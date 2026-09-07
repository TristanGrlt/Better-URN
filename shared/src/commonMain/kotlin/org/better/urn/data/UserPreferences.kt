package org.better.urn.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserPreferences {
    private val settings: Settings by lazy {
        try {
            Settings()
        } catch (_: Throwable) {
            fallbackSettings
        }
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

    var moodleUrl: String
        get() = settings.getString("moodle_url", "https://universitice.univ-rouen.fr")
        set(value) = settings.putString("moodle_url", value)

    var moodleToken: String
        get() {
            val secureToken = SecureStorage.getSecureString("moodle_token")
            if (!secureToken.isNullOrBlank()) {
                return secureToken
            }
            val legacyToken = settings.getStringOrNull("moodle_token")
            if (!legacyToken.isNullOrBlank()) {
                SecureStorage.saveSecureString("moodle_token", legacyToken)
                settings.remove("moodle_token")
                return legacyToken
            }
            return ""
        }
        set(value) {
            if (value.isNotBlank()) {
                SecureStorage.saveSecureString("moodle_token", value)
            } else {
                SecureStorage.removeSecureString("moodle_token")
            }
            settings.remove("moodle_token")
        }

    var moodlePassport: String?
        get() = settings.getStringOrNull("moodle_passport")
        set(value) {
            if (!value.isNullOrBlank()) {
                settings.putString("moodle_passport", value)
            } else {
                settings.remove("moodle_passport")
            }
        }

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

    fun getHiddenCourseIds(): Set<Int> {
        val key = "hidden_course_ids"
        return settings.getStringOrNull(key)?.let {
            try { json.decodeFromString(it) } catch (e: Exception) { emptySet() }
        } ?: emptySet()
    }

    fun setHiddenCourseIds(courseIds: Set<Int>) {
        val key = "hidden_course_ids"
        settings.putString(key, json.encodeToString(courseIds))
    }

    var isHiddenSectionExpanded: Boolean
        get() = settings.getBoolean("is_hidden_section_expanded", false)
        set(value) = settings.putBoolean("is_hidden_section_expanded", value)

    companion object {
        private val fallbackMap = mutableMapOf<String, Any>()
        private val fallbackSettings = object : Settings {
            override val keys: Set<String> get() = fallbackMap.keys
            override val size: Int get() = fallbackMap.size
            override fun clear() = fallbackMap.clear()
            override fun remove(key: String) { fallbackMap.remove(key) }
            override fun hasKey(key: String): Boolean = fallbackMap.containsKey(key)
            override fun putString(key: String, value: String) { fallbackMap[key] = value }
            override fun getString(key: String, defaultValue: String): String = (fallbackMap[key] as? String) ?: defaultValue
            override fun getStringOrNull(key: String): String? = fallbackMap[key] as? String
            override fun putInt(key: String, value: Int) { fallbackMap[key] = value }
            override fun getInt(key: String, defaultValue: Int): Int = (fallbackMap[key] as? Int) ?: defaultValue
            override fun getIntOrNull(key: String): Int? = fallbackMap[key] as? Int
            override fun putLong(key: String, value: Long) { fallbackMap[key] = value }
            override fun getLong(key: String, defaultValue: Long): Long = (fallbackMap[key] as? Long) ?: defaultValue
            override fun getLongOrNull(key: String): Long? = fallbackMap[key] as? Long
            override fun putFloat(key: String, value: Float) { fallbackMap[key] = value }
            override fun getFloat(key: String, defaultValue: Float): Float = (fallbackMap[key] as? Float) ?: defaultValue
            override fun getFloatOrNull(key: String): Float? = fallbackMap[key] as? Float
            override fun putDouble(key: String, value: Double) { fallbackMap[key] = value }
            override fun getDouble(key: String, defaultValue: Double): Double = (fallbackMap[key] as? Double) ?: defaultValue
            override fun getDoubleOrNull(key: String): Double? = fallbackMap[key] as? Double
            override fun putBoolean(key: String, value: Boolean) { fallbackMap[key] = value }
            override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (fallbackMap[key] as? Boolean) ?: defaultValue
            override fun getBooleanOrNull(key: String): Boolean? = fallbackMap[key] as? Boolean
        }
    }
}
