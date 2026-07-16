package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.util.formatIsoDateTime
import app.tastile.android.ui.designsystem.AppCorner
import app.tastile.android.ui.designsystem.AppEmptyState
import app.tastile.android.ui.designsystem.AppSectionHeader
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineSubScale
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileTokens

/**
 * Timeline sub-tab body. Renders the `dashboard_tiles_timeline_section_title`
 * header, a 4-button scale segmented control, two DatePickerDialog triggers
 * when scale = CUSTOM, and a vertical list of timestamped rows driven by
 * `DashboardViewModel.timeline`. Dot colour follows the web parity rule
 * "ended → success-green (`MobileTokens.Status.started`) · otherwise → primary".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilesTimelineBody(
    vm: DashboardViewModel,
    locale: AppLocale,
    modifier: Modifier = Modifier,
) {
    val timeline by vm.timeline.collectAsStateWithLifecycle()
    val scale by vm.timelineScale.collectAsStateWithLifecycle()
    val customStart by vm.customStartIso.collectAsStateWithLifecycle()
    val customEnd by vm.customEndIso.collectAsStateWithLifecycle()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AppSectionHeader(text = stringResource(R.string.dashboard_tiles_timeline_section_title))
        TimelineScaleHeader(
            current = scale,
            onPick = { vm.setTimelineScale(it) },
            customStart = customStart,
            customEnd = customEnd,
            onCustomRange = { start, end -> vm.setCustomRange(start, end) },
        )
        if (timeline.isEmpty()) {
            AppEmptyState(message = stringResource(R.string.dashboard_tiles_timeline_empty))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                timeline.forEach { item ->
                    TimelineRow(item = item, locale = locale)
                }
            }
        }
    }
}

@Composable
private fun TimelineScaleHeader(
    current: TimelineSubScale,
    onPick: (TimelineSubScale) -> Unit,
    customStart: String?,
    customEnd: String?,
    onCustomRange: (String?, String?) -> Unit,
) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppCorner.pillShape)
                .background(colors.surfaceVariant)
                .padding(AppTheme.spacing.xxs)
                .testTag("tiles-timeline-scale"),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            TimelineSubScale.entries.forEach { scale ->
                val active = scale == current
                Text(
                    scale.label(),
                    style = AppTheme.typography.labelMedium,
                    color = if (active) colors.onSurface else colors.onSurfaceVariant,
                    modifier = Modifier
                        .clip(AppCorner.smallShape)
                        .background(if (active) colors.surface else colors.surfaceVariant)
                        .clickable { onPick(scale) }
                        .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xxs),
                )
            }
        }
        if (current == TimelineSubScale.CUSTOM) {
            DateRangeRow(
                startIso = customStart,
                endIso = customEnd,
                onChange = onCustomRange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeRow(
    startIso: String?,
    endIso: String?,
    onChange: (String?, String?) -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { showStartPicker = true }
                .testTag("tiles-timeline-date-start"),
        ) {
            OutlinedTextField(
                value = startIso?.take(10) ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Start") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable { showEndPicker = true }
                .testTag("tiles-timeline-date-end"),
        ) {
            OutlinedTextField(
                value = endIso?.take(10) ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("End") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (showStartPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                AppPrimaryButton(
                    text = "OK",
                    leadingIcon = Icons.Outlined.Check,
                    onClick = {
                        onChange(pickerState.selectedDateMillis?.let(::isoFromMillis), endIso)
                        showStartPicker = false
                    },
                )
            },
            dismissButton = {
                AppTertiaryButton(
                    text = "Cancel",
                    leadingIcon = Icons.Outlined.Close,
                    onClick = { showStartPicker = false },
                )
            },
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }
    if (showEndPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                AppPrimaryButton(
                    text = "OK",
                    leadingIcon = Icons.Outlined.Check,
                    onClick = {
                        onChange(startIso, pickerState.selectedDateMillis?.let(::isoFromMillis))
                        showEndPicker = false
                    },
                )
            },
            dismissButton = {
                AppTertiaryButton(
                    text = "Cancel",
                    leadingIcon = Icons.Outlined.Close,
                    onClick = { showEndPicker = false },
                )
            },
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }
}

@Composable
private fun TimelineRow(item: CoreTimelineItem, locale: AppLocale) {
    val colors = AppTheme.colors
    val ended = item.type.endsWith("_ended") || item.status == "done" || item.status == "completed"
    val dotColor = if (ended) MobileTokens.Status.started else colors.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline-block-${item.id}")
            .padding(vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(AppCorner.pillShape)
                .background(dotColor),
        )
        Text(
            text = formatIsoDateTime(item.startAt, locale),
            style = AppTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = "${item.title} · ${item.type}",
            style = AppTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun TimelineSubScale.label(): String = when (this) {
    TimelineSubScale.DAY -> "Day"
    TimelineSubScale.WEEK -> "Week"
    TimelineSubScale.MONTH -> "Month"
    TimelineSubScale.CUSTOM -> "Custom"
}

private fun isoFromMillis(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).toString()
