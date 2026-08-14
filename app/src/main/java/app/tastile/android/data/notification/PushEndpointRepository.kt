package app.tastile.android.data.notification

import android.content.Context
import app.tastile.android.data.api.EndpointCapabilityPayload
import app.tastile.android.data.api.EndpointRegistrationPayload
import app.tastile.android.data.api.EndpointView
import app.tastile.android.data.api.V1ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Supplies a provider-issued opaque push token. Implementations must never log it. */
fun interface PushTokenProvider {
    suspend fun currentToken(): String?
}

/**
 * No-op push token provider. Firebase Cloud Messaging was removed when the
 * Android app dropped its Firebase project; the app no longer registers a
 * remote push endpoint with Core. The provider remains so the surrounding
 * `PushEndpointRepository.registerCurrentToken()` contract still resolves
 * cleanly (it returns `null` and the repository skips the network call).
 */
@Singleton
class NoOpPushTokenProvider @Inject constructor() : PushTokenProvider {
    override suspend fun currentToken(): String? = null
}

/** Persistent state is deliberately only endpoint IDs and token fingerprints, never raw tokens. */
interface PushEndpointStore {
    var endpointId: String?
    var tokenFingerprint: String?
    var retiredEndpointId: String?
}

class InMemoryPushEndpointStore : PushEndpointStore {
    override var endpointId: String? = null
    override var tokenFingerprint: String? = null
    override var retiredEndpointId: String? = null
}

@Singleton
class SharedPreferencesPushEndpointStore @Inject constructor(
    @ApplicationContext context: Context,
) : PushEndpointStore {
    private val preferences = context.getSharedPreferences("push_endpoint", Context.MODE_PRIVATE)

    override var endpointId: String?
        get() = preferences.getString(KEY_ENDPOINT_ID, null)
        set(value) = write(KEY_ENDPOINT_ID, value)

    override var tokenFingerprint: String?
        get() = preferences.getString(KEY_TOKEN_FINGERPRINT, null)
        set(value) = write(KEY_TOKEN_FINGERPRINT, value)

    override var retiredEndpointId: String?
        get() = preferences.getString(KEY_RETIRED_ENDPOINT_ID, null)
        set(value) = write(KEY_RETIRED_ENDPOINT_ID, value)

    private fun write(key: String, value: String?) {
        preferences.edit().also { editor ->
            if (value == null) editor.remove(key) else editor.putString(key, value)
        }.apply()
    }

    private companion object {
        const val KEY_ENDPOINT_ID = "endpoint_id"
        const val KEY_TOKEN_FINGERPRINT = "token_fingerprint"
        const val KEY_RETIRED_ENDPOINT_ID = "retired_endpoint_id"
    }
}

/**
 * Keeps a single Core PUSH endpoint for this app installation.
 *
 * A replacement is registered before its predecessor is removed, so a token
 * rotation cannot create a delivery blackout. If removal is interrupted, the
 * retired id remains persisted and is retried on the next call.
 */
@Singleton
class PushEndpointRepository @Inject constructor(
    private val api: V1ApiClient,
    private val store: PushEndpointStore,
    private val tokenProvider: PushTokenProvider,
) {
    suspend fun registerCurrentToken(): EndpointView? =
        tokenProvider.currentToken()?.takeIf(String::isNotBlank)?.let { register(it) }

    suspend fun register(token: String): EndpointView {
        require(token.isNotBlank()) { "push token must not be blank" }
        cleanupRetiredEndpoint()

        val fingerprint = sha256(token)
        val existingId = store.endpointId
        if (existingId != null && store.tokenFingerprint == fingerprint) {
            api.listEndpoints().firstOrNull { it.id == existingId && it.channel == PUSH_CHANNEL }
                ?.let { return it }
        }

        val replacement = api.registerEndpoint(
            EndpointRegistrationPayload(
                channel = PUSH_CHANNEL,
                token = token,
                capability = EndpointCapabilityPayload(),
            ),
        )
        check(replacement.channel == PUSH_CHANNEL) { "Core returned a non-push endpoint" }

        val predecessor = store.endpointId
        store.endpointId = replacement.id
        store.tokenFingerprint = fingerprint
        if (predecessor != null && predecessor != replacement.id) {
            store.retiredEndpointId = predecessor
            cleanupRetiredEndpoint()
        }
        return replacement
    }

    suspend fun unregister() {
        cleanupRetiredEndpoint()
        val activeId = store.endpointId ?: return
        api.deleteEndpoint(activeId)
        store.endpointId = null
        store.tokenFingerprint = null
    }

    private suspend fun cleanupRetiredEndpoint() {
        val retiredId = store.retiredEndpointId ?: return
        api.deleteEndpoint(retiredId)
        store.retiredEndpointId = null
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object { const val PUSH_CHANNEL = 0 }
}
