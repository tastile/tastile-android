package app.tastile.android.ui.billing

import androidx.lifecycle.ViewModel
import app.tastile.android.data.auth.CurrentUserProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin ViewModel for the embedded pricing WebView. Exposes the active
 * BetterAuth session token so [BillingScreen] can inject it into the
 * WebView's CookieManager before the first `/pricing` navigation —
 * Android's WebView cookie jar does not share state with the app's
 * `HttpURLConnection`-based BetterAuth flow.
 *
 * Synchronous read at the call site is intentional: the WebView factory
 * runs once at composition and the cookie injection happens before
 * `loadUrl(...)`. Re-observing `authState` is handled by the host
 * Activity (see `MainActivity.observeSessionForCoreSync`), which closes
 * this screen when the user signs out.
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
) : ViewModel() {
    /** The current BetterAuth session token, or `null` if signed out. */
    val sessionToken: String?
        get() = currentUserProvider.currentSessionToken()
}
