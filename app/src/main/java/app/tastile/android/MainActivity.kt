package app.tastile.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.KeyguardManager
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.tastile.android.data.auth.ApiTokenCache
import app.tastile.android.data.auth.AuthRepository
import app.tastile.android.data.auth.TastileAuthState
import app.tastile.android.data.notification.PushEndpointRepository
import app.tastile.android.data.user.UserSettingsRepository
import app.tastile.android.ui.app.AppShellViewModel
import app.tastile.android.ui.mobile.MobileNavGraph
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import app.tastile.android.sync.SyncCoordinator
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.core.designsystem.theme.SystemBarEffect
import app.tastile.android.core.designsystem.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val appShellViewModel: AppShellViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var apiTokenCache: ApiTokenCache

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    @Inject
    lateinit var executionNotificationCoordinator: ExecutionNotificationCoordinator

    @Inject
    lateinit var pushEndpointRepository: PushEndpointRepository

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    private var securityUnlockInProgress = false

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            executionNotificationCoordinator.start()
        }
    }

    private val requestSecurityUnlock = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        securityUnlockInProgress = false
        if (result.resultCode != RESULT_OK) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by dashboardViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = resolveDarkTheme(themeMode)

            TastileTheme(
                darkTheme = darkTheme,
            ) {
                SystemBarEffect(darkTheme = darkTheme)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileNavGraph(dashboardViewModel = dashboardViewModel)
                }
            }
        }

        // Cold-start deep link: MainActivity is launched by the system with
        // the `tastile://auth/callback?...` intent when the app was not
        // already running. Without this read the intent filter in the
        // manifest is decoration — the URI is dropped on the floor.
        handleAuthCallbackIntent(intent)

        observeSessionForCoreSync()
        requestSecurityUnlockIfNeeded()
    }

    /**
     * Warm-return deep link: MainActivity is `singleTask`, so a second
     * launch with a different intent fires `onNewIntent` instead of a fresh
     * `onCreate`. We must read `intent.data` here so the OAuth handoff is
     * picked up when the user returns to an already-running app.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep getIntent() in sync with the new intent so any later
        // observeSessionForCoreSync / fragment lookups see the latest data.
        setIntent(intent)
        handleAuthCallbackIntent(intent)
    }

    /**
     * Routes a [Intent.ACTION_VIEW] deep link to [AuthRepository]. Silently
     * ignores intents that are not auth callbacks (the activity has multiple
     * intent filters; the launcher MAIN/LAUNCHER filter is the common case).
     */
    private fun handleAuthCallbackIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        val accepted = runCatching { authRepository.completeAuthFromCallback(uri) }
            .onFailure { error ->
                Log.e(TAG, "completeAuthFromCallback threw", error)
                false
            }
            .getOrDefault(false)
        Log.i(TAG, "auth callback URI=${uri} accepted=$accepted")
    }

    override fun onStart() {
        super.onStart()
        requestSecurityUnlockIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            userSettingsRepository.recordSecurityLockLeftAt()
        }
    }

    private fun observeSessionForCoreSync() {
        lifecycleScope.launch {
            authRepository.authState.collectLatest { status ->
                if (status !is TastileAuthState.Authenticated) {
                    executionNotificationCoordinator.stop()
                    return@collectLatest
                }
                // The Core bridge needs the user identity plus the bearer
                // credentials. The v1 API token is the bearer for v1 calls;
                // the BetterAuth session token is the refresh credential
                // used to mint a fresh v1 token when the cached one is
                // rejected.
                val v1Token = apiTokenCache.currentCachedToken()
                    ?: apiTokenCache.getOrMint(
                        onMintFailed = { error ->
                            // Surface a one-line toast so the user sees that
                            // sync / API calls are running unauthenticated
                            // instead of failing silently. We keep the
                            // null return contract so the call site can
                            // still bail out cleanly.
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                getString(R.string.api_token_mint_failed),
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                            error.printStackTrace()
                        },
                    )
                    ?: return@collectLatest
                val sessionToken = authRepository.currentSessionToken()
                    ?: return@collectLatest
                requestNotificationPermissionIfNeeded()
                executionNotificationCoordinator.start()

                // Token registration is intentionally best-effort: a missing or
                // invalid push provider configuration must not block the user's
                // core schedule. The durable local endpoint state makes the next
                // authenticated launch retry token registration.
                runCatching { pushEndpointRepository.registerCurrentToken() }
                    .onFailure(Throwable::printStackTrace)

                runCatching {
                    syncCoordinator.onSessionAvailable(
                        userId = status.userId,
                        accessToken = v1Token,
                        refreshToken = sessionToken,
                    )
                }.onFailure { error ->
                    if (error is CoreBridgeError.NativeMethodUnavailable || error is CoreBridgeError.LibraryLoadFailed) {
                        syncCoordinator.markCoreBridgeUnavailable()
                    }
                    error.printStackTrace()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestSecurityUnlockIfNeeded() {
        if (securityUnlockInProgress || !userSettingsRepository.shouldRequireSecurityUnlock()) {
            return
        }
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (!keyguard.isDeviceSecure) {
            return
        }
        // createConfirmDeviceCredentialIntent is the only API that reuses the
        // device's existing PIN/pattern/password prompt without pulling in
        // androidx.biometric. BiometricPrompt is a richer replacement but adds
        // ~2 MB to the APK and forces the user through a biometric prompt
        // even when they've already configured a non-biometric lock screen.
        @Suppress("DEPRECATION")
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock Tastile",
            "Confirm your device lock to continue."
        ) ?: return
        securityUnlockInProgress = true
        requestSecurityUnlock.launch(intent)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
