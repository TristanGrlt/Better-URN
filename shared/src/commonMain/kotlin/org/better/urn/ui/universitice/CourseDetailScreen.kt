package org.better.urn.ui.universitice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.launch
import org.better.urn.data.Course
import org.better.urn.data.CourseSection
import org.better.urn.data.DownloadState
import org.better.urn.data.ViewableFile
import org.better.urn.ui.components.BetterUrnTopBar
import org.better.urn.ui.components.M3CourseDetailLoadingView
import org.better.urn.ui.universitice.components.CourseModuleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: Course,
    sections: ImmutableList<CourseSection>,
    collapsedSectionIds: ImmutableSet<Int>,
    isLoading: Boolean,
    errorMessage: String?,
    token: String,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSectionCollapsed: (Int) -> Unit,
    onOpenFile: ((ViewableFile) -> Unit)? = null,
    onDownloadFile: ((ViewableFile) -> Unit)? = null,
    downloadState: DownloadState? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberSaveable(course.id, saver = LazyListState.Saver) { LazyListState() }
    Column(modifier = Modifier.fillMaxSize()) {
        BetterUrnTopBar(
            title = course.fullname,
            onTitleClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onBackClick = onBackClick,
            onRefresh = onRefresh,
            isRefreshing = isLoading,
            refreshContentDescription = "Actualiser le contenu du cours"
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    if (errorMessage != null && sections.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (sections.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                M3CourseDetailLoadingView()
                            } else if (errorMessage != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onRefresh,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Réessayer")
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Aucune section disponible dans ce cours",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            sections.forEach { section ->
                                if (section.modules.isNotEmpty() || !section.summary.isNullOrBlank()) {
                                    val visibleModules = section.modules.filter { it.modname != "label" || it.name.isNotBlank() }
                                    val isExpanded = !collapsedSectionIds.contains(section.id)

                                    item(key = "section_header_${section.id}") {
                                        val onToggle = remember(section.id, onToggleSectionCollapsed) {
                                            { onToggleSectionCollapsed(section.id) }
                                        }
                                        CourseSectionHeaderItem(
                                            sectionName = section.name,
                                            moduleCount = visibleModules.size,
                                            isExpanded = isExpanded,
                                            onToggleExpand = onToggle
                                        )
                                    }

                                    if (isExpanded) {
                                        if (!section.summary.isNullOrBlank()) {
                                            item(key = "section_summary_${section.id}") {
                                                CourseSectionSummaryItem(summary = section.summary)
                                            }
                                        }

                                        items(
                                            items = visibleModules,
                                            key = { module -> "module_${section.id}_${module.id}" }
                                        ) { module ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                CourseModuleItem(
                                                    module = module,
                                                    token = token,
                                                    onOpenFile = onOpenFile,
                                                    onDownloadFile = onDownloadFile,
                                                    downloadState = downloadState
                                                )
                                            }
                                        }

                                        item(key = "section_spacer_${section.id}") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSectionHeaderItem(
    sectionName: String,
    moduleCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleExpand() }
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (sectionName.isBlank()) "Général" else sectionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (moduleCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$moduleCount ressource${if (moduleCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Réduire la section" else "Déplier la section",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CourseSectionSummaryItem(summary: String) {
    if (summary.isBlank()) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}
