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

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton as MaterialSegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow as MaterialSingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tastile single-choice segmented button. Wraps Material 3
 * [androidx.compose.material3.SingleChoiceSegmentedButtonRowScope.SegmentedButton].
 *
 * Must be invoked inside a [NiaSingleChoiceSegmentedButtonRow] scope.
 *
 * @param selected Whether this segment is the currently selected option.
 * @param onClick Called when the user taps this segment.
 * @param shape The shape of the segment, typically derived from
 * [NiaSegmentedButtonDefaults.itemShape]. Typed as [Shape] for call-site
 * flexibility; the underlying accepts [androidx.compose.ui.graphics.Shape].
 * @param modifier Modifier applied to the segment.
 * @param enabled Whether the segment is interactive.
 * @param icon Optional leading icon slot.
 * @param label Visible label slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
@Composable
fun SingleChoiceSegmentedButtonRowScope.NiaSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit = { NiaSegmentedButtonDefaults.Icon(selected) },
    label: @Composable () -> Unit,
) {
    MaterialSegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        label = label,
    )
}

/**
 * Tastile single-choice segmented button row. Wraps Material 3
 * [androidx.compose.material3.SingleChoiceSegmentedButtonRow].
 *
 * @param modifier Modifier applied to the row.
 * @param space Overlap between adjacent segments (typically the stroke width).
 * @param content Buttons placed inside the row. Use the extension function
 * [SingleChoiceSegmentedButtonRowScope.NiaSegmentedButton].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaSingleChoiceSegmentedButtonRow(
    modifier: Modifier = Modifier,
    space: Dp = NiaSegmentedButtonDefaults.BorderWidth,
    content: @Composable SingleChoiceSegmentedButtonRowScope.() -> Unit,
) {
    MaterialSingleChoiceSegmentedButtonRow(
        modifier = modifier,
        space = space,
        content = content,
    )
}

/**
 * Tastile segmented button defaults.
 */
@OptIn(ExperimentalMaterial3Api::class)
object NiaSegmentedButtonDefaults {
    /** Border width between adjacent segments. */
    val BorderWidth: Dp = SegmentedButtonDefaults.BorderWidth

    /**
     * Returns the shape a [NiaSegmentedButton] at [index] of [count] should
     * use. Delegates to [SegmentedButtonDefaults.itemShape]. The underlying
     * returns a [CornerBasedShape] which extends [Shape] and is the type
     * M3's `SegmentedButton` accepts.
     */
    @Composable
    fun itemShape(index: Int, count: Int, baseShape: CornerBasedShape = RoundedCornerShape(0.dp)): Shape =
        SegmentedButtonDefaults.itemShape(index = index, count = count, baseShape = baseShape)

    /**
     * Default leading check icon. Delegates to Material 3's default, which
     * animates with the selected state.
     */
    @Composable
    fun Icon(selected: Boolean) {
        SegmentedButtonDefaults.Icon(selected)
    }
}
