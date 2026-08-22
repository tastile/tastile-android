/*
 * DurationSubpanel.kt
 *
 * Authoring UI for the tile's minimum/maximum duration range and helpers
 * used by the completion subpanel to mint and update web-style
 * [QuickCreateTimeRequirement] values.
 *
 * Rows use the 24dp icon column + 16dp gap reservation provided
 * structurally by `FormFieldLayout`. The 16dp outer horizontal padding
 * is applied by `FormFieldRow`.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateDurationRange
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeRequirement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@Composable
internal fun DurationPanel(draft: QuickCreateDraftState, store: QuickCreateStateStore) {
    val duration = draft.time.durationMinMax
    FormFieldLayout {
        FilterChip(
            selected = duration.minMs == null && duration.maxMs == null,
            onClick = { store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(null, null))) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-duration-none"),
            label = { Text(stringResource(R.string.quickcreate_panel_duration_none)) },
            leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
        )
    }
    FormFieldLayout(icon = Icons.Outlined.Timer) {
        LocalNumberField(
            value = duration.minMs?.div(60_000L)?.toString() ?: "90",
            onValueChange = { value -> value.toLongOrNull()?.let { minutes ->
                val milliseconds = minutes.coerceIn(10L, 720L) * 60_000L
                store.updateTime(draft.time.copy(durationMinMax = QuickCreateDurationRange(milliseconds, milliseconds)))
            } },
            label = stringResource(R.string.quickcreate_panel_duration_label),
            suffix = stringResource(R.string.quickcreate_panel_duration_suffix),
            min = 10,
            max = 720,
            step = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-duration-minutes"),
        )
    }
    FormFieldLayout {
        NiaFilledTonalButton(
            onClick = { /* wired by caller if needed */ },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick-create-duration-completion-link"),
            leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
            text = { Text(stringResource(R.string.quickcreate_panel_use_for_completion)) },
        )
    }
}

/**
 * Mint a default web time-requirement for the completion tree, seeded from
 * the duration panel's current minimum (or 60 minutes if absent).
 */
internal fun webTimeRequirement(durationMinimumMs: Long?): QuickCreateTimeRequirement = QuickCreateTimeRequirement(
    id = UUID.randomUUID().toString(),
    observation = JsonObject(mapOf("scope" to JsonPrimitive(0))),
    required = JsonObject(mapOf("minMs" to JsonPrimitive(durationMinimumMs ?: 60 * 60_000L))),
)

internal fun updateTimeRequirement(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    index: Int,
    requirement: QuickCreateTimeRequirement,
) {
    store.updatePlan(
        draft.plan.copy(
            completion = draft.plan.completion.copy(
                timeRequirements = draft.plan.completion.timeRequirements.replace(index, requirement),
            ),
        ),
    )
}
