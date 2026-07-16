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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker as MaterialTimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState as materialRememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
fun NiaRememberTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = true,
): TimePickerState = materialRememberTimePickerState(
    initialHour = initialHour,
    initialMinute = initialMinute,
    is24Hour = is24Hour,
)
