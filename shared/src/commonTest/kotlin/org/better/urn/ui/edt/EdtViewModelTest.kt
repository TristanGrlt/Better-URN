package org.better.urn.ui.edt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
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
}
