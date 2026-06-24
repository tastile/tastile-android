package app.tastile.android.ui.mobile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayViewModel : ViewModel() {

    private val _current = MutableStateFlow<Overlay>(Overlay.Hidden)
    val current: StateFlow<Overlay> = _current.asStateFlow()

    fun show(o: Overlay) {
        _current.value = o
    }

    fun dismiss() {
        _current.value = Overlay.Hidden
    }
}
