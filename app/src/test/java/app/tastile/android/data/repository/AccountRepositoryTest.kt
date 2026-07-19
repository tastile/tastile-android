package app.tastile.android.data.repository

import app.tastile.android.data.api.CognitoAccountApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C7 — Preferences sidebar + sub-sheets wiring tests for
 * [AccountRepository]. Verifies that the repository delegates the
 * web-route account endpoints and the v1 token endpoints to
 * [CognitoAccountApi] in the right direction (no accidental swap of
 * the Next route base vs. the v1 base).
 *
 * Web routes at `/api/account/...` live on `COGNITO_WEB_AUTH_BASE_URL`
 * and use the Cognito id/access token.
 * V1 routes at `/v1/api-tokens...` live on `TASTILE_CORE_URL` and use
 * the Tastile API token.
 */
class AccountRepositoryTest {

    private val mockApi = mockk<CognitoAccountApi>(relaxed = true)
    private val mockAuth = mockk<AuthRepository>(relaxed = true)
    private val repository = AccountRepository(mockApi, mockAuth)

    @Test
    fun loadProfile_delegatesToCognitoAccountApi() = runTest {
        val expected = AccountProfile(
            username = "alice",
            sub = "sub-123",
            email = "alice@example.com",
            emailVerified = true,
            preferredUsername = null,
        )
        coEvery { mockApi.getProfile() } returns CognitoAccountApi.AccountProfileDto(
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
            CognitoAccountApi.AccountTokenDto(
                id = "v1-id",
                name = null,
                label = "CI bot",
                tokenPrefix = "tk_abcd",
                createdAt = "2026-07-15T00:00:00Z",
                lastUsedAt = null,
                lastUsedPath = null,
                revokedAt = null,
            ),
            CognitoAccountApi.AccountTokenDto(
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
        coEvery { mockApi.createToken("ci") } returns CognitoAccountApi.AccountTokenWithSecretDto(
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
    fun loadProfile_fallsBackToCachedJwtClaimsWhenWebRouteFails() = runTest {
        // C7-fallback: when /api/account/profile fails (Next proxy
        // unreachable / dev / staging), the repository decodes the
        // cached Cognito id_token so the user still sees email + sub.
        val jwt = makeIdToken(sub = "sub-xyz", email = "alice@example.com", emailVerified = true)
        coEvery { mockAuth.currentIdToken() } returns jwt
        coEvery { mockApi.getProfile() } throws java.io.IOException("http 502")

        val profile = repository.loadProfile()

        assertEquals("sub-xyz", profile.sub)
        assertEquals("alice@example.com", profile.email)
        assertTrue(profile.emailVerified)
        // No fallback data was supplied, so username falls back to sub.
        assertEquals("sub-xyz", profile.username)
        assertNull(profile.preferredUsername)
    }

    @Test
    fun loadProfile_throwsWhenBothApiAndFallbackUnavailable() = runTest {
        coEvery { mockAuth.currentIdToken() } returns null
        coEvery { mockApi.getProfile() } throws java.io.IOException("http 502")

        try {
            repository.loadProfile()
            org.junit.Assert.fail("expected IOException")
        } catch (e: java.io.IOException) {
            assertTrue(e.message!!.contains("502"))
        }
    }

    /** Encode `{ "sub": "sub", "email": "email", "email_verified": boolean }`
     *  as a Cognito-shape JWT (header.payload.signature). The literal
     *  sub/email keys are encoded as JSON object keys inside the payload. */
    private fun makeIdToken(sub: String, email: String, emailVerified: Boolean): String {
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""")
        val payload = base64Url(
            """{"sub":"$sub","email":"$email","email_verified":$emailVerified,"cognito:username":"$sub"}""",
        )
        val sig = base64Url("signature")
        return "$header.$payload.$sig"
    }

    private fun base64Url(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
            value.toByteArray(Charsets.UTF_8),
        )
}
