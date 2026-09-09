package org.better.urn.ui.edt.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.better.urn.data.EdtEvent
import org.better.urn.data.EventPosition
import org.better.urn.data.calculateEventPositions
import org.better.urn.data.formatShortDayName
import org.better.urn.data.toEndOfDayEpochMs
import org.better.urn.data.toStartOfDayEpochMs

@Composable
fun EdtWeekView(
    events: List<EdtEvent>,
    weekStart: LocalDate,
    modifier: Modifier = Modifier,
    pendingTaskSignatures: Set<String> = emptySet(),
    onEventClick: ((EdtEvent) -> Unit)? = null,
    startHour: Int = 8,
    endHour: Int = 20,
    hourHeight: Dp = 60.dp,
    timeAxisWidth: Dp = 44.dp
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }

    val weekEnd = remember(weekStart) {
        LocalDate.fromEpochDays(weekStart.toEpochDays() + 6)
    }

    val weekStartMs = remember(weekStart, timeZone) {
        weekStart.toStartOfDayEpochMs(timeZone)
    }
    val weekEndMs = remember(weekEnd, timeZone) {
        weekEnd.toEndOfDayEpochMs(timeZone)
    }

    val weekEvents = remember(events, weekStartMs, weekEndMs) {
        events.filter { it.startMs in weekStartMs..weekEndMs }
    }

    val hasSaturdayEvents = remember(weekEvents, timeZone) {
        weekEvents.any { event ->
            Instant.fromEpochMilliseconds(event.startMs).toLocalDateTime(timeZone).date.dayOfWeek == DayOfWeek.SATURDAY
        }
    }
    val hasSundayEvents = remember(weekEvents, timeZone) {
        weekEvents.any { event ->
            Instant.fromEpochMilliseconds(event.startMs).toLocalDateTime(timeZone).date.dayOfWeek == DayOfWeek.SUNDAY
        }
    }

    val numDays = when {
        hasSundayEvents -> 7
        hasSaturdayEvents -> 6
        else -> 5
    }

    val eventPositions = remember(weekEvents, weekStart, numDays, timeZone, startHour, endHour) {
        calculateEventPositions(
            events = weekEvents,
            weekStart = weekStart,
            numDays = numDays,
            timeZone = timeZone,
            startHour = startHour,
            endHour = endHour
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        WeekHeaderRow(
            weekStart = weekStart,
            numDays = numDays,
            today = today,
            timeAxisWidth = timeAxisWidth
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        val scrollState = rememberScrollState()
        val totalHours = endHour - startHour
        val gridHeight = hourHeight * totalHours

        val gridOutlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .drawBehind {
                        val timeAxisWidthPx = timeAxisWidth.toPx()
                        val availableWidth = size.width - timeAxisWidthPx
                        val colWidth = availableWidth / numDays
                        val hourHeightPx = hourHeight.toPx()

                        for (i in 0..totalHours) {
                            val y = i * hourHeightPx
                            drawLine(
                                color = gridOutlineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        for (d in 0..numDays) {
                            val x = timeAxisWidthPx + d * colWidth
                            drawLine(
                                color = gridOutlineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .width(timeAxisWidth)
                        .height(gridHeight)
                ) {
                    for (hour in startHour until endHour) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "${hour.toString().padStart(2, '0')}:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            WeekEventsLayout(
                eventPositions = eventPositions,
                numDays = numDays,
                timeAxisWidth = timeAxisWidth,
                hourHeight = hourHeight,
                totalHours = totalHours,
                pendingTaskSignatures = pendingTaskSignatures,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
private fun WeekHeaderRow(
    weekStart: LocalDate,
    numDays: Int,
    today: LocalDate,
    timeAxisWidth: Dp
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.width(timeAxisWidth))

            for (d in 0 until numDays) {
                val date = remember(weekStart, d) {
                    LocalDate.fromEpochDays(weekStart.toEpochDays() + d)
                }
                val isToday = date == today

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatShortDayName(date.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = date.day.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekEventsLayout(
    eventPositions: List<EventPosition>,
    numDays: Int,
    timeAxisWidth: Dp,
    hourHeight: Dp,
    totalHours: Int,
    pendingTaskSignatures: Set<String>,
    onEventClick: ((EdtEvent) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val timeAxisWidthPx = with(density) { timeAxisWidth.toPx() }
    val pixelsPerMinute = hourHeightPx / 60f
    val totalGridHeightPx = totalHours * 60 * pixelsPerMinute

    Layout(
        content = {
            eventPositions.forEach { pos ->
                EdtGridCard(
                    event = pos.event,
                    hasPendingTasks = pendingTaskSignatures.contains(pos.event.signature),
                    onClick = if (onEventClick != null) { { onEventClick(pos.event) } } else null,
                    modifier = Modifier
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(hourHeight * totalHours)
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val availableWidth = (totalWidth - timeAxisWidthPx).coerceAtLeast(0f)
        val columnWidth = availableWidth / numDays

        val placeables = measurables.mapIndexed { index, measurable ->
            val pos = eventPositions[index]
            val subColumnWidth = columnWidth / pos.totalSlots

            val cardWidthPx = (subColumnWidth - 2f).coerceAtLeast(1f).toInt()
            val cardHeightPx = (pos.durationMinutes * pixelsPerMinute).toInt().coerceAtLeast(1)

            val childConstraints = Constraints.fixed(cardWidthPx, cardHeightPx)
            val placeable = measurable.measure(childConstraints)

            val x = (timeAxisWidthPx + pos.dayIndex * columnWidth + pos.slotIndex * subColumnWidth + 1f).toInt()
            val y = (pos.startMinutesFromStartHour * pixelsPerMinute).toInt()

            Triple(placeable, x, y)
        }

        layout(totalWidth, totalGridHeightPx.toInt()) {
            placeables.forEach { (placeable, x, y) ->
                placeable.place(x, y)
            }
        }
    }
}
