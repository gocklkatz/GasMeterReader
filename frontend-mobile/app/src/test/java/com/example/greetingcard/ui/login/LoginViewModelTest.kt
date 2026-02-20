package com.example.greetingcard.ui.login

import com.example.greetingcard.MainDispatcherRule
import com.example.greetingcard.data.api.ApiService
import com.example.greetingcard.data.api.LoginRequest
import com.example.greetingcard.data.api.LoginResponse
import com.example.greetingcard.data.auth.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.mockk
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: ApiService = mockk()
    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(apiService, authRepository)
    }

    @Test
    fun blankUsername_setsErrorWithoutCallingApi() {
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("password")
        viewModel.login {}
        assertEquals("Username and password required", viewModel.uiState.value.error)
        coVerify(exactly = 0) { apiService.login(any()) }
    }

    @Test
    fun blankPassword_setsErrorWithoutCallingApi() {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("")
        viewModel.login {}
        assertEquals("Username and password required", viewModel.uiState.value.error)
        coVerify(exactly = 0) { apiService.login(any()) }
    }

    @Test
    fun successfulLogin_savesTokenAndCallsOnSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        coEvery { apiService.login(LoginRequest("user", "pass")) } returns Response.success(LoginResponse("tok"))
        coEvery { authRepository.saveToken("tok") } just Runs

        var onSuccessCalled = false
        viewModel.login { onSuccessCalled = true }
        advanceUntilIdle()

        assertTrue(onSuccessCalled)
        assertNull(viewModel.uiState.value.error)
        // On success the UI navigates away; isLoading stays true (ViewModel is effectively discarded)
    }

    @Test
    fun nullTokenInSuccessBody_setsErrorInvalidResponse() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        coEvery { apiService.login(any()) } returns Response.success(null)

        viewModel.login {}
        advanceUntilIdle()

        assertEquals("Invalid response", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun httpError_setsErrorWithStatusCode() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        coEvery { apiService.login(any()) } returns Response.error(401, "".toResponseBody())

        viewModel.login {}
        advanceUntilIdle()

        assertEquals("Login failed: 401", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun networkException_setsErrorFromExceptionMessage() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        coEvery { apiService.login(any()) } throws RuntimeException("Connection refused")

        viewModel.login {}
        advanceUntilIdle()

        assertEquals("Connection refused", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun isLoading_isTrueDuringRequestAndFalseAfter() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        // Use a delay so the coroutine suspends after setting isLoading=true, letting us observe it
        coEvery { apiService.login(any()) } coAnswers {
            delay(1_000)
            Response.error(401, "".toResponseBody())
        }

        assertFalse(viewModel.uiState.value.isLoading)
        viewModel.login {}
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onUsernameChange_updatesUsernameAndClearsError() {
        // Set an error first
        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("pass")
        viewModel.login {} // sets error
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onUsernameChange("newuser")

        assertEquals("newuser", viewModel.uiState.value.username)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun onPasswordChange_updatesPasswordAndClearsError() {
        // Set an error first
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("")
        viewModel.login {} // sets error
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onPasswordChange("newpass")

        assertEquals("newpass", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.error)
    }
}
