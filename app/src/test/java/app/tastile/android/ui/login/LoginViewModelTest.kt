package app.tastile.android.ui.login

import android.content.Context
import app.tastile.android.data.repository.AuthRepositoryContract
import app.tastile.android.data.repository.TastileAuthState
import io.mockk.mockk
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
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithCognito_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Cognito sign-in failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithCognito(context)

        assertEquals("Cognito sign-in failed", viewModel.error.value)
        assertEquals(false, viewModel.isSigningIn.value)
    }

    @Test
    fun signInWithCognito_whenRepositorySucceeds_resetsSigningInAndKeepsErrorNull() {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithCognito(context)

        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isSigningIn.value)
    }

    @Test
    fun signOut_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signOutError = IllegalStateException("Sign out failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signOut()

        assertEquals("Unable to sign out", viewModel.error.value)
    }

    @Test
    fun clearError_resetsCurrentError() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Cognito sign-in failed"))
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithCognito(context)
        viewModel.clearError()

        assertNull(viewModel.error.value)
    }

    private class FakeAuthRepository(
        private val signInError: Exception? = null,
        private val signOutError: Exception? = null
    ) : AuthRepositoryContract {
        private val status = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
        private val auth = MutableStateFlow<TastileAuthState>(TastileAuthState.Unauthenticated)

        override val authState: StateFlow<TastileAuthState> = auth
        override val sessionStatus: StateFlow<SessionStatus> = status

        override suspend fun signInWithCognito(context: Context) {
            signInError?.let { throw it }
        }

        override suspend fun signInWithGoogle(context: Context) {
            signInError?.let { throw it }
        }

        override suspend fun signOut() {
            signOutError?.let { throw it }
        }

        override fun currentIdToken(): String? = null
    }
}
