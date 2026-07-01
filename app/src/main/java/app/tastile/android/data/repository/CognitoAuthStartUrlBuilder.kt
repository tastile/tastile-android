package app.tastile.android.data.repository

import java.net.URLEncoder

object CognitoAuthStartUrlBuilder {
    fun build(
        webAuthBaseUrl: String,
        redirectUri: String,
        codeChallenge: String,
        state: String,
        identityProvider: String? = null,
        platform: String? = null
    ): String {
        val base = webAuthBaseUrl.trim().trimEnd('/')
        val params = linkedMapOf(
            "redirect_uri" to redirectUri,
            "state" to state,
            "code_challenge" to codeChallenge
        )
        if (!platform.isNullOrBlank()) {
            params["platform"] = platform
        }

        return if (identityProvider.isNullOrBlank()) {
            "$base/login?${encodeParams(params)}"
        } else {
            val providerParams = linkedMapOf("provider" to identityProvider)
            providerParams.putAll(params)
            "$base/auth/cognito/login?${encodeParams(providerParams)}"
        }
    }

    private fun encodeParams(params: Map<String, String>): String =
        params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
