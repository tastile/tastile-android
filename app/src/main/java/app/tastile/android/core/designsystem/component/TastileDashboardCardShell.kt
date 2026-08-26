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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens

/**
 * Outlined card shell for dashboard cards. Wraps [NiaOutlinedCard] with
 * consistent outer padding and renders a header row above arbitrary content.
 */
@Composable
fun TastileDashboardCardShell(
    modifier: Modifier = Modifier,
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalTastileSpacingTokens.current
    NiaOutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s)
            .testTag("dashboard_card_shell"),
    ) {
        Column {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs),
            ) {
                header()
            }
            content()
        }
    }
}

@ThemePreviews
@Composable
fun TastileDashboardCardShellPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileDashboardCardShell(
            header = {
                androidx.compose.material3.Text(
                    text = "Header",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            content = {
                androidx.compose.material3.Text(
                    text = "Body content",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}
