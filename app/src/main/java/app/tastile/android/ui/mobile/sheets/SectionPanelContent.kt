package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.dueAtDate
import app.tastile.android.data.model.isRecurring
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineScale
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
import app.tastile.android.ui.mobile.designsystem.MobileTokens
import app.tastile.android.ui.mobile.designsystem.SectionHeader
import app.tastile.android.ui.mobile.panels.schedule.ProjectsCheckboxSection as MovedProjectsCheckboxSection
import app.tastile.android.ui.mobile.panels.schedule.ScheduleRowList
import app.tastile.android.ui.mobile.panels.schedule.ScheduleViewToggle
import app.tastile.android.ui.mobile.panels.schedule.VIEW_RECURRING as SCHEDULE_VIEW_RECURRING
import app.tastile.android.ui.mobile.panels.schedule.VIEW_UPCOMING as SCHEDULE_VIEW_UPCOMING
import app.tastile.android.ui.mobile.panels.ReferencesSectionContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Section-aware right-pane content for [SidePanelSheet]. The page title lives
 * in the outer [PanelSheet] header (it switches with the pager page); this
 * composable renders only the section body. Back navigation is via swipe on
 * the pager, not a back button.
 */
@Composable
internal fun SectionPanelContent(
    section: SidePanelSection,
    dashboardViewModel: DashboardViewModel,
    overlayViewModel: OverlayViewModel = hiltViewModel(),
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (section) {
            SidePanelSection.Calendar -> TimelineSectionContent(dashboardViewModel)
            SidePanelSection.Schedule -> ScheduleSectionContent(dashboardViewModel)
            SidePanelSection.Projects -> ProjectsSectionContent(dashboardViewModel)
            SidePanelSection.References -> ReferencesSectionContent(dashboardViewModel = dashboardViewModel)
            SidePanelSection.Preferences -> PreferencesSectionContent(overlayViewModel)
        }
    }
}

// ─────────────────────────────────────────────
// Timeline (Calendar pane): C10 port lives in
// ui/mobile/panels/timeline/TimelineSectionContent.kt (title + meta pills +
// 4-tab scale + custom range + loading/empty/block list). Thin pass-through
// so the pager keeps resolving a local symbol.
// ─────────────────────────────────────────────
@Composable
private fun TimelineSectionContent(viewModel: DashboardViewModel) {
    app.tastile.android.ui.mobile.panels.timeline.TimelineSectionContent(
        dashboardViewModel = viewModel,
    )
}

// ─────────────────────────────────────────────
// Schedule, References, Preferences (real data)
// ─────────────────────────────────────────────
@Composable
private fun ScheduleSectionContent(viewModel: DashboardViewModel) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val scheduleView by viewModel.scheduleView.collectAsStateWithLifecycle()
    val now = LocalDate.now()
    val recurring = tiles.filter { it.isRecurring() }
    val upcoming = tiles.filter { tile ->
        tile.dueAtDate()
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { it in now..now.plusDays(7) } ?: false
    }
    val isRecurring = scheduleView == SCHEDULE_VIEW_RECURRING
    val visible = if (isRecurring) recurring else upcoming

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        ScheduleViewToggle(
            view = scheduleView,
            onViewChange = { viewModel.setScheduleView(it) },
        )
        ScheduleRowList(
            tiles = visible,
            recurring = isRecurring,
            onSelect = { id -> viewModel.selectTile(id) },
        )
        MovedProjectsCheckboxSection(tiles = tiles)
    }
}

@Composable
private fun PreferencesSectionContent(overlayViewModel: OverlayViewModel) {
    // C7 — 4-row vertical nav mirroring `PreferencesSidePanel` from
    // `tastile-web/src/components/panels/PreferencesSidePanel.tsx`. Each row
    // opens a sub-sheet (C9 General Preferences is TODO and renders a
    // no-op until C9 lands).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        SectionHeader(title = stringResource(R.string.nav_preferences))
        AppListItem(
            headline = stringResource(R.string.preferences_nav_general),
            leading = Icons.Outlined.Tune,
            trailing = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            onClick = { /* TODO(C9): open GeneralPreferencesSheet */ },
        )
        AppListItem(
            headline = stringResource(R.string.preferences_nav_profile),
            leading = Icons.Outlined.AccountCircle,
            trailing = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            onClick = { overlayViewModel.show(Overlay.AccountSettings) },
        )
        AppListItem(
            headline = stringResource(R.string.preferences_nav_subscription),
            leading = Icons.Outlined.CreditCard,
            trailing = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            onClick = { overlayViewModel.show(Overlay.Subscription) },
        )
        AppListItem(
            headline = stringResource(R.string.preferences_nav_tokens),
            leading = Icons.Outlined.Key,
            trailing = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            onClick = { overlayViewModel.show(Overlay.Tokens) },
        )
    }
}

// ─────────────────────────────────────────────
// Projects checkbox section (moved to ui/mobile/panels/schedule/
// ProjectsCheckboxSection.kt in C11 — now imported above as
// `MovedProjectsCheckboxSection`).
// ─────────────────────────────────────────────

@Composable
private fun ProjectsSectionContent(viewModel: DashboardViewModel) {
    // C5: full CRUD port — see ProjectsSectionContent.kt in ui/mobile/panels/.
    app.tastile.android.ui.mobile.panels.ProjectsSectionContent(dashboardViewModel = viewModel)
}
