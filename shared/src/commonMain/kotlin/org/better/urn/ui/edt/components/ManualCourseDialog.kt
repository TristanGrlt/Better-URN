package org.better.urn.ui.edt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.better.urn.data.EdtEvent
import org.better.urn.data.formatFullHeaderDate
import org.better.urn.data.parseHexColor

private val PRESET_COLORS = listOf(
    "#1E88E5", // Blue
    "#43A047", // Green
    "#FB8C00", // Orange
    "#8E24AA", // Purple
    "#E53935", // Red
    "#00ACC1", // Teal
    "#3F51B5", // Indigo
    "#D81B60"  // Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCourseDialog(
    initialEvent: EdtEvent? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        startMs: Long,
        endMs: Long,
        location: String,
        colorHex: String,
        description: String
    ) -> Unit,
    onDelete: ((eventId: String) -> Unit)? = null
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val now = remember { kotlin.time.Clock.System.now().toLocalDateTime(timeZone) }

    val initialStartDateTime = remember(initialEvent) {
        if (initialEvent != null && initialEvent.startMs > 0) {
            Instant.fromEpochMilliseconds(initialEvent.startMs).toLocalDateTime(timeZone)
        } else {
            LocalDateTime(now.year, now.month, now.day, 8, 0, 0, 0)
        }
    }

    val initialEndDateTime = remember(initialEvent) {
        if (initialEvent != null && initialEvent.endMs > 0) {
            Instant.fromEpochMilliseconds(initialEvent.endMs).toLocalDateTime(timeZone)
        } else {
            LocalDateTime(now.year, now.month, now.day, 10, 0, 0, 0)
        }
    }

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialEvent?.colorHex ?: PRESET_COLORS.first()) }

    var selectedDate by remember {
        mutableStateOf(
            LocalDate(initialStartDateTime.year, initialStartDateTime.month, initialStartDateTime.day)
        )
    }

    var startHour by remember { mutableStateOf(initialStartDateTime.hour) }
    var startMinute by remember { mutableStateOf(initialStartDateTime.minute) }
    var endHour by remember { mutableStateOf(initialEndDateTime.hour) }
    var endMinute by remember { mutableStateOf(initialEndDateTime.minute) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isTitleError = title.isNotBlank() && title.trim().isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEvent == null) "Ajouter un cours" else "Modifier le cours",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Course Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nom du cours *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Title,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    isError = isTitleError,
                    modifier = Modifier.fillMaxWidth()
                )

                // Location / Room
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Salle / Lieu") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Selection Card
                Text(
                    text = "Date et horaire",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Date du cours",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatFullHeaderDate(selectedDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Time Pickers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Début",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedCard(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Fin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Color Selection
                Text(
                    text = "Couleur d'affichage",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PRESET_COLORS.forEach { hex ->
                        val color = parseHexColor(hex)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Couleur sélectionnée",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Remarques / Description") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val ldtStart = LocalDateTime(
                            selectedDate.year,
                            selectedDate.month,
                            selectedDate.day,
                            startHour,
                            startMinute,
                            0,
                            0
                        )
                        val ldtEnd = LocalDateTime(
                            selectedDate.year,
                            selectedDate.month,
                            selectedDate.day,
                            endHour,
                            endMinute,
                            0,
                            0
                        )

                        val startMs = ldtStart.toInstant(timeZone).toEpochMilliseconds()
                        var endMs = ldtEnd.toInstant(timeZone).toEpochMilliseconds()

                        if (endMs <= startMs) {
                            endMs = startMs + (60 * 60 * 1000L) // Default 1 hour if end <= start
                        }

                        onConfirm(
                            title.trim(),
                            startMs,
                            endMs,
                            location.trim(),
                            selectedColorHex,
                            description.trim()
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (initialEvent == null) "Ajouter" else "Enregistrer")
            }
        },
        dismissButton = {
            Row {
                if (initialEvent != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialEvent.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Supprimer le cours",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )

    // M3 DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDateTime(
                selectedDate.year,
                selectedDate.month,
                selectedDate.day,
                12,
                0,
                0,
                0
            ).toInstant(timeZone).toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            val pickedLdt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC)
                            selectedDate = LocalDate(pickedLdt.year, pickedLdt.month, pickedLdt.day)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // M3 Start Time Picker Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )

        Dialog(onDismissRequest = { showStartTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Heure de début",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showStartTimePicker = false }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                startHour = timePickerState.hour
                                startMinute = timePickerState.minute
                                if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                                    endHour = (startHour + 1) % 24
                                    endMinute = startMinute
                                }
                                showStartTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    // M3 End Time Picker Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = true
        )

        Dialog(onDismissRequest = { showEndTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Heure de fin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEndTimePicker = false }) {
                            Text("Annuler")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                endHour = timePickerState.hour
                                endMinute = timePickerState.minute
                                showEndTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
