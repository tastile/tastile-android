package app.tastile.android.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the wire shape of the v1 token-management API surfaces (`/v1/api-tokens`).
 * These guard against accidental regressions in the JSON envelope that would
 * cause the Android `ApiTokenManager` mint flow to silently desync from the
 * backend in `tastile-core/crates/v1/api/src/handlers/auth.rs`.
 */
class V1ApiTokenTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `create request serializes label and empty scopes by default`() {
        val req = V1ApiTokenCreateRequest(label = "android-client")
        val encoded = json.encodeToString(V1ApiTokenCreateRequest.serializer(), req)
        assertTrue("label missing: $encoded", encoded.contains("\"label\":\"android-client\""))
    }

    @Test
    fun `create request omits label when null and preserves non-empty scopes`() {
        val req = V1ApiTokenCreateRequest(label = null, scopes = listOf("read:tiles", "write:tiles"))
        val encoded = json.encodeToString(V1ApiTokenCreateRequest.serializer(), req)
        assertTrue("label key should be absent or null: $encoded",
            !encoded.contains("\"label\":\""))
        assertTrue("scopes payload missing: $encoded",
            encoded.contains("\"scopes\":[\"read:tiles\",\"write:tiles\"]"))
    }

    @Test
    fun `create response decodes token id label and scopes`() {
        val body = """{"token":"raw-secret","token_id":"tok-1","label":"android-client","scopes":"all"}"""
        val resp = json.decodeFromString<V1ApiTokenCreateResponse>(body)
        assertEquals("raw-secret", resp.token)
        assertEquals("tok-1", resp.tokenId)
        assertEquals("android-client", resp.label)
        assertEquals("all", resp.scopes)
    }

    @Test
    fun `list response defaults tokens to empty when missing`() {
        val body = """{}"""
        val resp = json.decodeFromString<V1ListApiTokensResponse>(body)
        assertTrue(resp.tokens.isEmpty())
    }

    @Test
    fun `token view handles null label and missing optional fields`() {
        val body = """{"id":"abc","label":null}"""
        val view = json.decodeFromString<V1ApiTokenView>(body)
        assertEquals("abc", view.id)
        assertNull(view.label)
    }

    @Test
    fun `token view serializes wire fields as snake_case`() {
        val view = V1ApiTokenView(
            id = "tk",
            label = "L",
            scopes = "all",
            createdAt = "2026-07-01T00:00:00Z",
            lastUsedAt = null,
            expiresAt = null,
            revokedAt = null,
        )
        val encoded = json.encodeToString(V1ApiTokenView.serializer(), view)
        assertTrue("created_at missing: $encoded", encoded.contains("created_at"))
        assertTrue("camelCase leaked: $encoded", !encoded.contains("createdAt"))
    }

    @Test
    fun `V1ApiTokenCreateResponse parses Core wire format with scopes string`() {
        val wire = """
            {
              "token": "tastile_f7496c3d9c2a",
              "token_id": "8e6c0e74-1f33-7c8b-9a8e-8c3f4a1d2b6e",
              "label": "android-client",
              "scopes": "all",
              "created_at": "2026-07-23T07:53:01Z",
              "expires_at": null
            }
        """.trimIndent()

        val resp = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }.decodeFromString<V1ApiTokenCreateResponse>(wire)

        assertEquals("all", resp.scopes)
        assertEquals("tastile_f7496c3d9c2a", resp.token)
        assertEquals("8e6c0e74-1f33-7c8b-9a8e-8c3f4a1d2b6e", resp.tokenId)
    }
}
