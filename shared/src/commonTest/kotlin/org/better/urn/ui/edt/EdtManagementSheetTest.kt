package org.better.urn.ui.edt

import org.better.urn.data.EdtEvent
import org.better.urn.data.extractUniqueCourseTitles
import org.better.urn.data.formatEventHours
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EdtManagementSheetTest {

    @Test
    fun testExtractUniqueCourseTitlesForSpecificTimetable() {
        val events = listOf(
            EdtEvent("1", "tt1", "Maths CM", 0L, 1000L, "", "#FFFFFF"),
            EdtEvent("2", "tt1", "Maths CM", 1000L, 2000L, "", "#FFFFFF"),
            EdtEvent("3", "tt1", "Physique TD", 2000L, 3000L, "", "#FFFFFF"),
            EdtEvent("4", "tt2", "Anglais", 3000L, 4000L, "", "#FFFFFF"),
        )

        val tt1Courses = extractUniqueCourseTitles(events, "tt1")
        assertEquals(listOf("Maths CM", "Physique TD"), tt1Courses)

        val tt2Courses = extractUniqueCourseTitles(events, "tt2")
        assertEquals(listOf("Anglais"), tt2Courses)
    }

    @Test
    fun testHiddenEventDisplayLabelFormatting() {
        val startMs = 1715000000000L
        val endMs = 1715007200000L
        val event = EdtEvent("e100", "tt1", "Informatique TP", startMs, endMs, "Lab 3", "#123456")

        val formattedHours = formatEventHours(event.startMs, event.endMs)
        val displayLabel = "${event.title} ($formattedHours)"

        assertTrue(displayLabel.startsWith("Informatique TP ("))
        assertTrue(displayLabel.endsWith(")"))
    }

    @Test
    fun testHiddenCourseSetOperations() {
        val hiddenCourses = setOf("Maths CM", "Physique TP")
        val allCourses = listOf("Maths CM", "Physique TP", "Anglais CM", "Histoire TD")

        val visibleCourses = allCourses.filter { it !in hiddenCourses }
        val remainingHidden = allCourses.filter { it in hiddenCourses }

        assertEquals(listOf("Anglais CM", "Histoire TD"), visibleCourses)
        assertEquals(listOf("Maths CM", "Physique TP"), remainingHidden)
    }
}
