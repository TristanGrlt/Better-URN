package org.better.urn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.better.urn.data.Course
import org.better.urn.ui.components.CourseCard
import org.better.urn.ui.components.BetterUrnTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversiticeScreen(userName: String, courses: List<Course>, token: String, onCourseClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar()
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(courses) { course ->
                        CourseCard(course = course, token = token, onClick = { onCourseClick(course.id) })
                    }
                }
            }
        }
    }
}
