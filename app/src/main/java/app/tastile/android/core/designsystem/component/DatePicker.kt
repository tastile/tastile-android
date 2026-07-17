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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DatePicker as MaterialDatePicker
import androidx.compose.material3.DatePickerDialog as MaterialDatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState as materialRememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tastile date picker. Wraps Material 3 [androidx.compose.material3.DatePicker].
 *
 * @param state The state controlling the date picker.
 * @param modifier Modifier applied to the date picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaDatePicker(
    state: DatePickerState,
    modifier: Modifier = Modifier,
) {
    MaterialDatePicker(
        state = state,
        modifier = modifier,
    )
}

/**
 * Tastile date picker dialog. Wraps Material 3
 * [androidx.compose.material3.DatePickerDialog].
 *
 * @param onDismissRequest Called when the user tries to dismiss the dialog.
 * @param confirmButton The button used to confirm the selected date.
 * @param modifier Modifier applied to the dialog.
 * @param dismissButton Optional button used to dismiss the dialog without selecting.
 * @param content Dialog content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialDatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        content = content,
    )
}

/**
 * Tastile remembered date picker state. Delegates to Material 3
 * [androidx.compose.material3.rememberDatePickerState].
 *
 * @param initialSelectedDateMillis Initial selected date in epoch milliseconds.
 * @param initialDisplayMode Initial display mode (Picker or Input).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaRememberDatePickerState(
    initialSelectedDateMillis: Long? = null,
): DatePickerState = materialRememberDatePickerState(initialSelectedDateMillis = initialSelectedDateMillis)
