/*
 * SubpanelComponents.kt
 *
 * Shared design-system helpers used across multiple QuickCreate subpanels.
 * Anything reused by more than one subpanel lives here; single-subpanel
 * helpers stay co-located with their owner file.
 */

package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
// m2-allow: m3-component
import androidx.compose.material3.DatePicker
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.HorizontalDivider
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenu
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: m3-component
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.Surface
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens
import app.tastile.android.core.designsystem.theme.LocalTastileStatusTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.ui.mobile.components.picker.ReferenceOption
import app.tastile.android.ui.mobile.sheets.QuickCreateConditionNode
import app.tastile.android.ui.mobile.sheets.QuickCreatePlanReference
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneOffset

// ── Shared collection / JSON helpers used by multiple subpanels ──

internal fun <T> List<T>.replace(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }

internal fun JsonElement.jsonObjectOrEmpty(): JsonObject =
    this as? JsonObject ?: JsonObject(emptyMap())

internal fun JsonObject.string(key: String, fallback: String = ""): String =
    this[key]?.jsonPrimitive?.content?.takeUnless { it == "null" } ?: fallback

@Composable
internal fun LocalSectionHeader(title: String, subtitle: String? = null) {
    // The icon column is reserved structurally by `FormFieldLayout` (24dp
    // icon + 16dp gap) so the title text begins at the standard 48dp
    // content offset. The outer 16dp horizontal padding is applied by
    // `FormFieldRow`, which owns the column reservation for the sheet
    // body.
    FormFieldLayout(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 0.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
internal fun LocalNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    step: Long = 1,
    isEnabled: Boolean = true,
) {
    val current = value.trim().toLongOrNull()
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '-' }) },
        modifier = modifier,
        enabled = isEnabled,
        label = { Text(label) },
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        trailingIcon = {
            Column {
                IconButton(
                    onClick = {
                        val next = (current ?: 0L).plus(step).coerceAtMost(max)
                        onValueChange(next.toString())
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = {
                        val next = (current ?: 0L).minus(step).coerceAtLeast(min)
                        onValueChange(next.toString())
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
    )
}

/** M3 `FilterChip`-style color swatch with a selection ring — no text. */
@Composable
internal fun SwatchChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = LocalTastileCardRoleTokens.current.neutral.container,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) LocalTastileCardRoleTokens.current.actionable.border else LocalTastileCardRoleTokens.current.completed.border,
        ),
        modifier = modifier.size(36.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** M3 `FilterChip`-style icon chip with a selection ring — no text. */
@Composable
internal fun IconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) LocalTastileStatusTokens.current.done.container else LocalTastileCardRoleTokens.current.actionable.container,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) LocalTastileCardRoleTokens.current.actionable.border else LocalTastileCardRoleTokens.current.completed.border,
        ),
        modifier = modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LocalTastileStatusTokens.current.done.onContainer else LocalContentColor.current,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Bordered field that opens a [DropdownMenu] of id → label options. */
@Composable
internal fun LocalOptionPickerField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    LocalPickerField(
        label = label,
        value = value.ifBlank { "—" },
        onClick = { expanded = true },
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        options.forEach { (id, display) ->
            DropdownMenuItem(
                text = { Text(display) },
                onClick = {
                    expanded = false
                    onSelect(id)
                },
            )
        }
    }
}

/** Map a plan's references to picker options, mirroring `TimeSubpanel`. */
internal fun referenceOptionsFor(references: List<QuickCreatePlanReference>): List<ReferenceOption> =
    references.map { ref ->
        val targetObj = ref.target.jsonObjectOrEmpty()
        val refId = targetObj["referenceId"]?.jsonPrimitive?.content?.takeUnless { it == "null" } ?: ref.id
        ReferenceOption(id = refId, label = ref.id.ifBlank { refId })
    }

/**
 * Web-parity authoring header. Mirrors `tastile-web/.../QuickCreateHeader.tsx`:
 * a `FormRow` whose icon slot hosts a close affordance, whose content slot
 * hosts an unstyled title `TextInput`, and whose trailing slot hosts a
 * caller-supplied submit affordance.
 *
 * The base panels already use a larger fixed title in [QuickCreateHeader]
 * (the panel title row); this composable is intended for **modal** surfaces
 * (e.g. the sub-task editor) where the title is itself an editable input.
 */
@Composable
internal fun QuickCreateHeader(
    title: String,
    onTitleChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    titleTestTag: String? = null,
    closeTestTag: String? = null,
    isTitleRequired: Boolean = true,
    isTitleError: Boolean = false,
    submitSlot: (@Composable () -> Unit)? = null,
    padded: Boolean = true,
) {
    val formRow = FormRow(
        modifier = modifier,
        icon = {
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = LocalTastileCardRoleTokens.current.neutral.container,
                modifier = Modifier
                    .size(24.dp)
                    .then(if (closeTestTag != null) Modifier.testTag(closeTestTag) else Modifier),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cancel",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        content = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleLarge,
                        color = LocalContentColor.current,
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge,
                isError = isTitleError,
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    errorBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier),
            )
        },
        trailing = submitSlot,
    )
    if (padded) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
        ) { formRow }
    } else {
        formRow
    }
    // Reference isTitleRequired to keep the param callable from non-modal hosts
    // without compiler warnings.
    @Suppress("UNUSED_EXPRESSION") isTitleRequired
}

// ── Shared JSON helpers used by the sub-task editor ──

/** Self-pointing "task done" term — `taskId = id`, `state = 2` (complete). */
internal fun selfTaskTerm(id: String) = JsonObject(
    mapOf(
        "kind" to JsonPrimitive("task"),
        "value" to JsonObject(mapOf("taskId" to JsonPrimitive(id), "state" to JsonPrimitive(2))),
    ),
)

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.content?.takeUnless { it == "null" }

internal fun JsonObject.withStr(key: String, value: String): JsonObject =
    JsonObject(toMutableMap().also { it[key] = JsonPrimitive(value) })

internal fun JsonObject.withObj(key: String, value: JsonObject): JsonObject =
    JsonObject(toMutableMap().also { it[key] = value })

/** Rewrite `taskId == from` → `to` inside a condition tree, or null when absent. */
internal fun rewriteTaskId(node: QuickCreateConditionNode?, from: String, to: String): QuickCreateConditionNode? {
    if (node == null) return null
    val term = node.term?.let { t ->
        val obj = t as? JsonObject ?: return@let t
        val value = obj["value"] as? JsonObject
        if (obj.str("kind") == "task" && value?.str("taskId") == from) {
            obj.withObj("value", value.withStr("taskId", to))
        } else t
    } ?: node.term
    return node.copy(
        term = term,
        children = node.children.mapNotNull { rewriteTaskId(it, from, to) },
    )
}

/** Rewrite `taskId == from` → `to` inside a raw condition JSON element. */
internal fun rewriteTaskId(element: JsonElement?, from: String, to: String): JsonElement? {
    if (element == null) return null
    val obj = element as? JsonObject ?: return element
    val value = obj["value"] as? JsonObject
    val newValue = if (obj.str("kind") == "task" && value?.str("taskId") == from) {
        value.withStr("taskId", to)
    } else value
    val newChildren = (obj["children"] as? JsonArray)?.let { array ->
        JsonArray(array.mapNotNull { rewriteTaskId(it, from, to) })
    }
    return JsonObject(
        obj.toMutableMap().apply {
            newValue?.let { put("value", it) }
            newChildren?.let { put("children", it) }
        },
    )
}

@Composable
internal fun LocalPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The leading icon is provided by the host `FormFieldLayout(icon = ...)`
    // (the same 24dp icon column + 16dp gap reservation the main panels use),
    // so this composable renders only the label/value column and trailing
    // chevron inside a clickable `Surface` and the whole row stays tappable.
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalTastileCardRoleTokens.current.completed.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun LocalWeekdayPicker(
    selectedMask: Int,
    onToggle: (Int) -> Unit,
    enabled: Boolean,
    testTag: (Int) -> String,
) {
    val days = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    ScrollableChipRow(spacing = 8.dp) {
        days.forEachIndexed { index, name ->
            val bit = 1 shl index
            FilterChip(
                selected = (selectedMask and bit) != 0,
                onClick = { if (enabled) onToggle(index) },
                label = { Text(name) },
                enabled = enabled,
                modifier = Modifier.testTag(testTag(index)),
            )
        }
    }
}

// ── Shared date picker dialog wrapper ──

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun NativeDateField(label: String, value: String, tag: String, onSelected: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FormFieldLayout(icon = Icons.Outlined.CalendarMonth, modifier = Modifier.fillMaxWidth()) {
        LocalPickerField(
            label = label,
            value = value.ifBlank { "—" },
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
    if (open) {
        val state = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                NiaButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis -> onSelected(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toString()) }
                        open = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.dialog_ok)) },
                )
            },
            dismissButton = {
                NiaTextButton(
                    onClick = { open = false },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    text = { Text(stringResource(R.string.dialog_cancel)) },
                )
            },
        ) { DatePicker(state = state) }
    }
}
