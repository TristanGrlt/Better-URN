package org.better.urn.data

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EdtUtilsTest {

    @Test
    fun testNormalizeUrlWithoutScheme() {
        val input = "schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        val expected = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        assertEquals(expected, normalizeUrl(input))
    }

    @Test
    fun testNormalizeUrlWithWebcal() {
        val input = "webcal://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        val expected = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        assertEquals(expected, normalizeUrl(input))
    }

    @Test
    fun testNormalizeUrlWithWebcals() {
        val input = "webcals://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        val expected = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        assertEquals(expected, normalizeUrl(input))
    }

    @Test
    fun testNormalizeUrlWithHttps() {
        val input = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        val expected = "https://schedule.univ-rouen.fr/direct/myade/cal?projectId=1"
        assertEquals(expected, normalizeUrl(input))
    }

    @Test
    fun testNormalizeUrlWithHttp() {
        val input = "http://localhost:8080/cal.ics"
        val expected = "http://localhost:8080/cal.ics"
        assertEquals(expected, normalizeUrl(input))
    }

    @Test
    fun testCleanSubjectName() {
        val summary = "CM Maths TD1 Examen (Gr A)"
        val cleaned = cleanSubjectName(summary)
        assertEquals("Maths", cleaned)
    }

    @Test
    fun testFormatBreakDuration() {
        assertEquals("Pause de 15 min", formatBreakDuration(15))
        assertEquals("Pause de 1 h", formatBreakDuration(60))
        assertEquals("Pause de 1 h 30 min", formatBreakDuration(90))
    }

    @Test
    fun testFormatHeaderDate() {
        val monday = LocalDate(2026, 9, 14)
        val tuesday = LocalDate(2026, 9, 15)
        assertEquals("Lundi 14 Sept.", formatHeaderDate(monday))
        assertEquals("Mardi 15 Sept.", formatHeaderDate(tuesday))
    }

    @Test
    fun testFormatShortDayName() {
        assertEquals("LUN.", formatShortDayName(DayOfWeek.MONDAY))
        assertEquals("MAR.", formatShortDayName(DayOfWeek.TUESDAY))
        assertEquals("DIM.", formatShortDayName(DayOfWeek.SUNDAY))
    }

    @Test
    fun testFormatFullHeaderDate() {
        val date = LocalDate(2026, 9, 15)
        assertEquals("Mardi 15 Septembre", formatFullHeaderDate(date))
    }

    @Test
    fun testGetRelativeDayLabel() {
        val today = LocalDate(2026, 9, 15)
        val yesterday = LocalDate(2026, 9, 14)
        val tomorrow = LocalDate(2026, 9, 16)
        val nextWeek = LocalDate(2026, 9, 22)

        assertEquals("Aujourd'hui", getRelativeDayLabel(today, today))
        assertEquals("Hier", getRelativeDayLabel(yesterday, today))
        assertEquals("Demain", getRelativeDayLabel(tomorrow, today))
        assertEquals(null, getRelativeDayLabel(nextWeek, today))
    }

    @Test
    fun testGroupEventsAndInsertBreaksEmpty() {
        val result = groupEventsAndInsertBreaks(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testGroupEventsAndInsertBreaksNoOverlapSmallBreak() {
        val e1 = EdtEvent("1", "tt", "Maths", 1000L, 2000L, "A", "#FF0000")
        val e2 = EdtEvent("2", "tt", "Info", 2000L + 900000L, 4000L, "B", "#00FF00")

        val result = groupEventsAndInsertBreaks(listOf(e1, e2))
        assertEquals(2, result.size)
        assertTrue(result[0] is AgendaDayItem.EventGroup)
        assertEquals(listOf(e1), (result[0] as AgendaDayItem.EventGroup).events)
        assertTrue(result[1] is AgendaDayItem.EventGroup)
        assertEquals(listOf(e2), (result[1] as AgendaDayItem.EventGroup).events)
    }

    @Test
    fun testGroupEventsAndInsertBreaksNoOverlapLongBreak() {
        val e1 = EdtEvent("1", "tt", "Maths", 1000L, 2000L, "A", "#FF0000")
        val breakMs = 30 * 60 * 1000L
        val e2 = EdtEvent("2", "tt", "Info", 2000L + breakMs, 5000L, "B", "#00FF00")

        val result = groupEventsAndInsertBreaks(listOf(e1, e2))
        assertEquals(3, result.size)
        assertTrue(result[0] is AgendaDayItem.EventGroup)
        assertEquals(listOf(e1), (result[0] as AgendaDayItem.EventGroup).events)

        assertTrue(result[1] is AgendaDayItem.Break)
        assertEquals(30, (result[1] as AgendaDayItem.Break).durationMinutes)

        assertTrue(result[2] is AgendaDayItem.EventGroup)
        assertEquals(listOf(e2), (result[2] as AgendaDayItem.EventGroup).events)
    }

    @Test
    fun testGroupEventsAndInsertBreaksOverlap() {
        val e1 = EdtEvent("1", "tt", "Maths", 1000L, 3000L, "A", "#FF0000")
        val e2 = EdtEvent("2", "tt", "Physique", 2000L, 4000L, "B", "#0000FF")

        val result = groupEventsAndInsertBreaks(listOf(e1, e2))
        assertEquals(1, result.size)
        assertTrue(result[0] is AgendaDayItem.EventGroup)
        assertEquals(listOf(e1, e2), (result[0] as AgendaDayItem.EventGroup).events)
    }

    @Test
    fun testAllowBreakAnywhere() {
        val input = "Maths"
        val expected = "M\u200Ba\u200Bt\u200Bh\u200Bs"
        assertEquals(expected, input.allowBreakAnywhere())
    }

    @Test
    fun testParseHexColor() {
        val red = parseHexColor("#FF0000")
        assertEquals(Color(0xFFFF0000), red)

        val green = parseHexColor("00FF00")
        assertEquals(Color(0xFF00FF00), green)

        val invalid = parseHexColor("invalid")
        assertEquals(Color.Gray, invalid)
    }

    @Test
    fun testGetMondayOfWeek() {
        val monday = LocalDate(2026, 9, 14)
        val wednesday = LocalDate(2026, 9, 16)
        val sunday = LocalDate(2026, 9, 20)
        val startOfSeptember = LocalDate(2026, 9, 1) // Tuesday

        assertEquals(monday, getMondayOfWeek(monday))
        assertEquals(monday, getMondayOfWeek(wednesday))
        assertEquals(monday, getMondayOfWeek(sunday))
        assertEquals(LocalDate(2026, 8, 31), getMondayOfWeek(startOfSeptember))
    }

    @Test
    fun testFormatWeekRange() {
        val sameMonth = LocalDate(2026, 9, 14)
        assertEquals("Semaine du 14 au 20 Septembre", formatWeekRange(sameMonth))

        val diffMonthSameYear = LocalDate(2026, 9, 28)
        assertEquals("Semaine du 28 Sept. au 4 Octobre", formatWeekRange(diffMonthSameYear))

        val diffYear = LocalDate(2026, 12, 28)
        assertEquals("Semaine du 28 Déc. 2026 au 3 Janv. 2027", formatWeekRange(diffYear))
    }

    @Test
    fun testCalculateEventPositionsSingleAndOverlapping() {
        val tz = TimeZone.UTC
        val weekStart = LocalDate(2026, 9, 14) // Monday

        val monday9am = LocalDateTime(2026, Month.SEPTEMBER, 14, 9, 0).toInstant(tz).toEpochMilliseconds()
        val monday1030am = LocalDateTime(2026, Month.SEPTEMBER, 14, 10, 30).toInstant(tz).toEpochMilliseconds()

        val monday10am = LocalDateTime(2026, Month.SEPTEMBER, 14, 10, 0).toInstant(tz).toEpochMilliseconds()
        val monday1130am = LocalDateTime(2026, Month.SEPTEMBER, 14, 11, 30).toInstant(tz).toEpochMilliseconds()

        val tuesday2pm = LocalDateTime(2026, Month.SEPTEMBER, 15, 14, 0).toInstant(tz).toEpochMilliseconds()
        val tuesday4pm = LocalDateTime(2026, Month.SEPTEMBER, 15, 16, 0).toInstant(tz).toEpochMilliseconds()

        val previousSunday = LocalDateTime(2026, Month.SEPTEMBER, 13, 10, 0).toInstant(tz).toEpochMilliseconds()
        val previousSundayEnd = LocalDateTime(2026, Month.SEPTEMBER, 13, 12, 0).toInstant(tz).toEpochMilliseconds()

        val e1 = EdtEvent("1", "tt", "Maths", monday9am, monday1030am, "Amphi A", "#FF0000")
        val e2 = EdtEvent("2", "tt", "Physique", monday10am, monday1130am, "Lab B", "#00FF00")
        val e3 = EdtEvent("3", "tt", "Info", tuesday2pm, tuesday4pm, "Lab C", "#0000FF")
        val eOut = EdtEvent("4", "tt", "Hors Semaine", previousSunday, previousSundayEnd, "A", "#000000")

        val events = listOf(e1, e2, e3, eOut)
        val positions = calculateEventPositions(
            events = events,
            weekStart = weekStart,
            numDays = 5,
            timeZone = tz,
            startHour = 8,
            endHour = 20
        )

        // eOut is outside the 5-day week starting on Monday -> should not be included
        assertEquals(3, positions.size)

        // Positions for Monday events (e1 and e2)
        val p1 = positions.first { it.event.id == "1" }
        val p2 = positions.first { it.event.id == "2" }

        assertEquals(0, p1.dayIndex)
        assertEquals(60, p1.startMinutesFromStartHour) // 9:00 - 8:00 = 60 min
        assertEquals(90, p1.durationMinutes) // 90 min

        assertEquals(0, p2.dayIndex)
        assertEquals(120, p2.startMinutesFromStartHour) // 10:00 - 8:00 = 120 min
        assertEquals(90, p2.durationMinutes)

        // e1 and e2 overlap on Monday -> totalSlots should be 2, slotIndex should be 0 and 1
        assertEquals(2, p1.totalSlots)
        assertEquals(2, p2.totalSlots)
        assertTrue(p1.slotIndex != p2.slotIndex)

        // Position for Tuesday event (e3)
        val p3 = positions.first { it.event.id == "3" }
        assertEquals(1, p3.dayIndex)
        assertEquals(360, p3.startMinutesFromStartHour) // 14:00 - 8:00 = 360 min
        assertEquals(120, p3.durationMinutes)
        assertEquals(1, p3.totalSlots)
        assertEquals(0, p3.slotIndex)
    }

    @Test
    fun testGenerateEventSignatureAndEventProperty() {
        val title = "CM Maths TD1"
        val startMs = 1700000000000L
        val signature = generateEventSignature(title, startMs)

        val expected = "${"Maths".hashCode()}_$startMs"
        assertEquals(expected, signature)

        val event = EdtEvent(
            id = "e1",
            timetableId = "tt1",
            title = title,
            startMs = startMs,
            endMs = 1700003600000L,
            location = "Amphi A",
            colorHex = "#FF0000"
        )
        assertEquals(expected, event.signature)
    }

    @Test
    fun testFilterAgendaEventsFiltersPastEventsAndKeepsTodayAndFuture() {
        val tz = TimeZone.UTC
        val today = LocalDate(2026, 9, 15)

        val pastMs = LocalDateTime(2026, Month.SEPTEMBER, 14, 10, 0).toInstant(tz).toEpochMilliseconds()
        val todayMs = LocalDateTime(2026, Month.SEPTEMBER, 15, 8, 30).toInstant(tz).toEpochMilliseconds()
        val futureMs = LocalDateTime(2026, Month.SEPTEMBER, 16, 14, 0).toInstant(tz).toEpochMilliseconds()

        val pastEvent = EdtEvent("e1", "tt", "Past Course", pastMs, pastMs + 3600000L, "Room A", "#FF0000")
        val todayEvent = EdtEvent("e2", "tt", "Today Course", todayMs, todayMs + 3600000L, "Room B", "#00FF00")
        val futureEvent = EdtEvent("e3", "tt", "Future Course", futureMs, futureMs + 3600000L, "Room C", "#0000FF")

        val events = listOf(pastEvent, todayEvent, futureEvent)
        val filtered = filterAgendaEvents(events = events, today = today, timeZone = tz)

        assertEquals(2, filtered.size)
        assertEquals(listOf(todayEvent, futureEvent), filtered)
    }

    @Test
    fun testFilterAgendaEventsEmptyList() {
        val tz = TimeZone.UTC
        val today = LocalDate(2026, 9, 15)

        val filtered = filterAgendaEvents(events = emptyList(), today = today, timeZone = tz)
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun testCalculateEventPositions7DaysIncludesSaturdayAndSunday() {
        val tz = TimeZone.UTC
        val weekStart = LocalDate(2026, 9, 14) // Monday

        val saturday10am = LocalDateTime(2026, Month.SEPTEMBER, 19, 10, 0).toInstant(tz).toEpochMilliseconds()
        val saturday12pm = LocalDateTime(2026, Month.SEPTEMBER, 19, 12, 0).toInstant(tz).toEpochMilliseconds()

        val sunday2pm = LocalDateTime(2026, Month.SEPTEMBER, 20, 14, 0).toInstant(tz).toEpochMilliseconds()
        val sunday4pm = LocalDateTime(2026, Month.SEPTEMBER, 20, 16, 0).toInstant(tz).toEpochMilliseconds()

        val satEvent = EdtEvent("sat", "tt", "Samedi Course", saturday10am, saturday12pm, "Room S", "#FF0000")
        val sunEvent = EdtEvent("sun", "tt", "Dimanche Course", sunday2pm, sunday4pm, "Room D", "#00FF00")

        val events = listOf(satEvent, sunEvent)
        val positions = calculateEventPositions(
            events = events,
            weekStart = weekStart,
            numDays = 7,
            timeZone = tz,
            startHour = 8,
            endHour = 20
        )

        assertEquals(2, positions.size)

        val satPos = positions.first { it.event.id == "sat" }
        assertEquals(5, satPos.dayIndex) // Saturday = 5th day from Monday (0-indexed)
        assertEquals(120, satPos.startMinutesFromStartHour) // 10:00 - 8:00 = 120 min
        assertEquals(120, satPos.durationMinutes)

        val sunPos = positions.first { it.event.id == "sun" }
        assertEquals(6, sunPos.dayIndex) // Sunday = 6th day from Monday (0-indexed)
        assertEquals(360, sunPos.startMinutesFromStartHour) // 14:00 - 8:00 = 360 min
        assertEquals(120, sunPos.durationMinutes)
    }

    @Test
    fun testFormatShortDayNameSaturdayAndSunday() {
        assertEquals("SAM.", formatShortDayName(DayOfWeek.SATURDAY))
        assertEquals("DIM.", formatShortDayName(DayOfWeek.SUNDAY))
    }
}
