package org.better.urn.data

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import org.better.urn.ui.universitice.UniversiticeUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseHidingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testCourseDeserializationWithHiddenField() {
        val jsonText = """{"id":101,"fullname":"Mobile Dev","shortname":"MD101","hidden":true}"""
        val course = json.decodeFromString<Course>(jsonText)

        assertEquals(101, course.id)
        assertTrue(course.isHidden)
    }

    @Test
    fun testCourseDeserializationDefaultHiddenField() {
        val jsonText = """{"id":102,"fullname":"Web Dev","shortname":"WD102"}"""
        val course = json.decodeFromString<Course>(jsonText)

        assertEquals(102, course.id)
        assertFalse(course.isHidden)
    }

    @Test
    fun testUserPreferencesHiddenCoursesStorage() {
        val preferences = UserPreferences()
        val initialHidden = preferences.getHiddenCourseIds()

        val newSet = setOf(101, 202)
        preferences.setHiddenCourseIds(newSet)

        assertEquals(newSet, preferences.getHiddenCourseIds())

        preferences.setHiddenCourseIds(initialHidden)
    }

    @Test
    fun testUiStateVisibleAndHiddenCoursePartitioning() {
        val courses = listOf(
            Course(id = 1, fullname = "Algebra", shortname = "ALG"),
            Course(id = 2, fullname = "Biology", shortname = "BIO"),
            Course(id = 3, fullname = "Chemistry", shortname = "CHM")
        ).toImmutableList()

        val state = UniversiticeUiState(
            courses = courses,
            hiddenCourseIds = persistentSetOf(2)
        )

        assertEquals(2, state.visibleCourses.size)
        assertEquals(listOf(1, 3), state.visibleCourses.map { it.id })

        assertEquals(1, state.hiddenCourses.size)
        assertEquals(2, state.hiddenCourses.first().id)
    }

    @Test
    fun testUiStateSearchQueryAppliesToVisibleAndHidden() {
        val courses = listOf(
            Course(id = 1, fullname = "Algebra 101", shortname = "ALG"),
            Course(id = 2, fullname = "Advanced Algebra", shortname = "AALG"),
            Course(id = 3, fullname = "Chemistry", shortname = "CHM")
        ).toImmutableList()

        val state = UniversiticeUiState(
            courses = courses,
            hiddenCourseIds = persistentSetOf(2),
            searchQuery = "Algebra"
        )

        assertEquals(1, state.filteredVisibleCourses.size)
        assertEquals(1, state.filteredVisibleCourses.first().id)

        assertEquals(1, state.filteredHiddenCourses.size)
        assertEquals(2, state.filteredHiddenCourses.first().id)
    }
}
