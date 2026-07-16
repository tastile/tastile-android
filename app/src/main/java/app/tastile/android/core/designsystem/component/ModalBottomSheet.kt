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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet as MaterialModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState as materialRememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Tastile modal bottom sheet. Wraps Material 3
 * [androidx.compose.material3.ModalBottomSheet].
 *
 * @param onDismissRequest Called when the user tries to dismiss the sheet.
 * @param modifier Modifier applied to the sheet container.
 * @param sheetState State of the sheet.
 * @param sheetMaxWidth Maximum width of the sheet.
 * @param content Sheet content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = NiaRememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        content = content,
    )
}

/**
 * Tastile remembered modal bottom sheet state. Delegates to Material 3
 * [androidx.compose.material3.rememberModalBottomSheetState]. Not annotated
 * `@Stable` because [SheetState] is itself `@Stable` on the Material side but
 * the underlying functions are `@ExperimentalMaterial3Api`; callers must opt
 * in the same way they would with the raw M3 function.
 *
 * @param skipPartiallyExpanded Whether the sheet should skip the partially
 * expanded state and snap directly to expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NiaRememberModalBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
): SheetState = materialRememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
