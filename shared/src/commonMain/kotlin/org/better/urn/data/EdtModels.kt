package org.better.urn.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Timetable(
    val id: String,
    val name: String,
    val url: String,
    val isVisible: Boolean = true
)

@Immutable
@Serializable
data class EdtEvent(
    val id: String,
    val timetableId: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val location: String,
    val colorHex: String,
    val isManual: Boolean = false
)
