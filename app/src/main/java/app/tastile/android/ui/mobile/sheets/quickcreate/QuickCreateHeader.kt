package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
import app.tastile.android.R

/**
 * Title row shared across the QuickCreate workflow panels — directly
 * mirrors `tastile-web/src/features/create-tile/ui/QuickCreateHeader.tsx`.
 */
@Composable
fun QuickCreateHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    titleTestTag: String = "quick-create-title",
    placeholder: String = stringResource(R.string.quick_create_title),
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    trailing: @Composable (() -> Unit)? = null,
    onClose: () -> Unit = { },
) {
    FormRow(
        modifier = modifier,
        icon = null,
        content = {
            UnderlineTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = placeholder,
                textStyle = textStyle,
                imeAction = ImeAction.Done,
                testTag = titleTestTag,
            )
        },
        trailing = trailing,
    )
}
