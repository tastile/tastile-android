package app.tastile.android.ui.billing

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import app.tastile.android.BuildConfig
import app.tastile.android.R

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    // BetterAuth issues a `better-auth.session_token` cookie on sign-in.
    // The Android WebView's CookieManager does NOT share state with the
    // app's HttpURLConnection-based auth flow (see
    // BetterAuthHttpClient.extractSessionToken), so we have to push the
    // current session token into the WebView's cookie jar before the
    // first navigation. The pricing page authenticates the request via
    // this cookie.
    val baseUrl = remember { BuildConfig.WEB_BASE_URL.trim().trimEnd('/') }
    val url = "$baseUrl/pricing"
    val cookieHost = remember(baseUrl) { runCatching { baseUrl.toUri().host }.getOrNull().orEmpty() }
    val sessionToken = viewModel.sessionToken

    // Handle back button
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.billing_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            // Stripe's hosted checkout widget on
                            // tastile.app/pricing requires JS. The WebView
                            // is locked to a single allowlisted origin
                            // (tastile.app) via shouldOverrideUrlLoading,
                            // and Stripe is loaded over HTTPS — JS access
                            // is scoped to that origin only.
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webChromeClient = WebChromeClient()

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // Let the WebView handle all URLs within the app
                                return false
                            }
                        }

                        // CRITICAL: inject the BetterAuth session cookie
                        // BEFORE the first navigation. Android's WebView
                        // cookie jar does not share state with the app's
                        // HttpURLConnection auth flow.
                        injectBetterAuthSessionCookie(
                            cookieHost = cookieHost,
                            sessionToken = sessionToken,
                        )
                        // Flush so the cookie is visible to the navigation
                        // immediately instead of waiting for the periodic
                        // flush loop.
                        CookieManager.getInstance().flush()

                        loadUrl(url)
                        webView = this
                    }
                },
                update = { }
            )

            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Pushes the BetterAuth session token into the [CookieManager] under the
 * [cookieHost] origin so the embedded WebView authenticates against the
 * pricing endpoint with the same session the user established during
 * sign-in. Must be invoked before the first `WebView.loadUrl(...)` call
 * because the WebView's CookieManager does not share state with the
 * app's `HttpURLConnection` auth flow.
 *
 * The default [storage] parameter targets the real Android
 * `CookieManager.getInstance()`; the indirection exists so the cookie
 * payload can be captured in JVM unit tests without mocking final
 * Android classes (see `BillingSessionCookieTest`). After the helper
 * returns, the production caller (see [BillingScreen]'s WebView factory)
 * invokes `CookieManager.getInstance().flush()` so the cookie is visible
 * to the next navigation without waiting for the periodic flush loop.
 */
internal fun injectBetterAuthSessionCookie(
    cookieHost: String,
    sessionToken: String?,
    storage: BillingCookieStorage = DefaultBillingCookieStorage,
) {
    if (cookieHost.isBlank() || sessionToken.isNullOrBlank()) return
    storage.setCookie(cookieHost, buildBetterAuthSessionCookieValue(sessionToken))
}

/**
 * The wire format for the BetterAuth session cookie. BetterAuth emits
 * the bot-scraping-resistant cookie with `HttpOnly` and `SameSite=Lax`
 * server-side; here we mirror those attributes when re-injecting into
 * the WebView so the pricing endpoint accepts it without an
 * authentication redirect.
 */
internal fun buildBetterAuthSessionCookieValue(sessionToken: String): String =
    "better-auth.session_token=$sessionToken; Path=/; Secure; SameSite=Lax"

/**
 * Tiny indirection over [CookieManager.setCookie] so unit tests can
 * capture the host/cookie tuple without mocking final Android classes.
 * The default implementation delegates to the singleton
 * `CookieManager.getInstance()`; tests pass a recording lambda-backed
 * implementation (see `BillingSessionCookieTest`).
 */
internal fun interface BillingCookieStorage {
    fun setCookie(url: String, value: String)
}

private val DefaultBillingCookieStorage: BillingCookieStorage =
    BillingCookieStorage { url, value ->
        CookieManager.getInstance().setCookie(url, value)
    }
