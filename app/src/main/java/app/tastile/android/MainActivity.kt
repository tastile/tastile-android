package app.tastile.android

import android.content.Intent
import android.content.pm.PackageManager
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.os.Build
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
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.data.repository.TastileAuthState
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.navigation.TastileNavGraph
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import app.tastile.android.sync.SyncCoordinator
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.theme.TastileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    @Inject
    lateinit var executionNotificationCoordinator: ExecutionNotificationCoordinator

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
        
        // Handle deep links for OAuth
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            val themeMode by dashboardViewModel.themeMode.collectAsStateWithLifecycle()

            TastileTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TastileNavGraph(dashboardViewModel = dashboardViewModel)
                }
            }
        }

        observeSessionForCoreSync()
        requestSecurityUnlockIfNeeded()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.data == null) return
        lifecycleScope.launch {
            runCatching {
                authRepository.handleDeepLink(intent)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private fun observeSessionForCoreSync() {
        lifecycleScope.launch {
            authRepository.authState.collectLatest { status ->
                if (status !is TastileAuthState.Authenticated) {
                    executionNotificationCoordinator.stop()
                    return@collectLatest
                }
                val refreshToken = status.refreshToken ?: return@collectLatest
                requestNotificationPermissionIfNeeded()
                executionNotificationCoordinator.start()

                runCatching {
                    syncCoordinator.onSessionAvailable(
                        userId = status.userId,
                        accessToken = status.idToken,
                        refreshToken = refreshToken
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

    @Suppress("DEPRECATION") // createConfirmDeviceCredentialIntent is the only API that reuses the device's existing PIN/pattern/password prompt without pulling in androidx.biometric.
    private fun requestSecurityUnlockIfNeeded() {
        if (securityUnlockInProgress || !userSettingsRepository.shouldRequireSecurityUnlock()) {
            return
        }
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (!keyguard.isDeviceSecure) {
            return
        }
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock Tastile",
            "Confirm your device lock to continue."
        ) ?: return
        securityUnlockInProgress = true
        requestSecurityUnlock.launch(intent)
    }
}
