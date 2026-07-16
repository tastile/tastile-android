package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaListItem
import app.tastile.android.core.designsystem.component.NiaModalBottomSheet
import app.tastile.android.core.designsystem.component.NiaRememberModalBottomSheetState

data class ReferenceOption(val id: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferencePickerSheet(
    references: List<ReferenceOption>,
    onSelect: (ReferenceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = NiaRememberModalBottomSheetState(skipPartiallyExpanded = true)

    NiaModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.picker_reference_label), style = MaterialTheme.typography.titleMedium)
            if (references.isEmpty()) {
                Text(
                    stringResource(R.string.empty_projects_title),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                references.forEach { ref ->
                    NiaListItem(
                        headlineContent = { Text(ref.label) },
                        supportingContent = { Text(ref.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                        modifier = Modifier.clickable { onSelect(ref) },
                    )
                }
            }
        }
    }
}
