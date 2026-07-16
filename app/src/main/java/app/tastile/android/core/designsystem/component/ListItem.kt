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

import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Tastile list item. Wraps Material 3 [androidx.compose.material3.ListItem].
 *
 * @param headlineContent The headline content of the list item, typically a [Text].
 * @param modifier Modifier applied to the list item.
 * @param overlineContent Optional content shown above the headline.
 * @param supportingContent Optional secondary content shown below the headline.
 * @param leadingContent Optional content displayed at the leading edge.
 * @param trailingContent Optional content displayed at the trailing edge.
 * @param colors [ListItemColors] used to render the list item.
 */
@Stable
@Composable
fun NiaListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
) {
    MaterialListItem(
        headlineContent = headlineContent,
        modifier = modifier,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
    )
}

/**
 * Tastile list item default values.
 */
object NiaListItemDefaults {
    /**
     * Default [ListItemColors]; mirrors [ListItemDefaults.colors] but exposes
     * it through the Tastile design system so callers do not need to import
     * Material 3 directly. The parameter names follow the M3 1.3 signature.
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        headlineColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        overlineColor: Color = Color.Unspecified,
        supportingColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledHeadlineColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
    ): ListItemColors = ListItemDefaults.colors(
        containerColor = containerColor,
        headlineColor = headlineColor,
        leadingIconColor = leadingIconColor,
        overlineColor = overlineColor,
        supportingColor = supportingColor,
        trailingIconColor = trailingIconColor,
        disabledHeadlineColor = disabledHeadlineColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
    )
}
