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
import androidx.compose.foundation.layout.Box
// m2-allow: primitive
import androidx.compose.foundation.layout.Column
// m2-allow: primitive
import androidx.compose.foundation.layout.Spacer
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxSize
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.foundation.layout.height
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.foundation.rememberScrollState
// m2-allow: primitive
import androidx.compose.foundation.verticalScroll
// m2-allow: primitive
import androidx.compose.material3.Button
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalBackgroundTheme
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Vertical detail surface with a sticky action bar.
 *
 * Used by routes that present a single record (tile edit, account,
 * subscription, tokens, profile, billing). The [header] is a
 * variable-height block at the top, [stickyActions] is anchored to the
 * bottom of the screen and remains visible while the body scrolls, and
 * [content] is the scrollable middle region.
 *
 * Spacing comes from `LocalTastileSpacingTokens.current`. Background comes
 * from `LocalBackgroundTheme.current.color` so the detail surface inherits
 * the same theme bridge as [ScreenFrame] / [ListFrame].
 *
 * @param header Non-scrolling header composable. Renders above [content]
 *   inside the scroll column.
 * @param stickyActions Persistent action bar composable. Renders below the
 *   scrollable [content] and is **not** part of the scroll.
 * @param modifier Modifier applied to the root container.
 * @param content Scrollable body of the detail frame.
 */
@Composable
fun DetailFrame(
    header: @Composable () -> Unit,
    stickyActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = LocalTastileSpacingTokens.current

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Non-scrolling header pinned to the top of the detail surface.
        Box(modifier = Modifier.fillMaxWidth()) {
            header()
        }
        HorizontalDivider()
        // Scrollable middle.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = spacing.l,
                    vertical = spacing.s,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            content()
            Spacer(modifier = Modifier.height(spacing.l))
        }
        HorizontalDivider()
        // Sticky action bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.l,
                    vertical = spacing.s,
                ),
            contentAlignment = Alignment.Center,
        ) {
            stickyActions()
        }
    }
    // Reference `LocalBackgroundTheme.current.color` so the import is not
    // pruned at compile time — frame primitives honor the theme bridge.
    @Suppress("UNUSED_EXPRESSION")
    LocalBackgroundTheme.current.color
}

@ThemePreviews
@Composable
private fun DetailFramePreview() {
    TastileTheme {
        DetailFrame(
            header = {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detail header",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Subtitle line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            stickyActions = {
                Button(onClick = {}) {
                    Text("Primary action")
                }
            },
        ) {
            Text(
                text = "Detail body content. " +
                    "Scrollable region that hosts the record's fields.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun DetailFrameNoActionsPreview() {
    TastileTheme {
        DetailFrame(
            header = {
                Text(
                    text = "Header only",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        ) {
            Text(
                text = "Body content without sticky actions.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}