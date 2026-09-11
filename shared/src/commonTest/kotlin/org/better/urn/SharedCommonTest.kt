package org.better.urn

import kotlinx.collections.immutable.toImmutableList
import org.better.urn.data.Course
import org.better.urn.ui.universitice.UniversiticeUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun testCourseSearchAccentInsensitive() {
        val courses = listOf(
            Course(id = 1, fullname = "Mathématiques et Systèmes", shortname = "MATH101"),
            Course(id = 2, fullname = "Réseaux & Télécoms", shortname = "NET201"),
            Course(id = 3, fullname = "Algorithmique", shortname = "ALG301"),
        ).toImmutableList()

        // Search "mathematique" without accents
        val state1 = UniversiticeUiState(courses = courses, searchQuery = "mathematique")
        assertEquals(1, state1.filteredCourses.size)
        assertEquals("Mathématiques et Systèmes", state1.filteredCourses.first().fullname)

        // Search "reseau" without accents
        val state2 = UniversiticeUiState(courses = courses, searchQuery = "reseau")
        assertEquals(1, state2.filteredCourses.size)
        assertEquals("Réseaux & Télécoms", state2.filteredCourses.first().fullname)

        // Search with accents "réseau" for unaccented query/course
        val state3 = UniversiticeUiState(courses = courses, searchQuery = "réseau")
        assertEquals(1, state3.filteredCourses.size)

        // Fuzzy search with typo: "mathematiq" (missing 'ue')
        val state4 = UniversiticeUiState(courses = courses, searchQuery = "mathematiq")
        assertEquals(1, state4.filteredCourses.size)
        assertEquals("Mathématiques et Systèmes", state4.filteredCourses.first().fullname)

        // Fuzzy search with typo: "algotithme" ('t' instead of 'r')
        val state5 = UniversiticeUiState(courses = courses, searchQuery = "algotithme")
        assertEquals(1, state5.filteredCourses.size)
        assertEquals("Algorithmique", state5.filteredCourses.first().fullname)
    }

    @Test
    fun testEmptySearchQueryReturnsAllCourses() {
        val courses = listOf(
            Course(id = 1, fullname = "Course 1", shortname = "C1"),
            Course(id = 2, fullname = "Course 2", shortname = "C2")
        ).toImmutableList()

        val state = UniversiticeUiState(courses = courses, searchQuery = "")
        assertEquals(2, state.filteredCourses.size)
        assertEquals(courses.toList(), state.filteredCourses.toList())
    }

    @Test
    fun testMoodleErrorDetectionValidJson() {
        val validJson = """{"userid":123,"fullname":"John Doe","userpictureurl":"https://example.com/pic.jpg"}"""
        // Should not throw any exception
        org.better.urn.data.MoodleClient.checkMoodleError(validJson)
    }

    @Test
    fun testMoodleErrorDetectionInvalidToken() {
        val errorJson = """{"exception":"moodle_exception","errorcode":"invalidtoken","message":"Clé d'accès non valide - jeton introuvable"}"""
        kotlin.test.assertFailsWith<org.better.urn.data.MoodleTokenExpiredException> {
            org.better.urn.data.MoodleClient.checkMoodleError(errorJson)
        }
    }

    @Test
    fun testMoodleErrorDetectionTokenExpired() {
        val errorJson = """{"exception":"moodle_exception","errorcode":"tokenexpired","message":"Jeton expiré"}"""
        kotlin.test.assertFailsWith<org.better.urn.data.MoodleTokenExpiredException> {
            org.better.urn.data.MoodleClient.checkMoodleError(errorJson)
        }
    }

    @Test
    fun testMoodleErrorDetectionGenericError() {
        val errorJson = """{"exception":"moodle_exception","errorcode":"invalidcourse","message":"Cours introuvable"}"""
        val ex = kotlin.test.assertFailsWith<IllegalStateException> {
            org.better.urn.data.MoodleClient.checkMoodleError(errorJson)
        }
        assertEquals("Moodle : Cours introuvable", ex.message)
    }
}
