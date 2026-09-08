package org.better.urn.ui.edt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.better.urn.data.EdtEvent

@Composable
fun EdtEventCard(
    event: EdtEvent,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(parseHexColor(event.colorHex))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (event.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val formattedHours = formatEventHours(event.startMs, event.endMs)
                if (formattedHours.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedHours,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun parseHexColor(hexString: String): Color {
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

private fun formatEventHours(startMs: Long, endMs: Long): String {
    if (startMs == 0L || endMs == 0L) return ""
    return try {
        val timeZone = TimeZone.currentSystemDefault()
        val startDateTime = Instant.fromEpochMilliseconds(startMs).toLocalDateTime(timeZone)
        val endDateTime = Instant.fromEpochMilliseconds(endMs).toLocalDateTime(timeZone)

        fun formatTime(dt: LocalDateTime): String {
            val hour = dt.hour.toString().padStart(2, '0')
            val minute = dt.minute.toString().padStart(2, '0')
            return "$hour:$minute"
        }

        "${formatTime(startDateTime)} - ${formatTime(endDateTime)}"
    } catch (_: Exception) {
        ""
    }
}
