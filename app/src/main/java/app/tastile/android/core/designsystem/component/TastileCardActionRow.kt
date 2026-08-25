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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens

/**
 * Action row for a dashboard card. Replaces the legacy `CardPrimaryActions`
 * `when (status)` block. Phase 1 keeps the same button set per branch as
 * the original implementation. Phase 2 may extend the action set.
 *
 * Labels are resolved through the supplied label composables so callers stay
 * in control of string resources.
 */
@Composable
fun TastileCardActionRow(
    actions: TastileCardActions,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit,
    startLabel: @Composable () -> Unit,
    completeLabel: @Composable () -> Unit,
    deferLabel: @Composable () -> Unit,
    deleteLabel: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalTastileSpacingTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (actions) {
            TastileCardActions.Ready -> {
                NiaButton(onClick = onStart, text = startLabel, modifier = Modifier.testTag("card_action_start"))
                NiaOutlinedButton(onClick = onDelete, text = deleteLabel, modifier = Modifier.testTag("card_action_delete"))
            }
            TastileCardActions.Started -> {
                NiaButton(onClick = onComplete, text = completeLabel, modifier = Modifier.testTag("card_action_complete"))
                NiaFilledTonalButton(onClick = onDefer, text = deferLabel, modifier = Modifier.testTag("card_action_defer"))
            }
            TastileCardActions.DoneOrArchived -> {
                NiaOutlinedButton(onClick = onDelete, text = deleteLabel, modifier = Modifier.testTag("card_action_delete"))
            }
        }
    }
}

@ThemePreviews
@Composable
fun TastileCardActionRowPreview() {
    app.tastile.android.core.designsystem.theme.TastileTheme {
        TastileCardActionRow(
            actions = TastileCardActions.Ready,
            onStart = {},
            onComplete = {},
            onDefer = {},
            onDelete = {},
            startLabel = { Text("Start") },
            completeLabel = { Text("Complete") },
            deferLabel = { Text("Defer") },
            deleteLabel = { Text("Delete") },
        )
    }
}
