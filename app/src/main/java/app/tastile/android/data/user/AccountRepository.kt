package app.tastile.android.data.user

import app.tastile.android.data.api.BetterAuthAccountApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountApi: BetterAuthAccountApi,
    private val authState: AuthStateProvider,
) {
    suspend fun loadProfile(): AccountProfile {
        val fallbackUserId = authState.currentUserId()
        val fallbackEmail = authState.currentEmail()
        return try {
            val dto = accountApi.getProfile()
            AccountProfile(
                username = dto.username.ifBlank { fallbackUserId.orEmpty() },
                sub = dto.sub ?: fallbackUserId,
                email = dto.email ?: fallbackEmail,
                emailVerified = if (dto.sub != null) dto.emailVerified else false,
                preferredUsername = dto.preferredUsername,
            )
        } catch (e: Exception) {
            if (fallbackUserId != null) {
                AccountProfile(
                    username = fallbackUserId,
                    sub = fallbackUserId,
                    email = fallbackEmail,
                    emailVerified = false,
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

interface AuthStateProvider {
    fun currentUserId(): String?
    fun currentEmail(): String?
}
