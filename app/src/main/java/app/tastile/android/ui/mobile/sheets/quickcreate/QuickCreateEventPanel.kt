package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Switch
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreatePanel
import app.tastile.android.ui.mobile.sheets.QuickCreateProject
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTimeOfDayMode

/**
 * Event workflow - directly mirrors
 * `tastile-web/src/features/create-tile/ui/QuickCreateEvent.tsx`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCreateEventPanel(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    isSubmitting: Boolean,
    submitError: String?,
    projects: List<QuickCreateProject>,
    knownTags: List<String>,
    batchScrollState: ScrollState? = null,
) {
    FormFieldColumn(
        modifier = Modifier
            .testTag("quick-create-event")
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        // Each child owns its own icon column reservation. Do not wrap in
        // FormFieldLayout { } — that would double-book the 20dp + 12dp
        // icon slot and push the title 32dp right of the close icon.
        QuickCreateHeader(
            title = draft.identity.title,
            onTitleChange = { store.updateIdentity(draft.identity.copy(title = it)) },
            modifier = Modifier.testTag("quick-create-event-header"),
        )
        WorkflowBatch(
            workflow = draft.workflow,
            onWorkflowChange = { kind -> store.setWorkflow(kind) },
            modifier = Modifier.testTag("quick-create-event-batch"),
            scrollState = batchScrollState,
        )
        FormFieldLayout {
            DateTimeRow(
                dateIso = draft.time.span.start,
                timeIso = draft.time.timeOfDayStart.takeIf { it.isNotBlank() },
                onDateChange = { newStart ->
                    val newEnd = if (draft.time.span.end.isBlank()) newStart else draft.time.span.end
                    store.updateTime(
                        draft.time.copy(
                            span = draft.time.span.copy(start = newStart, end = newEnd),
                            timeOfDayStart = draft.time.timeOfDayStart,
                        )
                    )
                },
                onTimeChange = { newTime ->
                    store.updateTime(draft.time.copy(timeOfDayStart = newTime))
                },
                datePlaceholder = stringResource(R.string.quick_create_picker_start_date_placeholder),
                timePlaceholder = stringResource(R.string.quick_create_picker_start_time_placeholder),
                dateTestTag = "event-start-date",
                timeTestTag = "event-start-time",
                showTime = draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range,
            )
        }
        FormFieldLayout {
            DateTimeRow(
                dateIso = draft.time.span.end,
                timeIso = draft.time.timeOfDayEnd.takeIf { it.isNotBlank() },
                onDateChange = { newEnd ->
                    store.updateTime(draft.time.copy(span = draft.time.span.copy(end = newEnd)))
                },
                onTimeChange = { newTime ->
                    store.updateTime(draft.time.copy(timeOfDayEnd = newTime))
                },
                datePlaceholder = stringResource(R.string.quick_create_picker_end_date_placeholder),
                timePlaceholder = stringResource(R.string.quick_create_picker_end_time_placeholder),
                dateTestTag = "event-end-date",
                timeTestTag = "event-end-time",
                showTime = draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.Range,
            )
        }
        FormRow(
            icon = null,
            content = {
                Text(
                    text = stringResource(R.string.quick_create_all_day),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            trailing = {
                Switch(
                    checked = draft.time.timeOfDayMode == QuickCreateTimeOfDayMode.AllDay,
                    onCheckedChange = { checked ->
                        store.setAllDay(checked)
                    },
                    modifier = Modifier.testTag("quick-create-event-allday"),
                )
            },
        )
        DetailsAffordanceButton(
            label = stringResource(R.string.tile_edit_open_time),
            onOpen = { store.openSubpanel(QuickCreatePanel.Time) },
            modifier = Modifier.testTag("quick-create-event-details-time"),
        )
        ProjectColorRow(
            projects = projects,
            selectedProjectId = draft.meta.ownerSubjectId,
            selectedColor = parseHexColor(draft.identity.visual.color),
            onProjectChange = { id ->
                store.updateMeta(draft.meta.copy(ownerSubjectId = id))
            },
            onColorChange = { color ->
                store.updateIdentity(
                    draft.identity.copy(
                        visual = draft.identity.visual.copy(color = color.toHexString())
                    )
                )
            },
            modifier = Modifier.testTag("quick-create-event-project-color"),
        )
        MemoSection(
            memo = draft.meta.memo,
            onMemoChange = { store.updateMeta(draft.meta.copy(memo = it)) },
        )
    }
}


