package app.tastile.android.data.repository

import android.util.Base64
import app.tastile.android.data.api.CognitoAccountApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that exposes the account-management surface used by the
 * mobile `AccountSheet` / `TokensSheet`. Wraps [CognitoAccountApi] so
 * the ViewModel never sees the bearer-token plumbing, and provides a
 * JWT-claim fallback for the `getProfile()` call when the
 * `/api/account/profile` Next route is unreachable.
 *
 * See `tastile-web/src/app/dashboard/preferences/account/page.tsx` for
 * the web composition that this repository feeds.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountApi: CognitoAccountApi,
    private val authRepository: AuthRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadProfile(): AccountProfile = withContext(Dispatchers.IO) {
        // TODO(C7-fallback): the `/api/account/profile` route is a Next.js
        // proxy. If the v1 daemon ever exposes a `/v1/account/profile`
        // equivalent, prefer it. Until then, on HTTP error we fall back
        // to the cached Cognito id_token claims so the user still sees
        // their email + sub on Android.
        val fallbackClaims = authRepository.currentIdToken()?.let { decodeClaims(it) }
        try {
            val dto = accountApi.getProfile()
            AccountProfile(
                username = dto.username.ifBlank { fallbackClaims?.username ?: "" },
                sub = dto.sub ?: fallbackClaims?.sub,
                email = dto.email ?: fallbackClaims?.email,
                emailVerified = if (dto.sub != null) dto.emailVerified
                else (fallbackClaims?.emailVerified ?: false),
                preferredUsername = dto.preferredUsername,
            )
        } catch (e: Exception) {
            if (fallbackClaims != null) {
                AccountProfile(
                    username = fallbackClaims.username,
                    sub = fallbackClaims.sub,
                    email = fallbackClaims.email,
                    emailVerified = fallbackClaims.emailVerified,
                    preferredUsername = null,
                )
            } else {
                throw e
            }
        }
    }

    suspend fun startEmailChange(email: String) {
        accountApi.startEmailChange(email)
    }

    suspend fun verifyEmailChange(code: String) {
        accountApi.verifyEmailChange(code)
    }

    suspend fun listTokens(): List<AccountTokenView> =
        accountApi.listTokens().map { dto ->
            AccountTokenView(
                id = dto.canonicalId,
                displayName = dto.displayName,
                tokenPrefix = dto.tokenPrefix.orEmpty(),
                createdAt = dto.createdAt,
                lastUsedAt = dto.lastUsedAt,
                lastUsedPath = dto.lastUsedPath,
                revokedAt = dto.revokedAt,
            )
        }

    suspend fun createToken(label: String?): AccountTokenWithSecret =
        accountApi.createToken(label).let { dto ->
            AccountTokenWithSecret(
                id = dto.tokenId,
                displayName = (dto.label ?: dto.name ?: "").trim(),
                tokenPrefix = dto.tokenPrefix.orEmpty(),
                createdAt = dto.createdAt,
                secret = dto.secret,
            )
        }

    suspend fun revokeToken(id: String) {
        accountApi.revokeToken(id)
    }

    /**
     * Decode the cached Cognito id_token JWT payload (no signature
     * verification — Android already trusts the token because it came
     * from the same EncryptedSharedPreferences the AuthRepository uses).
     */
    private fun decodeClaims(idToken: String): ProfileClaims? {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
            )
            val obj = json.parseToJsonElement(payload).jsonObject
            val sub = obj["sub"]?.jsonPrimitive?.contentOrNull ?: return null
            val email = obj["email"]?.jsonPrimitive?.contentOrNull
            val emailVerified = obj["email_verified"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val username = obj["cognito:username"]?.jsonPrimitive?.contentOrNull
                ?: obj["preferred_username"]?.jsonPrimitive?.contentOrNull
                ?: sub
            ProfileClaims(sub = sub, email = email, emailVerified = emailVerified, username = username)
        } catch (_: Exception) {
            null
        }
    }
}

data class AccountProfile(
    val username: String,
    val sub: String?,
    val email: String?,
    val emailVerified: Boolean,
    val preferredUsername: String?,
)

data class AccountTokenView(
    val id: String,
    val displayName: String,
    val tokenPrefix: String,
    val createdAt: String?,
    val lastUsedAt: String?,
    val lastUsedPath: String?,
    val revokedAt: String?,
) {
    val isRevoked: Boolean get() = !revokedAt.isNullOrBlank()
}

data class AccountTokenWithSecret(
    val id: String,
    val displayName: String,
    val tokenPrefix: String,
    val createdAt: String?,
    val secret: String,
)

private data class ProfileClaims(
    val sub: String,
    val email: String?,
    val emailVerified: Boolean,
    val username: String,
)
