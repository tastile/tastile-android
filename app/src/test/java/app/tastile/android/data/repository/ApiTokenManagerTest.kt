package app.tastile.android.data.repository

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the [ApiTokenManager] mint/cache flow.
 *
 * These run under Robolectric so that `EncryptedSharedPreferences` can resolve
 * a Keystore-backed master key. The HTTP target ([V1ApiClient]) is mocked so
 * the test exercises the manager's control flow without a real backend.
 */
@RunWith(AndroidJUnit4::class)
class ApiTokenManagerTest {

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
    fun getOrMint_returns_null_when_no_bootstrap_token() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns null
        val api = mockk<V1ApiClient>(relaxed = true)
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        assertNull(mgr.getOrMint())
        coVerify(exactly = 0) { api.mintApiToken(any(), any()) }
    }

    @Test
    fun getOrMint_invokes_mintApiToken_with_cognito_id_token_as_bootstrap_bearer() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken("cognito-id-token-xyz", any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret",
            tokenId = "tok-1",
            label = "android-client",
        )
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        val token = mgr.getOrMint()
        assertEquals("tastile-secret", token)
        coVerify(exactly = 1) { api.mintApiToken("cognito-id-token-xyz", any()) }
        // The cached value is from the response, not the bootstrap.
        coVerify(exactly = 1) {
            api.mintApiToken(
                match { it == "cognito-id-token-xyz" },
                match { it.label == "android-client" && it.scopes.isEmpty() },
            )
        }
    }

    @Test
    fun getOrMint_returns_cached_token_on_subsequent_calls_without_reminting() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret",
            tokenId = "tok-1",
            label = "android-client",
        )
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        assertEquals("tastile-secret", mgr.getOrMint())
        assertEquals("tastile-secret", mgr.getOrMint())
        assertEquals("tastile-secret", mgr.getOrMint())
        coVerify(exactly = 1) { api.mintApiToken(any(), any()) }
    }

    @Test
    fun getOrMint_swallows_mint_failure_and_returns_null() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken(any(), any()) } throws IllegalStateException("network unreachable")
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        assertNull(mgr.getOrMint())
    }

    @Test
    fun invalidate_drops_in_memory_cache_so_next_call_remints() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken(any(), any()) } returnsMany listOf(
            V1ApiTokenCreateResponse(token = "first-secret", tokenId = "tok-1"),
            V1ApiTokenCreateResponse(token = "second-secret", tokenId = "tok-2"),
        )
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        assertEquals("first-secret", mgr.getOrMint())
        mgr.invalidate()
        assertEquals("second-secret", mgr.getOrMint())
        coVerify(exactly = 2) { api.mintApiToken(any(), any()) }
    }

    @Test
    fun signOut_clears_encrypted_prefs_and_in_memory_cache() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        assertEquals("tastile-secret", mgr.getOrMint())
        mgr.signOut()
        // After signOut, the encrypted prefs no longer contain a token.
        assertNull(EncryptedTokenStorage.apiTokenPrefs(context).getString("api_token", null))
        // Next getOrMint should mint again with no cached fallback.
        coVerify(exactly = 1) { api.mintApiToken(any(), any()) }
    }

    @Test
    fun request_uses_documented_label_for_android_client() = runTest {
        val auth = mockk<CurrentUserProvider>(relaxed = true)
        every { auth.currentIdToken() } returns "cognito-id-token-xyz"
        val api = mockk<V1ApiClient>(relaxed = true)
        coEvery { api.mintApiToken(any(), any()) } returns V1ApiTokenCreateResponse(
            token = "tastile-secret", tokenId = "tok-1",
        )
        val mgr = ApiTokenManager(context, lazyOf(api), auth)
        mgr.getOrMint()
        coVerify(exactly = 1) {
            api.mintApiToken(any(), match<V1ApiTokenCreateRequest> { it.label == "android-client" })
        }
    }
}

private fun lazyOf(api: V1ApiClient): Lazy<V1ApiClient> = Lazy { api }
