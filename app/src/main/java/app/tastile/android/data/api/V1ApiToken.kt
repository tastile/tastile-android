package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `domain::ApiToken` in `tastile-core/crates/v1/domain/src/auth.rs`.
 *
 * The v1 backend exposes a dedicated token-management API:
 *
 *   POST   /v1/api-tokens        — mint a new raw token (returned exactly once).
 *   GET    /v1/api-tokens        — list tokens for the authenticated owner.
 *   PATCH  /v1/api-tokens/{id}   — rename / re-scope.
 *   DELETE /v1/api-tokens/{id}   — revoke.
 *
 * The Android client uses these endpoints the same way the web client does:
 * after Cognito login issues an `id_token`, the client calls
 * `POST /v1/api-tokens` (with the Cognito `id_token` as the bootstrap bearer)
 * to obtain a Tastile API token, stores it under encrypted preferences, and
 * uses it as the only `Authorization` header for all subsequent v1 calls.
 *
 * See PROJECT-TRUTH.md ("Authentication") and DEEP-REVIEW-NOTES.md
 * ("Android Token Storage Is Not Production Safe").
 */

@Serializable
data class V1ApiTokenCreateRequest(
    val label: String? = null,
    val scopes: List<String> = emptyList(),
)

/**
 * Response from `POST /v1/api-tokens`. The `token` field is the raw
 * authoritative value — it is returned exactly once at mint time and is not
 * retrievable from any subsequent endpoint. Losing it means the user must
 * mint a new one.
 */
@Serializable
data class V1ApiTokenCreateResponse(
    @SerialName("token") val token: String,
    @SerialName("token_id") val tokenId: String,
    @SerialName("label") val label: String? = null,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

/**
 * Response from `GET /v1/api-tokens`. Never contains the raw token — only the
 * stored metadata for the owner.
 */
@Serializable
data class V1ApiTokenView(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String? = null,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("revoked_at") val revokedAt: String? = null,
)

@Serializable
data class V1ListApiTokensResponse(
    val tokens: List<V1ApiTokenView> = emptyList(),
)
