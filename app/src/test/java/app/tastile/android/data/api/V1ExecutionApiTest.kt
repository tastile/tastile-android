package app.tastile.android.data.api

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class V1ExecutionApiTest {
    @Test
    fun execution_api_uses_core_paths_and_command_envelopes() = runBlocking {
        val requests = CopyOnWriteArrayList<Pair<String, String>>()
        val bodies = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests += exchange.requestMethod to exchange.requestURI.path
            bodies += exchange.requestBody.bufferedReader().use { it.readText() }
            val response = when (exchange.requestURI.path) {
                "/v1/active-tile" -> """{"tile_id":"tile-1","placement_id":"placement-1","execution_id":"execution-1","title":"Focus"}"""
                "/v1/executions/execution-1" -> """{"id":"execution-1","tile_id":"tile-1","owner_id":"owner-1","revision":4,"state":1,"placement_id":"placement-1","segment_count":1,"fact_count":0,"task_run_count":0,"created_at":"2026-07-28T00:00:00Z","updated_at":"2026-07-28T00:01:00Z"}"""
                else -> commandResponse
            }
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        try {
            val api = V1ApiClient({ "token" }, "http://127.0.0.1:${server.address.port}")
            api.getActiveTile()
            api.readExecution("execution-1")
            api.startExecution("placement-1")
            api.pauseExecution("execution-1")
            api.resumeExecution("execution-1")
            api.finishExecution("execution-1", kind = 0, note = null)
        } finally {
            server.stop(0)
        }

        assertEquals(
            listOf(
                "GET" to "/v1/active-tile",
                "GET" to "/v1/executions/execution-1",
                "POST" to "/v1/placements/placement-1/executions",
                "POST" to "/v1/executions/execution-1/pause",
                "POST" to "/v1/executions/execution-1/resume",
                "POST" to "/v1/executions/execution-1/finish",
            ),
            requests,
        )
        assertEquals("placement-1", Json.parseToJsonElement(bodies[2]).jsonObject["payload"]!!.jsonObject["placement_id"].toString().trim('"'))
    }

    private companion object {
        const val commandResponse =
            """{"command_id":"018f4a7c-4f7a-7000-8000-000000000001","accepted_at":"2026-07-28T00:00:00Z","aggregate":{"kind":2,"id":"execution-1"},"revision":1,"result":0,"pending":[]}"""
    }
}
