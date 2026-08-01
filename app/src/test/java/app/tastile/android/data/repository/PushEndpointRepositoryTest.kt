package app.tastile.android.data.repository

import app.tastile.android.data.api.EndpointCapabilityPayload
import app.tastile.android.data.api.EndpointCapabilityView
import app.tastile.android.data.api.EndpointRegistrationPayload
import app.tastile.android.data.api.EndpointView
import app.tastile.android.data.api.V1ApiClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushEndpointRepositoryTest {
    @Test
    fun unchanged_token_reuses_verified_endpoint_without_registering_again() = runTest {
        val api = mockk<V1ApiClient>()
        val store = InMemoryPushEndpointStore().apply {
            endpointId = "endpoint-1"
            tokenFingerprint = SHA_256_TOKEN_1
        }
        coEvery { api.listEndpoints() } returns listOf(endpoint("endpoint-1"))
        val repository = PushEndpointRepository(api, store) { "token-1" }

        assertEquals("endpoint-1", repository.registerCurrentToken()?.id)
        coVerify(exactly = 0) { api.registerEndpoint(any()) }
    }

    @Test
    fun token_rotation_registers_replacement_before_retiring_old_endpoint() = runTest {
        val api = mockk<V1ApiClient>()
        val store = InMemoryPushEndpointStore().apply {
            endpointId = "endpoint-old"
            tokenFingerprint = SHA_256_TOKEN_1
        }
        val calls = mutableListOf<String>()
        coEvery { api.listEndpoints() } returns emptyList()
        coEvery { api.registerEndpoint(any()) } answers {
            calls += "register"
            endpoint("endpoint-new")
        }
        coEvery { api.deleteEndpoint("endpoint-old") } answers { calls += "delete-old" }
        val repository = PushEndpointRepository(api, store) { "token-2" }

        repository.registerCurrentToken()

        assertEquals(listOf("register", "delete-old"), calls)
        assertEquals("endpoint-new", store.endpointId)
        assertNull(store.retiredEndpointId)
    }

    @Test
    fun failed_retired_endpoint_cleanup_is_persisted_for_retry() = runTest {
        val api = mockk<V1ApiClient>()
        val store = InMemoryPushEndpointStore().apply { retiredEndpointId = "endpoint-old" }
        coEvery { api.deleteEndpoint("endpoint-old") } throws IllegalStateException("offline")
        val repository = PushEndpointRepository(api, store) { "token-1" }

        runCatching { repository.registerCurrentToken() }

        assertEquals("endpoint-old", store.retiredEndpointId)
    }

    @Test
    fun registration_payload_declares_interactive_push_capabilities() = runTest {
        val api = mockk<V1ApiClient>()
        val store = InMemoryPushEndpointStore()
        coEvery { api.registerEndpoint(any()) } returns endpoint("endpoint-1")
        val repository = PushEndpointRepository(api, store) { "token-1" }

        repository.registerCurrentToken()

        coVerify {
            api.registerEndpoint(
                EndpointRegistrationPayload(
                    channel = 0,
                    token = "token-1",
                    capability = EndpointCapabilityPayload(),
                ),
            )
        }
    }

    private fun endpoint(id: String) = EndpointView(
        id = id,
        ownerId = "owner-1",
        channel = 0,
        capability = EndpointCapabilityView(supportsInteraction = true, supportsActionReply = true),
        createdAt = "2026-07-29T00:00:00Z",
        updatedAt = "2026-07-29T00:00:00Z",
    )

    private companion object {
        const val SHA_256_TOKEN_1 = "3f08aace122ee2368432c1ca23a049bc640bafbf00fdf33a52429f38ba12dbf9"
    }
}
