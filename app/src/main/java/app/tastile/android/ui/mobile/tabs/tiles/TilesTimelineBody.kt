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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.util.formatIsoDateTime
import app.tastile.android.ui.dashboard.DashboardViewModel
import app.tastile.android.ui.dashboard.TimelineSubScale

/**
 * Timeline sub-tab body. Renders the `dashboard_tiles_timeline_section_title`
 * header, a 4-button scale segmented control, two DatePickerDialog triggers
 * when scale = CUSTOM, and a vertical list of timestamped rows driven by
 * `DashboardViewModel.timeline`. Dot colour follows the web parity rule
 * "ended → success-green · otherwise → primary".
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dashboard_tiles_timeline_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TimelineScaleHeader(
            current = scale,
            onPick = { vm.setTimelineScale(it) },
            customStart = customStart,
            customEnd = customEnd,
            onCustomRange = { start, end -> vm.setCustomRange(start, end) },
        )
        if (timeline.isEmpty()) {
            Text(
                text = stringResource(R.string.dashboard_tiles_timeline_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp)
                .testTag("tiles-timeline-scale"),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TimelineSubScale.entries.forEach { scale ->
                val active = scale == current
                Text(
                    scale.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { onPick(scale) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                NiaButton(
                    onClick = {
                        onChange(pickerState.selectedDateMillis?.let(::isoFromMillis), endIso)
                        showStartPicker = false
                    },
                    text = { Text("OK") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Check, contentDescription = null) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { showStartPicker = false },
                    text = { Text("Cancel") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            },
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }
    if (showEndPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                NiaButton(
                    onClick = {
                        onChange(startIso, pickerState.selectedDateMillis?.let(::isoFromMillis))
                        showEndPicker = false
                    },
                    text = { Text("OK") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Check, contentDescription = null) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { showEndPicker = false },
                    text = { Text("Cancel") },
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            },
        ) { androidx.compose.material3.DatePicker(state = pickerState) }
    }
}

@Composable
private fun TimelineRow(item: CoreTimelineItem, locale: AppLocale) {
    val ended = item.type.endsWith("_ended") || item.status == "done" || item.status == "completed"
    val dotColor = if (ended) StatusStartedGreen else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline-block-${item.id}")
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = formatIsoDateTime(item.startAt, locale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${item.title} · ${item.type}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
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

// Mirrors the legacy MobileTokens.Status.started success-green (0xFF0D8A72).
private val StatusStartedGreen = Color(0xFF0D8A72)
