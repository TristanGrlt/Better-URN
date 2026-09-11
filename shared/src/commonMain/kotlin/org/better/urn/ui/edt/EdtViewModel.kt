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
import org.better.urn.data.filterVisibleEvents
import org.better.urn.data.normalizeUrl

class EdtViewModel(
    private val repository: EdtRepository = EdtRepository(),
    private val isDarkTheme: Boolean = false,
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

    private val _manualEvents = MutableStateFlow<List<EdtEvent>>(emptyList())
    val manualEvents: StateFlow<List<EdtEvent>> = _manualEvents.asStateFlow()

    private val _hiddenEventIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenEventIds: StateFlow<Set<String>> = _hiddenEventIds.asStateFlow()

    private val _hiddenCourseTitles = MutableStateFlow<Set<String>>(emptySet())
    val hiddenCourseTitles: StateFlow<Set<String>> = _hiddenCourseTitles.asStateFlow()

    private val _allRawEvents = MutableStateFlow<List<EdtEvent>>(emptyList())
    val allRawEvents: StateFlow<List<EdtEvent>> = _allRawEvents.asStateFlow()

    init {
        loadSavedHiddenEventsAndCourses()
        loadSavedManualEvents()
        loadSavedTimetables()
        loadSavedTasks()
    }

    private fun loadSavedHiddenEventsAndCourses() {
        val hiddenEventsJson = CacheStorage.getString(KEY_HIDDEN_EVENT_IDS)
        if (!hiddenEventsJson.isNullOrBlank()) {
            try {
                _hiddenEventIds.value = json.decodeFromString<Set<String>>(hiddenEventsJson)
            } catch (_: Exception) {}
        }
        val hiddenTitlesJson = CacheStorage.getString(KEY_HIDDEN_COURSE_TITLES)
        if (!hiddenTitlesJson.isNullOrBlank()) {
            try {
                _hiddenCourseTitles.value = json.decodeFromString<Set<String>>(hiddenTitlesJson)
            } catch (_: Exception) {}
        }
    }

    private fun saveHiddenEventIds(set: Set<String>) {
        try {
            CacheStorage.saveString(KEY_HIDDEN_EVENT_IDS, json.encodeToString(set))
        } catch (_: Exception) {}
    }

    private fun saveHiddenCourseTitles(set: Set<String>) {
        try {
            CacheStorage.saveString(KEY_HIDDEN_COURSE_TITLES, json.encodeToString(set))
        } catch (_: Exception) {}
    }

    private fun loadSavedManualEvents() {
        val jsonString = CacheStorage.getString(KEY_MANUAL_EVENTS)
        if (!jsonString.isNullOrBlank()) {
            try {
                val savedManualEvents = json.decodeFromString<List<EdtEvent>>(jsonString)
                _manualEvents.value = savedManualEvents
            } catch (_: Exception) {
            }
        }
    }

    private fun saveManualEvents(events: List<EdtEvent>) {
        try {
            val jsonString = json.encodeToString(events)
            CacheStorage.saveString(KEY_MANUAL_EVENTS, jsonString)
        } catch (_: Exception) {
        }
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

    fun addManualEvent(
        title: String,
        startMs: Long,
        endMs: Long,
        location: String,
        colorHex: String,
        description: String = ""
    ) {
        val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val newEvent = EdtEvent(
            id = "manual_${nowMs}_${(100000..999999).random()}",
            timetableId = "manual",
            title = title.trim(),
            startMs = startMs,
            endMs = endMs,
            location = location.trim(),
            colorHex = colorHex,
            description = description.trim(),
            isManual = true
        )
        val updated = _manualEvents.value + newEvent
        _manualEvents.value = updated
        saveManualEvents(updated)
        loadEvents()
    }

    fun updateManualEvent(event: EdtEvent) {
        val updated = _manualEvents.value.map {
            if (it.id == event.id) event else it
        }
        _manualEvents.value = updated
        saveManualEvents(updated)
        loadEvents()
    }

    fun deleteManualEvent(eventId: String) {
        val updated = _manualEvents.value.filterNot { it.id == eventId }
        _manualEvents.value = updated
        saveManualEvents(updated)
        loadEvents()
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

    fun hideEvent(eventId: String) {
        val updated = _hiddenEventIds.value + eventId
        _hiddenEventIds.value = updated
        saveHiddenEventIds(updated)
        updateVisibleEvents()
    }

    fun unhideEvent(eventId: String) {
        val updated = _hiddenEventIds.value - eventId
        _hiddenEventIds.value = updated
        saveHiddenEventIds(updated)
        updateVisibleEvents()
    }

    fun hideCourseTitle(courseTitle: String) {
        val title = courseTitle.trim()
        if (title.isBlank()) return
        val updated = _hiddenCourseTitles.value + title
        _hiddenCourseTitles.value = updated
        saveHiddenCourseTitles(updated)
        updateVisibleEvents()
    }

    fun unhideCourseTitle(courseTitle: String) {
        val title = courseTitle.trim()
        val updatedTitles = _hiddenCourseTitles.value - title
        _hiddenCourseTitles.value = updatedTitles
        saveHiddenCourseTitles(updatedTitles)

        val rawList = _allRawEvents.value
        val eventIdsToRemove = rawList.asSequence().filter {
            it.title.trim() == title
        }.map { it.id }.toSet()

        if (eventIdsToRemove.isNotEmpty()) {
            val updatedEvents = _hiddenEventIds.value - eventIdsToRemove
            _hiddenEventIds.value = updatedEvents
            saveHiddenEventIds(updatedEvents)
        }

        updateVisibleEvents()
    }

    fun toggleCourseTitleVisibility(courseTitle: String) {
        val title = courseTitle.trim()
        if (title in _hiddenCourseTitles.value) {
            unhideCourseTitle(title)
        } else {
            hideCourseTitle(title)
        }
    }

    fun unhideAllCoursesForTimetable(timetableId: String) {
        val rawList = _allRawEvents.value.filter { it.timetableId == timetableId }
        val titlesToRemove = rawList.map { it.title.trim() }.toSet()
        val eventIdsToRemove = rawList.map { it.id }.toSet()

        val updatedTitles = _hiddenCourseTitles.value - titlesToRemove
        _hiddenCourseTitles.value = updatedTitles
        saveHiddenCourseTitles(updatedTitles)

        val updatedEvents = _hiddenEventIds.value - eventIdsToRemove
        _hiddenEventIds.value = updatedEvents
        saveHiddenEventIds(updatedEvents)

        updateVisibleEvents()
    }

    fun unhideAllHidden() {
        _hiddenCourseTitles.value = emptySet()
        _hiddenEventIds.value = emptySet()
        saveHiddenCourseTitles(emptySet())
        saveHiddenEventIds(emptySet())
        updateVisibleEvents()
    }

    private fun updateVisibleEvents() {
        val rawList = _allRawEvents.value
        val visible = filterVisibleEvents(rawList, _hiddenEventIds.value, _hiddenCourseTitles.value)
        val currentState = _uiState.value
        if (currentState is EdtUiState.Success) {
            _uiState.value = currentState.copy(events = visible)
        } else {
            _uiState.value = EdtUiState.Success(
                events = visible,
                isRefreshing = false,
                lastSyncTimestamp = loadLastSyncTimestamp()
            )
        }
    }

    fun loadEvents(isDarkTheme: Boolean = this.isDarkTheme) {
        viewModelScope.launch {
            val visibleTimetables = _timetables.value.filter { it.isVisible }
            val lastSync = loadLastSyncTimestamp()
            val manualList = _manualEvents.value

            if (visibleTimetables.isEmpty()) {
                val combined = manualList.sortedBy { it.startMs }
                _allRawEvents.value = combined
                val visible = filterVisibleEvents(combined, _hiddenEventIds.value, _hiddenCourseTitles.value)
                _uiState.value = EdtUiState.Success(
                    events = visible,
                    isRefreshing = false,
                    lastSyncTimestamp = lastSync
                )
                return@launch
            }

            val cached = loadCachedEvents()
            val visibleIds = visibleTimetables.map { it.id }.toSet()
            val filteredCached = cached.filter { (it.timetableId in visibleIds) || (it.timetableId == "default") || visibleIds.isEmpty() }
            val combinedCached = (filteredCached + manualList).distinctBy { it.id }.sortedBy { it.startMs }
            _allRawEvents.value = combinedCached
            val visibleCached = filterVisibleEvents(combinedCached, _hiddenEventIds.value, _hiddenCourseTitles.value)

            if (visibleCached.isNotEmpty()) {
                _uiState.value = EdtUiState.Success(
                    events = visibleCached,
                    isRefreshing = true,
                    lastSyncTimestamp = lastSync,
                    refreshError = null
                )
            } else if (combinedCached.isNotEmpty()) {
                _uiState.value = EdtUiState.Success(
                    events = emptyList(),
                    isRefreshing = true,
                    lastSyncTimestamp = lastSync,
                    refreshError = null
                )
            } else {
                _uiState.value = EdtUiState.Loading
            }

            try {
                val remoteEvents = visibleTimetables.flatMap { timetable ->
                    repository.fetchAndParseIcs(timetable.url, isDarkTheme, timetable.id)
                }

                val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
                saveCachedEvents(remoteEvents)
                saveLastSyncTimestamp(nowMs)

                val allEvents = (remoteEvents + manualList).distinctBy { it.id }.sortedBy { it.startMs }
                _allRawEvents.value = allEvents
                val visibleEvents = filterVisibleEvents(allEvents, _hiddenEventIds.value, _hiddenCourseTitles.value)

                _uiState.value = EdtUiState.Success(
                    events = visibleEvents,
                    isRefreshing = false,
                    lastSyncTimestamp = nowMs,
                    refreshError = null
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Erreur inconnue"
                if (combinedCached.isNotEmpty()) {
                    _uiState.value = EdtUiState.Success(
                        events = filterVisibleEvents(combinedCached, _hiddenEventIds.value, _hiddenCourseTitles.value),
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
        private const val KEY_MANUAL_EVENTS = "edt_manual_events"
        private const val KEY_HIDDEN_EVENT_IDS = "edt_hidden_event_ids"
        private const val KEY_HIDDEN_COURSE_TITLES = "edt_hidden_course_titles"
    }
}
