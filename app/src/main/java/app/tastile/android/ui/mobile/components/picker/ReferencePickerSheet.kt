package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.ModalBottomSheet
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppListItem
import app.tastile.android.ui.mobile.designsystem.MobileSpacing

data class ReferenceOption(val id: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferencePickerSheet(
    references: List<ReferenceOption>,
    onSelect: (ReferenceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MobileSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
        ) {
            Text(stringResource(R.string.picker_reference_label), style = MaterialTheme.typography.titleMedium)
            if (references.isEmpty()) {
                Text(
                    stringResource(R.string.empty_projects_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(MobileSpacing.md),
                )
            } else {
                references.forEach { ref ->
                    AppListItem(
                        headline = ref.label,
                        supporting = ref.id,
                        leading = Icons.Outlined.Tag,
                        onClick = { onSelect(ref) },
                    )
                }
            }
        }
    }
}
