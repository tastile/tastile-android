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
import androidx.compose.foundation.layout.Arrangement
// m2-allow: primitive
import androidx.compose.foundation.layout.Box
// m2-allow: primitive
import androidx.compose.foundation.layout.Column
// m2-allow: primitive
import androidx.compose.foundation.layout.PaddingValues
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxSize
// m2-allow: primitive
import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: primitive
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.foundation.lazy.LazyColumn
// m2-allow: m3-component
import androidx.compose.foundation.lazy.LazyListScope
// m2-allow: m3-component
import androidx.compose.foundation.lazy.LazyListState
// m2-allow: m3-component
import androidx.compose.foundation.lazy.items
// m2-allow: m3-component
import androidx.compose.foundation.lazy.rememberLazyListState
// m2-allow: primitive
import androidx.compose.material3.Button
// m2-allow: primitive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.ListItem
// m2-allow: primitive
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// m2-allow: m3-component
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.tastile.android.core.designsystem.theme.LocalBackgroundTheme
import app.tastile.android.core.designsystem.theme.LocalTastileSpacingTokens
import app.tastile.android.core.designsystem.theme.TastileTheme

/**
 * Sentinel status indicating which state the [ListFrame] should render.
 *
 * - [ListState.Idle] — render the user-supplied [LazyListScope.content]
 *   block. Default.
 * - [ListState.Loading] — render the loading placeholder. The user-supplied
 *   content is not rendered.
 * - [ListState.Empty] — render the empty placeholder.
 * - [ListState.Error] — render the error placeholder; the optional
 *   [ListState.Error.retry] callback is exposed via a button.
 */
sealed interface ListState {
    data object Idle : ListState
    data object Loading : ListState
    data object Empty : ListState
    data class Error(val message: String, val retry: (() -> Unit)? = null) : ListState
}

/**
 * Standard list surface for the `release:0-6-0` UI rebuild.
 *
 * [ListFrame] is the **only** sanctioned primitive for scrollable lists in
 * the new architecture. It internally renders a [LazyColumn] so that
 * every screen — Tasks, Tiles, Projects, Schedule, References, Timeline,
 * Notifications, Memo — shares the same scroll behaviour and avoids the
 * scroll-death regressions introduced by ad-hoc `Column { forEach { ... } }`
 * loops in the legacy code.
 *
 * Callers provide the rows via [content] which is a [LazyListScope] block.
 * Optional [stickyHeader] / [empty] / [error] / [loading] slots cover the
 * loading lifecycle so individual screens never have to wrap a list in
 * their own state-aware container.
 *
 * Pull-to-refresh is exposed via [onRefresh] + `isRefreshing`. When both
 * are `null`, the underlying [PullToRefreshBox] is rendered without a
 * refresh handler (effectively a plain `LazyColumn` wrapper).
 *
 * @param state Lifecycle state for the list. See [ListState].
 * @param modifier Modifier applied to the underlying [Box] that contains
 *   the [LazyColumn].
 * @param listState Hoisted [LazyListState]. Defaults to
 *   `rememberLazyListState()`.
 * @param stickyHeader Optional sticky header rendered at the top of the
 *   list. Renders **inside** the LazyColumn so it sticks correctly during
 *   overscroll.
 * @param empty Composable rendered when [state] is [ListState.Empty].
 * @param error Composable rendered when [state] is [ListState.Error]. When
 *   the supplied state is [ListState.Error], the default slot uses the
 *   state message and surfaces the optional retry callback as a button.
 * @param loading Composable rendered when [state] is [ListState.Loading].
 * @param isRefreshing Pull-to-refresh indicator flag.
 * @param onRefresh Pull-to-refresh callback. When `null`, the list does
 *   not respond to over-scroll gestures.
 * @param content Caller-provided [LazyListScope] block that emits the list
 *   rows. **Required** by the lint rule — every list screen must place
 *   its rows inside this block. The lambda is optional only so preview
 *   composables that target [ListState.Loading] / [ListState.Empty] /
 *   [ListState.Error] do not have to emit rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListFrame(
    state: ListState = ListState.Idle,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    stickyHeader: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    error: (@Composable () -> Unit)? = null,
    loading: (@Composable () -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit = {},
) {
    val background = LocalBackgroundTheme.current.color
    val spacing = LocalTastileSpacingTokens.current
    val pullState = rememberPullToRefreshState()

    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            ListState.Loading -> loading?.invoke() ?: DefaultLoadingPlaceholder()
            ListState.Empty -> empty?.invoke() ?: DefaultEmptyPlaceholder()
            is ListState.Error ->
                error?.invoke() ?: DefaultErrorPlaceholder(state.message, state.retry)
            ListState.Idle -> PullToRefreshBox(
                state = pullState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh ?: { /* no-op */ },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = spacing.l,
                        vertical = spacing.s,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    stickyHeader?.let { header ->
                        stickyHeader {
                            header()
                        }
                    }
                    content()
                }
            }
        }
    }
    // Reference [background] so the import is not pruned at compile time.
    @Suppress("UNUSED_EXPRESSION")
    background
}

@Composable
private fun DefaultLoadingPlaceholder() {
    val spacing = LocalTastileSpacingTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DefaultEmptyPlaceholder() {
    val spacing = LocalTastileSpacingTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No items.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DefaultErrorPlaceholder(message: String, retry: (() -> Unit)?) {
    val spacing = LocalTastileSpacingTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        if (retry != null) {
            Button(
                onClick = retry,
                modifier = Modifier.padding(top = spacing.s),
            ) {
                Text("Retry")
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ListFrameIdlePreview() {
    TastileTheme {
        ListFrame {
            val sample = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
            items(sample) { label ->
                ListItem(
                    headlineContent = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ListFrameLoadingPreview() {
    TastileTheme {
        ListFrame(state = ListState.Loading)
    }
}

@ThemePreviews
@Composable
private fun ListFrameEmptyPreview() {
    TastileTheme {
        ListFrame(state = ListState.Empty)
    }
}

@ThemePreviews
@Composable
private fun ListFrameErrorPreview() {
    TastileTheme {
        ListFrame(
            state = ListState.Error(message = "Could not load items."),
        )
    }
}