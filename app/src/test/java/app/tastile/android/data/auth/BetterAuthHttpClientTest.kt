package app.tastile.android.data.auth

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest

/**
 * Verifies the cookie + JSON parsing logic in [BetterAuthHttpClient]. The
 * client is built around `HttpURLConnection`; the tests use MockWebServer
 * to simulate a live BetterAuth server without owning the network.
 */
class BetterAuthHttpClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: BetterAuthHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = BetterAuthHttpClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun signIn_returnsSessionWithTokenFromSetCookieHeader() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "better-auth.session_token=abc123; Path=/; HttpOnly; SameSite=Lax")
                // Realistic BetterAuth payload: session-level fields live
                // under the `session` subobject, user-level fields under
                // `user`.
                .setBody(
                    """{"user":{"id":"user-1","email":"alice@example.com"},"session":{"expiresAt":4102444800,"token":"abc123"}}""",
                ),
        )

        val session = client.signIn(email = "alice@example.com", password = "hunter2")

        assertEquals("abc123", session.sessionToken)
        assertEquals("user-1", session.userId)
        assertEquals("alice@example.com", session.email)
        assertEquals(4102444800L, session.expiresAtEpochSeconds)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/sign-in/email", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"email\":\"alice@example.com\""))
        assertTrue(body.contains("\"password\":\"hunter2\""))
    }

    @Test
    fun signIn_fallsBackToTopLevelExpiresAt_whenSessionSubobjectMissing() = runTest {
        // Older / custom BetterAuth-compatible servers may emit the
        // session-level fields at the top level. The decoder must still
        // pick them up so the Keystore-backed preferences can persist an
        // expiry timestamp.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "better-auth.session_token=legacy-token; Path=/")
                .setBody(
                    """{"user":{"id":"user-9","email":"legacy@example.com"},"expiresAt":4200000000}""",
                ),
        )

        val session = client.signIn(email = "legacy@example.com", password = "pw")

        assertEquals("user-9", session.userId)
        assertEquals(4200000000L, session.expiresAtEpochSeconds)
    }

    @Test
    fun signIn_throwsWhenSessionCookieMissing() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"user":{"id":"user-1","email":"alice@example.com"}}"""),
        )

        val ex = assertThrows(BetterAuthException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.signIn(email = "alice@example.com", password = "hunter2")
            }
        }
        assertTrue(ex.message!!.contains("session cookie"))
    }

    @Test
    fun signIn_throwsOnNon2xxStatus() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"invalid_credentials"}"""),
        )

        val ex = assertThrows(BetterAuthException::class.java) {
            kotlinx.coroutines.runBlocking {
                client.signIn(email = "alice@example.com", password = "wrong")
            }
        }
        assertTrue(ex.message!!.contains("401"))
    }

    @Test
    fun signUp_postsEmailPasswordAndNameAndReturnsSession() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "better-auth.session_token=new-xyz; Path=/; HttpOnly")
                .setBody("""{"user":{"id":"user-2","email":"bob@example.com"}}"""),
        )

        val session = client.signUp(email = "bob@example.com", password = "hunter2", name = "Bob")

        assertEquals("new-xyz", session.sessionToken)
        assertEquals("user-2", session.userId)
        assertEquals("bob@example.com", session.email)
        assertNull(session.expiresAtEpochSeconds)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/sign-up/email", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Bob\""))
    }

    @Test
    fun signOut_sendsAuthorizationBearerAndAcceptsNon2xx() = runTest {
        // Server revokes a session token. The client must:
        //   1. POST with Authorization: Bearer <token>
        //   2. Not raise on 4xx (the local wipe happens regardless).
        server.enqueue(MockResponse().setResponseCode(204))
        client.signOut(sessionToken = "to-revoke")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/sign-out", recorded.path)
        assertEquals("Bearer to-revoke", recorded.getHeader("Authorization"))
    }

    @Test
    fun getSession_returnsSessionOn200() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                // Realistic BetterAuth shape: expiresAt lives under
                // `session`, not at the top level.
                .setBody(
                    """{"user":{"id":"user-1","email":"alice@example.com"},"session":{"expiresAt":4102444800}}""",
                ),
        )

        val session = client.getSession(sessionToken = "valid-token")

        assertEquals("valid-token", session?.sessionToken)
        assertEquals("user-1", session?.userId)
        assertEquals("alice@example.com", session?.email)
        assertEquals(4102444800L, session?.expiresAtEpochSeconds)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("Bearer valid-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun getSession_returnsNullOn401() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val session = client.getSession(sessionToken = "expired")

        assertNull(session)
    }

    @Test
    fun extractSessionToken_parsesFirstMatchingCookieValue() {
        val extracted = client.extractSessionToken(
            listOf(
                "__Host-other=xyz; Path=/",
                "better-auth.session_token=target-token; Path=/; HttpOnly",
            ),
        )
        assertEquals("target-token", extracted)
    }

    @Test
    fun extractSessionToken_returnsNullWhenNoMatchingHeader() {
        val extracted = client.extractSessionToken(
            listOf("foo=bar; Path=/", "baz=qux"),
        )
        assertNull(extracted)
    }

    @Test
    fun extractSessionToken_ignoresBlankValue() {
        val extracted = client.extractSessionToken(
            listOf("better-auth.session_token=; Path=/"),
        )
        assertNull(extracted)
    }
}
