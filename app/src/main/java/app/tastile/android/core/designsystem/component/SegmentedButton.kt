/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3Api::class)

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.tastile.android.core.designsystem.theme.NiaTheme

/**
 * Tastile segmented button. Wraps Material 3 [SegmentedButton].
 *
 * @param selected Whether this button is selected.
 * @param onClick Called when the user clicks the button.
 * @param shape The shape of the button (typically obtained from
 * [SegmentedButtonDefaults.itemShape]).
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button is enabled.
 * @param colors [SegmentedButtonColors] used to render the button.
 * @param border The border to draw around the button.
 * @param icon Optional icon shown on the button.
 * @param label The text label shown on the button.
 */
@Composable
fun NiaSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
    ),
    border: androidx.compose.foundation.BorderStroke = SegmentedButtonDefaults.borderStroke(
        color = MaterialTheme.colorScheme.outline,
    ),
    icon: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        border = border,
        icon = icon,
        label = label,
    )
}

/**
 * Tastile single-choice segmented button row. Wraps Material 3
 * [SingleChoiceSegmentedButtonRow].
 *
 * @param modifier Modifier applied to the row.
 * @param space The spacing between segments; default aligns with [SegmentedButtonDefaults.BorderWidth].
 * @param content The row content (typically multiple [NiaSegmentedButton]s).
 */
@Composable
fun NiaSingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: androidx.compose.ui.unit.Dp = SegmentedButtonDefaults.BorderWidth,
    content: @Composable RowScope.() -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
        space = space,
        content = content,
    )
}

@ThemePreviews
@Composable
private fun NiaSegmentedButtonRowPreview() {
    NiaTheme {
        NiaSingleChoiceSegmentedButtonRow {
            NiaSegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Option A") }
            NiaSegmentedButton(
                selected = false,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Option B") }
        }
    }
}
