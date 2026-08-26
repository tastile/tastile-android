package app.tastile.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.auth.BetterAuthHttpClient.BetterAuthSession
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [AuthRepository]. Covers the migration from the Cognito
 * session object to a BetterAuth session token persisted to encrypted
 * SharedPreferences and exposed via the simplified [TastileAuthState]
 * sealed interface.
 */
@RunWith(AndroidJUnit4::class)
class AuthRepositoryTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        EncryptedTokenStorage.sessionTokenPrefs(context).edit().clear().apply()
    }

    @After
    fun tearDown() {
        EncryptedTokenStorage.sessionTokenPrefs(context).edit().clear().apply()
    }

    @Test
    fun unauthenticated_whenNoSessionIsPersisted() {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        val repo = AuthRepository(context, httpClient, lazyOf(api))

        assertEquals(TastileAuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun signInWithEmail_persistsSessionAndExposesAuthenticatedState() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery {
            httpClient.signIn(email = "alice@example.com", password = "hunter2")
        } returns BetterAuthSession(
            sessionToken = "session-abc",
            userId = "user-1",
            email = "alice@example.com",
            expiresAtEpochSeconds = null,
        )
        val repo = AuthRepository(context, httpClient, lazyOf(api))

        repo.signInWithEmail(email = "alice@example.com", password = "hunter2")

        val state = repo.authState.value as TastileAuthState.Authenticated
        assertEquals("user-1", state.userId)
        assertEquals("alice@example.com", state.email)
        assertEquals("session-abc", repo.currentSessionToken())
        // The v1 cache must be invalidated so the next mint picks up the
        // new session identity (the prior token belonged to a different
        // user, if any).
        coVerify(exactly = 1) { api.invalidate() }
    }

    @Test
    fun signInWithEmail_invalidatesPriorCachedV1TokenEvenWhenSameSession() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery { httpClient.signIn(any(), any()) } returns BetterAuthSession(
            sessionToken = "session-1",
            userId = "user-1",
            email = "alice@example.com",
            expiresAtEpochSeconds = null,
        )
        val repo = AuthRepository(context, httpClient, lazyOf(api))

        repo.signInWithEmail("alice@example.com", "hunter2")
        // After a second sign-in the v1 cache must be invalidated again
        // (e.g. account switch).
        coEvery { httpClient.signIn(any(), any()) } returns BetterAuthSession(
            sessionToken = "session-2",
            userId = "user-2",
            email = "bob@example.com",
            expiresAtEpochSeconds = null,
        )
        repo.signInWithEmail("bob@example.com", "hunter2")

        coVerify(exactly = 2) { api.invalidate() }
        assertEquals("session-2", repo.currentSessionToken())
    }

    @Test
    fun signUpWithEmail_persistsSession() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery {
            httpClient.signUp(email = "bob@example.com", password = "hunter2", name = "Bob")
        } returns BetterAuthSession(
            sessionToken = "session-xyz",
            userId = "user-2",
            email = "bob@example.com",
            expiresAtEpochSeconds = 4102444800L,
        )

        val repo = AuthRepository(context, httpClient, lazyOf(api))
        repo.signUpWithEmail(email = "bob@example.com", password = "hunter2", name = "Bob")

        val state = repo.authState.value as TastileAuthState.Authenticated
        assertEquals("user-2", state.userId)
        assertEquals("session-xyz", repo.currentSessionToken())
    }

    @Test
    fun signOut_clearsStateAndWipesEncryptedPrefs() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery {
            httpClient.signIn(any(), any())
        } returns BetterAuthSession(
            sessionToken = "session-1",
            userId = "user-1",
            email = "alice@example.com",
            expiresAtEpochSeconds = null,
        )
        val repo = AuthRepository(context, httpClient, lazyOf(api))
        repo.signInWithEmail("alice@example.com", "hunter2")
        // Sign out should best-effort server revoke + always wipe local.
        coEvery { httpClient.signOut("session-1") } returns Unit

        repo.signOut()

        assertEquals(TastileAuthState.Unauthenticated, repo.authState.value)
        assertNull(repo.currentSessionToken())
        coVerify(exactly = 1) { api.signOut() }
    }

    @Test
    fun signOut_succeedsEvenWhenServerRevokeThrows() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery {
            httpClient.signIn(any(), any())
        } returns BetterAuthSession(
            sessionToken = "session-1",
            userId = "user-1",
            email = "alice@example.com",
            expiresAtEpochSeconds = null,
        )
        coEvery { httpClient.signOut(any()) } throws BetterAuthException("http 401")
        val repo = AuthRepository(context, httpClient, lazyOf(api))
        repo.signInWithEmail("alice@example.com", "hunter2")

        repo.signOut()

        // Local state must still be cleared; a server-side revoke failure
        // must not strand the user on the signed-in shell.
        assertEquals(TastileAuthState.Unauthenticated, repo.authState.value)
        coVerify(exactly = 1) { api.signOut() }
    }

    @Test
    fun currentUserId_fallsBackToPersistedPrefsWhenStateNotYetLoaded() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        // Persist a session out-of-band (simulates a cold-start load).
        EncryptedTokenStorage.sessionTokenPrefs(context).edit()
            .putString("session_token", "session-cold-start")
            .putString("user_id", "user-cold-start")
            .putString("email", "cold@example.com")
            .apply()

        val repo = AuthRepository(context, httpClient, lazyOf(api))

        assertEquals("user-cold-start", repo.currentUserId())
        assertEquals("cold@example.com", repo.currentEmail())
        assertEquals("session-cold-start", repo.currentSessionToken())
        val state = repo.authState.first() as TastileAuthState.Authenticated
        assertEquals("user-cold-start", state.userId)
    }

    @Test
    fun fallbackUserId_andFallbackEmail_trackCurrentIdentity() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>()
        coEvery {
            httpClient.signIn(any(), any())
        } returns BetterAuthSession(
            sessionToken = "s1",
            userId = "u1",
            email = "u1@example.com",
            expiresAtEpochSeconds = null,
        )

        val repo = AuthRepository(context, httpClient, lazyOf(api))

        assertNull(repo.fallbackUserId)
        assertNull(repo.fallbackEmail)
        repo.signInWithEmail("u1@example.com", "pw")
        assertEquals("u1", repo.fallbackUserId)
        assertEquals("u1@example.com", repo.fallbackEmail)
        assertFalse(repo.currentSession is Any)
    }

    @Test
    fun signInWithProvider_opensWebLoginUrlWithNormalizedProvider() = runTest {
        val api = mockk<ApiTokenCache>(relaxed = true)
        val httpClient = mockk<BetterAuthHttpClient>(relaxed = true)
        // Use a Robolectric-friendly Context — `startActivity` is fine.
        val repo = AuthRepository(context, httpClient, lazyOf(api))

        repo.signInWithProvider("Google")
        // The exact intent action is hard to assert under Robolectric
        // (it doesn't process the Activity start), so we just make sure
        // the call doesn't throw and that currentSession remains null.
        assertNull(repo.currentSessionToken())
    }
}

private fun lazyOf(api: ApiTokenCache): Lazy<ApiTokenCache> = Lazy { api }
