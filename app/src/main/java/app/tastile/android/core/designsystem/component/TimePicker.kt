/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker as MaterialTimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState as materialRememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/**
 * Tastile time picker. Wraps Material 3 [androidx.compose.material3.TimePicker].
 *
 * @param state The state controlling the time picker.
 * @param modifier Modifier applied to the time picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaTimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
) {
    MaterialTimePicker(
        state = state,
        modifier = modifier,
    )
}

/**
 * Tastile remembered time picker state. Delegates to Material 3
 * [androidx.compose.material3.rememberTimePickerState].
 *
 * @param initialHour Initial hour (0-23).
 * @param initialMinute Initial minute (0-59).
 * @param is24Hour Whether the picker uses 24-hour format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberNiaTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = true,
): TimePickerState = materialRememberTimePickerState(
    initialHour = initialHour,
    initialMinute = initialMinute,
    is24Hour = is24Hour,
)

/**
 * Tastile time picker dialog. Material 3 does not ship a dedicated
 * `TimePickerDialog`; the official pattern is to use [AlertDialog] with
 * `properties = DialogProperties(usePlatformDefaultWidth = false)`.
 *
 * @param onDismissRequest Called when the user tries to dismiss the dialog.
 * @param confirmButton The button used to confirm the selected time.
 * @param modifier Modifier applied to the dialog.
 * @param dismissButton Optional button used to dismiss the dialog without selecting.
 * @param content Dialog content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaTimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = content,
    )
}
