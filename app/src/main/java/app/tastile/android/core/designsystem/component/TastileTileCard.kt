/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.data.model.TileLifecycle

/**
 * Expandable tile card. Replaces the legacy `TileExpandableCard` from
 * `DashboardScreens.kt`. State hoisting: callers own the [expanded] flag and
 * receive toggles via [onToggleExpanded]. Phase 3 will animate the
 * expand/collapse transition.
 */
@Composable
fun TastileTileCard(
    title: String,
    lifecycle: TileLifecycle,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    expandedContent: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandToggleContentDescription: String? = null,
) {
    val spacing = LocalTastileSpacingTokens.current
    Column(modifier = modifier.fillMaxWidth().testTag("tastile_tile_card")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(horizontal = spacing.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TastileStatusCircle(lifecycle = lifecycle)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = expandToggleContentDescription,
                modifier = Modifier.testTag("tile_card_chevron"),
            )
        }
        if (expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier.fillMaxWidth().padding(spacing.m),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                expandedContent()
                Row(modifier = Modifier.fillMaxWidth()) { actions() }
            }
        }
    }
}

@ThemePreviews
@Composable
fun TastileTileCardPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileTileCard(
            title = "Sample tile",
            subtitle = "Ready",
            lifecycle = TileLifecycle.READY,
            expanded = true,
            onToggleExpanded = {},
            expandedContent = {
                Text("Detail body", style = MaterialTheme.typography.bodySmall)
            },
            actions = {
                NiaButton(onClick = {}, text = { Text("Start") })
            },
        )
    }
}
