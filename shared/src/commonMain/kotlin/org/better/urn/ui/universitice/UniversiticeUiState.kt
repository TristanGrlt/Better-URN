package org.better.urn.ui.universitice

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.better.urn.data.Course
import org.better.urn.data.CourseSection
import org.better.urn.data.DownloadState
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
    val hiddenCourseIds: ImmutableSet<Int> = persistentSetOf(),
    val isHiddenSectionExpanded: Boolean = false,
    val searchQuery: String = "",
    val filteredCourses: ImmutableList<Course> = if (courses.isEmpty()) persistentListOf() else filterCourses(courses, searchQuery).toImmutableList(),
    val selectedCourse: Course? = null,
    val courseSections: ImmutableList<CourseSection> = persistentListOf(),
    val collapsedSectionIds: ImmutableSet<Int> = persistentSetOf(),
    val isLoadingCourseContent: Boolean = false,
    val activeFileViewer: ViewableFile? = null,
    val downloadState: DownloadState? = null
) {
    val visibleCourses: ImmutableList<Course>
        get() = courses.filter { it.id !in hiddenCourseIds }.toImmutableList()

    val hiddenCourses: ImmutableList<Course>
        get() = courses.filter { it.id in hiddenCourseIds }.toImmutableList()

    val filteredVisibleCourses: ImmutableList<Course>
        get() = filteredCourses.filter { it.id !in hiddenCourseIds }.toImmutableList()

    val filteredHiddenCourses: ImmutableList<Course>
        get() = filteredCourses.filter { it.id in hiddenCourseIds }.toImmutableList()
}
