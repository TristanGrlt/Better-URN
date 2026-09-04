package org.better.urn.ui.universitice

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.better.urn.data.Course
import org.better.urn.data.CourseSection
import org.better.urn.data.MoodleUser
import org.better.urn.data.ViewableFile

@Immutable
data class UniversiticeUiState(
    val isLogged: Boolean = false,
    val token: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: MoodleUser? = null,
    val courses: ImmutableList<Course> = persistentListOf(),
    val searchQuery: String = "",
    val filteredCourses: ImmutableList<Course> = if (courses.isEmpty()) persistentListOf() else filterCourses(courses, searchQuery).toImmutableList(),
    val selectedCourse: Course? = null,
    val courseSections: ImmutableList<CourseSection> = persistentListOf(),
    val collapsedSectionIds: ImmutableSet<Int> = persistentSetOf(),
    val isLoadingCourseContent: Boolean = false,
    val activeFileViewer: ViewableFile? = null
)
