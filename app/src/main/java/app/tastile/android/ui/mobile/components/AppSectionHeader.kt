/*
 * Tastile section-header placeholder. Thin entry-point in the
 * `ui.mobile.components` package that delegates to the canonical
 * `AppSectionHeader` wrapper living in `core.designsystem.component`.
 *
 * The legacy `ui.mobile.designsystem.AppSectionHeader` symbol was deleted as
 * part of the M2 migration. Keeping this thin redirect preserves call-site
 * imports for files that still reference the old path; new code should
 * import the canonical wrapper from
 * `app.tastile.android.core.designsystem.component.AppSectionHeader`.
 */
package app.tastile.android.ui.mobile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.tastile.android.core.designsystem.component.AppSectionHeader as CoreAppSectionHeader

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    CoreAppSectionHeader(text = title, modifier = modifier)
}
