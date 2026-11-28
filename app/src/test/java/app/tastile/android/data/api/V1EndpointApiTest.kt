package app.tastile.android.data.api

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class V1EndpointApiTest {
    @Test
    fun endpoint_api_uses_authenticated_plain_json_registration_and_raw_delete() = runBlocking {
        val requests = CopyOnWriteArrayList<Pair<String, String>>()
        val bodies = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests += exchange.requestMethod to exchange.requestURI.path
            bodies += exchange.requestBody.bufferedReader().use { it.readText() }
            val response = when (exchange.requestMethod) {
                "GET" -> endpointListJson
                "POST" -> endpointJson
                else -> ""
            }
            exchange.sendResponseHeaders(if (exchange.requestMethod == "DELETE") 204 else 200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        try {
            val api = V1ApiClient({ "token" }, "http://127.0.0.1:${server.address.port}")
            api.listEndpoints()
            api.registerEndpoint(
                EndpointRegistrationPayload(
                    channel = 0,
                    token = "opaque-token",
                    capability = EndpointCapabilityPayload(),
                ),
            )
            api.deleteEndpoint("endpoint-1")
        } finally {
            server.stop(0)
        }

        assertEquals(
            listOf(
                "GET" to "/v1/endpoints",
                "POST" to "/v1/endpoints",
                "DELETE" to "/v1/endpoints/endpoint-1",
            ),
            requests,
        )
        val registration = Json.parseToJsonElement(bodies[1]).jsonObject
        assertEquals("opaque-token", registration["token"]!!.jsonPrimitive.content)
        assertTrue(registration["payload"] == null)
    }

    @Test
    fun listEndpoints_followsNextCursorAcrossPages() = runBlocking {
        val queries = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/endpoints") { exchange ->
            queries += exchange.requestURI.query.orEmpty()
            val body = if (exchange.requestURI.query.orEmpty().contains("cursor=cursor-1")) {
                endpointListPage(listOf(endpointJsonPage2), nextCursor = null)
            } else {
                endpointListPage(listOf(endpointJsonPage1), nextCursor = "cursor-1")
            }
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        try {
            val api = V1ApiClient({ "token" }, "http://127.0.0.1:${server.address.port}")
            val endpoints = api.listEndpoints()

            assertEquals(listOf("endpoint-1", "endpoint-2"), endpoints.map { it.id })
            assertEquals(2, queries.size)
            assertTrue(queries[0].contains("limit=1000"))
            assertTrue(queries[1].contains("cursor=cursor-1"))
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        fun endpointListPage(items: List<String>, nextCursor: String?): String =
            """{"items":[${items.joinToString(",")}],"count":${items.size},"next_cursor":${nextCursor?.let { "\"$it\"" } ?: "null"}}"""

        const val endpointJson =
            """{"id":"endpoint-1","owner_id":"owner-1","channel":0,"capability":{"supports_interaction":true,"supports_action_reply":true},"created_at":"2026-07-29T00:00:00Z","updated_at":"2026-07-29T00:00:00Z"}"""
        const val endpointJsonPage1 = endpointJson
        const val endpointJsonPage2 =
            """{"id":"endpoint-2","owner_id":"owner-1","channel":0,"capability":{"supports_interaction":true,"supports_action_reply":true},"created_at":"2026-07-28T00:00:00Z","updated_at":"2026-07-28T00:00:00Z"}"""
        val endpointListJson = endpointListPage(listOf(endpointJson), nextCursor = null)
    }
}
