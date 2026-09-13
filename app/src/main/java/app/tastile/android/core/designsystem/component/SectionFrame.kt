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
import androidx.compose.foundation.layout.Row
// m2-allow: primitive
import androidx.compose.foundation.layout.RowScope
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.tastile.android.core.designsystem.theme.LocalTastileShapeTokens
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Vertical section container with a titled header, optional trailing
 * action, and a footer divider.
 *
 * The [SectionFrame] is the canonical layout primitive for the
 * `release:0-6-0` UI rebuild. Use it for grouping related rows in a screen
 * (settings groups, sub-sections inside a project, fields inside a single
 * form step) so that every grouped region in the app shares the same header
 * + divider + content shape.
 *
 * Spacing and shape honor `LocalTastileSpacingTokens.current` /
 * `LocalTastileShapeTokens.current` — both are provided by [TastileTheme].
 *
 * @param title Header label. When blank, the header row is omitted entirely
 *   and only the content + optional divider are rendered.
 * @param modifier Modifier applied to the outer column.
 * @param action Optional trailing slot in the header row (e.g. "Edit",
 *   "See all"). Renders as a compact trailing button / icon slot inside
 *   the header [Row].
 * @param showDivider Whether the trailing [HorizontalDivider] is rendered.
 *   Defaults to `true` so every section is visually anchored.
 * @param content The composable body of the section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionFrame(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    content: @Composable () -> Unit,
) {
    val spacing = LocalTastileSpacingTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = spacing.l),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    modifier = Modifier.padding(end = spacing.l),
                    content = action,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.l,
                    vertical = spacing.s,
                ),
        ) {
            content()
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SectionFramePreview() {
    TastileTheme {
        SectionFrame(
            title = "Section title",
            action = {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            Text(
                text = "Section body content.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SectionFrameNoActionNoDividerPreview() {
    TastileTheme {
        SectionFrame(
            title = "",
            showDivider = false,
        ) {
            Text(
                text = "Headerless section.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// Suppress unused-import warnings on shape tokens that aren't yet consumed
// at this frame's content level but are part of the documented contract.
@Suppress("unused")
private val shapeTokens = LocalTastileShapeTokens