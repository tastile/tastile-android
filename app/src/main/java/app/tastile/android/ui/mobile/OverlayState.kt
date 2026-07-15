package app.tastile.android.ui.mobile

sealed interface Overlay {
    data object Hidden : Overlay
    data object QuickCreate : Overlay
    data class TileEdit(val tileId: String) : Overlay
    data object Search : Overlay
    data object Notifications : Overlay
    data object AccountMenu : Overlay
    data class SidePanel(val section: SidePanelSection) : Overlay
    // C7 — preferences sub-sheets (opened from PreferencesSectionContent
    // and AccountMenuSheet rows). The body lives in ui/mobile/account/.
    data object AccountSettings : Overlay
    data object Subscription : Overlay
    data object Tokens : Overlay
}

enum class SidePanelSection { Calendar, Schedule, Projects, References, Preferences }
