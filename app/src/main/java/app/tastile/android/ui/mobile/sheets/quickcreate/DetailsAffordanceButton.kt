package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaOutlinedButton

/**
 * Single-row affordance that opens a sub-panel sheet (Time / Schedule /
 * Meta).
 */
@Composable
fun DetailsAffordanceButton(
    label: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "details-affordance",
) {
    FormRow(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current,
            )
        },
        trailing = {
            NiaOutlinedButton(
                onClick = onOpen,
                text = { Text(stringResource(R.string.tile_edit_open_label)) },
                modifier = Modifier.testTag("$testTag-open"),
            )
        },
    )
}
