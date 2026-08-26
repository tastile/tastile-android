package app.tastile.android.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.data.api.V1ApiClient
import app.tastile.android.data.api.V1ApiTokenCreateRequest
import app.tastile.android.data.api.V1ApiTokenCreateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import dagger.Lazy
import kotlinx.coroutines.test.runTest
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentSessionToken() } returns null
        val api = mockk<V1ApiClient>(relaxed = true)
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertNull(cache.getOrMint())
        coVerify(exactly = 0) { api.mintApiTokenViaWeb(any(), any()) }
    }

    @Test
    fun getOrMint_invokes_web_mint_with_betterauth_session_token() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentSessionToken() } returns "better-auth-session-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiTokenViaWeb(any(), any()) } throws IllegalStateException("network unreachable")
        val cache = ApiTokenCache(context, lazyOf(api), auth)
        assertNull(cache.getOrMint())
    }

    @Test
    fun getOrMint_invokes_onMintFailed_callback_on_mint_exception() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
        val auth = mockk<CurrentUserProvider>(relaxed = true)
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
}

private fun lazyOf(api: V1ApiClient): Lazy<V1ApiClient> = Lazy { api }
