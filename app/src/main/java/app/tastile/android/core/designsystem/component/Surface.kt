/*
 * Copyright 2026 The Tastile Authors.
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

import androidx.compose.foundation.layout.fillMaxSize
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.tastile.android.core.designsystem.theme.LocalBackgroundTheme
import app.tastile.android.core.designsystem.theme.LocalTintTheme
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Top-level Tastile surface that paints a background via
 * [LocalBackgroundTheme] and forwards a content color from
 * [LocalTintTheme].
 *
 * Frame primitives and screens should wrap their root content in
 * [TastileSurface] rather than calling
 * `androidx.compose.material3.Surface` directly so the background honors
 * `LocalBackgroundTheme.current.color` / `tonalElevation` and the foreground
 * is sourced from `LocalTintTheme.current.iconTint`.
 *
 * The composable deliberately does not pass through every Material 3
 * Surface parameter. Callers that need shape / border / shadow / tonal
 * elevation should fall back to the M3 [MaterialSurface] directly with an
 * adjacent `// m2-allow: m3-component` marker.
 *
 * @param modifier Modifier applied to the underlying [MaterialSurface].
 * @param content The composable body to render on top of the surface.
 */
@Composable
fun TastileSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backgroundTheme = LocalBackgroundTheme.current
    val color = if (backgroundTheme.color == Color.Unspecified) {
        // Fall back to the active Material 3 surface when no background
        // theme is provided so the canvas never renders transparent.
        MaterialTheme.colorScheme.surface
    } else {
        backgroundTheme.color
    }
    val contentColor = LocalTintTheme.current.iconTint.let { tint ->
        if (tint == Color.Unspecified) {
            MaterialTheme.colorScheme.onSurface
        } else {
            tint
        }
    }
    MaterialSurface(
        modifier = modifier.fillMaxSize(),
        color = color,
        contentColor = contentColor,
        tonalElevation = backgroundTheme.tonalElevation,
        content = content,
    )
}

@ThemePreviews
@Composable
private fun TastileSurfacePreview() {
    TastileTheme {
        TastileSurface {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                androidx.compose.material3.Text(
                    text = "TastileSurface body",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}