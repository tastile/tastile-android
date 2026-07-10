package app.tastile.android.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import app.tastile.android.BuildConfig
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiTokenManager: Lazy<ApiTokenManager>,
) : CurrentUserProvider, AuthRepositoryContract {
    private companion object {
        private const val TAG = "AuthRepository"
    }

    private val oauthRedirectUrl = BuildConfig.COGNITO_REDIRECT_URI
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = EncryptedTokenStorage.cognitoPrefs(context)
    private val _authState = MutableStateFlow(loadStoredAuthState())
    val currentSession: Any? get() = null
    override val authState: StateFlow<TastileAuthState> = _authState.asStateFlow()

    override fun currentUserId(): String? =
        (_authState.value as? TastileAuthState.Authenticated)?.userId

    override fun currentIdToken(): String? =
        (_authState.value as? TastileAuthState.Authenticated)?.idToken?.takeIf { !isJwtExpired(it) }
            ?: refreshCognitoSessionOrNull()?.idToken

    override fun currentAccessToken(): String? =
        (_authState.value as? TastileAuthState.Authenticated)?.accessToken?.takeIf { !isJwtExpired(it) }
            ?: refreshCognitoSessionOrNull()?.accessToken

    override suspend fun signInWithCognito(context: Context) {
        startCognitoHostedUi(context, platform = "android")
    }

    override suspend fun signInWithGoogle(context: Context) {
        startCognitoHostedUi(context, identityProvider = "Google", platform = "android")
    }

    override suspend fun signOut() {
        apiTokenManager.get().signOut()
        prefs.edit { clear() }
        _authState.value = TastileAuthState.Unauthenticated
    }

    suspend fun handleDeepLink(intent: Intent): Boolean {
        val data = intent.data ?: return false
        val deeplink = data.toString()

        val isSupportedCallback =
            deeplink.startsWith(oauthRedirectUrl) ||
            deeplink.startsWith("https://app.tastile.app/auth/callback")

        if (!isSupportedCallback) {
            return false
        }

        val fragmentParams = parseParams(data.fragment)
        val queryParams = parseParams(data.query)
        val fragmentState = fragmentParams["state"] ?: queryParams["state"]
        val idToken = fragmentParams["id_token"]
        val accessToken = fragmentParams["access_token"]
        if (!idToken.isNullOrBlank() && !accessToken.isNullOrBlank() && !fragmentState.isNullOrBlank()) {
            importCognitoTokenSession(
                idToken = idToken,
                accessToken = accessToken,
                refreshToken = fragmentParams["refresh_token"],
                state = fragmentState
            )
            return true
        }

        val code = queryParams["code"]
        val state = queryParams["state"]
        if (!code.isNullOrBlank() && !state.isNullOrBlank()) {
            exchangeCognitoCodeForSession(code, state)
            return true
        }

        throw IllegalStateException("Cognito callback missing authorization code")
    }

    private fun parseParams(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@mapNotNull null
                val key = Uri.decode(part.substring(0, idx))
                val value = Uri.decode(part.substring(idx + 1))
                key to value
            }
            .toMap()
    }

    private fun startCognitoHostedUi(context: Context, identityProvider: String? = null, platform: String? = null) {
        val redirectUri = BuildConfig.COGNITO_REDIRECT_URI
        val webAuthBaseUrl = BuildConfig.COGNITO_WEB_AUTH_BASE_URL
        if (BuildConfig.COGNITO_CLIENT_ID.isBlank() ||
            BuildConfig.COGNITO_REGION.isBlank() ||
            BuildConfig.COGNITO_HOSTED_UI_DOMAIN.isBlank() ||
            redirectUri.isBlank() ||
            webAuthBaseUrl.isBlank()
        ) {
            throw IllegalStateException("Cognito config is missing")
        }

        val verifier = generatePkceVerifier()
        val challenge = pkceS256(verifier)
        val state = generateState()
        prefs.edit {
            putString("pkce_verifier", verifier)
            putString("oauth_state", state)
        }

        val authUrl = CognitoAuthStartUrlBuilder.build(
            webAuthBaseUrl = webAuthBaseUrl,
            redirectUri = redirectUri,
            codeChallenge = challenge,
            state = state,
            identityProvider = identityProvider,
            platform = platform
        ).toUri()

        context.startActivity(
            Intent(Intent.ACTION_VIEW, authUrl).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private suspend fun exchangeCognitoCodeForSession(code: String, state: String) {
        val expectedState = prefs.getString("oauth_state", null)
        val verifier = prefs.getString("pkce_verifier", null)
        if (expectedState.isNullOrBlank() || verifier.isNullOrBlank() || expectedState != state) {
            throw IllegalStateException("OAuth state mismatch")
        }

        val token = withContext(Dispatchers.IO) {
            exchangeCognitoCode(code, verifier)
        }
        val claims = parseIdTokenClaims(token.idToken)
        prefs.edit {
            remove("oauth_state")
            remove("pkce_verifier")
            putString("id_token", token.idToken)
            putString("access_token", token.accessToken)
            putString("refresh_token", token.refreshToken)
            putString("user_id", claims.sub)
            putString("email", claims.email)
        }
        _authState.value = TastileAuthState.Authenticated(
            userId = claims.sub,
            email = claims.email,
            idToken = token.idToken,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken
        )
    }

    private fun importCognitoTokenSession(
        idToken: String,
        accessToken: String,
        refreshToken: String?,
        state: String
    ) {
        val expectedState = prefs.getString("oauth_state", null)
        if (expectedState.isNullOrBlank() || expectedState != state) {
            throw IllegalStateException("OAuth state mismatch")
        }

        val claims = parseIdTokenClaims(idToken)
        prefs.edit {
            remove("oauth_state")
            remove("pkce_verifier")
            putString("id_token", idToken)
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id", claims.sub)
            putString("email", claims.email)
        }
        _authState.value = TastileAuthState.Authenticated(
            userId = claims.sub,
            email = claims.email,
            idToken = idToken,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun exchangeCognitoCode(code: String, verifier: String): CognitoTokenSession {
        val region = BuildConfig.COGNITO_REGION
        val domain = BuildConfig.COGNITO_HOSTED_UI_DOMAIN
        val endpoint = URL("https://$domain.auth.$region.amazoncognito.com/oauth2/token")
        val body = listOf(
            "grant_type" to "authorization_code",
            "client_id" to BuildConfig.COGNITO_CLIENT_ID,
            "code" to code,
            "redirect_uri" to BuildConfig.COGNITO_REDIRECT_URI,
            "code_verifier" to verifier
        ).joinToString("&") { (key, value) ->
            "${Uri.encode(key)}=${Uri.encode(value)}"
        }

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Cognito token exchange failed: HTTP $status $responseText")
        }

        val response = json.parseToJsonElement(responseText).jsonObject
        val idToken = response["id_token"]?.jsonPrimitive?.contentOrNull
        val accessToken = response["access_token"]?.jsonPrimitive?.contentOrNull
        val refreshToken = response["refresh_token"]?.jsonPrimitive?.contentOrNull
        if (idToken.isNullOrBlank() || accessToken.isNullOrBlank()) {
            throw IllegalStateException("Cognito response missing id/access token")
        }
        return CognitoTokenSession(idToken, accessToken, refreshToken)
    }

    private fun refreshCognitoSessionOrNull(): TastileAuthState.Authenticated? {
        val refreshToken = prefs.getString("refresh_token", null)?.takeIf { it.isNotBlank() }
            ?: return null
        return try {
            val token = refreshCognitoToken(refreshToken)
            val claims = parseIdTokenClaims(token.idToken)
            prefs.edit {
                putString("id_token", token.idToken)
                putString("access_token", token.accessToken)
                putString("refresh_token", token.refreshToken ?: refreshToken)
                putString("user_id", claims.sub)
                putString("email", claims.email)
            }
            val authenticated = TastileAuthState.Authenticated(
                userId = claims.sub,
                email = claims.email,
                idToken = token.idToken,
                accessToken = token.accessToken,
                refreshToken = token.refreshToken ?: refreshToken
            )
            _authState.value = authenticated
            authenticated
        } catch (e: Exception) {
            Log.e(TAG, "Cognito refresh failed", e)
            val rejectedByCognito = e is IllegalStateException &&
                e.message?.let { msg ->
                    val httpMatch = Regex("HTTP (\\d{3})").find(msg)
                    val code = httpMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                    code != null && code in 400..499
                } == true
            if (rejectedByCognito) {
                prefs.edit { clear() }
                _authState.value = TastileAuthState.Unauthenticated
            }
            null
        }
    }

    private fun refreshCognitoToken(refreshToken: String): CognitoTokenSession {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            var result: CognitoTokenSession? = null
            var error: Throwable? = null
            Thread {
                try {
                    result = refreshCognitoTokenBlocking(refreshToken)
                } catch (t: Throwable) {
                    error = t
                }
            }.apply { start(); join() }
            error?.let { throw it }
            return result!!
        }
        return refreshCognitoTokenBlocking(refreshToken)
    }

    private fun refreshCognitoTokenBlocking(refreshToken: String): CognitoTokenSession {
        val region = BuildConfig.COGNITO_REGION
        val domain = BuildConfig.COGNITO_HOSTED_UI_DOMAIN
        val endpoint = URL("https://$domain.auth.$region.amazoncognito.com/oauth2/token")
        val body = listOf(
            "grant_type" to "refresh_token",
            "client_id" to BuildConfig.COGNITO_CLIENT_ID,
            "refresh_token" to refreshToken
        ).joinToString("&") { (key, value) ->
            "${Uri.encode(key)}=${Uri.encode(value)}"
        }

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

        val status = connection.responseCode
        val responseText = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Cognito refresh failed: HTTP $status $responseText")
        }

        val response = json.parseToJsonElement(responseText).jsonObject
        val idToken = response["id_token"]?.jsonPrimitive?.contentOrNull
        val accessToken = response["access_token"]?.jsonPrimitive?.contentOrNull
        val nextRefreshToken = response["refresh_token"]?.jsonPrimitive?.contentOrNull
        if (idToken.isNullOrBlank() || accessToken.isNullOrBlank()) {
            throw IllegalStateException("Cognito refresh response missing id/access token")
        }
        return CognitoTokenSession(idToken, accessToken, nextRefreshToken)
    }

    private fun loadStoredAuthState(): TastileAuthState {
        val userId = prefs.getString("user_id", null)
        val idToken = prefs.getString("id_token", null)
        val accessToken = prefs.getString("access_token", null)
        if (userId.isNullOrBlank() || idToken.isNullOrBlank() || accessToken.isNullOrBlank()) {
            return TastileAuthState.Unauthenticated
        }
        return TastileAuthState.Authenticated(
            userId = userId,
            email = prefs.getString("email", null),
            idToken = idToken,
            accessToken = accessToken,
            refreshToken = prefs.getString("refresh_token", null)
        )
    }

    private data class CognitoTokenSession(
        val idToken: String,
        val accessToken: String,
        val refreshToken: String?
    )

    private data class IdTokenClaims(val sub: String, val email: String?)

    private fun parseIdTokenClaims(idToken: String): IdTokenClaims {
        val parts = idToken.split(".")
        if (parts.size < 2) throw IllegalStateException("Invalid Cognito id token")
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val sub = jsonObject["sub"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("Cognito id token missing sub")
        val email = jsonObject["email"]?.jsonPrimitive?.contentOrNull
        return IdTokenClaims(sub, email)
    }

    private fun isJwtExpired(idToken: String): Boolean {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            val exp = json.parseToJsonElement(payload).jsonObject["exp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: return true
            exp <= (System.currentTimeMillis() / 1000L) + 30L
        } catch (_: Exception) {
            true
        }
    }

    private fun generatePkceVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun pkceS256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
