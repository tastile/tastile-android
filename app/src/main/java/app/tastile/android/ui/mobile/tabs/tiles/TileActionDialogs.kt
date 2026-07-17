package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.text.KeyboardOptions
// m2-allow: m3-component
import androidx.compose.material3.AlertDialog
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButton
// m2-allow: m3-component
import androidx.compose.material3.SegmentedButtonDefaults
// m2-allow: m3-component
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Matches the web defer dialog's explicit next-start selection while also
 * allowing a relative duration for touch-first use. No API request is made
 * until the user confirms a valid value.
 */
@Composable
fun DeferTileDialog(
    tileTitle: String?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var relative by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0).toString().take(5)) }
    var minutes by remember { mutableStateOf("30") }
    var inputError by remember { mutableStateOf<String?>(null) }

    fun resolve(): String? = runCatching {
        if (relative) {
            val duration = minutes.toLong().takeIf { it > 0 } ?: error("Duration must be greater than zero")
            Instant.now().plusSeconds(duration * 60).toString()
        } else {
            LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))
                .atZone(ZoneId.systemDefault()).toInstant().toString()
        }
    }.getOrElse { error ->
        inputError = error.message ?: "Enter a valid date and time"
        null
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Defer tile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tileTitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag("defer-mode")) {
                    SegmentedButton(
                        selected = !relative,
                        onClick = { relative = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("defer-mode-datetime"),
                        icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                        label = { Text("Date & time") },
                    )
                    SegmentedButton(
                        selected = relative,
                        onClick = { relative = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("defer-mode-duration"),
                        icon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
                        label = { Text("Duration") },
                    )
                }
                if (relative) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it; inputError = null },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("defer-duration-minutes"),
                    )
                } else {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it; inputError = null },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.testTag("defer-date"),
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it; inputError = null },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.testTag("defer-time"),
                    )
                }
                inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            NiaButton(
                onClick = { resolve()?.let(onConfirm) },
                modifier = Modifier.testTag("defer-confirm"),
                text = { Text("Confirm") },
                leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) },
            )
        },
        dismissButton = {
            NiaTextButton(
                onClick = onCancel,
                text = { Text("Cancel") },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
            )
        },
        modifier = Modifier.testTag("defer-dialog"),
    )
}

/** A prompt request is intentional and observable, never a silent network call. */
@Composable
fun PromptRequestDialog(tileTitle: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Request prompt?") },
        text = { Text("Create a decision prompt for ${tileTitle ?: "this tile"}.") },
        confirmButton = {
            NiaButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("prompt-request-confirm"),
                text = { Text("Request") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null) },
            )
        },
        dismissButton = {
            NiaTextButton(
                onClick = onCancel,
                text = { Text("Cancel") },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
            )
        },
        modifier = Modifier.testTag("prompt-request-dialog"),
    )
}
