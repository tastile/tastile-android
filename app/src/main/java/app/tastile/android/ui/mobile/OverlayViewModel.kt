package app.tastile.android.ui.mobile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class OverlayViewModel @Inject constructor() : ViewModel() {

    private val _current = MutableStateFlow<Overlay>(Overlay.Hidden)
    val current: StateFlow<Overlay> = _current.asStateFlow()

    fun show(o: Overlay) {
        _current.value = o
    }

    fun dismiss() {
        _current.value = Overlay.Hidden
    }
}
