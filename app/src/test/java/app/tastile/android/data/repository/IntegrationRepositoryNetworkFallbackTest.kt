package app.tastile.android.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException

class IntegrationRepositoryNetworkFallbackTest {

    @Test
    fun defaultDaemonBaseUrls_prefersCloudApiBeforeLocalhost() {
        val urls = defaultDaemonBaseUrls()
        assertEquals(
            listOf("https://api.tastile.app", "http://127.0.0.1:3140", "http://10.0.2.2:3140"),
            urls
        )
    }

    @Test
    fun runWithDaemonFallback_usesSecondEndpointAfterConnectFailure() {
        val visited = mutableListOf<String>()
        val result = runWithDaemonFallback(listOf("http://127.0.0.1:3140", "http://10.0.2.2:3140")) { baseUrl ->
            visited += baseUrl
            if (baseUrl.contains("127.0.0.1")) {
                throw ConnectException("Connection refused")
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf("http://127.0.0.1:3140", "http://10.0.2.2:3140"), visited)
    }

    @Test
    fun runWithDaemonFallback_throwsWhenAllCandidatesFail() {
        try {
            runWithDaemonFallback(listOf("http://127.0.0.1:3140", "http://10.0.2.2:3140")) {
                throw ConnectException("Connection refused")
            }
            throw AssertionError("Expected IllegalStateException")
        } catch (error: IllegalStateException) {
            assertTrue(error.message!!.contains("Failed to reach daemon endpoint"))
            assertTrue(error.cause is ConnectException)
        }
    }

    @Test
    fun runWithDaemonFallback_rethrowsNonRecoverableErrors() {
        val expected = IOException("Broken payload")
        try {
            runWithDaemonFallback(listOf("http://127.0.0.1:3140")) {
                throw expected
            }
            throw AssertionError("Expected IOException")
        } catch (error: IOException) {
            assertEquals(expected, error)
        }
    }
}
