package app.tastile.android.ui.login

import app.tastile.android.data.repository.AuthRepositoryContract
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithGoogle_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Google sign-in failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithGoogle()

        assertEquals("Google sign-in failed", viewModel.error.value)
    }

    @Test
    fun signOut_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signOutError = IllegalStateException("Sign out failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signOut()

        assertEquals("Sign out failed", viewModel.error.value)
    }

    @Test
    fun clearError_resetsCurrentError() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Google sign-in failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithGoogle()
        viewModel.clearError()

        assertNull(viewModel.error.value)
    }

    private class FakeAuthRepository(
        private val signInError: Exception? = null,
        private val signOutError: Exception? = null
    ) : AuthRepositoryContract {
        private val status = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))

        override val sessionStatus: StateFlow<SessionStatus> = status

        override suspend fun signInWithGoogle() {
            signInError?.let { throw it }
        }

        override suspend fun signOut() {
            signOutError?.let { throw it }
        }
    }
}
