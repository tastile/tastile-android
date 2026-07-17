package app.tastile.android.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import app.tastile.android.util.MainActivityTestRule
import org.junit.Test

/**
 * R17 (audit 2026-07-16): regression coverage for the top-level auth gate.
 *
 * On a cold start with no signed-in session the [app.tastile.android.MobileNavGraph]
 * routes the user to [app.tastile.android.ui.login.LoginScreen]. This test
 * exercises that path against a real [app.tastile.android.MainActivity] and
 * fails CI if the sign-in header is ever removed, renamed, or hidden behind
 * an unauthenticated screen state.
 *
 * Stable assertion target: the literal text "Sign in to Tastile" rendered by
 * the LoginScreen header. The brand label "Tastile" is asserted as a secondary
 * anchor so accidental removal of the brand block is also caught.
 */
class MainActivityAuthGateTest : MainActivityTestRule() {

    @Test
    fun mainActivity_unauthenticated_rendersLoginScreen() {
        composeTestRule
            .onNodeWithText("Sign in to Tastile")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Tastile")
            .assertIsDisplayed()
    }
}
