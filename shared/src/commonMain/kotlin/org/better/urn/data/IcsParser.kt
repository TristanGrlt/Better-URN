package org.better.urn.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object IcsParser {

    suspend fun parseIcs(
        icsContent: String,
        timetableId: String,
        isDarkTheme: Boolean
    ): List<EdtEvent> {
        if (icsContent.isBlank()) return emptyList()

        val unfoldedContent = icsContent
            .replace("\r\n ", "")
            .replace("\r\n\t", "")
            .replace("\n ", "")
            .replace("\n\t", "")
            .replace("\r", "")

        val blocks = unfoldedContent.split("BEGIN:VEVENT")
        val events = mutableListOf<EdtEvent>()

        for (i in 1 until blocks.size) {
            val block = blocks[i]
            val eventBlock = if (block.contains("END:VEVENT")) {
                block.substringBefore("END:VEVENT")
            } else {
                block
            }

            var uid: String? = null
            var summary: String? = null
            var location: String? = null
            var description: String? = null
            var dtStartRaw: String? = null
            var dtStartTzId: String? = null
            var dtEndRaw: String? = null
            var dtEndTzId: String? = null

            val lines = eventBlock.lines()
            for (line in lines) {
                val colonIndex = line.indexOf(':')
                if (colonIndex <= 0) continue

                val keyAndParams = line.substring(0, colonIndex).trim()
                val rawValue = line.substring(colonIndex + 1).trim()

                val paramParts = keyAndParams.split(';')
                val key = paramParts[0].uppercase().trim()

                var tzId: String? = null
                for (p in paramParts.drop(1)) {
                    if (p.uppercase().startsWith("TZID=")) {
                        tzId = p.substring(5).trim('"')
                    }
                }

                val unescapedVal = unescapeIcsValue(rawValue)

                when (key) {
                    "UID" -> uid = unescapedVal
                    "SUMMARY" -> summary = unescapedVal
                    "LOCATION" -> location = unescapedVal
                    "DESCRIPTION" -> description = unescapedVal
                    "DTSTART" -> {
                        dtStartRaw = rawValue
                        dtStartTzId = tzId
                    }
                    "DTEND" -> {
                        dtEndRaw = rawValue
                        dtEndTzId = tzId
                    }
                }
            }

            if (dtStartRaw != null && dtEndRaw != null) {
                val title = summary ?: "Sans titre"
                val loc = location ?: ""
                val desc = description ?: ""
                val color = generateColorFromSubject(title, isDarkTheme)
                val startMs = parseIcsDateToEpochMs(dtStartRaw, dtStartTzId)
                val endMs = parseIcsDateToEpochMs(dtEndRaw, dtEndTzId)
                val eventId = uid ?: "event_${timetableId}_${events.size}_$startMs"

                events.add(
                    EdtEvent(
                        id = eventId,
                        timetableId = timetableId,
                        title = title,
                        startMs = startMs,
                        endMs = endMs,
                        location = loc,
                        colorHex = color,
                        description = desc
                    )
                )
            }
        }

        return events
    }

    private fun unescapeIcsValue(value: String): String {
        return value
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .trim()
    }

    private fun parseIcsDateToEpochMs(rawDateStr: String, tzIdParam: String? = null): Long {
        val cleanStr = rawDateStr.trim()
        val isUtc = cleanStr.endsWith("Z", ignoreCase = true)

        val isoStr = if (cleanStr.contains("-") || cleanStr.contains(":")) {
            cleanStr
        } else if (cleanStr.length >= 15 && cleanStr[8] == 'T') {
            val year = cleanStr.substring(0, 4)
            val month = cleanStr.substring(4, 6)
            val day = cleanStr.substring(6, 8)
            val hour = cleanStr.substring(9, 11)
            val min = cleanStr.substring(11, 13)
            val sec = cleanStr.substring(13, 15)
            val z = if (isUtc) "Z" else ""
            "$year-$month-${day}T$hour:$min:$sec$z"
        } else if (cleanStr.length == 8) {
            val year = cleanStr.substring(0, 4)
            val month = cleanStr.substring(4, 6)
            val day = cleanStr.substring(6, 8)
            "$year-$month-${day}T00:00:00"
        } else {
            cleanStr
        }

        return try {
            if (isoStr.endsWith("Z", ignoreCase = true)) {
                Instant.parse(isoStr).toEpochMilliseconds()
            } else {
                val ldt = LocalDateTime.parse(isoStr)
                val timeZone = if (!tzIdParam.isNullOrBlank()) {
                    runCatching { TimeZone.of(tzIdParam) }.getOrDefault(TimeZone.currentSystemDefault())
                } else {
                    TimeZone.currentSystemDefault()
                }
                ldt.toInstant(timeZone).toEpochMilliseconds()
            }
        } catch (e: Exception) {
            0L
        }
    }
}
