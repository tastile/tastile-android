package app.tastile.android.ui.billing

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.ui.login.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BillingScreen(
    onBack: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    // Pass the Cognito ID token through the callback-aware web entrypoint.
    val authState by loginViewModel.authState.collectAsStateWithLifecycle()
    val accessToken = (authState as? TastileAuthState.Authenticated)?.idToken ?: ""
    
    // Build URL with access token for authentication
    val baseUrl = "https://tastile.app/pricing"
    val url = if (accessToken.isNotEmpty()) {
        "$baseUrl?access_token=$accessToken"
    } else {
        baseUrl
    }

    // Handle back button
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Billing",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
