@file:Suppress("DEPRECATION")

package app.tastile.android.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Centralized helper that opens the Keystore-backed encrypted SharedPreferences
 * files used by the app. Fails fast if the AndroidX security-crypto backend is
 * not initialized (e.g. Keystore unavailable on a tampered device).
 *
 * Two distinct prefs files are exposed:
 *
 * - [sessionTokenPrefs]: holds the BetterAuth session token issued by
 *   `POST /api/auth/sign-in/email`. This token is the bootstrap credential
 *   used to mint a Tastile API token; it is *not* the bearer for v1 API calls.
 * - [apiTokenPrefs]: holds the minted Tastile API token used as the
 *   `Authorization: Bearer` credential against v1 endpoints.
 *
 * Both files are excluded from cloud backup and device-to-device transfer via
 * `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`, and they
 * are also backed by `android:allowBackup="false"` in AndroidManifest.xml.
 */
object EncryptedTokenStorage {
    const val SESSION_TOKEN_PREFS = "tastile_better_auth_session"
    const val API_TOKEN_PREFS = "tastile_v1_api_tokens"

    @Volatile private var cachedSessionToken: SharedPreferences? = null
    @Volatile private var cachedApiTokens: SharedPreferences? = null

    fun sessionTokenPrefs(context: Context): SharedPreferences =
        cachedSessionToken ?: synchronized(this) {
            cachedSessionToken ?: openEncrypted(context, SESSION_TOKEN_PREFS).also { cachedSessionToken = it }
        }

    fun apiTokenPrefs(context: Context): SharedPreferences =
        cachedApiTokens ?: synchronized(this) {
            cachedApiTokens ?: openEncrypted(context, API_TOKEN_PREFS).also { cachedApiTokens = it }
        }

    private fun openEncrypted(context: Context, fileName: String): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            if (!isRobolectric()) throw e
            context.getSharedPreferences("${fileName}_robolectric", Context.MODE_PRIVATE)
        }
    }

    private fun isRobolectric(): Boolean {
        return runCatching {
            Class.forName("org.robolectric.RuntimeEnvironment")
        }.isSuccess
    }
}
