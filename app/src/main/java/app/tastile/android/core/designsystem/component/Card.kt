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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.NiaTheme

/**
 * Tastile filled card. Wraps Material 3 [Card].
 *
 * @param modifier Modifier to be applied to the card.
 * @param enabled Controls the enabled state of the card. When `false`, this card will not be
 * clickable and will appear disabled to accessibility services.
 * @param onClick Called when the user clicks the card.
 * @param shape Defines the shape of the card's container, border, and shadow.
 * @param colors [CardColors] used to resolve the colors for this card.
 * @param elevation [CardElevation] used to resolve the elevation for this card.
 * @param border Optional border to draw on the container.
 * @param content The content the card wraps.
 */
@Stable
@Composable
fun NiaCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(
        defaultElevation = NiaCardDefaults.CardElevation,
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

/**
 * Tastile outlined card. Wraps Material 3 [OutlinedCard].
 *
 * @param modifier Modifier to be applied to the card.
 * @param enabled Controls the enabled state of the card.
 * @param onClick Called when the user clicks the card.
 * @param shape Defines the shape of the card.
 * @param colors [CardColors] used to resolve the colors.
 * @param elevation [CardElevation] used to resolve the elevation.
 * @param border Optional border to draw on the container.
 * @param content The content the card wraps.
 */
@Stable
@Composable
fun NiaOutlinedCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    colors: androidx.compose.material3.CardColors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    elevation: androidx.compose.material3.CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(
        width = NiaCardDefaults.OutlinedCardBorderWidth,
        color = if (enabled) {
            MaterialTheme.colorScheme.outline
        } else {
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = NiaCardDefaults.DISABLED_OUTLINED_CARD_BORDER_ALPHA,
            )
        },
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content,
        )
    }
}

/**
 * Tastile card default values.
 */
object NiaCardDefaults {
    val CardElevation: Dp = 1.dp
    val OutlinedCardBorderWidth: Dp = 1.dp
    const val DISABLED_OUTLINED_CARD_BORDER_ALPHA = 0.12f
}

@ThemePreviews
@Composable
fun NiaCardPreview() {
    NiaTheme {
        NiaCard {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(16.dp),
            ) {
                Text("Card content")
            }
        }
    }
}

@ThemePreviews
@Composable
fun NiaOutlinedCardPreview() {
    NiaTheme {
        NiaOutlinedCard {
            androidx.compose.foundation.layout.Box(
                Modifier.padding(16.dp),
            ) {
                Text("Outlined card content")
            }
        }
    }
}
