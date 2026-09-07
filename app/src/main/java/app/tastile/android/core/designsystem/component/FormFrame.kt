/*
 * Copyright 2026 The Tastile Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tastile.android.core.designsystem.component

// m2-allow: primitive
import androidx.compose.foundation.layout.Arrangement
// m2-allow: primitive
import androidx.compose.foundation.layout.Column
// m2-allow: primitive
import androidx.compose.foundation.layout.ColumnScope
// m2-allow: primitive
import androidx.compose.foundation.layout.Row
// m2-allow: primitive
import androidx.compose.foundation.layout.RowScope
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Layout direction of a [FormFrame].
 *
 * - [FormLayout.Vertical] — fields are stacked top-to-bottom. Default.
 * - [FormLayout.Horizontal] — fields are placed side-by-side in a row,
 *   each taking an equal column. Use for compact two-field rows.
 */
enum class FormLayout { Vertical, Horizontal }

/**
 * Grouped form surface for the `release:0-6-0` UI rebuild.
 *
 * [FormFrame] composes a single labelled field group with an optional
 * helper text slot and the field body. It exists so every form in the
 * app (project subpanels, account edits, tile edit detail) shares the
 * same `label / helper / field` rhythm and spacing.
 *
 * Spacing comes from `LocalTastileSpacingTokens.current` so updates to the
 * token family ripple through automatically.
 *
 * @param label Group label rendered above the field body.
 * @param modifier Modifier applied to the outer [Column] / [Row].
 * @param layout Whether fields render vertically (default) or horizontally.
 * @param helper Optional helper text rendered below the label and above
 *   the field body. Renders in `bodySmall` with `onSurfaceVariant` color.
 * @param required Marks the label with a `*` indicator. Use for any field
 *   that must be completed before the form can be submitted.
 * @param error Optional error text. When supplied, the helper text slot
 *   is replaced by the error message rendered in `errorContainer` color.
 * @param content Field composable(s). The lambda's receiver matches the
 *   requested [layout] so callers can emit children idiomatically.
 */
@Composable
fun FormFrame(
    label: String,
    modifier: Modifier = Modifier,
    layout: FormLayout = FormLayout.Vertical,
    helper: String? = null,
    required: Boolean = false,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    FormFrameImpl(
        label = label,
        modifier = modifier,
        layout = layout,
        helper = helper,
        required = required,
        error = error,
        contentColumn = content,
        contentRow = {},
    )
}

/**
 * Horizontal-layout overload of [FormFrame] for two-up compact rows.
 * The content lambda is a [RowScope] receiver so callers can stack fields
 * via the standard `Row { ... }` API.
 */
@Composable
fun FormFrameHorizontal(
    label: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    required: Boolean = false,
    error: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FormFrameImpl(
        label = label,
        modifier = modifier,
        layout = FormLayout.Horizontal,
        helper = helper,
        required = required,
        error = error,
        contentColumn = {},
        contentRow = content,
    )
}

@Composable
private fun FormFrameImpl(
    label: String,
    modifier: Modifier,
    layout: FormLayout,
    helper: String?,
    required: Boolean,
    error: String?,
    contentColumn: @Composable ColumnScope.() -> Unit,
    contentRow: @Composable RowScope.() -> Unit,
) {
    val spacing = LocalTastileSpacingTokens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        // Label row (with optional required asterisk).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.padding(horizontal = spacing.l),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (required) {
                Text(
                    text = "*",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        // Helper or error text below the label.
        when {
            error != null -> Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = spacing.l),
            )
            helper != null -> Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.l),
            )
            else -> Unit
        }
        // Field body.
        when (layout) {
            FormLayout.Vertical -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                contentColumn()
            }
            FormLayout.Horizontal -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l),
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.Top,
            ) {
                contentRow()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun FormFrameVerticalPreview() {
    TastileTheme {
        FormFrame(
            label = "Title",
            helper = "Visible to all collaborators.",
            required = true,
        ) {
            Text(
                text = "Field body (vertical layout).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun FormFrameHorizontalPreview() {
    TastileTheme {
        FormFrameHorizontal(
            label = "Start / end",
            helper = "Use 24-hour format.",
        ) {
            Text(
                text = "Start",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "End",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun FormFrameErrorPreview() {
    TastileTheme {
        FormFrame(
            label = "Email",
            required = true,
            error = "Enter a valid email address.",
        ) {
            Text(
                text = "Field body (error state).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}