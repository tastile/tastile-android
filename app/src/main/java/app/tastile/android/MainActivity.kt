package app.tastile.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.tastile.android.data.repository.AuthRepository
import app.tastile.android.navigation.TastileNavGraph
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.theme.TastileTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle deep links for OAuth
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
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
}
