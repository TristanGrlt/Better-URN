package org.better.urn.data

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IcsParserTest {

    @Test
    fun testCleanSubjectName() {
        val input1 = "Mathématiques - CM (TD1 Gr 2)"
        val cleaned1 = cleanSubjectName(input1)
        assertEquals("Mathématiques", cleaned1)

        val input2 = "Informatique TP2 Examen CT"
        val cleaned2 = cleanSubjectName(input2)
        assertEquals("Informatique", cleaned2)

        val input3 = "Physique - CC (Gr A1)"
        val cleaned3 = cleanSubjectName(input3)
        assertEquals("Physique", cleaned3)
    }

    @Test
    fun testGenerateColorFromSubject() {
        val colorLight = generateColorFromSubject("Mathématiques CM", isDarkTheme = false)
        val colorDark = generateColorFromSubject("Mathématiques CM", isDarkTheme = true)

        assertTrue(colorLight.startsWith("#"))
        assertEquals(7, colorLight.length)
        assertTrue(colorDark.startsWith("#"))
        assertEquals(7, colorDark.length)

        // Same cleaned subject name should produce same hue/color
        val colorLightTd = generateColorFromSubject("Mathématiques TD1", isDarkTheme = false)
        assertEquals(colorLight, colorLightTd)
    }

    @Test
    fun testParseIcsWithDummyContent() = runTest {
        val dummyIcs = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Better URN Test//EN
            BEGIN:VEVENT
            UID:test-event-1@better.urn
            SUMMARY:Mathématiques - CM (TD1)
            LOCATION:Amphi A - Batiment Sciences
            DTSTART:20260908T080000Z
            DTEND:20260908T100000Z
            END:VEVENT
            BEGIN:VEVENT
            UID:test-event-2@better.urn
            SUMMARY:Informatique TP2
            LOCATION:Salle 102
            DESCRIPTION:Prof: M. Dupont\nChit-Chat & TP Java
            DTSTART;TZID=Europe/Paris:20260908T140000
            DTEND;TZID=Europe/Paris:20260908T160000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = IcsParser.parseIcs(
            icsContent = dummyIcs,
            timetableId = "tt_123",
            isDarkTheme = false
        )

        assertEquals(2, events.size)

        val event1 = events[0]
        assertEquals("test-event-1@better.urn", event1.id)
        assertEquals("tt_123", event1.timetableId)
        assertEquals("Mathématiques - CM (TD1)", event1.title)
        assertEquals("Amphi A - Batiment Sciences", event1.location)
        assertEquals("", event1.description)
        assertTrue(event1.colorHex.startsWith("#"))

        // Check startMs conversion (2026-09-08 08:00:00 UTC)
        val startInstant1 = Instant.fromEpochMilliseconds(event1.startMs)
        val startUtc1 = startInstant1.toLocalDateTime(TimeZone.UTC)
        assertEquals(2026, startUtc1.year)
        @Suppress("DEPRECATION")
        assertEquals(9, startUtc1.monthNumber)
        @Suppress("DEPRECATION")
        assertEquals(8, startUtc1.dayOfMonth)
        assertEquals(8, startUtc1.hour)
        assertEquals(0, startUtc1.minute)

        // Check endMs conversion (2026-09-08 10:00:00 UTC)
        val endInstant1 = Instant.fromEpochMilliseconds(event1.endMs)
        val endUtc1 = endInstant1.toLocalDateTime(TimeZone.UTC)
        assertEquals(10, endUtc1.hour)

        val event2 = events[1]
        assertEquals("test-event-2@better.urn", event2.id)
        assertEquals("Informatique TP2", event2.title)
        assertEquals("Salle 102", event2.location)
        assertEquals("Prof: M. Dupont\nChit-Chat & TP Java", event2.description)

        // Europe/Paris in September is UTC+2, so 14:00 Paris = 12:00 UTC
        val startInstant2 = Instant.fromEpochMilliseconds(event2.startMs)
        val startUtc2 = startInstant2.toLocalDateTime(TimeZone.UTC)
        assertEquals(12, startUtc2.hour)
    }
}
