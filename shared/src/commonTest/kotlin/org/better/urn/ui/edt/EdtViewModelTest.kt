package org.better.urn.ui.edt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.better.urn.data.CacheStorage
import org.better.urn.data.EdtEvent
import org.better.urn.data.EdtRepository
import org.better.urn.data.formatLastSyncTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EdtViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        CacheStorage.clear()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CacheStorage.clear()
    }

    @Test
    fun testEdtEventIsManualDefaultValue() {
        val event = EdtEvent(
            id = "e1",
            timetableId = "default",
            title = "Maths CM",
            startMs = 1700000000000L,
            endMs = 1700003600000L,
            location = "Amphi A",
            colorHex = "#FF0000"
        )
        assertFalse(event.isManual)
    }

    @Test
    fun testViewModelInitialStateIsEmptySuccess() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel = EdtViewModel(repository = fakeRepo)

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(emptyList(), state.events)
        assertEquals(emptyList(), viewModel.timetables.value)
    }

    @Test
    fun testAddTimetableAndFetchEvents() = runTest {
        val sampleEvents = listOf(
            EdtEvent(
                id = "e1",
                timetableId = "default",
                title = "Maths CM",
                startMs = 1700000000000L,
                endMs = 1700003600000L,
                location = "Amphi A",
                colorHex = "#FF0000",
                isManual = false
            )
        )

        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return sampleEvents
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addTimetable("Mon EDT", "https://example.com/cal.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.timetables.value.size)
        assertEquals("Mon EDT", viewModel.timetables.value.first().name)
        assertEquals("https://example.com/cal.ics", viewModel.timetables.value.first().url)

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(sampleEvents, state.events)
        assertNotNull(state.lastSyncTimestamp)
    }

    @Test
    fun testViewModelErrorStateOnAddTimetableWhenNoCache() = runTest {
        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                throw Exception("Erreur réseau: UnknownHostException")
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addTimetable("EDT Erreur", "https://bad-url.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Error)
        assertEquals("Erreur réseau: UnknownHostException", state.message)
    }

    @Test
    fun testCachedEventsLoadedFirstBeforeNetworkUpdate() = runTest {
        val cachedEvents = listOf(
            EdtEvent("e1", "default", "Histoire Ancien Cache", 1700000000000L, 1700003600000L, "Amphi B", "#00FF00")
        )
        val freshEvents = listOf(
            EdtEvent("e1", "default", "Histoire Nouveau Réseau", 1700000000000L, 1700003600000L, "Amphi B", "#00FF00")
        )

        val fakeRepo1 = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return cachedEvents
            }
        }

        val viewModel1 = EdtViewModel(repository = fakeRepo1)
        viewModel1.addTimetable("Mon EDT", "https://example.com/cal.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        // Create new VM with fakeRepo2 that provides freshEvents after a small suspension delay
        val fakeRepo2 = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                kotlinx.coroutines.delay(100L)
                return freshEvents
            }
        }

        val viewModel2 = EdtViewModel(repository = fakeRepo2)
        testDispatcher.scheduler.runCurrent()

        val initialState = viewModel2.uiState.value
        assertTrue(initialState is EdtUiState.Success)
        assertEquals(cachedEvents, initialState.events)
        assertTrue(initialState.isRefreshing)

        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel2.uiState.value
        assertTrue(updatedState is EdtUiState.Success)
        assertEquals(freshEvents, updatedState.events)
        assertFalse(updatedState.isRefreshing)
        assertNotNull(updatedState.lastSyncTimestamp)
    }

    @Test
    fun testCachedEventsRetainedWhenNetworkFetchFails() = runTest {
        val cachedEvents = listOf(
            EdtEvent("e1", "default", "Physique MPSI", 1700000000000L, 1700003600000L, "Salle 12", "#FF00FF")
        )

        val fakeRepoSuccess = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return cachedEvents
            }
        }

        val viewModel1 = EdtViewModel(repository = fakeRepoSuccess)
        viewModel1.addTimetable("Mon EDT", "https://example.com/cal.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val fakeRepoFail = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                throw Exception("Erreur réseau: Pas de connexion internet")
            }
        }

        val viewModel2 = EdtViewModel(repository = fakeRepoFail)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel2.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(cachedEvents, state.events)
        assertFalse(state.isRefreshing)
        assertEquals("Erreur réseau: Pas de connexion internet", state.refreshError)
    }

    @Test
    fun testToggleVisibility() = runTest {
        val sampleEvents = listOf(
            EdtEvent(
                id = "e1",
                timetableId = "default",
                title = "Maths CM",
                startMs = 1700000000000L,
                endMs = 1700003600000L,
                location = "Amphi A",
                colorHex = "#FF0000"
            )
        )

        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return sampleEvents
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addTimetable("Mon EDT", "https://example.com/cal.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val timetableId = viewModel.timetables.value.first().id

        // Toggle visibility to false
        viewModel.toggleVisibility(timetableId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.timetables.value.first().isVisible)
        val stateHidden = viewModel.uiState.value
        assertTrue(stateHidden is EdtUiState.Success)
        assertEquals(emptyList(), stateHidden.events)

        // Toggle visibility back to true
        viewModel.toggleVisibility(timetableId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.timetables.value.first().isVisible)
        val stateVisible = viewModel.uiState.value
        assertTrue(stateVisible is EdtUiState.Success)
        assertEquals(sampleEvents, stateVisible.events)
    }

    @Test
    fun testDeleteTimetable() = runTest {
        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return listOf(
                    EdtEvent("e1", "default", "Maths", 1700000000000L, 1700003600000L, "A", "#FF0000")
                )
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addTimetable("EDT 1", "https://example.com/1.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val id = viewModel.timetables.value.first().id
        viewModel.deleteTimetable(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.timetables.value.isEmpty())
        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(emptyList(), state.events)
    }

    @Test
    fun testPersistenceWithCacheStorage() = runTest {
        val sampleEvents = listOf(
            EdtEvent("e1", "default", "Info", 1700000000000L, 1700003600000L, "B", "#00FF00")
        )
        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return sampleEvents
            }
        }

        val viewModel1 = EdtViewModel(repository = fakeRepo)
        viewModel1.addTimetable("EDT Persistant", "https://example.com/p.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val viewModel2 = EdtViewModel(repository = fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel2.timetables.value.size)
        assertEquals("EDT Persistant", viewModel2.timetables.value.first().name)
        val state = viewModel2.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(sampleEvents, state.events)
    }

    @Test
    fun testLoadEventsSortingByStartMs() = runTest {
        val eventLater = EdtEvent("e2", "default", "Anglais", 1700005000000L, 1700008000000L, "C1", "#0000FF")
        val eventEarlier = EdtEvent("e1", "default", "Maths", 1700000000000L, 1700003600000L, "Amphi A", "#FF0000")
        val unorderedEvents = listOf(eventLater, eventEarlier)

        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return unorderedEvents
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addTimetable("EDT Test", "https://example.com/test.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(listOf(eventEarlier, eventLater), state.events)
    }

    @Test
    fun testTaskManagement() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel = EdtViewModel(repository = fakeRepo)

        val sig = "12345_1700000000000"
        assertTrue(viewModel.tasks.value.isEmpty())

        // Add task
        viewModel.addTask(sig, "   Faire les exercices 1 à 3   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.tasks.value.size)
        val task = viewModel.tasks.value.first()
        assertEquals(sig, task.eventSignature)
        assertEquals("Faire les exercices 1 à 3", task.description)
        assertFalse(task.isDone)

        // Toggle task state
        viewModel.toggleTaskState(task.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.tasks.value.first().isDone)

        // Toggle back
        viewModel.toggleTaskState(task.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.tasks.value.first().isDone)

        // Delete task
        viewModel.deleteTask(task.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.tasks.value.isEmpty())
    }

    @Test
    fun testFormatLastSyncTime() {
        assertEquals("Jamais", formatLastSyncTime(null))
        assertEquals("Jamais", formatLastSyncTime(0L))
        val formattedPast = formatLastSyncTime(1700000000000L)
        assertTrue(formattedPast.contains("à"))
    }

    @Test
    fun testAddManualEvent() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel = EdtViewModel(repository = fakeRepo)

        assertTrue(viewModel.manualEvents.value.isEmpty())

        viewModel.addManualEvent(
            title = "  Projet Mobile  ",
            startMs = 1700000000000L,
            endMs = 1700003600000L,
            location = "Salle 202",
            colorHex = "#1E88E5",
            description = "Examen final"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.manualEvents.value.size)
        val added = viewModel.manualEvents.value.first()
        assertEquals("Projet Mobile", added.title)
        assertEquals(1700000000000L, added.startMs)
        assertEquals(1700003600000L, added.endMs)
        assertEquals("Salle 202", added.location)
        assertEquals("#1E88E5", added.colorHex)
        assertEquals("Examen final", added.description)
        assertTrue(added.isManual)

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(1, state.events.size)
        assertEquals("Projet Mobile", state.events.first().title)
    }

    @Test
    fun testUpdateManualEvent() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel = EdtViewModel(repository = fakeRepo)

        viewModel.addManualEvent(
            title = "Maths",
            startMs = 1700000000000L,
            endMs = 1700003600000L,
            location = "Amphi A",
            colorHex = "#FF0000"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val created = viewModel.manualEvents.value.first()
        val updatedEvent = created.copy(
            title = "Maths Appliquées",
            location = "Amphi B"
        )

        viewModel.updateManualEvent(updatedEvent)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.manualEvents.value.size)
        val current = viewModel.manualEvents.value.first()
        assertEquals("Maths Appliquées", current.title)
        assertEquals("Amphi B", current.location)

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals("Maths Appliquées", state.events.first().title)
    }

    @Test
    fun testDeleteManualEvent() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel = EdtViewModel(repository = fakeRepo)

        viewModel.addManualEvent("Chimie", 1700000000000L, 1700003600000L, "Lab 1", "#00FF00")
        testDispatcher.scheduler.advanceUntilIdle()

        val created = viewModel.manualEvents.value.first()
        viewModel.deleteManualEvent(created.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.manualEvents.value.isEmpty())
        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertTrue(state.events.isEmpty())
    }

    @Test
    fun testMergeManualEventsWithRemoteTimetableEvents() = runTest {
        val remoteEvent = EdtEvent(
            id = "remote_1",
            timetableId = "tt_1",
            title = "Cours Réseau",
            startMs = 1700000000000L,
            endMs = 1700003600000L,
            location = "Amphi C",
            colorHex = "#8E24AA",
            isManual = false
        )

        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean, timetableId: String): List<EdtEvent> {
                return listOf(remoteEvent)
            }
        }

        val viewModel = EdtViewModel(repository = fakeRepo)
        viewModel.addManualEvent(
            title = "Atelier Perso",
            startMs = 1700005000000L,
            endMs = 1700008000000L,
            location = "FabLab",
            colorHex = "#FB8C00"
        )
        viewModel.addTimetable("EDT Univ", "https://example.com/cal.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(2, state.events.size)
        assertEquals("Cours Réseau", state.events[0].title)
        assertEquals("Atelier Perso", state.events[1].title)
    }

    @Test
    fun testManualEventsPersistence() = runTest {
        val fakeRepo = object : EdtRepository() {}
        val viewModel1 = EdtViewModel(repository = fakeRepo)

        viewModel1.addManualEvent("Algorithmique", 1700000000000L, 1700003600000L, "S01", "#1E88E5")
        testDispatcher.scheduler.advanceUntilIdle()

        val viewModel2 = EdtViewModel(repository = fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel2.manualEvents.value.size)
        assertEquals("Algorithmique", viewModel2.manualEvents.value.first().title)
        val state = viewModel2.uiState.value
        assertTrue(state is EdtUiState.Success)
        assertEquals(1, state.events.size)
        assertEquals("Algorithmique", state.events.first().title)
    }
}
