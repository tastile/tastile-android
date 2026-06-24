package app.tastile.android.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OverlayLayer(overlay: OverlayViewModel = hiltViewModel()) {
    val current by overlay.current.collectAsStateWithLifecycle()
    when (current) {
        Overlay.Hidden -> Unit
        else -> Unit /* populated in Task 20 */
    }
}