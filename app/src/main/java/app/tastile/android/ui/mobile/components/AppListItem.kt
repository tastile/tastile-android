/*
 * Tastile list-item placeholder. Thin entry-point in the
 * `ui.mobile.components` package that delegates to the canonical
 * `AppListItem` wrapper living in `core.designsystem.component`.
 */
package app.tastile.android.ui.mobile.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.tastile.android.core.designsystem.component.AppListItem as CoreAppListItem

@Composable
fun AppListItem(
    headline: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    supporting: String? = null,
) {
    CoreAppListItem(
        headline = headline,
        leading = leading,
        trailing = trailing,
        onClick = onClick,
        modifier = modifier,
        supporting = supporting,
    )
}
