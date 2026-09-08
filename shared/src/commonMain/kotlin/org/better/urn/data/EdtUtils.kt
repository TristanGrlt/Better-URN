package org.better.urn.data

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Instant

sealed interface AgendaDayItem {
    data class EventGroup(val events: List<EdtEvent>) : AgendaDayItem
    data class Break(val durationMinutes: Int) : AgendaDayItem
}

/**
 * Holds calculated positioning and overlap allocation data for a single [EdtEvent] in week grid view.
 */
data class EventPosition(
    val event: EdtEvent,
    val dayIndex: Int,
    val startMinutesFromStartHour: Int,
    val durationMinutes: Int,
    val slotIndex: Int,
    val totalSlots: Int
)

private data class RawPosition(
    val event: EdtEvent,
    val dayIndex: Int,
    val startMinutesFromStartHour: Int,
    val durationMinutes: Int
)

/**
 * Returns epoch milliseconds for 00:00:00 of the given [LocalDate] in [timeZone].
 */
fun LocalDate.toStartOfDayEpochMs(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val ldt = LocalDateTime(this.year, this.month, this.day, 0, 0, 0, 0)
    return ldt.toInstant(timeZone).toEpochMilliseconds()
}

/**
 * Returns epoch milliseconds for 23:59:59 of the given [LocalDate] in [timeZone].
 */
fun LocalDate.toEndOfDayEpochMs(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val ldt = LocalDateTime(this.year, this.month, this.day, 23, 59, 59, 999_999_999)
    return ldt.toInstant(timeZone).toEpochMilliseconds()
}

/**
 * Groups overlapping events together in an [AgendaDayItem.EventGroup] and inserts
 * an [AgendaDayItem.Break] when the interval between consecutive events exceeds 20 minutes.
 */
fun groupEventsAndInsertBreaks(events: List<EdtEvent>): List<AgendaDayItem> {
    if (events.isEmpty()) return emptyList()

    val result = mutableListOf<AgendaDayItem>()
    var currentGroup = mutableListOf<EdtEvent>()
    var groupMaxEndMs = 0L

    for (event in events) {
        if (currentGroup.isEmpty()) {
            currentGroup.add(event)
            groupMaxEndMs = event.endMs
        } else {
            if (event.startMs < groupMaxEndMs) {
                currentGroup.add(event)
                groupMaxEndMs = maxOf(groupMaxEndMs, event.endMs)
            } else {
                val prevGroupMaxEndMs = groupMaxEndMs
                result.add(AgendaDayItem.EventGroup(currentGroup.toList()))

                val breakMs = event.startMs - prevGroupMaxEndMs
                if (breakMs > (20 * 60 * 1000L)) {
                    val durationMinutes = (breakMs / (60 * 1000L)).toInt()
                    result.add(AgendaDayItem.Break(durationMinutes))
                }

                currentGroup = mutableListOf(event)
                groupMaxEndMs = event.endMs
            }
        }
    }

    if (currentGroup.isNotEmpty()) {
        result.add(AgendaDayItem.EventGroup(currentGroup.toList()))
    }

    return result
}

/**
 * Formats a [DayOfWeek] into a 3-letter abbreviated uppercase string (e.g. "LUN.").
 */
fun formatShortDayName(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "LUN."
        DayOfWeek.TUESDAY -> "MAR."
        DayOfWeek.WEDNESDAY -> "MER."
        DayOfWeek.THURSDAY -> "JEU."
        DayOfWeek.FRIDAY -> "VEN."
        DayOfWeek.SATURDAY -> "SAM."
        DayOfWeek.SUNDAY -> "DIM."
    }
}

/**
 * Formats a [LocalDate] into a full French localized header string (e.g. "Lundi 15 Septembre").
 */
fun formatFullHeaderDate(date: LocalDate): String {
    val dayName = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lundi"
        DayOfWeek.TUESDAY -> "Mardi"
        DayOfWeek.WEDNESDAY -> "Mercredi"
        DayOfWeek.THURSDAY -> "Jeudi"
        DayOfWeek.FRIDAY -> "Vendredi"
        DayOfWeek.SATURDAY -> "Samedi"
        DayOfWeek.SUNDAY -> "Dimanche"
    }

    val monthName = when (date.month) {
        Month.JANUARY -> "Janvier"
        Month.FEBRUARY -> "Février"
        Month.MARCH -> "Mars"
        Month.APRIL -> "Avril"
        Month.MAY -> "Mai"
        Month.JUNE -> "Juin"
        Month.JULY -> "Juillet"
        Month.AUGUST -> "Août"
        Month.SEPTEMBER -> "Septembre"
        Month.OCTOBER -> "Octobre"
        Month.NOVEMBER -> "Novembre"
        Month.DECEMBER -> "Décembre"
    }

    return "$dayName ${date.day} $monthName"
}

/**
 * Returns a relative day string ("Aujourd'hui", "Demain", "Hier") if [date] matches [today] or adjacent days.
 */
fun getRelativeDayLabel(date: LocalDate, today: LocalDate): String? {
    val diff = date.toEpochDays() - today.toEpochDays()
    return when (diff) {
        0L -> "Aujourd'hui"
        1L -> "Demain"
        -1L -> "Hier"
        else -> null
    }
}

/**
 * Formats a [LocalDate] into a French localized header string (e.g. "Lundi 15 Sept.").
 */
fun formatHeaderDate(date: LocalDate): String {
    val dayName = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Lundi"
        DayOfWeek.TUESDAY -> "Mardi"
        DayOfWeek.WEDNESDAY -> "Mercredi"
        DayOfWeek.THURSDAY -> "Jeudi"
        DayOfWeek.FRIDAY -> "Vendredi"
        DayOfWeek.SATURDAY -> "Samedi"
        DayOfWeek.SUNDAY -> "Dimanche"
    }

    val monthName = when (date.month) {
        Month.JANUARY -> "Janv."
        Month.FEBRUARY -> "Févr."
        Month.MARCH -> "Mars"
        Month.APRIL -> "Avril"
        Month.MAY -> "Mai"
        Month.JUNE -> "Juin"
        Month.JULY -> "Juil."
        Month.AUGUST -> "Août"
        Month.SEPTEMBER -> "Sept."
        Month.OCTOBER -> "Oct."
        Month.NOVEMBER -> "Nov."
        Month.DECEMBER -> "Déc."
    }

    return "$dayName ${date.day} $monthName"
}

/**
 * Formats event start and end timestamps into a time range string (e.g. "09:00 - 10:30").
 */
fun formatEventHours(startMs: Long, endMs: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    if (startMs == 0L || endMs == 0L) return ""
    return try {
        val startDateTime = Instant.fromEpochMilliseconds(startMs).toLocalDateTime(timeZone)
        val endDateTime = Instant.fromEpochMilliseconds(endMs).toLocalDateTime(timeZone)

        fun formatTime(dt: LocalDateTime): String {
            val hour = dt.hour.toString().padStart(2, '0')
            val minute = dt.minute.toString().padStart(2, '0')
            return "$hour:$minute"
        }

        "${formatTime(startDateTime)}\u00A0-\u00A0${formatTime(endDateTime)}"
    } catch (_: Exception) {
        ""
    }
}

/**
 * Formats break duration in minutes into a human readable String (e.g. "Pause de 45 min", "Pause de 1 h 30 min").
 */
fun formatBreakDuration(durationMinutes: Int): String {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    return when {
        hours == 0 -> "Pause de $minutes min"
        minutes == 0 -> "Pause de $hours h"
        else -> "Pause de $hours h $minutes min"
    }
}

fun normalizeUrl(rawUrl: String): String {
    var trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return trimmed

    if (trimmed.startsWith("webcal://", ignoreCase = true)) {
        trimmed = "https://" + trimmed.substring(9)
    } else if (trimmed.startsWith("webcals://", ignoreCase = true)) {
        trimmed = "https://" + trimmed.substring(10)
    } else if (!trimmed.startsWith("http://", ignoreCase = true) &&
        !trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed = "https://$trimmed"
    }
    return trimmed
}

fun cleanSubjectName(summary: String): String {
    val regex = Regex("""(?i)\b(TD[0-9]*|TP[0-9]*|CM|CC|CT|Examen|Gr[ ]?[A-Z0-9]+)\b""")
    val withoutKeywords = regex.replace(summary, "")
    val withoutSpecialChars = withoutKeywords
        .replace('-', ' ')
        .replace('(', ' ')
        .replace(')', ' ')
    return withoutSpecialChars.replace(Regex("""\s+"""), " ").trim()
}

fun generateColorFromSubject(subjectName: String, isDarkTheme: Boolean): String {
    val cleaned = cleanSubjectName(subjectName)
    val absHash = abs(cleaned.hashCode())
    val hue = (absHash % 360).toFloat()
    val saturation = 0.6f
    val lightness = if (isDarkTheme) 0.7f else 0.35f
    return hslToHex(hue, saturation, lightness)
}

private fun hslToHex(hue: Float, saturation: Float, lightness: Float): String {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f

    val (rPrime, gPrime, bPrime) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255)

    fun Int.toTwoHexDigits(): String {
        val hex = this.toString(16).uppercase()
        return if (hex.length < 2) "0$hex" else hex
    }

    return "#${r.toTwoHexDigits()}${g.toTwoHexDigits()}${b.toTwoHexDigits()}"
}

/**
 * Inserts zero-width spaces (\u200B) between characters to allow Compose Text layout
 * to break lines anywhere, even in the middle of words or numbers.
 */
fun String.allowBreakAnywhere(): String {
    if (this.isEmpty()) return this
    return this.toCharArray().joinToString("\u200B")
}

/**
 * Parses a hex color string into a Compose [Color].
 */
fun parseHexColor(hexString: String): Color {
    val cleanHex = hexString.trim().removePrefix("#")
    if (cleanHex.length != 6 && cleanHex.length != 8) return Color.Gray
    return try {
        val colorLong = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorLong)
        } else {
            Color(colorLong)
        }
    } catch (_: Exception) {
        Color.Gray
    }
}

/**
 * Returns the Monday of the week containing [date].
 */
fun getMondayOfWeek(date: LocalDate): LocalDate {
    val daysFromMonday = date.dayOfWeek.ordinal
    return LocalDate.fromEpochDays(date.toEpochDays() - daysFromMonday)
}

/**
 * Formats a week date range into a French localized header string (e.g., "Semaine du 14 au 20 Septembre").
 */
fun formatWeekRange(weekStart: LocalDate): String {
    val weekEnd = LocalDate.fromEpochDays(weekStart.toEpochDays() + 6)

    val startShortMonth = when (weekStart.month) {
        Month.JANUARY -> "Janv."
        Month.FEBRUARY -> "Févr."
        Month.MARCH -> "Mars"
        Month.APRIL -> "Avril"
        Month.MAY -> "Mai"
        Month.JUNE -> "Juin"
        Month.JULY -> "Juil."
        Month.AUGUST -> "Août"
        Month.SEPTEMBER -> "Sept."
        Month.OCTOBER -> "Oct."
        Month.NOVEMBER -> "Nov."
        Month.DECEMBER -> "Déc."
    }

    val endFullMonth = when (weekEnd.month) {
        Month.JANUARY -> "Janvier"
        Month.FEBRUARY -> "Février"
        Month.MARCH -> "Mars"
        Month.APRIL -> "Avril"
        Month.MAY -> "Mai"
        Month.JUNE -> "Juin"
        Month.JULY -> "Juillet"
        Month.AUGUST -> "Août"
        Month.SEPTEMBER -> "Septembre"
        Month.OCTOBER -> "Octobre"
        Month.NOVEMBER -> "Novembre"
        Month.DECEMBER -> "Décembre"
    }

    return when {
        weekStart.month == weekEnd.month && weekStart.year == weekEnd.year -> {
            "Semaine du ${weekStart.day} au ${weekEnd.day} $endFullMonth"
        }
        weekStart.year == weekEnd.year -> {
            "Semaine du ${weekStart.day} $startShortMonth au ${weekEnd.day} $endFullMonth"
        }
        else -> {
            val endShortMonth = when (weekEnd.month) {
                Month.JANUARY -> "Janv."
                Month.FEBRUARY -> "Févr."
                Month.MARCH -> "Mars"
                Month.APRIL -> "Avril"
                Month.MAY -> "Mai"
                Month.JUNE -> "Juin"
                Month.JULY -> "Juil."
                Month.AUGUST -> "Août"
                Month.SEPTEMBER -> "Sept."
                Month.OCTOBER -> "Oct."
                Month.NOVEMBER -> "Nov."
                Month.DECEMBER -> "Déc."
            }
            "Semaine du ${weekStart.day} $startShortMonth ${weekStart.year} au ${weekEnd.day} $endShortMonth ${weekEnd.year}"
        }
    }
}

/**
 * Calculates event positioning and overlap slot allocations for rendering in the week grid view.
 */
fun calculateEventPositions(
    events: List<EdtEvent>,
    weekStart: LocalDate,
    numDays: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    startHour: Int = 8,
    endHour: Int = 20
): List<EventPosition> {
    val startHourMinutes = startHour * 60
    val totalGridMinutes = (endHour - startHour) * 60
    val weekStartEpochDays = weekStart.toEpochDays()

    val rawPositions = mutableListOf<RawPosition>()

    for (event in events) {
        val startDateTime = Instant.fromEpochMilliseconds(event.startMs).toLocalDateTime(timeZone)
        val endDateTime = Instant.fromEpochMilliseconds(event.endMs).toLocalDateTime(timeZone)

        val dayIndex = (startDateTime.date.toEpochDays() - weekStartEpochDays).toInt()
        if (dayIndex !in 0 until numDays) continue

        val startMinutesOfDay = startDateTime.hour * 60 + startDateTime.minute
        val endMinutesOfDay = endDateTime.hour * 60 + endDateTime.minute

        val startMinutesFromStartHour = (startMinutesOfDay - startHourMinutes).coerceAtLeast(0)
        val endMinutesFromStartHour = (endMinutesOfDay - startHourMinutes).coerceAtMost(totalGridMinutes)
        val duration = (endMinutesFromStartHour - startMinutesFromStartHour).coerceAtLeast(15)

        rawPositions.add(
            RawPosition(
                event = event,
                dayIndex = dayIndex,
                startMinutesFromStartHour = startMinutesFromStartHour,
                durationMinutes = duration
            )
        )
    }

    val result = mutableListOf<EventPosition>()

    for (day in 0 until numDays) {
        val dayItems = rawPositions.filter { it.dayIndex == day }
            .sortedWith(
                compareBy<RawPosition> { it.startMinutesFromStartHour }
                    .thenByDescending { it.durationMinutes }
            )

        if (dayItems.isEmpty()) continue

        val clusters = mutableListOf<MutableList<RawPosition>>()
        var currentCluster = mutableListOf<RawPosition>()
        var clusterEnd = -1

        for (item in dayItems) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(item)
                clusterEnd = item.startMinutesFromStartHour + item.durationMinutes
            } else {
                if (item.startMinutesFromStartHour < clusterEnd) {
                    currentCluster.add(item)
                    clusterEnd = maxOf(clusterEnd, item.startMinutesFromStartHour + item.durationMinutes)
                } else {
                    clusters.add(currentCluster)
                    currentCluster = mutableListOf(item)
                    clusterEnd = item.startMinutesFromStartHour + item.durationMinutes
                }
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        for (cluster in clusters) {
            val slotEndTimes = mutableListOf<Int>()
            val tempAssignments = mutableListOf<Pair<RawPosition, Int>>()

            for (item in cluster) {
                val start = item.startMinutesFromStartHour
                val end = start + item.durationMinutes

                var assignedSlot = -1
                for (i in slotEndTimes.indices) {
                    if (slotEndTimes[i] <= start) {
                        assignedSlot = i
                        slotEndTimes[i] = end
                        break
                    }
                }
                if (assignedSlot == -1) {
                    assignedSlot = slotEndTimes.size
                    slotEndTimes.add(end)
                }
                tempAssignments.add(Pair(item, assignedSlot))
            }

            val totalSlots = slotEndTimes.size
            for ((item, slotIndex) in tempAssignments) {
                result.add(
                    EventPosition(
                        event = item.event,
                        dayIndex = item.dayIndex,
                        startMinutesFromStartHour = item.startMinutesFromStartHour,
                        durationMinutes = item.durationMinutes,
                        slotIndex = slotIndex,
                        totalSlots = totalSlots
                    )
                )
            }
        }
    }

    return result
}
