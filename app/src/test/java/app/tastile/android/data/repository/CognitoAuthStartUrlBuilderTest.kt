package app.tastile.android.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class CognitoAuthStartUrlBuilderTest {
    @Test
    fun build_withoutProvider_usesCustomTastileLoginPage() {
        val url = CognitoAuthStartUrlBuilder.build(
            webAuthBaseUrl = "https://app.tastile.app/",
            redirectUri = "tastile://auth/callback",
            codeChallenge = "challenge_1234567890",
            state = "state_1234567890"
        )

        assertEquals(
            "https://app.tastile.app/login?redirect_uri=tastile%3A%2F%2Fauth%2Fcallback&state=state_1234567890&code_challenge=challenge_1234567890",
            url
        )
    }

    @Test
    fun build_withGoogleProvider_usesCallbackAwareCognitoRoute() {
        val url = CognitoAuthStartUrlBuilder.build(
            webAuthBaseUrl = "https://app.tastile.app",
            redirectUri = "tastile://auth/callback",
            codeChallenge = "challenge_1234567890",
            state = "state_1234567890",
            identityProvider = "Google"
        )

        assertEquals(
            "https://app.tastile.app/auth/cognito/login?provider=Google&redirect_uri=tastile%3A%2F%2Fauth%2Fcallback&state=state_1234567890&code_challenge=challenge_1234567890",
            url
        )
    }
}
