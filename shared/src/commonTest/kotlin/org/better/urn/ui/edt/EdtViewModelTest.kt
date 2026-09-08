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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
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
    }

    @Test
    fun testViewModelErrorStateOnAddTimetable() = runTest {
        val fakeRepo = object : EdtRepository() {
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
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
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
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
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
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
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
                return sampleEvents
            }
        }

        val viewModel1 = EdtViewModel(repository = fakeRepo)
        viewModel1.addTimetable("EDT Persistant", "https://example.com/p.ics")
        testDispatcher.scheduler.advanceUntilIdle()

        // Create a new ViewModel instance to simulate app restart
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
            override suspend fun fetchAndParseIcs(url: String, isDarkTheme: Boolean): List<EdtEvent> {
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
}
