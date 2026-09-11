package org.better.urn.ui.izly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.better.urn.data.izly.IzlyAuthState
import org.better.urn.data.izly.IzlyOperation
import org.better.urn.data.izly.IzlyRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeIzlyRepository(
    var isSessionValid: Boolean = false,
    var shouldFailLogin: Boolean = false,
    var shouldFailTokenize: Boolean = false,
    var shouldFailHistory: Boolean = false,
    var mockOperations: List<IzlyOperation> = listOf(
        IzlyOperation(
            id = "1",
            amount = 3.30f,
            date = "10/09/2026 - 12:30",
            type = "Resto U",
            isCredit = false,
        )
    )
) : IzlyRepository {
    var internalSavedPhone: String? = null

    override fun getSavedPhone(): String? = internalSavedPhone

    override fun savePhone(phone: String) {
        internalSavedPhone = phone
    }

    override suspend fun hasValidSession(): Boolean = isSessionValid

    override suspend fun login(phone: String, pin: String): Result<Boolean> {
        return if (shouldFailLogin) {
            Result.failure(IllegalArgumentException("Identifiants invalides"))
        } else {
            internalSavedPhone = phone
            Result.success(true)
        }
    }

    override suspend fun tokenize(smsLink: String): Result<Unit> {
        return if (shouldFailTokenize) {
            Result.failure(IllegalArgumentException("Code d'activation invalide ou expiré."))
        } else {
            isSessionValid = true
            Result.success(Unit)
        }
    }

    override suspend fun getBalance(): Result<Float> {
        return Result.success(15.50f)
    }

    override suspend fun getHistory(): Result<List<IzlyOperation>> {
        return if (shouldFailHistory) {
            Result.failure(IllegalStateException("Erreur réseau"))
        } else {
            Result.success(mockOperations)
        }
    }

    override suspend fun logout() {
        isSessionValid = false
        internalSavedPhone = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IzlyViewModelTest {

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
    fun login_withEmptyFields_setsErrorState() {
        val fakeRepo = FakeIzlyRepository()
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("")
        viewModel.onPinChanged("")

        viewModel.login()

        assertTrue(viewModel.uiState.value.authState is IzlyAuthState.Error)
        assertEquals("Veuillez remplir tous les champs", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun login_withValidCredentials_updatesAuthStateToActivationRequired() = runTest {
        val fakeRepo = FakeIzlyRepository()
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("0601020304")
        viewModel.onPinChanged("1234")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IzlyAuthState.ActivationRequired("0601020304"), viewModel.uiState.value.authState)
    }

    @Test
    fun login_withInvalidCredentials_setsErrorState() = runTest {
        val fakeRepo = FakeIzlyRepository(shouldFailLogin = true)
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("0601020304")
        viewModel.onPinChanged("0000")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.authState is IzlyAuthState.Error)
        assertEquals("Identifiants invalides", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitActivationLink_withValidLink_updatesAuthStateToLoggedInAndLoadsHistory() = runTest {
        val fakeRepo = FakeIzlyRepository()
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("0601020304")
        viewModel.onPinChanged("1234")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitActivationLink("https://mon-espace.izly.fr/tools/Activation/33601020304/59e4ec7314d34a99980be42ef9d62edf")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IzlyAuthState.LoggedIn, viewModel.uiState.value.authState)
        assertTrue(viewModel.uiState.value.operations.isNotEmpty())
    }

    @Test
    fun submitActivationLink_withInvalidLink_setsErrorState() = runTest {
        val fakeRepo = FakeIzlyRepository(shouldFailTokenize = true)
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("0601020304")
        viewModel.onPinChanged("1234")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.submitActivationLink("https://mon-espace.izly.fr/tools/Activation/invalid")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IzlyAuthState.ActivationRequired("0601020304"), viewModel.uiState.value.authState)
        assertEquals("Code d'activation invalide ou expiré.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun fetchHistory_whenFails_updatesErrorMessage() = runTest {
        val fakeRepo = FakeIzlyRepository(shouldFailHistory = true)
        val viewModel = IzlyViewModel(fakeRepo)

        viewModel.fetchHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Erreur réseau", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun logout_resetsStateToIdle() = runTest {
        val fakeRepo = FakeIzlyRepository()
        val viewModel = IzlyViewModel(fakeRepo)
        viewModel.onPhoneChanged("0601020304")
        viewModel.onPinChanged("1234")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IzlyAuthState.Idle, viewModel.uiState.value.authState)
        assertEquals("", viewModel.uiState.value.phoneInput)
        assertEquals("", viewModel.uiState.value.pinInput)
        assertEquals("", viewModel.uiState.value.activationLinkInput)
        assertTrue(viewModel.uiState.value.operations.isEmpty())
    }

    @Test
    fun processDeath_restoresStateFromSavedStateHandle() = runTest {
        val fakeRepo = FakeIzlyRepository()
        val savedStateHandle = androidx.lifecycle.SavedStateHandle(
            mapOf(
                "izly_phone_input" to "0601020304",
                "izly_is_activation_required" to true,
                "izly_activation_phone" to "0601020304"
            )
        )
        val viewModel = IzlyViewModel(fakeRepo, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(IzlyAuthState.ActivationRequired("0601020304"), viewModel.uiState.value.authState)
        assertEquals("0601020304", viewModel.uiState.value.phoneInput)
    }
}
