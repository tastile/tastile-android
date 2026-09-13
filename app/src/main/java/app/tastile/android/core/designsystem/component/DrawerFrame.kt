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
import androidx.compose.foundation.layout.Row
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: m3-component
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.ModalDrawerSheet
// m2-allow: m3-component
import androidx.compose.material3.NavigationDrawerItem
// m2-allow: m3-component
import androidx.compose.material3.NavigationDrawerItemDefaults
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Description of a single row inside a [DrawerFrame].
 *
 * @param key Stable identifier; surfaced as the route key passed to
 *   [DrawerFrame.onSelect].
 * @param label Display label rendered as the row's headline content.
 * @param icon Leading icon composable rendered in the row's `leadingContent`
 *   slot. May be `null` when the row is intentionally text-only.
 * @param selected Whether the row is currently selected. When `true`,
 *   [NavigationDrawerItem] highlights the row.
 */
data class DrawerItem(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
)

/**
 * Primary side-sheet container based on Material 3
 * [ModalDrawerSheet]. Renders the brand header, a list of [items], and an
 * optional footer (commonly a Settings shortcut).
 *
 * Use [DrawerFrame] for the primary navigation drawer (the "side panel"
 * that holds the timeline / tasks / projects / settings entries). It is
 * the canonical replacement for the legacy `SidePanelDrawerContent` —
 * call sites should migrate to [DrawerFrame] in Phase 3 / Phase 6.
 *
 * The composable does **not** own the [DrawerState] / scaffold
 * navigation; callers wrap it in a ModalNavigationDrawer with their own
 * `drawerState` and pass it the [items] they want to render.
 *
 * @param items Ordered list of drawer rows. The first row is rendered
 *   directly under the brand header; the rest follow in order.
 * @param onSelect Callback invoked when the user taps a row. Receives the
 *   row's [DrawerItem.key] as a single string so the caller can route
 *   without depending on the item's display label.
 * @param brandHeader Optional brand header composable. Defaults to a
 *   simple "Tastile" text — callers typically override with a brand mark
 *   image.
 * @param footer Optional footer composable rendered below the last item,
 *   separated by a [HorizontalDivider].
 * @param modifier Modifier applied to the underlying [ModalDrawerSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerFrame(
    items: List<DrawerItem>,
    onSelect: (key: String) -> Unit,
    modifier: Modifier = Modifier,
    brandHeader: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalTastileSpacingTokens.current

    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Brand header — defaults to a plain "Tastile" title; callers
            // typically provide a logo via [brandHeader].
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.l,
                        end = spacing.l,
                        top = spacing.l,
                        bottom = spacing.s,
                    ),
            ) {
                if (brandHeader != null) {
                    brandHeader()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    ) {
                        Text(
                            text = "Tastile",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
            // Drawer items.
            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label) },
                    selected = item.selected,
                    onClick = { onSelect(item.key) },
                    icon = item.icon?.let { icon ->
                        {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(),
                    modifier = Modifier.padding(horizontal = spacing.xs),
                )
            }
            // Footer slot (e.g. a Settings shortcut).
            if (footer != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = spacing.l,
                        vertical = spacing.s,
                    ),
                )
                footer()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DrawerFramePreview() {
    TastileTheme {
        DrawerFrame(
            items = listOf(
                DrawerItem(
                    key = "timeline",
                    label = "Timeline",
                    selected = true,
                ),
                DrawerItem(key = "tasks", label = "Tasks"),
                DrawerItem(key = "projects", label = "Projects"),
            ),
            onSelect = {},
            footer = {
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {},
                )
            },
        )
    }
}

@ThemePreviews
@Composable
private fun DrawerFrameEmptyPreview() {
    TastileTheme {
        DrawerFrame(
            items = emptyList(),
            onSelect = {},
        )
    }
}

// Reference the 0.dp literal indirectly to keep design-system tokens aligned
// to M3 defaults. The file is in the design-system root so hardcoded 0.dp is
// the only acceptable literal.
@Suppress("unused")
private val zeroDp = 0.dp