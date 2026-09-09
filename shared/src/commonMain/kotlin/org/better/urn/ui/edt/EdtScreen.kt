package org.better.urn.ui.edt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.better.urn.data.AgendaDayItem
import org.better.urn.data.EdtEvent
import org.better.urn.data.filterAgendaEvents
import org.better.urn.data.formatWeekRange
import org.better.urn.data.getMondayOfWeek
import org.better.urn.data.groupEventsAndInsertBreaks
import org.better.urn.ui.components.M3CoursesLoadingView
import org.better.urn.ui.edt.components.AddTimetableDialog
import org.better.urn.ui.edt.components.BreakDivider
import org.better.urn.ui.edt.components.CourseDetailSheet
import org.better.urn.ui.edt.components.EdtDayHeader
import org.better.urn.ui.edt.components.EdtEventCard
import org.better.urn.ui.edt.components.EdtManagementSheet
import org.better.urn.ui.edt.components.EdtWeekView
import org.better.urn.ui.edt.components.ManualCourseDialog
import org.better.urn.ui.edt.components.UpcomingTasksSheet
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.rememberModalBottomSheetState
import org.better.urn.data.EdtViewMode
import org.better.urn.data.EdtWeekDays

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EdtScreen(
    onProfileClick: (() -> Unit)? = null,
    viewModel: EdtViewModel = viewModel { EdtViewModel() },
    initialViewMode: EdtViewMode = EdtViewMode.AGENDA,
    weekDays: EdtWeekDays = EdtWeekDays.SEVEN
) {
    val uiState by viewModel.uiState.collectAsState()
    val timetables by viewModel.timetables.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val allRawEvents by viewModel.allRawEvents.collectAsState()
    val hiddenCourseTitles by viewModel.hiddenCourseTitles.collectAsState()
    val hiddenEventIds by viewModel.hiddenEventIds.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddChoiceSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EdtEvent?>(null) }
    var showManagerSheet by remember { mutableStateOf(false) }
    var showUpcomingTasksSheet by remember { mutableStateOf(false) }
    var selectedEventForDetail by remember { mutableStateOf<EdtEvent?>(null) }

    val pendingTaskSignatures = remember(tasks) {
        tasks.filter { !it.isDone }.map { it.eventSignature }.toSet()
    }

    val tabs = remember { listOf("Agenda", "Semaine") }
    val initialPage = when (initialViewMode) {
        EdtViewMode.AGENDA -> 0
        EdtViewMode.SEMAINE -> 1
    }
    val mainPagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SecondaryTabRow(
                        selectedTabIndex = mainPagerState.currentPage,
                        modifier = Modifier.widthIn(max = 280.dp),
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = mainPagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        mainPagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (mainPagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showUpcomingTasksSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Assignment,
                            contentDescription = "Tâches à venir"
                        )
                    }
                    IconButton(onClick = { showManagerSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Centre de gestion EDT"
                        )
                    }
                    IconButton(
                        onClick = { onProfileClick?.invoke() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Profil utilisateur",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if ((uiState as? EdtUiState.Success)?.isRefreshing == true) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                HorizontalPager(
                    state = mainPagerState,
                    userScrollEnabled = EdtPagerPolicy.isOuterSwipeEnabled(mainPagerState.currentPage),
                    modifier = Modifier.weight(1f)
                ) { mainPage ->
                    when (mainPage) {
                        0 -> { // Agenda View
                            when (val state = uiState) {
                                is EdtUiState.Loading -> {
                                    M3CoursesLoadingView(
                                        message = "Chargement de l'emploi du temps...",
                                        subtitle = "Récupération de vos cours"
                                    )
                                }
                                is EdtUiState.Success -> {
                                    val timeZone = remember { TimeZone.currentSystemDefault() }
                                    val today = remember {
                                        Clock.System.now().toLocalDateTime(timeZone).date
                                    }
                                    val agendaEvents = remember(state.events, today, timeZone) {
                                        filterAgendaEvents(state.events, today, timeZone)
                                    }

                                    if (agendaEvents.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Aucun événement à afficher",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    } else {
                                        val groupedEvents = remember(agendaEvents, timeZone) {
                                            agendaEvents.groupBy { event ->
                                                Instant.fromEpochMilliseconds(event.startMs)
                                                    .toLocalDateTime(timeZone)
                                                    .date
                                            }
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .widthIn(max = 800.dp)
                                                    .fillMaxSize(),
                                                contentPadding = PaddingValues(bottom = 88.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                groupedEvents.entries.forEachIndexed { groupIndex, (date, dayEvents) ->
                                                    if (groupIndex > 0) {
                                                        item(key = "sep_$date") {
                                                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                HorizontalDivider(
                                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                                )
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                            }
                                                        }
                                                    }

                                                    stickyHeader(key = "header_$date") {
                                                        Surface(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            color = MaterialTheme.colorScheme.surface
                                                        ) {
                                                            EdtDayHeader(
                                                                date = date,
                                                                today = today,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 16.dp,
                                                                    vertical = if (groupIndex == 0) 8.dp else 4.dp
                                                                )
                                                            )
                                                        }
                                                    }

                                                    val dayItems = groupEventsAndInsertBreaks(dayEvents)
                                                    items(
                                                        items = dayItems,
                                                        key = { item ->
                                                            when (item) {
                                                                is AgendaDayItem.EventGroup -> "group_${item.events.first().id}_${item.events.size}"
                                                                is AgendaDayItem.Break -> "break_${date}_${item.durationMinutes}_${dayItems.indexOf(item)}"
                                                            }
                                                        }
                                                    ) { item ->
                                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                            when (item) {
                                                                is AgendaDayItem.EventGroup -> {
                                                                    if (item.events.size == 1) {
                                                                        val event = item.events.first()
                                                                        EdtEventCard(
                                                                            event = event,
                                                                            hasPendingTasks = pendingTaskSignatures.contains(event.signature),
                                                                            onClick = { selectedEventForDetail = event }
                                                                        )
                                                                    } else {
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                        ) {
                                                                            item.events.forEach { event ->
                                                                                EdtEventCard(
                                                                                    event = event,
                                                                                    hasPendingTasks = pendingTaskSignatures.contains(event.signature),
                                                                                    onClick = { selectedEventForDetail = event },
                                                                                    modifier = Modifier.weight(1f)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                is AgendaDayItem.Break -> {
                                                                    BreakDivider(durationMinutes = item.durationMinutes)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                is EdtUiState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = { viewModel.loadEvents() }) {
                                                Text("Réessayer")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Semaine View
                            val timeZone = remember { TimeZone.currentSystemDefault() }
                            val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }
                            val initialWeekStart = remember(today) { getMondayOfWeek(today) }

                            val initialPage = 1000
                            val pagerState = rememberPagerState(initialPage = initialPage) { 2000 }
                            val coroutineScope = rememberCoroutineScope()

                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val isDesktop = maxWidth >= 600.dp
                                val currentWeekOffset = pagerState.currentPage - initialPage
                                val currentWeekStart = remember(initialWeekStart, currentWeekOffset) {
                                    LocalDate.fromEpochDays(initialWeekStart.toEpochDays() + currentWeekOffset * 7)
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (isDesktop) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Semaine précédente"
                                                )
                                            }

                                            Text(
                                                text = formatWeekRange(currentWeekStart),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                                    contentDescription = "Semaine suivante"
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }

                                    when (val state = uiState) {
                                        is EdtUiState.Loading -> {
                                            M3CoursesLoadingView(
                                                message = "Chargement de l'emploi du temps...",
                                                subtitle = "Récupération de vos cours"
                                            )
                                        }
                                        is EdtUiState.Success -> {
                                            if (state.events.isEmpty()) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Aucun événement à afficher",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            } else {
                                                EdtWeekView(
                                                    events = state.events,
                                                    initialWeekStart = initialWeekStart,
                                                    initialPage = initialPage,
                                                    pagerState = pagerState,
                                                    pendingTaskSignatures = pendingTaskSignatures,
                                                    onEventClick = { event -> selectedEventForDetail = event },
                                                    modifier = Modifier.fillMaxSize(),
                                                    numDays = weekDays.dayCount
                                                )
                                            }
                                        }
                                        is EdtUiState.Error -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = state.message,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.error,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Button(onClick = { viewModel.loadEvents() }) {
                                                        Text("Réessayer")
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

            FloatingActionButton(
                onClick = { showAddChoiceSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Ajouter un cours ou un calendrier"
                )
            }
        }

        if (showAddCourseDialog) {
            ManualCourseDialog(
                onDismiss = { showAddCourseDialog = false },
                onConfirm = { title, startMs, endMs, location, colorHex, description ->
                    viewModel.addManualEvent(
                        title = title,
                        startMs = startMs,
                        endMs = endMs,
                        location = location,
                        colorHex = colorHex,
                        description = description
                    )
                    showAddCourseDialog = false
                }
            )
        }

        eventToEdit?.let { event ->
            ManualCourseDialog(
                initialEvent = event,
                onDismiss = { eventToEdit = null },
                onConfirm = { title, startMs, endMs, location, colorHex, description ->
                    viewModel.updateManualEvent(
                        event.copy(
                            title = title,
                            startMs = startMs,
                            endMs = endMs,
                            location = location,
                            colorHex = colorHex,
                            description = description
                        )
                    )
                    eventToEdit = null
                },
                onDelete = { eventId ->
                    viewModel.deleteManualEvent(eventId)
                    eventToEdit = null
                }
            )
        }

        if (showAddDialog) {
            AddTimetableDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, url ->
                    viewModel.addTimetable(name, url)
                    showAddDialog = false
                }
            )
        }

        if (showManagerSheet) {
            val successState = uiState as? EdtUiState.Success
            EdtManagementSheet(
                timetables = timetables,
                allEvents = allRawEvents,
                hiddenCourseTitles = hiddenCourseTitles,
                hiddenEventIds = hiddenEventIds,
                isRefreshing = successState?.isRefreshing ?: false,
                lastSyncTimestamp = successState?.lastSyncTimestamp,
                refreshError = successState?.refreshError,
                onToggleVisibility = { id -> viewModel.toggleVisibility(id) },
                onDeleteTimetable = { id -> viewModel.deleteTimetable(id) },
                onAddTimetableClick = { showAddDialog = true },
                onForceRefresh = { viewModel.loadEvents() },
                onToggleCourseTitleVisibility = { courseTitle -> viewModel.toggleCourseTitleVisibility(courseTitle) },
                onUnhideCourseTitle = { courseTitle -> viewModel.unhideCourseTitle(courseTitle) },
                onUnhideEvent = { eventId -> viewModel.unhideEvent(eventId) },
                onUnhideAllCoursesForTimetable = { timetableId -> viewModel.unhideAllCoursesForTimetable(timetableId) },
                onUnhideAllHidden = { viewModel.unhideAllHidden() },
                onDismiss = { showManagerSheet = false }
            )
        }

        selectedEventForDetail?.let { event ->
            CourseDetailSheet(
                event = event,
                tasks = tasks,
                onAddTask = { description -> viewModel.addTask(event.signature, description) },
                onToggleTask = { taskId -> viewModel.toggleTaskState(taskId) },
                onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
                onEditManualEvent = { eventToEdit = it },
                onDeleteManualEvent = { eventId -> viewModel.deleteManualEvent(eventId) },
                onHideEvent = { eventId -> viewModel.hideEvent(eventId) },
                onHideCourseTitle = { courseTitle -> viewModel.hideCourseTitle(courseTitle) },
                onDismiss = { selectedEventForDetail = null }
            )
        }

        if (showUpcomingTasksSheet) {
            val eventsList = (uiState as? EdtUiState.Success)?.events ?: emptyList()
            UpcomingTasksSheet(
                tasks = tasks,
                events = eventsList,
                onToggleTask = { taskId -> viewModel.toggleTaskState(taskId) },
                onDeleteTask = { taskId -> viewModel.deleteTask(taskId) },
                onDismiss = { showUpcomingTasksSheet = false }
            )
        }
    }
}
