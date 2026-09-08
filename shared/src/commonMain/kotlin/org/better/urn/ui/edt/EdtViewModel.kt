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
import org.better.urn.data.EdtRepository
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

    init {
        loadSavedTimetables()
    }

    private fun loadSavedTimetables() {
        val jsonString = CacheStorage.getString(KEY_TIMETABLES)
        if (!jsonString.isNullOrBlank()) {
            try {
                val savedTimetables = json.decodeFromString<List<Timetable>>(jsonString)
                _timetables.value = savedTimetables
                if (savedTimetables.any { it.isVisible }) {
                    loadEvents()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveTimetables(timetables: List<Timetable>) {
        try {
            val jsonString = json.encodeToString(timetables)
            CacheStorage.saveString(KEY_TIMETABLES, jsonString)
        } catch (_: Exception) {
        }
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
            if (visibleTimetables.isEmpty()) {
                _uiState.value = EdtUiState.Success(emptyList())
                return@launch
            }

            _uiState.value = EdtUiState.Loading
            try {
                val allEvents = visibleTimetables.flatMap { timetable ->
                    repository.fetchAndParseIcs(timetable.url, isDarkTheme)
                }.sortedBy { it.startMs }
                _uiState.value = EdtUiState.Success(allEvents)
            } catch (e: Exception) {
                _uiState.value = EdtUiState.Error(
                    e.message ?: "Erreur inconnue"
                )
            }
        }
    }

    companion object {
        private const val KEY_TIMETABLES = "edt_timetables"
    }
}
