package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R

/**
 * Memo / description row — shared across the specialized workflow forms.
 */
@Composable
fun MemoSection(
    memo: String,
    onMemoChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "memo-section",
    placeholder: String = stringResource(R.string.quick_create_memo_placeholder),
) {
    FormRow(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            UnderlineTextArea(
                value = memo,
                onValueChange = onMemoChange,
                placeholder = placeholder,
                testTag = testTag,
            )
        },
    )
}
