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
data class EdtTask(
    val id: String = generateTaskId(),
    val eventSignature: String,
    val description: String,
    val isDone: Boolean = false
)

private fun generateTaskId(): String = "task_${kotlin.time.Clock.System.now().toEpochMilliseconds()}_${(100000..999999).random()}"

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
    val description: String = "",
    val isManual: Boolean = false
) {
    val signature: String
        get() = generateEventSignature(title, startMs)
}
