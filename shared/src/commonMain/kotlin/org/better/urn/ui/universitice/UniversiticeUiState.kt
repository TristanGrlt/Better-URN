package org.better.urn.ui.universitice

import org.better.urn.data.Course
import org.better.urn.data.MoodleUser

data class UniversiticeUiState(
    val isLogged: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: MoodleUser? = null,
    val courses: List<Course> = emptyList(),
    val searchQuery: String = ""
) {
    val filteredCourses: List<Course>
        get() {
            val normalizedQuery = searchQuery.normalizeForSearch()
            return if (normalizedQuery.isBlank()) {
                courses
            } else {
                courses.filter { course ->
                    course.fullname.normalizeForSearch().contains(normalizedQuery) ||
                    course.shortname.normalizeForSearch().contains(normalizedQuery)
                }
            }
        }
}

/**
 * Strips accents and converts string to lowercase for accent-insensitive search matching.
 */
private fun String.normalizeForSearch(): String {
    val builder = StringBuilder(length)
    for (c in this.lowercase()) {
        val replacement = when (c) {
            'à', 'á', 'â', 'ã', 'ä', 'å' -> 'a'
            'è', 'é', 'ê', 'ë' -> 'e'
            'ì', 'í', 'î', 'ï' -> 'i'
            'ò', 'ó', 'ô', 'õ', 'ö' -> 'o'
            'ù', 'ú', 'û', 'ü' -> 'u'
            'ç' -> 'c'
            'ñ' -> 'n'
            'ÿ' -> 'y'
            else -> c
        }
        builder.append(replacement)
    }
    return builder.toString()
}