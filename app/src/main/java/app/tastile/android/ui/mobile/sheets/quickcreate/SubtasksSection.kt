/*
 * SubtasksSection.kt
 *
 * Authoring UI for the tile's `plan.completion.tasks` list (web parity with
 * `tastile-web/src/features/create-tile/ui/sections/SubtasksSection.tsx`).
 *
 * Renders a header row with the progress chip, a per-row checkbox + inline
 * title text field + per-row overflow menu (Move up / Move down / Edit /
 * Duplicate / Delete), and an **inline** sub-task title input at the bottom
 * that adds a task on IME Done. The overflow-menu "Edit" item still opens
 * [TaskDefinitionEditorModal] for the full note / order / show / complete
 * editor.
 *
 * v0.5+:
 *  - Inline title add replaces the previous "Add a sub-task" full-row
 *    button. The button no longer opens a modal just to type a title.
 *  - Every row including the add-input row is wrapped in [FormRow] so the
 *    24dp + 16dp icon-column reservation matches the rest of the body.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistAdd
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: m3-component
import androidx.compose.material3.Checkbox
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Switch
// m2-allow: m3-component
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileStatusTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreateDraftState
import app.tastile.android.ui.mobile.sheets.QuickCreateStateStore
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskContent
import app.tastile.android.ui.mobile.sheets.QuickCreateTaskDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

private const val NEW_TASK_SENTINEL = "__new__"
private const val ORDER_BEFORE = 0
private const val ORDER_AFTER = 1

private data class OrderRule(val id: String, var relation: Int, var targetTaskId: String)

@Composable
fun SubtasksSection(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    modifier: Modifier = Modifier,
    testTag: String = "subtasks-section",
) {
    val tasks = draft.plan.completion.tasks
    val doneCount = tasks.count { it.done }
    val total = tasks.size

    var editorTaskId by remember { mutableStateOf<String?>(null) }

    // Inline add row state. Lives at the bottom of the section so the user
    // can type a title and tap IME Done to add a new sub-task — no modal
    // hop. The overflow-menu "Edit" item still opens the full editor
    // modal for the structured note / order / show / complete fields.
    var inlineTitle by remember { mutableStateOf("") }
    val inlineAria = stringResource(R.string.quickcreate_subtasks_inline_aria)
    val inlinePlaceholder = stringResource(R.string.quickcreate_subtasks_inline_placeholder)
    val addAria = stringResource(R.string.quickcreate_subtasks_add_inline_cd)

    fun submitInlineTitle() {
        val title = inlineTitle.trim()
        if (title.isEmpty()) return
        store.addTask(title = title)
        inlineTitle = ""
    }

    FormRow(
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = addAria }
                    .testTag("$testTag-icon"),
            )
        },
        content = {
            Text(
                text = stringResource(R.string.quickcreate_subtasks_header),
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
        },
        trailing = {
            if (total > 0) {
                Text(
                    text = stringResource(R.string.quickcreate_subtasks_progress, doneCount, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                    modifier = Modifier.testTag("$testTag-progress"),
                )
            }
        },
    )
    if (tasks.isEmpty()) {
        FormRow(
            modifier = Modifier.testTag("$testTag-empty"),
            icon = null,
            content = {
                Text(
                    text = stringResource(R.string.quickcreate_subtasks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current,
                )
            },
        )
    } else {
        tasks.forEachIndexed { index, task ->
            SubtaskRow(
                task = task,
                testTagPrefix = "$testTag-task",
                isFirst = index == 0,
                isLast = index == tasks.lastIndex,
                onDoneChange = { store.updateTask(task.copy(done = it)) },
                onTitleChange = { title ->
                    store.updateTask(task.copy(content = QuickCreateTaskContent(title = title, note = task.content.note)))
                },
                onEdit = { editorTaskId = task.id },
                onMoveUp = { store.reorderTask(index, index - 1) },
                onMoveDown = { store.reorderTask(index, index + 1) },
                onDuplicate = { store.duplicateTask(task.id) },
                onDelete = { store.removeTask(task.id) },
            )
        }
    }

    // Inline add row — owns its own icon slot (PlaylistAdd) so the icon
    // column reservation matches every other row in the section.
    FormRow(
        modifier = Modifier.testTag("$testTag-inline-add"),
        icon = {
            Icon(
                imageVector = Icons.Outlined.PlaylistAdd,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        },
        content = {
            OutlinedTextField(
                value = inlineTitle,
                onValueChange = { inlineTitle = it },
                placeholder = { Text(inlinePlaceholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitInlineTitle() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("$testTag-inline-input")
                    .semantics { contentDescription = inlineAria },
            )
        },
    )

    editorTaskId?.let { id ->
        TaskDefinitionEditorModal(
            draft = draft,
            store = store,
            taskId = id,
            onClose = { editorTaskId = null },
            testIdSuffix = "subtask-edit",
        )
    }
}

@Composable
private fun SubtaskRow(
    task: QuickCreateTaskDefinition,
    testTagPrefix: String,
    isFirst: Boolean,
    isLast: Boolean,
    onDoneChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val markDoneCd = stringResource(R.string.quickcreate_subtasks_mark_done_aria)
    val markUndoneCd = stringResource(R.string.quickcreate_subtasks_mark_undone_aria)
    val menuCd = stringResource(R.string.quickcreate_subtasks_menu_aria)
    val removeCd = stringResource(R.string.quickcreate_subtasks_remove_aria)
    FormRow(
        modifier = Modifier.testTag(testTagPrefix),
        icon = {
            Checkbox(
                checked = task.done,
                onCheckedChange = onDoneChange,
                modifier = Modifier
                    .size(20.dp)
                    .testTag("$testTagPrefix-done")
                    .semantics {
                        contentDescription = if (task.done) markUndoneCd else markDoneCd
                    },
            )
        },
        content = {
            OutlinedTextField(
                value = task.content.title,
                onValueChange = onTitleChange,
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.quickcreate_subtasks_untitled),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = LocalContentColor.current,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("$testTagPrefix-title"),
            )
        },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .testTag("$testTagPrefix-menu")
                        .semantics { contentDescription = menuCd },
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_subtasks_move_up)) },
                        enabled = !isFirst,
                        onClick = { menuExpanded = false; onMoveUp() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_subtasks_move_down)) },
                        enabled = !isLast,
                        onClick = { menuExpanded = false; onMoveDown() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_subtasks_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_subtasks_duplicate)) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.quickcreate_subtasks_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() },
                        modifier = Modifier.semantics { contentDescription = removeCd },
                    )
                }
            }
        },
    )
}

@Composable
private fun TaskDefinitionEditorModal(
    draft: QuickCreateDraftState,
    store: QuickCreateStateStore,
    taskId: String?,
    onClose: () -> Unit,
    testIdSuffix: String,
) {
    val source = remember(taskId, draft.plan.completion.tasks) { taskId?.let { id -> draft.plan.completion.tasks.find { it.id == id } } }
    val references = referenceOptionsFor(draft.plan.references)
    val siblingTasks = draft.plan.completion.tasks
    val isNew = source == null
    val effectiveId = source?.id ?: NEW_TASK_SENTINEL

    var title by remember(taskId) { mutableStateOf(source?.content?.title.orEmpty()) }
    var note by remember(taskId) { mutableStateOf(source?.content?.note.orEmpty()) }
    var done by remember(taskId) { mutableStateOf(source?.done ?: false) }
    val initialShow = source?.show
    val showNode = remember(taskId) {
        mutableStateOf(
            initialShow?.let { conditionFromJson(it) } ?: QuickCreateConditionNode(3, term = defaultTermValue("calendar")),
        )
    }
    val completeNode = remember(taskId) {
        mutableStateOf(
            source?.complete ?: QuickCreateConditionNode(3, term = selfTaskTerm(NEW_TASK_SENTINEL)),
        )
    }
    val orderRules = remember(taskId) {
        mutableStateListOf<OrderRule>().apply {
            source?.order?.forEach { element ->
                val obj = element as? JsonObject ?: return@forEach
                OrderRule(
                    id = obj.str("id") ?: UUID.randomUUID().toString(),
                    relation = obj["relation"]?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() } ?: ORDER_BEFORE,
                    targetTaskId = obj.str("targetTaskId").orEmpty(),
                ).also(::add)
            }
        }
    }

    val titleError = title.isBlank()
    val canSubmit = title.isNotBlank()

    fun handleSave() {
        if (!canSubmit) return
        val newId = if (isNew) store.addTask(title = title) else source!!.id
        val finalComplete = if (isNew) {
            rewriteTaskId(completeNode.value, NEW_TASK_SENTINEL, newId) ?: completeNode.value
        } else completeNode.value
        val finalShow = if (isNew) {
            rewriteTaskId(showNode.value, NEW_TASK_SENTINEL, newId) ?: showNode.value
        } else showNode.value
        val orderJson = JsonArray(
            orderRules
                .filter { it.targetTaskId.isNotBlank() }
                .map { rule ->
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(rule.id),
                            "relation" to JsonPrimitive(rule.relation),
                            "targetTaskId" to JsonPrimitive(rule.targetTaskId),
                        ),
                    )
                },
        )
        val task = QuickCreateTaskDefinition(
            id = newId,
            content = QuickCreateTaskContent(title = title, note = note.ifBlank { null }),
            show = conditionToJson(finalShow),
            complete = finalComplete,
            order = orderJson,
            done = done,
        )
        if (!isNew) store.updateTask(task)
        onClose()
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            QuickCreateHeader(
                title = title,
                onTitleChange = { title = it },
                onClose = onClose,
                placeholder = stringResource(R.string.quickcreate_tile_title_placeholder),
                isTitleError = titleError,
                titleTestTag = "task-editor-$testIdSuffix-title",
                closeTestTag = "task-editor-$testIdSuffix-cancel",
                padded = false,
                submitSlot = {
                    NiaButton(
                        onClick = { handleSave() },
                        enabled = canSubmit,
                        text = {
                            Text(
                                text = stringResource(
                                    if (isNew) R.string.quickcreate_task_editor_add
                                    else R.string.quickcreate_task_editor_save,
                                ),
                            )
                        },
                        modifier = Modifier.testTag("task-editor-$testIdSuffix-submit"),
                    )
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("task-editor-$testIdSuffix"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (titleError) {
                    Text(
                        text = stringResource(R.string.quick_create_error_title_required),
                        color = LocalTastileStatusTokens.current.archived.icon,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("task-editor-$testIdSuffix-title-error"),
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.quickcreate_task_editor_note)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task-editor-$testIdSuffix-note"),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.quickcreate_task_editor_done),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = done,
                        onCheckedChange = { done = it },
                        modifier = Modifier.testTag("task-editor-$testIdSuffix-done"),
                    )
                }
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.quickcreate_task_editor_order_header),
                    style = MaterialTheme.typography.titleSmall,
                )
                orderRules.forEachIndexed { index, rule ->
                    OrderRuleRow(
                        rule = rule,
                        targets = siblingTasks.filter { it.id != effectiveId }
                            .map { it.id to it.content.title.ifBlank { stringResource(R.string.quickcreate_subtasks_untitled) } },
                        onRelationChange = { orderRules[index] = rule.copy(relation = it) },
                        onTargetChange = { orderRules[index] = rule.copy(targetTaskId = it) },
                        onRemove = { orderRules.removeAt(index) },
                        testTagPrefix = "task-editor-$testIdSuffix-order-$index",
                    )
                }
                FormFieldLayout {
                    NiaButton(
                        onClick = {
                            val target = siblingTasks.firstOrNull { it.id != effectiveId }?.id.orEmpty()
                            orderRules.add(OrderRule(id = UUID.randomUUID().toString(), relation = ORDER_BEFORE, targetTaskId = target))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        text = { Text(stringResource(R.string.quickcreate_task_editor_order_add)) },
                    )
                }
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.quickcreate_task_editor_show_header),
                    style = MaterialTheme.typography.titleSmall,
                )
                ConditionControls(
                    node = showNode.value,
                    onChange = { showNode.value = it },
                    path = "$testIdSuffix-show",
                    allowTermKind = true,
                    tasks = siblingTasks,
                    requirements = draft.plan.completion.timeRequirements,
                    references = references,
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.quickcreate_task_editor_complete_header),
                    style = MaterialTheme.typography.titleSmall,
                )
                ConditionControls(
                    node = completeNode.value,
                    onChange = { completeNode.value = it },
                    path = "$testIdSuffix-complete",
                    allowTermKind = true,
                    tasks = siblingTasks,
                    requirements = draft.plan.completion.timeRequirements,
                    references = references,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
private fun OrderRuleRow(
    rule: OrderRule,
    targets: List<Pair<String, String>>,
    onRelationChange: (Int) -> Unit,
    onTargetChange: (String) -> Unit,
    onRemove: () -> Unit,
    testTagPrefix: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = rule.relation == ORDER_BEFORE,
            onClick = { onRelationChange(ORDER_BEFORE) },
            label = { Text(stringResource(R.string.quickcreate_task_editor_order_before)) },
            modifier = Modifier.testTag("$testTagPrefix-relation-before"),
        )
        FilterChip(
            selected = rule.relation == ORDER_AFTER,
            onClick = { onRelationChange(ORDER_AFTER) },
            label = { Text(stringResource(R.string.quickcreate_task_editor_order_after)) },
            modifier = Modifier.testTag("$testTagPrefix-relation-after"),
        )
        LocalOptionPickerField(
            label = stringResource(R.string.quickcreate_task_editor_order_target),
            value = targets.firstOrNull { it.first == rule.targetTaskId }?.second.orEmpty(),
            options = targets,
            onSelect = onTargetChange,
            modifier = Modifier
                .weight(1f)
                .testTag("$testTagPrefix-target"),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.testTag("$testTagPrefix-remove"),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
        }
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.content?.takeUnless { it == "null" }
