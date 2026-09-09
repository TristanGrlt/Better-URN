package org.better.urn.ui.edt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.better.urn.data.CacheStorage
import org.better.urn.data.EdtEvent
import org.better.urn.data.EdtRepository
import org.better.urn.data.EdtTask
import org.better.urn.data.Timetable
import org.better.urn.data.normalizeUrl

class EdtViewModel(
    private val repository: EdtRepository = EdtRepository(),
    private val isDarkTheme: Boolean = false
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _uiState = MutableStateFlow<EdtUiState>(EdtUiState.Success(emptyList()))
    val uiState: StateFlow<EdtUiState> = _uiState.asStateFlow()

    private val _timetables = MutableStateFlow<List<Timetable>>(emptyList())
    val timetables: StateFlow<List<Timetable>> = _timetables.asStateFlow()

    private val _tasks = MutableStateFlow<List<EdtTask>>(emptyList())
    val tasks: StateFlow<List<EdtTask>> = _tasks.asStateFlow()

    init {
        loadSavedTimetables()
        loadSavedTasks()
    }

    private fun loadSavedTimetables() {
        val jsonString = CacheStorage.getString(KEY_TIMETABLES)
        if (!jsonString.isNullOrBlank()) {
            try {
                val savedTimetables = json.decodeFromString<List<Timetable>>(jsonString)
                _timetables.value = savedTimetables
            } catch (_: Exception) {
            }
        }
        loadEvents()
    }

    private fun saveTimetables(timetables: List<Timetable>) {
        try {
            val jsonString = json.encodeToString(timetables)
            CacheStorage.saveString(KEY_TIMETABLES, jsonString)
        } catch (_: Exception) {
        }
    }

    private fun loadSavedTasks() {
        val jsonString = CacheStorage.getString(KEY_TASKS)
        if (!jsonString.isNullOrBlank()) {
            try {
                val savedTasks = json.decodeFromString<List<EdtTask>>(jsonString)
                _tasks.value = savedTasks
            } catch (_: Exception) {
            }
        }
    }

    private fun saveTasks(tasks: List<EdtTask>) {
        try {
            val jsonString = json.encodeToString(tasks)
            CacheStorage.saveString(KEY_TASKS, jsonString)
        } catch (_: Exception) {
        }
    }

    private fun loadCachedEvents(): List<EdtEvent> {
        val jsonString = CacheStorage.getString(KEY_EVENTS) ?: return emptyList()
        return try {
            json.decodeFromString<List<EdtEvent>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCachedEvents(events: List<EdtEvent>) {
        try {
            val jsonString = json.encodeToString(events)
            CacheStorage.saveString(KEY_EVENTS, jsonString)
        } catch (_: Exception) {
        }
    }

    private fun loadLastSyncTimestamp(): Long? {
        return CacheStorage.getString(KEY_LAST_SYNC)?.toLongOrNull()
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        try {
            CacheStorage.saveString(KEY_LAST_SYNC, timestamp.toString())
        } catch (_: Exception) {
        }
    }

    fun addTask(eventSignature: String, description: String) {
        if (description.isBlank()) return
        val newTask = EdtTask(
            eventSignature = eventSignature,
            description = description.trim()
        )
        val updatedList = _tasks.value + newTask
        _tasks.value = updatedList
        saveTasks(updatedList)
    }

    fun toggleTaskState(taskId: String) {
        val updatedList = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isDone = !task.isDone)
            } else {
                task
            }
        }
        _tasks.value = updatedList
        saveTasks(updatedList)
    }

    fun deleteTask(taskId: String) {
        val updatedList = _tasks.value.filterNot { it.id == taskId }
        _tasks.value = updatedList
        saveTasks(updatedList)
    }

    fun addTimetable(name: String, url: String) {
        val newTimetable = Timetable(
            id = "tt_${name.hashCode()}_${(100000..999999).random()}",
            name = name,
            url = normalizeUrl(url),
            isVisible = true
        )
        val updatedList = _timetables.value + newTimetable
        _timetables.value = updatedList
        saveTimetables(updatedList)
        loadEvents()
    }

    fun toggleVisibility(id: String) {
        val updatedList = _timetables.value.map { timetable ->
            if (timetable.id == id) {
                timetable.copy(isVisible = !timetable.isVisible)
            } else {
                timetable
            }
        }
        _timetables.value = updatedList
        saveTimetables(updatedList)
        loadEvents()
    }

    fun deleteTimetable(id: String) {
        val updatedList = _timetables.value.filterNot { it.id == id }
        _timetables.value = updatedList
        saveTimetables(updatedList)
        loadEvents()
    }

    fun loadEvents(isDarkTheme: Boolean = this.isDarkTheme) {
        viewModelScope.launch {
            val visibleTimetables = _timetables.value.filter { it.isVisible }
            val lastSync = loadLastSyncTimestamp()

            if (visibleTimetables.isEmpty()) {
                _uiState.value = EdtUiState.Success(
                    events = emptyList(),
                    isRefreshing = false,
                    lastSyncTimestamp = lastSync
                )
                return@launch
            }

            val cached = loadCachedEvents()
            val visibleIds = visibleTimetables.map { it.id }.toSet()
            val filteredCached = cached.filter { it.timetableId in visibleIds || it.timetableId == "default" || visibleIds.isEmpty() }

            if (filteredCached.isNotEmpty()) {
                _uiState.value = EdtUiState.Success(
                    events = filteredCached,
                    isRefreshing = true,
                    lastSyncTimestamp = lastSync,
                    refreshError = null
                )
            } else {
                _uiState.value = EdtUiState.Loading
            }

            try {
                val allEvents = visibleTimetables.flatMap { timetable ->
                    repository.fetchAndParseIcs(timetable.url, isDarkTheme, timetable.id)
                }.sortedBy { it.startMs }

                val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
                saveCachedEvents(allEvents)
                saveLastSyncTimestamp(nowMs)

                _uiState.value = EdtUiState.Success(
                    events = allEvents,
                    isRefreshing = false,
                    lastSyncTimestamp = nowMs,
                    refreshError = null
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Erreur inconnue"
                if (filteredCached.isNotEmpty()) {
                    _uiState.value = EdtUiState.Success(
                        events = filteredCached,
                        isRefreshing = false,
                        lastSyncTimestamp = lastSync,
                        refreshError = errorMsg
                    )
                } else {
                    _uiState.value = EdtUiState.Error(errorMsg)
                }
            }
        }
    }

    companion object {
        private const val KEY_TIMETABLES = "edt_timetables"
        private const val KEY_TASKS = "edt_tasks"
        private const val KEY_EVENTS = "edt_cached_events"
        private const val KEY_LAST_SYNC = "edt_last_sync_timestamp"
    }
}
