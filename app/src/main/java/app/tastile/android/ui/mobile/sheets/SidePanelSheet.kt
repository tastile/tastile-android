package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.SidePanelSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidePanelSheet(overlay: OverlayViewModel) {
    val current by overlay.current.collectAsStateWithLifecycle()
    val section = (current as? Overlay.SidePanel)?.section

    if (section != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var calendarView by remember { mutableStateOf("Day") }

        ModalBottomSheet(
            onDismissRequest = { overlay.dismiss() },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(section.name, style = MaterialTheme.typography.titleSmall)
                when (section) {
                    SidePanelSection.Calendar -> CalendarBlock(
                        current = calendarView,
                        onChange = { calendarView = it },
                    )
                    SidePanelSection.Schedule -> Text(
                        "All-day / time-anchored placements",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.Projects -> Text(
                        "Projects list — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.References -> Text(
                        "References — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SidePanelSection.Preferences -> Text(
                        "Preferences — wire to data source in next pass",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarBlock(current: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Day", "Week", "Month").forEach { label ->
            val isActive = label == current
            Text(
                text = label,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = if (isActive) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = "Select $label view",
                        role = Role.Button,
                        onClick = { onChange(label) },
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) { },
            )
        }
    }
    HorizontalDivider()
    Text(
        text = "$current view — populated by TimelineScreen in Task 20",
        style = MaterialTheme.typography.bodySmall,
    )
}
