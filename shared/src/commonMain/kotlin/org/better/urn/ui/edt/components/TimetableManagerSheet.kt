package org.better.urn.ui.edt.components

import androidx.compose.runtime.Composable
import org.better.urn.data.Timetable

@Composable
fun TimetableManagerSheet(
    timetables: List<Timetable>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    EdtManagementSheet(
        timetables = timetables,
        isRefreshing = false,
        lastSyncTimestamp = null,
        refreshError = null,
        onToggleVisibility = onToggle,
        onDeleteTimetable = onDelete,
        onAddTimetableClick = {},
        onForceRefresh = {},
        onDismiss = onDismiss
    )
}
