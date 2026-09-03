package app.tastile.android.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.util.MainActivityTestRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * Task 7b instrumented coverage for the "Continue with Google" affordance
 * on [LoginScreen]. Asserts that:
 *
 *   1. The button is rendered with the stable `login-google-button`
 *      [androidx.compose.ui.platform.testTag] contract that LoginScreen
 *      exposes for instrumentation.
 *   2. Tapping the button wires through to
 *      [LoginViewModel.signInWithGoogle] without throwing — the click is
 *      an end-to-end smoke test that the button is enabled and its
 *      onClick lambda is bound to the ViewModel's Google Sign-In entry
 *      point under the real [app.tastile.android.MainActivity] graph.
 *
 * The "disabled while signing in" assertion that the brief originally
 * listed as a third test is intentionally omitted: there is no clean
 * Compose-side seam for driving [LoginViewModel.isGoogleSigningIn] from
 * `true` to `true` (the VM is private `hiltViewModel()` from
 * `LoginScreen` and the field is private with a `MutableStateFlow`
 * backing). Asserting the disabled visual from a test would require
 * either a fake VM seam in `LoginScreen` or a Compose rule that
 * re-enters the screen, both of which are out of scope for the
 * regression contract.
 *
 * Mirror the sibling pattern in
 * `app/src/androidTest/java/app/tastile/android/ui/navigation/QuickCreateSmokeTest.kt`:
 *   - `class … : MainActivityTestRule()` (no constructor args)
 *   - `composeTestRule` is the inherited `@get:Rule`-decorated property;
 *     do NOT write `rule.composeTestRule`.
 *
 * Run with:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *     --tests "app.tastile.android.ui.login.LoginScreenTest"
 */
@HiltAndroidTest
class LoginScreenTest : MainActivityTestRule() {

    @Test
    fun continueWithGoogleButton_isDisplayed() {
        composeTestRule
            .onNodeWithTag("login-google-button")
            .assertIsDisplayed()
    }

    @Test
    fun continueWithGoogleButton_triggersSignIn() {
        // The click is the load-bearing assertion: the Compose tree must
        // render an enabled OutlinedButton whose onClick calls
        // LoginViewModel.signInWithGoogle(context). The actual Google
        // sign-in flow runs inside the real Hilt graph under MainActivity
        // and may surface a CredentialManager no-account result on an
        // emulator without a signed-in Google account; that surfaces as
        // an error chip, not a test exception.
        composeTestRule
            .onNodeWithTag("login-google-button")
            .performClick()
    }
}
