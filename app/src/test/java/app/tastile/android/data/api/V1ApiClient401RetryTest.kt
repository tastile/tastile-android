package app.tastile.android.data.api

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins the Core 401 recovery contract of `V1ApiClient.withBearer`
 * (android#92): discard the rejected token, re-mint it through
 * [TokenRefresher], retry the original request exactly once, surface a
 * failed re-mint as [V1Error.Auth], and treat 401 as the only refresh
 * trigger. A second 401 must propagate — never a third attempt.
 *
 * The local [HttpServer] stands in for Core; no real backend is contacted.
 */
class V1ApiClient401RetryTest {

    private lateinit var server: HttpServer
    private val requestCount = AtomicInteger()
    private val authHeaders = CopyOnWriteArrayList<String?>()
    private val requestBodies = CopyOnWriteArrayList<String>()

    /** Per-test responder: 0-based request index → (status, body). */
    @Volatile
    private var respond: (Int) -> Pair<Int, String> = { 200 to "[]" }

    @Before
    fun setUp() {
        requestCount.set(0)
        authHeaders.clear()
        requestBodies.clear()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/endpoints") { exchange ->
            val index = requestCount.getAndIncrement()
            authHeaders += exchange.requestHeaders.getFirst("Authorization")
            requestBodies += exchange.requestBody.bufferedReader().use { it.readText() }
            val (status, body) = respond(index)
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, if (status == 204) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun clientUrl() = "http://127.0.0.1:${server.address.port}"

    @Test
    fun unauthorized_getDiscardsStaleTokenAndRetriesOnceWithFreshToken() = runBlocking {
        respond = { index -> if (index == 0) 401 to "{}" else 200 to "[]" }
        val refreshedStaleTokens = CopyOnWriteArrayList<String>()
        val api = V1ApiClient({ "stale-token" }, clientUrl()) { stale ->
            refreshedStaleTokens += stale
            "fresh-token"
        }

        val endpoints = api.listEndpoints()

        assertEquals(0, endpoints.size)
        assertEquals(2, requestCount.get())
        assertEquals(listOf("Bearer stale-token", "Bearer fresh-token"), authHeaders)
        assertEquals(listOf("stale-token"), refreshedStaleTokens)
    }

    @Test
    fun unauthorized_postRetryRewritesRequestBodyWithFreshToken() = runBlocking {
        respond = { index -> if (index == 0) 401 to "{}" else 200 to endpointJson }
        val api = V1ApiClient({ "stale-token" }, clientUrl()) { "fresh-token" }

        val endpoint = api.registerEndpoint(
            EndpointRegistrationPayload(
                channel = 0,
                token = "opaque-token",
                capability = EndpointCapabilityPayload(),
            ),
        )

        assertEquals("endpoint-1", endpoint.id)
        assertEquals(2, requestCount.get())
        assertEquals(listOf("Bearer stale-token", "Bearer fresh-token"), authHeaders)
        assertTrue(requestBodies[0].contains("opaque-token"))
        assertTrue(requestBodies[1].contains("opaque-token"))
    }

    @Test
    fun unauthorized_secondRejectionPropagatesWithoutAnotherRetry() {
        respond = { 401 to "{}" }
        var refreshCalls = 0
        val api = V1ApiClient({ "stale-token" }, clientUrl()) {
            refreshCalls++
            "fresh-token"
        }

        assertThrows(V1Error.Unauthorized::class.java) {
            runBlocking { api.listEndpoints() }
        }

        assertEquals(2, requestCount.get())
        assertEquals(1, refreshCalls)
        assertEquals(listOf("Bearer stale-token", "Bearer fresh-token"), authHeaders)
    }

    @Test
    fun unauthorized_failedRefreshSurfacesAuthAndDoesNotRetryRequest() {
        respond = { 401 to "{}" }
        var refreshCalls = 0
        val api = V1ApiClient({ "stale-token" }, clientUrl()) {
            refreshCalls++
            null
        }

        assertThrows(V1Error.Auth::class.java) {
            runBlocking { api.listEndpoints() }
        }

        assertEquals(1, requestCount.get())
        assertEquals(1, refreshCalls)
    }

    @Test
    fun nonUnauthorizedStatusNeverTriggersRefresh() {
        respond = { 500 to "boom" }
        var refreshCalls = 0
        val api = V1ApiClient({ "stale-token" }, clientUrl()) {
            refreshCalls++
            "fresh-token"
        }

        val error = assertThrows(V1Error.Unknown::class.java) {
            runBlocking { api.listEndpoints() }
        }

        assertTrue(error.message!!.contains("500"))
        assertEquals(1, requestCount.get())
        assertEquals(0, refreshCalls)
        assertEquals(listOf("Bearer stale-token"), authHeaders)
    }

    @Test
    fun missingTokenFailsAuthBeforeAnyRequest() {
        var refreshCalls = 0
        val api = V1ApiClient({ null }, clientUrl()) {
            refreshCalls++
            "fresh-token"
        }

        assertThrows(V1Error.Auth::class.java) {
            runBlocking { api.listEndpoints() }
        }

        assertEquals(0, requestCount.get())
        assertEquals(0, refreshCalls)
    }

    private companion object {
        const val endpointJson =
            """{"id":"endpoint-1","owner_id":"owner-1","channel":0,"capability":{"supports_interaction":true,"supports_action_reply":true},"created_at":"2026-07-29T00:00:00Z","updated_at":"2026-07-29T00:00:00Z"}"""
    }
}
