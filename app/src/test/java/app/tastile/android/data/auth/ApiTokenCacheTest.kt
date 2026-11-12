package app.tastile.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ApiTokenCreateRequest
import app.tastile.android.data.api.V1ApiTokenCreateResponse
import app.tastile.android.data.api.V1Error
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import dagger.Lazy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the [ApiTokenCache] mint/cache flow.
 *
 * These run under Robolectric so that `EncryptedSharedPreferences` can resolve
 * a Keystore-backed master key. The HTTP target ([V1ApiClient]) is mocked so
 * the test exercises the cache's control flow without a real backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ApiTokenCacheTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // Ensure each run starts from a clean prefs state.
        EncryptedTokenStorage.apiTokenPrefs(context).edit().clear().apply()
    }

    @After
    fun tearDown() {
        EncryptedTokenStorage.apiTokenPrefs(context).edit().clear().apply()
    }

    @Test
    fun getOrMint_returns_null_when_no_bootstrap_session_token() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns null
        val api = mockk<V1ApiClient>(relaxed = true)
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertNull(cache.getOrMint())
        coVerify(exactly = 0) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun getOrMint_invokes_web_mint_with_betterauth_session_token() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret",
            tokenId = "tok-1",
            label = "android-client",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        val token = cache.getOrMint()
        assertEquals("tastile-secret", token)
        coVerify(exactly = 1) { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) }
        // The cached value is from the response, not the bootstrap.
        coVerify(exactly = 1) {
            api.mintApiTokenViaWeb(
                match { it == "better-auth-session-xyz" },
                match { it.label == "android-client" && it.scopes.isEmpty() },
            )
        }
    }

    @Test
    fun getOrMint_returns_cached_token_on_subsequent_calls_without_reminting() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret",
            tokenId = "tok-1",
            label = "android-client",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("tastile-secret", cache.getOrMint())
        assertEquals("tastile-secret", cache.getOrMint())
        assertEquals("tastile-secret", cache.getOrMint())
        coVerify(exactly = 1) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun getOrMint_swallows_mint_failure_and_returns_null() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } throws IllegalStateException("network unreachable")
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertNull(cache.getOrMint())
    }

    @Test
    fun getOrMint_invokes_onMintFailed_callback_on_mint_exception() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        val mintError = IllegalStateException("network unreachable")
        coEvery { api.mintApiTokenViaWeb(any(), any()) } throws mintError
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        var captured: Throwable? = null
        assertNull(cache.getOrMint(onMintFailed = { captured = it }))
        assertSame(mintError, captured)
    }

    @Test
    fun getOrMint_does_not_invoke_onMintFailed_when_mint_succeeds() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        var callbackFired = false
        val token = cache.getOrMint(onMintFailed = { callbackFired = true })
        assertEquals("tastile-secret", token)
        assertTrue(!callbackFired)
    }

    @Test
    fun getOrMint_does_not_invoke_onMintFailed_when_no_session_token() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns null
        val api = mockk<V1ApiClient>(relaxed = true)
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        var callbackFired = false
        assertNull(cache.getOrMint(onMintFailed = { callbackFired = true }))
        assertTrue(!callbackFired)
        coVerify(exactly = 0) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun invalidate_drops_in_memory_cache_so_next_call_remints() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returnsMany listOf(
            V1ApiTokenCreateResponse(token = "first-secret", tokenId = "tok-1"),
            V1ApiTokenCreateResponse(token = "second-secret", tokenId = "tok-2"),
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("first-secret", cache.getOrMint())
        cache.invalidate()
        assertEquals("second-secret", cache.getOrMint())
        coVerify(exactly = 2) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun signOut_clears_encrypted_prefs_and_in_memory_cache() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("tastile-secret", cache.getOrMint())
        cache.signOut()
        // After signOut, the encrypted prefs no longer contain a token.
        assertNull(EncryptedTokenStorage.apiTokenPrefs(context).getString("api_token", null))
        // Next getOrMint should mint again with no cached fallback.
        coVerify(exactly = 1) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun request_uses_documented_label_for_android_client() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        cache.getOrMint()
        coVerify(exactly = 1) {
            api.mintApiTokenViaWeb(any(), match<V1ApiTokenCreateRequest> { it.label == "android-client" })
        }
    }

    @Test
    fun currentCachedToken_returns_null_before_mint_and_value_after() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertNull(cache.currentCachedToken())
        cache.getOrMint()
        assertEquals("tastile-secret", cache.currentCachedToken())
}
 
    @Test
    fun refreshAfterUnauthorized_discardsStaleTokenAndRemintsOnSuccess() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "new-tastile-secret",
            tokenId = "tok-2",
            label = "android-client",
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        // First mint
        assertEquals("new-tastile-secret", cache.getOrMint())
        // Simulate 401 with the stale token
        val fresh = cache.refreshAfterUnauthorized("new-tastile-secret")
        assertEquals("new-tastile-secret", fresh)
        // Initial mint + exactly one re-mint after discarding the stale token.
        coVerify(exactly = 2) { api.mintApiTokenViaWeb(any(), any()) }
    }
 
    @Test
    fun refreshAfterUnauthorized_singleFlightUnderConcurrent401s() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        // Simulate a slow mint so we can verify single-flight
        val mintDeferred = CompletableDeferred<V1ApiTokenCreateResponse>()
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } coAnswers { mintDeferred.await() }
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        // First mint establishes the token
        val firstMint = async { cache.getOrMint() }
        mintDeferred.complete(V1ApiTokenCreateResponse(token = "initial-token", tokenId = "tok-1"))
        assertEquals("initial-token", firstMint.await())
 
        // Now simulate concurrent 401s - all should share one re-mint
        val secondMintDeferred = CompletableDeferred<V1ApiTokenCreateResponse>()
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } coAnswers { secondMintDeferred.await() }
 
        val concurrentRefreshes = List(5) { async { cache.refreshAfterUnauthorized("initial-token") } }
        secondMintDeferred.complete(V1ApiTokenCreateResponse(token = "refreshed-token", tokenId = "tok-2"))
 
        val results = concurrentRefreshes.awaitAll()
        results.forEach { assertEquals("refreshed-token", it) }
        // Only one additional mint call for the concurrent refreshes
        coVerify(exactly = 2) { api.mintApiTokenViaWeb(any(), any()) }
    }
 
    @Test
    fun refreshAfterUnauthorized_mintFailureReturnsNullAndClearsToken() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "initial-token", tokenId = "tok-1"
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("initial-token", cache.getOrMint())
 
        // Now make mint fail on refresh
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } throws IOException("network error")
 
        val fresh = cache.refreshAfterUnauthorized("initial-token")
        assertNull(fresh)
        // Token should be cleared from cache
        assertNull(cache.currentCachedToken())
    }
 
    /**
     * android#92 AC2: a failed re-mint must not retry forever. Each failure
     * class — network error, 4xx, 5xx — gets one bounded round of
     * [ApiTokenCache] re-mint attempts (initial mint + 3 attempts) with
     * 500ms/1000ms backoff in between (virtual time under runTest), and
     * then the cache forces the explicit unauthenticated state exactly
     * once.
     */
    @Test
    fun refreshAfterUnauthorized_boundsRetriesWithBackoffThenGoesUnauthenticated() = runTest {
        val failures = listOf(
            IOException("network unreachable"),
            V1Error.Unknown(403, """{"kind":3,"message":"forbidden"}"""),
            V1Error.Unknown(502, "bad gateway"),
        )
        for (failure in failures) {
            val auth = mockk<AuthRepositoryContract>(relaxed = true)
            every { auth.currentSessionToken() } returns "better-auth-session-xyz"
            val api = mockk<V1ApiClient>(relaxed = true)
            coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
                token = "initial-token", tokenId = "tok-1"
            )
            val cache = ApiTokenCache(context, lazyOf(api), auth)
            assertEquals("initial-token", cache.getOrMint())

            coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } throws failure
            val startedAt = testScheduler.currentTime
            val fresh = cache.refreshAfterUnauthorized("initial-token")

            assertNull(fresh)
            assertNull(cache.currentCachedToken())
            // Bounded: initial mint + exactly 3 re-mint attempts, nothing more.
            coVerify(exactly = 4) { api.mintApiTokenViaWeb(any(), any()) }
            // Backoff between attempts: 500ms + 1000ms of virtual time.
            assertEquals(1500L, testScheduler.currentTime - startedAt)
            verify(exactly = 1) { auth.markUnauthenticated() }
        }
    }

    @Test
    fun refreshAfterUnauthorized_failsFastAfterBoundedRetryRound() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "initial-token", tokenId = "tok-1"
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("initial-token", cache.getOrMint())

        // First refresh runs one bounded round: exactly 3 re-mint attempts.
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } throws IOException("network error")
        assertNull(cache.refreshAfterUnauthorized("initial-token"))
        verify(exactly = 1) { auth.markUnauthenticated() }

        // While unauthenticated, a repeat 401 fails fast — no new mint round.
        assertNull(cache.refreshAfterUnauthorized("initial-token"))

        coVerify(exactly = 4) { api.mintApiTokenViaWeb(any(), any()) }
        verify(exactly = 1) { auth.markUnauthenticated() }
    }

    /**
     * android#92 AC3 (failure variant): concurrent 401s must share one
     * bounded retry round — five simultaneous refreshes cost the initial
     * mint + 3 attempts, not five independent rounds, and the
     * unauthenticated transition fires once.
     */
    @Test
    fun refreshAfterUnauthorized_concurrent401sShareOneBoundedRetryRound() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "initial-token", tokenId = "tok-1"
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("initial-token", cache.getOrMint())

        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } throws IOException("network unreachable")
        val results = List(5) { async { cache.refreshAfterUnauthorized("initial-token") } }.awaitAll()

        results.forEach { assertNull(it) }
        coVerify(exactly = 4) { api.mintApiTokenViaWeb(any(), any()) }
        verify(exactly = 1) { auth.markUnauthenticated() }
    }
 
    @Test
    fun refreshAfterUnauthorized_clearsEncryptedPrefsOnStaleToken() = runTest {
        val auth = mockk<AuthRepositoryContract>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "initial-token", tokenId = "tok-1"
        )
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertEquals("initial-token", cache.getOrMint())
        // Verify token is persisted
        assertEquals("initial-token", EncryptedTokenStorage.apiTokenPrefs(context).getString("api_token", null))
 
        // Refresh fails - encrypted prefs should be cleared
        coEvery { api.mintApiTokenViaWeb("better-auth-session-xyz", any()) } throws IOException("network error")
        cache.refreshAfterUnauthorized("initial-token")
 
        // Encrypted prefs should be cleared
        assertNull(EncryptedTokenStorage.apiTokenPrefs(context).getString("api_token", null))
    }
 
}
 
private fun lazyOf(api: V1ApiClient): Lazy<V1ApiClient> = Lazy { api }
