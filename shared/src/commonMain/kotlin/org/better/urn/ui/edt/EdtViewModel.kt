package org.better.urn.ui.edt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.better.urn.data.EdtRepository
import org.better.urn.data.Timetable

class EdtViewModel(
    private val repository: EdtRepository = EdtRepository(),
    private val isDarkTheme: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow<EdtUiState>(EdtUiState.Success(emptyList()))
    val uiState: StateFlow<EdtUiState> = _uiState.asStateFlow()

    private val _timetables = MutableStateFlow<List<Timetable>>(emptyList())
    val timetables: StateFlow<List<Timetable>> = _timetables.asStateFlow()

    fun addTimetable(name: String, url: String) {
        val newTimetable = Timetable(
            id = "tt_${name.hashCode()}_${(100000..999999).random()}",
            name = name,
            url = url,
            isVisible = true
        )
        _timetables.value = _timetables.value + newTimetable
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
                }
                _uiState.value = EdtUiState.Success(allEvents)
            } catch (e: Exception) {
                _uiState.value = EdtUiState.Error(
                    e.message ?: "Erreur inconnue"
                )
            }
        }
    }
}
