package app.tastile.android.ui.login

import android.content.Context
import app.tastile.android.R
import app.tastile.android.data.auth.AuthRepositoryContract
import app.tastile.android.data.auth.GoogleSignInFailedException
import app.tastile.android.data.auth.GoogleSignInUnavailableException
import app.tastile.android.data.auth.TastileAuthState
import io.mockk.every
import io.mockk.mockk
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
    private val context = mockk<Context>(relaxed = true).also {
        every { it.getString(R.string.login_error_sign_in_failed) } returns "Sign-in failed"
        every { it.getString(R.string.login_error_sign_out_failed) } returns "Unable to sign out"
        every { it.getString(R.string.login_error_google_unavailable) } returns "Google Sign-In isn't available on this device"
        every { it.getString(R.string.login_error_google_failed) } returns "Unable to verify Google account"
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signInWithEmail_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Sign-in failed"))
        val viewModel = LoginViewModel(repository, context)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.signInWithEmail(context)

        assertEquals("Sign-in failed", viewModel.error.value)
        assertEquals(false, viewModel.isSigningIn.value)
    }

    @Test
    fun signInWithEmail_whenRepositorySucceeds_resetsSigningInAndKeepsErrorNull() {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository, context)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.signInWithEmail(context)

        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isSigningIn.value)
    }

    @Test
    fun signInWithEmail_whenFieldsBlank_setsErrorWithoutCallingRepository() {
        var called = false
        val repository = FakeAuthRepository(onSignIn = { _, _ -> called = true })
        val viewModel = LoginViewModel(repository, context)

        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")
        viewModel.signInWithEmail(context)

        assertEquals("Sign-in failed", viewModel.error.value)
        assertEquals(false, called)
    }

    @Test
    fun signUp_derivesNameFromEmailAndCallsRepository() {
        var captured: Triple<String, String, String>? = null
        val repository = FakeAuthRepository(onSignUp = { email, password, name ->
            captured = Triple(email, password, name)
        })
        val viewModel = LoginViewModel(repository, context)

        viewModel.onEmailChange("alice@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.signUp(context)

        assertEquals(Triple("alice@example.com", "hunter2", "alice"), captured)
        assertEquals(false, viewModel.isSigningIn.value)
    }

    @Test
    fun signOut_whenRepositoryFails_exposesErrorMessage() {
        val repository = FakeAuthRepository(signOutError = IllegalStateException("Sign out failed"))
        val viewModel = LoginViewModel(repository, context)

        viewModel.signOut()

        assertEquals("Unable to sign out", viewModel.error.value)
    }

    @Test
    fun clearError_resetsCurrentError() {
        val repository = FakeAuthRepository(signInError = IllegalStateException("Sign-in failed"))
        val viewModel = LoginViewModel(repository, context)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("hunter2")
        viewModel.signInWithEmail(context)
        viewModel.clearError()

        assertNull(viewModel.error.value)
    }

    @Test
    fun signInWithGoogle_launchesAuthRepo() {
        var googleCalled = false
        val repository = FakeAuthRepository(onSignInWithGoogle = { googleCalled = true })
        val viewModel = LoginViewModel(repository, context)

        viewModel.signInWithGoogle(context)

        assertEquals(true, googleCalled)
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    @Test
    fun signInWithGoogle_unavailableFallbackToWebHandoff() {
        var googleCalled = false
        var webHandoffProvider: String? = null
        val repository = FakeAuthRepository(
            onSignInWithGoogle = { googleCalled = true },
            signInWithGoogleError = GoogleSignInUnavailableException(RuntimeException("no account")),
            onSignInWithProvider = { provider -> webHandoffProvider = provider },
        )
        val viewModel = LoginViewModel(repository, context)

        viewModel.signInWithGoogle(context)

        assertEquals(true, googleCalled)
        assertEquals("google", webHandoffProvider)
        // Silent fallback — no error chip.
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    @Test
    fun signInWithGoogle_serverErrorSurfacesAsChip() {
        val repository = FakeAuthRepository(
            signInWithGoogleError = RuntimeException("HTTP 401"),
        )
        val viewModel = LoginViewModel(repository, context)

        viewModel.signInWithGoogle(context)

        assertEquals("HTTP 401", viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    @Test
    fun signInWithGoogle_failedExceptionSurfacesAsChip() {
        val repository = FakeAuthRepository(
            signInWithGoogleError = GoogleSignInFailedException("bad credential type"),
        )
        val viewModel = LoginViewModel(repository, context)

        viewModel.signInWithGoogle(context)

        assertEquals("bad credential type", viewModel.error.value)
        assertEquals(false, viewModel.isGoogleSigningIn.value)
    }

    private class FakeAuthRepository(
        private val signInError: Exception? = null,
        private val signOutError: Exception? = null,
        private val onSignIn: (String, String) -> Unit = { _, _ -> },
        private val onSignUp: (String, String, String) -> Unit = { _, _, _ -> },
        private val onSignInWithGoogle: () -> Unit = {},
        private val signInWithGoogleError: Throwable? = null,
        private val onSignInWithProvider: (String) -> Unit = { _ -> },
        private val signInWithProviderError: Throwable? = null,
    ) : AuthRepositoryContract {
        private val auth = MutableStateFlow<TastileAuthState>(TastileAuthState.Unauthenticated)

        override val authState: StateFlow<TastileAuthState> = auth

        override suspend fun signInWithEmail(email: String, password: String) {
            onSignIn(email, password)
            signInError?.let { throw it }
        }

        override suspend fun signUpWithEmail(email: String, password: String, name: String) {
            onSignUp(email, password, name)
            signInError?.let { throw it }
        }

        override suspend fun signInWithProvider(provider: String) {
            onSignInWithProvider(provider)
            signInWithProviderError?.let { throw it }
        }

        override suspend fun signInWithGoogle() {
            onSignInWithGoogle()
            signInWithGoogleError?.let { throw it }
        }

        override suspend fun signOut() {
            signOutError?.let { throw it }
        }

        override fun currentSessionToken(): String? = null
    }
}
