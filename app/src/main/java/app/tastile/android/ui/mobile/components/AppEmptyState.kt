/*
 * Tastile empty-state placeholder. Thin entry-point in the
 * `ui.mobile.components` package that delegates to the canonical
 * `AppEmptyState` wrapper living in `core.designsystem.component`.
 *
 * The legacy `ui.mobile.designsystem.AppEmptyState` symbol was deleted as part
 * of the M2 migration. Keeping this thin redirect in `ui.mobile.components`
 * (a non-deleted package) preserves backwards-compatible imports for files
 * that still reference the old path; new code should import the canonical
 * wrapper from `app.tastile.android.core.designsystem.component.AppEmptyState`.
 */
package app.tastile.android.ui.mobile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.tastile.android.core.designsystem.component.AppEmptyState as CoreAppEmptyState

@Composable
fun AppEmptyState(
    modifier: Modifier = Modifier,
    message: String = "",
    title: String = "",
    hint: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    CoreAppEmptyState(
        message = message,
        modifier = modifier,
        title = title,
        hint = hint,
        actionLabel = actionLabel,
        onAction = onAction,
        icon = icon,
    )
}
