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

// m2-allow: primitive
import androidx.compose.foundation.layout.Box
// m2-allow: primitive
import androidx.compose.foundation.layout.PaddingValues
// m2-allow: primitive
import androidx.compose.foundation.layout.WindowInsets
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxSize
// m2-allow: primitive
import androidx.compose.foundation.layout.ime
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
// m2-allow: primitive
import androidx.compose.foundation.layout.systemBars
// m2-allow: primitive
import androidx.compose.foundation.layout.union
// m2-allow: primitive
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.FabPosition
// m2-allow: primitive
import androidx.compose.material3.FloatingActionButton
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.Scaffold
// m2-allow: m3-component
import androidx.compose.material3.ScaffoldDefaults
// m2-allow: m3-component
import androidx.compose.material3.SnackbarHost
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.tastile.android.core.designsystem.theme.LocalBackgroundTheme
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme
// m2-allow: state-holder
import androidx.compose.material3.SnackbarHostState

/**
 * Top-level screen surface for the `release:0-6-0` UI rebuild.
 *
 * Wraps Material 3 [Scaffold] with:
 *
 * - `LocalBackgroundTheme` as the container background so screens respect
 *   the same theme bridge the legacy [NiaBackground] uses.
 * - Edge-to-edge content by default — `WindowInsets.systemBars` is honored
 *   via [Scaffold]'s built-in `contentWindowInsets` and the IME is appended
 *   through `WindowInsets.ime` so chat-style inputs do not overlap the
 *   system bar.
 * - Optional `topBar`, `floatingActionButton`, `snackbar`, and the standard
 *   `content` slot.
 *
 * Use [ScreenFrame] for full-bleed routes (login, onboarding, account,
 * settings, tile edit detail, calendar full-screen) where the frame owns the
 * surface. Do **not** use it for inline list / detail panels — those go
 * through [ListFrame] / [DetailFrame].
 *
 * @param modifier Modifier applied to the root container.
 * @param topBar Top app bar slot; typically a `TopAppBar` instance.
 * @param floatingActionButton Optional FAB slot.
 * @param floatingActionButtonPosition FAB anchor position.
 * @param snackbar Optional snackbar host. Accepts a [SnackbarHostState] for
 *   simple wiring or a composable for callers that need a custom host.
 * @param content Insets-aware screen content. Receives [PaddingValues]
 *   describing the non-overlapping insets from [topBar] /
 *   [floatingActionButton] / system insets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbar: SnackbarHostConfig = SnackbarHostConfig.None,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backgroundColor = LocalBackgroundTheme.current.color
    val spacing = LocalTastileSpacingTokens.current

    val snackbarHost: @Composable () -> Unit = when (snackbar) {
        SnackbarHostConfig.None -> ({})
        is SnackbarHostConfig.State -> {
            { SnackbarHost(hostState = snackbar.state) }
        }
        is SnackbarHostConfig.Custom -> ({ snackbar.content() })
    }

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = snackbarHost,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = spacing.l,
                    end = spacing.l,
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
        ) {
            content(paddingValues)
        }
    }
}

/**
 * Sealed union describing how [ScreenFrame] should expose its snackbar host.
 *
 * - [SnackbarHostConfig.None] (default) — no snackbar host is rendered.
 * - [SnackbarHostConfig.State] — a Material 3 [SnackbarHostState] is wired
 *   into [Scaffold.snackbarHost] with default styling.
 * - [SnackbarHostConfig.Custom] — the caller supplies a fully composable
 *   snackbar host slot for custom layouts.
 */
sealed interface SnackbarHostConfig {
    data object None : SnackbarHostConfig
    data class State(val state: SnackbarHostState) : SnackbarHostConfig
    data class Custom(val content: @Composable () -> Unit) : SnackbarHostConfig
}

@ThemePreviews
@Composable
private fun ScreenFramePreview() {
    TastileTheme {
        ScreenFrame(
            topBar = {
                Text(
                    text = "Screen title",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            snackbar = SnackbarHostConfig.State(remember { SnackbarHostState() }),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    text = "ScreenFrame content area",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ScreenFrameWithFabPreview() {
    TastileTheme {
        ScreenFrame(
            topBar = {
                Text(
                    text = "With FAB",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Text("+")
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    text = "Floating action button is anchored bottom-end.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// `WindowInsets.systemBars` and `WindowInsets.ime` are consumed inside
// the [ScreenFrame] composable itself (see the `contentWindowInsets`
// argument to [Scaffold]); no synthetic reference holders are needed.