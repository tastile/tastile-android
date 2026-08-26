package app.tastile.android.data.user

import app.tastile.android.data.api.BetterAuthAccountApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [AccountRepository]. Verifies that the repository delegates
 * the web-route account endpoints (`/api/account/...`) and the v1 token
 * endpoints (`/v1/api-tokens*`) to [BetterAuthAccountApi] correctly. The
 * `AuthStateProvider` is mocked separately so the fallback path (used
 * when the web route is unreachable) can be exercised end-to-end without
 * dragging in the full `AuthRepository` machinery.
 */
class AccountRepositoryTest {

    private val mockApi = mockk<BetterAuthAccountApi>(relaxed = true)
    private val mockAuthState = mockk<AuthStateProvider>(relaxed = true)
    private val repository = AccountRepository(mockApi, mockAuthState)

    @Test
    fun loadProfile_delegatesToBetterAuthAccountApi() = runTest {
        val expected = AccountProfile(
            username = "alice",
            sub = "sub-123",
            email = "alice@example.com",
            emailVerified = true,
            preferredUsername = null,
        )
        coEvery { mockApi.getProfile() } returns BetterAuthAccountApi.AccountProfileDto(
            username = "alice",
            sub = "sub-123",
            email = "alice@example.com",
            emailVerified = true,
        )

        val actual = repository.loadProfile()

        coVerify(exactly = 1) { mockApi.getProfile() }
        assertEquals(expected.username, actual.username)
        assertEquals(expected.sub, actual.sub)
        assertEquals(expected.email, actual.email)
        assertEquals(expected.emailVerified, actual.emailVerified)
    }

    @Test
    fun startEmailChange_delegatesWithEmailArgument() = runTest {
        coEvery { mockApi.startEmailChange("new@example.com") } returns Unit

        repository.startEmailChange("new@example.com")

        coVerify(exactly = 1) { mockApi.startEmailChange("new@example.com") }
    }

    @Test
    fun verifyEmailChange_delegatesWithCodeArgument() = runTest {
        coEvery { mockApi.verifyEmailChange("123456") } returns Unit

        repository.verifyEmailChange("123456")

        coVerify(exactly = 1) { mockApi.verifyEmailChange("123456") }
    }

    @Test
    fun listTokens_mapsDtoToViewWithCanonicalId() = runTest {
        coEvery { mockApi.listTokens() } returns listOf(
            BetterAuthAccountApi.AccountTokenDto(
                id = "v1-id",
                name = null,
                label = "CI bot",
                tokenPrefix = "tk_abcd",
                createdAt = "2026-07-15T00:00:00Z",
                lastUsedAt = null,
                lastUsedPath = null,
                revokedAt = null,
            ),
            BetterAuthAccountApi.AccountTokenDto(
                id = "",
                tokenId = "web-id",
                name = "Local script",
                label = null,
                tokenPrefix = "tk_efgh",
                createdAt = "2026-07-14T00:00:00Z",
                revokedAt = "2026-07-14T12:00:00Z",
            ),
        )

        val tokens = repository.listTokens()

        assertEquals(2, tokens.size)
        assertEquals("v1-id", tokens[0].id)
        assertEquals("CI bot", tokens[0].displayName)
        assertEquals("tk_abcd", tokens[0].tokenPrefix)
        assertEquals("web-id", tokens[1].id)
        assertEquals("Local script", tokens[1].displayName)
        assertTrue(tokens[1].isRevoked)
    }

    @Test
    fun createToken_delegatesAndMapsResponse() = runTest {
        coEvery { mockApi.createToken("ci") } returns BetterAuthAccountApi.AccountTokenWithSecretDto(
            token = "tk_secret_xyz",
            tokenId = "tk-id",
            label = "ci",
            tokenPrefix = "tk_secret_xyz".take(8),
            createdAt = "2026-07-15T00:00:00Z",
        )

        val result = repository.createToken("ci")

        coVerify(exactly = 1) { mockApi.createToken("ci") }
        assertEquals("tk-id", result.id)
        assertEquals("ci", result.displayName)
        assertEquals("tk_secret_xyz", result.secret)
    }

    @Test
    fun revokeToken_delegatesWithId() = runTest {
        coEvery { mockApi.revokeToken("tk-id") } returns Unit

        repository.revokeToken("tk-id")

        coVerify(exactly = 1) { mockApi.revokeToken("tk-id") }
    }

    @Test
    fun loadProfile_fallsBackToCachedAuthStateWhenWebRouteFails() = runTest {
        // The /api/account/profile route is best-effort: when the Next.js
        // proxy is unreachable (dev / staging), the repository falls back
        // to the locally-cached AuthStateProvider values. After the
        // Cognito -> BetterAuth migration, the fallback source is the
        // session payload, not a decoded JWT.
        every { mockAuthState.currentUserId() } returns "sub-xyz"
        every { mockAuthState.currentEmail() } returns "alice@example.com"
        coEvery { mockApi.getProfile() } throws java.io.IOException("http 502")

        val profile = repository.loadProfile()

        assertEquals("sub-xyz", profile.sub)
        assertEquals("alice@example.com", profile.email)
        assertEquals(false, profile.emailVerified)
        // No fallback data was supplied for username, so the username
        // falls back to the user id (matches the C7 contract).
        assertEquals("sub-xyz", profile.username)
        assertEquals(null, profile.preferredUsername)
    }

    @Test
    fun loadProfile_throwsWhenBothApiAndFallbackUnavailable() = runTest {
        every { mockAuthState.currentUserId() } returns null
        coEvery { mockApi.getProfile() } throws java.io.IOException("http 502")

        try {
            repository.loadProfile()
            org.junit.Assert.fail("expected IOException")
        } catch (e: java.io.IOException) {
            assertTrue(e.message!!.contains("502"))
        }
    }
}
