package app.tastile.android

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.AlarmManager
import android.provider.Settings
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
import app.tastile.android.navigation.TastileNavGraph
import app.tastile.android.core.CoreBridgeError
import app.tastile.android.notifications.ExecutionNotificationCoordinator
import app.tastile.android.sync.SyncCoordinator
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.theme.TastileTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var exactAlarmAccessRequested = false

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var syncCoordinator: SyncCoordinator

    @Inject
    lateinit var executionNotificationCoordinator: ExecutionNotificationCoordinator

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            executionNotificationCoordinator.start()
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
                val handled = authRepository.handleDeepLink(intent)
                if (!handled) {
                    supabaseClient.handleDeeplinks(intent)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    private fun observeSessionForCoreSync() {
        lifecycleScope.launch {
            authRepository.sessionStatus.collectLatest { status ->
                if (status !is SessionStatus.Authenticated) {
                    executionNotificationCoordinator.stop()
                    return@collectLatest
                }
                val session = status.session
                val userId = session.user?.id ?: return@collectLatest
                val accessToken = session.accessToken
                val refreshToken = session.refreshToken
                if (accessToken.isBlank() || refreshToken.isBlank()) return@collectLatest
                requestNotificationPermissionIfNeeded()
                requestExactAlarmAccessIfNeeded()
                executionNotificationCoordinator.start()

                runCatching {
                    syncCoordinator.onSessionAvailable(
                        userId = userId,
                        accessToken = accessToken,
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

    private fun requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || exactAlarmAccessRequested) return
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return
        if (alarmManager.canScheduleExactAlarms()) return
        exactAlarmAccessRequested = true
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
}
