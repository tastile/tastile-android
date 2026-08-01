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
                "GET" -> "[]"
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

    private companion object {
        const val endpointJson =
            """{"id":"endpoint-1","owner_id":"owner-1","channel":0,"capability":{"supports_interaction":true,"supports_action_reply":true},"created_at":"2026-07-29T00:00:00Z","updated_at":"2026-07-29T00:00:00Z"}"""
    }
}
