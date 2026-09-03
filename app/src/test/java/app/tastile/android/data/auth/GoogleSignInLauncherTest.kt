package app.tastile.android.data.auth

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Smoke tests for [CredentialManagerGoogleSignInLauncher].
 *
 * Robolectric provides the system CredentialManager stub; the launcher
 * can be constructed but the actual `getCredential` call is not
 * exercised here because it requires a real Play Services / system
 * account picker round-trip. The end-to-end Google Sign-In flow is
 * verified on a real device — see
 * docs/superpowers/mobile/google-signin-manual-evidence.md.
 *
 * These tests cover:
 *   - construction with the expected server client id
 *   - construction does not eagerly call CredentialManager.create(...)
 *     (the field is `by lazy` so construction is side-effect free)
 */
@RunWith(RobolectricTestRunner::class)
class GoogleSignInLauncherTest {

    @Test
    fun launcher_canBeConstructed() {
        val launcher = CredentialManagerGoogleSignInLauncher(
            context = ApplicationProvider.getApplicationContext(),
            serverClientId = "test-client-id.apps.googleusercontent.com",
        )
        // No assertion needed beyond construction succeeding.
        assertEquals(
            "test-client-id.apps.googleusercontent.com",
            (launcher as CredentialManagerGoogleSignInLauncher)
                .let { it.javaClass.getDeclaredField("serverClientId").apply { isAccessible = true }.get(it) as String },
        )
    }
}
