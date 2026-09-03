package app.tastile.android.ui.login

import android.content.Context
import app.tastile.android.R
import app.tastile.android.data.auth.AuthRepositoryContract
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

    private class FakeAuthRepository(
        private val signInError: Exception? = null,
        private val signOutError: Exception? = null,
        private val onSignIn: (String, String) -> Unit = { _, _ -> },
        private val onSignUp: (String, String, String) -> Unit = { _, _, _ -> },
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

        override suspend fun signInWithProvider(provider: String) = Unit

        override suspend fun signInWithGoogle() = Unit

        override suspend fun signOut() {
            signOutError?.let { throw it }
        }

        override fun currentSessionToken(): String? = null
    }
}
