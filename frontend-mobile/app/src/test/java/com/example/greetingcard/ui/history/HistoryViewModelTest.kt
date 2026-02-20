package com.example.greetingcard.ui.history

import com.example.greetingcard.MainDispatcherRule
import com.example.greetingcard.data.api.ReadingDto
import com.example.greetingcard.data.readings.ReadingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val readingRepository: ReadingRepository = mockk()

    private fun createViewModel() = HistoryViewModel(readingRepository)

    @Test
    fun initialState_isLoading() {
        coEvery { readingRepository.getReadings() } returns Result.success(emptyList())
        val viewModel = createViewModel()
        assertEquals(ReadingsState.Loading, viewModel.state.value)
    }

    @Test
    fun loadReadings_success_emitsSuccessWithReadingsSortedByTimestampDescending() =
        runTest(mainDispatcherRule.testDispatcher) {
            val readings = listOf(
                ReadingDto(1, "2026-02-17T08:00:00Z", "path1.jpg"),
                ReadingDto(2, "2026-02-19T08:00:00Z", "path2.jpg"),
                ReadingDto(3, "2026-02-18T08:00:00Z", "path3.jpg"),
            )
            coEvery { readingRepository.getReadings() } returns Result.success(readings)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state is ReadingsState.Success)
            val sorted = (state as ReadingsState.Success).readings
            assertEquals(3, sorted.size)
            assertEquals("2026-02-19T08:00:00Z", sorted[0].timestamp)
            assertEquals("2026-02-18T08:00:00Z", sorted[1].timestamp)
            assertEquals("2026-02-17T08:00:00Z", sorted[2].timestamp)
        }

    @Test
    fun loadReadings_failure_emitsErrorWithMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { readingRepository.getReadings() } returns Result.failure(Exception("Network error"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state is ReadingsState.Error)
            assertEquals("Network error", (state as ReadingsState.Error).message)
        }

    @Test
    fun loadReadings_emptyList_emitsSuccessWithEmptyList() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { readingRepository.getReadings() } returns Result.success(emptyList())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state is ReadingsState.Success)
            assertTrue((state as ReadingsState.Success).readings.isEmpty())
        }

    @Test
    fun callingLoadReadingsAgain_resetsToLoadingThenSuccess() =
        runTest(mainDispatcherRule.testDispatcher) {
            val readings = listOf(ReadingDto(1, "2026-02-19T08:00:00Z", "path1.jpg"))
            // Use delay so the coroutine suspends after setting Loading, letting us observe it
            coEvery { readingRepository.getReadings() } coAnswers {
                delay(100)
                Result.success(readings)
            }

            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.state.value is ReadingsState.Success)

            viewModel.loadReadings()
            mainDispatcherRule.testDispatcher.scheduler.runCurrent()
            assertEquals(ReadingsState.Loading, viewModel.state.value)

            advanceUntilIdle()
            assertTrue(viewModel.state.value is ReadingsState.Success)
        }
}
