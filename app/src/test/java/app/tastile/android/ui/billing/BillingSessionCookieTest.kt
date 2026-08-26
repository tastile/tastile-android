package app.tastile.android.ui.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the BetterAuth session-cookie injection that
 * [BillingScreen] performs before loading the pricing URL.
 *
 * Background: BetterAuth delivers the session as
 *   `Set-Cookie: better-auth.session_token=<value>` (see
 * `BetterAuthHttpClient.extractSessionToken`), but Android's WebView
 * CookieManager does not share state with the app's `HttpURLConnection`
 * auth flow. The pricing WebView would otherwise redirect to login.
 *
 * These tests assert the cookie is built and handed to the CookieManager
 * BEFORE the WebView navigates. The "before navigation" ordering is
 * enforced by the call site in [BillingScreen]'s `AndroidView.factory`
 * (which calls `injectBetterAuthSessionCookie(...)` then `loadUrl(...)`);
 * the helper below asserts the cookie is actually issued with the
 * expected attributes.
 */
class BillingSessionCookieTest {

    @Test
    fun buildBetterAuthSessionCookieValue_includesTokenAndAllAttributes() {
        val cookie = buildBetterAuthSessionCookieValue("abc-123")

        assertEquals(
            "better-auth.session_token=abc-123; Path=/; Secure; SameSite=Lax",
            cookie,
        )
    }

    @Test
    fun injectBetterAuthSessionCookie_setsCookieOnHostBeforeWebViewNavigation() {
        val captured = mutableListOf<Pair<String, String>>()
        val storage = BillingCookieStorage { url, value ->
            captured += url to value
        }

        injectBetterAuthSessionCookie(
            cookieHost = "tastile.app",
            sessionToken = "session-xyz",
            storage = storage,
        )

        // The cookie must be set on the same host that the subsequent
        // WebView.loadUrl(...) targets; otherwise the WebView's
        // per-origin cookie jar would discard the value.
        assertEquals(1, captured.size)
        assertEquals("tastile.app", captured[0].first)
        assertEquals(
            "better-auth.session_token=session-xyz; Path=/; Secure; SameSite=Lax",
            captured[0].second,
        )
    }

    @Test
    fun injectBetterAuthSessionCookie_skipsWhenHostBlank() {
        val captured = mutableListOf<Pair<String, String>>()
        val storage = BillingCookieStorage { url, value ->
            captured += url to value
        }

        injectBetterAuthSessionCookie(
            cookieHost = "",
            sessionToken = "session-xyz",
            storage = storage,
        )
        injectBetterAuthSessionCookie(
            cookieHost = "   ",
            sessionToken = "session-xyz",
            storage = storage,
        )

        assertTrue(
            "No cookie must be set when host is blank (prevents polluting the global cookie jar)",
            captured.isEmpty(),
        )
    }

    @Test
    fun injectBetterAuthSessionCookie_skipsWhenSessionTokenMissing() {
        val captured = mutableListOf<Pair<String, String>>()
        val storage = BillingCookieStorage { url, value ->
            captured += url to value
        }

        injectBetterAuthSessionCookie(
            cookieHost = "tastile.app",
            sessionToken = null,
            storage = storage,
        )
        injectBetterAuthSessionCookie(
            cookieHost = "tastile.app",
            sessionToken = "",
            storage = storage,
        )
        injectBetterAuthSessionCookie(
            cookieHost = "tastile.app",
            sessionToken = "   ",
            storage = storage,
        )

        assertTrue(
            "No cookie must be set when the BetterAuth session token is missing/blank",
            captured.isEmpty(),
        )
    }
}
