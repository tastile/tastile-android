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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileTheme

@Composable
fun NiaLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
    wheelSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    LoadingIndicator(
        modifier = modifier
            .size(wheelSize)
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .testTag("loadingWheel"),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun NiaOverlayLoadingWheel(
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(60.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.83f),
        modifier = modifier
            .size(60.dp),
    ) {
        NiaLoadingWheel(
            contentDesc = contentDesc,
        )
    }
}

@ThemePreviews
@Composable
fun NiaLoadingWheelPreview() {
    TastileTheme {
        Surface {
            NiaLoadingWheel(contentDesc = "LoadingWheel")
        }
    }
}

@ThemePreviews
@Composable
fun NiaOverlayLoadingWheelPreview() {
    TastileTheme {
        Surface {
            NiaOverlayLoadingWheel(contentDesc = "LoadingWheel")
        }
    }
}
