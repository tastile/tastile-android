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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.data.model.TileLifecycle

/**
 * Compact single-row tile representation. Mirrors the legacy `TileCompactCard`
 * from `DashboardScreens.kt`. Phase 1 uses raw `10.dp` horizontal padding that
 * matches the original layout; Phase 2 swaps it for `LocalTastileSpacingTokens`.
 */
@Composable
fun TastileCompactTileRow(
    title: String,
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val spacing = LocalTastileSpacingTokens.current
    val baseModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Row(
        modifier = baseModifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        TastileStatusCircle(lifecycle = lifecycle)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@ThemePreviews
@Composable
fun TastileCompactTileRowPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileCompactTileRow(
            title = "Sample tile",
            lifecycle = TileLifecycle.READY,
        )
    }
}