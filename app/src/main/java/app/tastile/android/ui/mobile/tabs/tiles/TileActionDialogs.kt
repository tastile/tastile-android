package app.tastile.android.ui.mobile.tabs.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
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
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                tileTitle?.let { Text(it, style = AppTheme.typography.bodyMedium) }
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
                inputError?.let { Text(it, color = AppTheme.colors.error) }
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Confirm",
                onClick = { resolve()?.let(onConfirm) },
                leadingIcon = Icons.Outlined.Check,
                modifier = Modifier.testTag("defer-confirm"),
            )
        },
        dismissButton = {
            AppTertiaryButton(
                text = "Cancel",
                onClick = onCancel,
                leadingIcon = Icons.Outlined.Close,
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
            AppPrimaryButton(
                text = "Request",
                onClick = onConfirm,
                leadingIcon = Icons.Outlined.Send,
                modifier = Modifier.testTag("prompt-request-confirm"),
            )
        },
        dismissButton = {
            AppTertiaryButton(
                text = "Cancel",
                onClick = onCancel,
                leadingIcon = Icons.Outlined.Close,
            )
        },
        modifier = Modifier.testTag("prompt-request-dialog"),
    )
}
