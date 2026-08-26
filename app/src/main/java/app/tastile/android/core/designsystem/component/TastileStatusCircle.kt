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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.LocalTastileShapeTokens
import app.tastile.android.data.model.TileLifecycle

/**
 * Lifecycle-aware status indicator. Phase 1 renders the same Unicode glyph
 * as `StatusCircle` from `DashboardScreens.kt`. Phase 2 will swap the glyph
 * for an icon and read colors from `LocalTastileStatusTokens`.
 *
 * @param lifecycle The current lifecycle state.
 * @param onClick Optional click handler. When non-null, the indicator
 * becomes clickable.
 * @param modifier Modifier applied to the indicator.
 */
@Composable
fun TastileStatusCircle(
    lifecycle: TileLifecycle,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val glyph = when (lifecycle) {
        TileLifecycle.DONE -> "✓"
        TileLifecycle.STARTED -> "▶"
        TileLifecycle.READY -> "○"
        TileLifecycle.ARCHIVED -> "·"
    }
    val shape = LocalTastileShapeTokens.current.large
    val taggedModifier = modifier
        .testTag("tastile_status_circle")
        .size(shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    Box(
        modifier = taggedModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@ThemePreviews
@Composable
fun TastileStatusCirclePreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            TastileStatusCircle(lifecycle = TileLifecycle.READY)
            TastileStatusCircle(lifecycle = TileLifecycle.STARTED)
            TastileStatusCircle(lifecycle = TileLifecycle.DONE)
            TastileStatusCircle(lifecycle = TileLifecycle.ARCHIVED)
        }
    }
}
