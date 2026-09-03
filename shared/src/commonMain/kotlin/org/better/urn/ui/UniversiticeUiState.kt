package org.better.urn.ui

import org.better.urn.data.Course
import org.better.urn.data.MoodleUser

data class UniversiticeUiState(
    val isLogged: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: MoodleUser? = null,
    val courses: List<Course> = emptyList()
)
