package app.tastile.android.ui.mobile

sealed interface Overlay {
    data object Hidden : Overlay
    data object QuickCreate : Overlay
    data class TileEdit(val tileId: String) : Overlay
    data object Search : Overlay
    data object Notifications : Overlay
    data object AccountMenu : Overlay
    data class SidePanel(val section: SidePanelSection) : Overlay
}

enum class SidePanelSection { Calendar, Schedule, Projects, References, Preferences }
