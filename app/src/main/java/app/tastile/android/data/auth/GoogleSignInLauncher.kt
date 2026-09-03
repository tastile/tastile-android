package app.tastile.android.data.auth

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
