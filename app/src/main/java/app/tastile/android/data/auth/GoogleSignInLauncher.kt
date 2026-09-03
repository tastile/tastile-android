package app.tastile.android.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Wraps Credential Manager + Google Identity Services for the native
 * Google Sign-In flow on Android. Implementations are responsible for
 * presenting the system account picker, returning a Google ID token, or
 * throwing [GoogleSignInUnavailableException] when the user cannot or
 * will not complete the flow (no Google account on device, no Play
 * Services, user cancel).
 */
interface GoogleSignInLauncher {
    /**
     * Returns a Google ID token suitable for exchange against
     * BetterAuth's /api/auth/sign-in/social idToken branch.
     *
     * @throws GoogleSignInUnavailableException when no Google account is
     *   available, Play Services is missing, or the user cancels the
     *   system account picker. The caller is expected to fall back to
     *   the web OAuth handoff.
     * @throws GoogleSignInFailedException for any other failure (network
     *   error during token exchange, unexpected credential type).
     */
    suspend fun getIdToken(): String
}

/**
 * The user cannot or will not complete native Google Sign-In.
 * Callers should fall back to the web OAuth handoff
 * (`AuthRepository.signInWithProvider("google")`).
 */
class GoogleSignInUnavailableException(cause: Throwable) : RuntimeException(cause)

/**
 * A failure other than user unavailability — typically a server-side
 * rejection or an unexpected credential type. Surfaces as an error
 * chip in the UI rather than triggering the web fallback.
 */
class GoogleSignInFailedException(message: String) : RuntimeException(message)

/**
 * [GoogleSignInLauncher] backed by [CredentialManager] +
 * [GetGoogleIdOption]. Presents the system account picker (or auto-
 * selects when [GetGoogleIdOption.setAutoSelectEnabled] is true and
 * exactly one Google account is on the device), and returns the issued
 * Google ID token for exchange against BetterAuth.
 *
 * Filtering is disabled ([GetGoogleIdOption.setFilterByAuthorizedAccounts]
 * = false) so first-time users also see the picker; BetterAuth's server
 * creates the user account on first sign-in.
 */
@Singleton
class CredentialManagerGoogleSignInLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("googleAndroidClientId") private val serverClientId: String,
) : GoogleSignInLauncher {

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    override suspend fun getIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialException) {
            throw GoogleSignInUnavailableException(e)
        }

        val credential = result.credential
        if (credential !is GoogleIdTokenCredential) {
            throw GoogleSignInFailedException(
                "Unexpected credential type: ${credential::class.java.name}",
            )
        }
        return credential.idToken
            ?: throw GoogleSignInFailedException("GoogleIdTokenCredential.idToken is null")
    }
}
