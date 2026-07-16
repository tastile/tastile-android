/*
 * Tastile picker button. Compact dropdown-style button that shows a leading
 * label/value column with an optional leading icon and a trailing chevron.
 *
 * Thin entry-point in `ui.mobile.components` that delegates to the canonical
 * `AppPickerButton` wrapper living in `core.designsystem.component`. The
 * legacy `ui.mobile.designsystem.AppPickerButton` symbol was deleted as part
 * of the M2 migration; new code should import the canonical wrapper directly.
 */
package app.tastile.android.ui.mobile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.tastile.android.core.designsystem.component.AppPickerButton as CoreAppPickerButton

@Composable
fun AppPickerButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
) {
    CoreAppPickerButton(
        label = label,
        value = value,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        testTag = testTag,
    )
}
