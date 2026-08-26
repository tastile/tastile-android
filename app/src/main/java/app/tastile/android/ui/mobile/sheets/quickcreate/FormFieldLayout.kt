package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable

/**
 * M3 `ListItem`-compliant leading column width: 24dp icon + 16dp gap
 * = 40dp reserved before the content cell. Matches the header × icon's
 * M3 `IconButton` centerline at x = 16dp (outer padding) + 12dp (half
 * of [IconColumnWidth]) = 28dp, so the body's icon column shares its
 * centerline with the sheet's close icon.
 *
 * These values are pinned locally rather than read from `PanelTokens`
 * to avoid an import cycle between `quickcreate/` and
 * `core/designsystem/`. If you change them, update the matching
 * comments in `QuickCreateSheetMobile.kt` (`QuickCreateHandleRow`)
 * and any drag-handle row that lines up against this column.
 */
private val IconColumnWidth: Dp = 24.dp
private val IconContentGap: Dp = 16.dp

/**
 * Outer horizontal padding applied by every row in the form family.
 * Matches the sheet's header `× close` button left padding so the
 * body's content always lines up vertically with that icon.
 */
private val RowHorizontalPadding: Dp = 16.dp

/**
 * Minimum height for every row in the form family.
 *
 * Mirrors M3 `ListItem`'s single-line default (`minHeight = 48.dp`).
 * Even rows whose intrinsic content is shorter than this — a single
 * line of `labelLarge` text plus a 24dp icon — get a uniform 48dp
 * hit-target row. This keeps the body's vertical rhythm consistent
 * across rows whose content height varies (one line vs. two lines,
 * compact chip vs. dense date trigger, etc.) and matches the
 * touch-target standard from Material Design.
 *
 * Callers can opt into a different alignment inside the row via the
 * `verticalAlignment` parameter (e.g. `Alignment.Top` for a two-line
 * body). The 48dp minimum itself is not configurable from the
 * outside; it is the panel-wide default.
 */
internal val RowMinHeight: Dp = 48.dp

/**
 * Vertical gap between consecutive rows in the form family.
 *
 * Mirrors the web's section rhythm (`py-3` / `space-y` in the
 * `create-tile` UI) and the 4dp spacing the subpanels already use.
 * `FormFieldColumn` applies this via `Arrangement.spacedBy`, so every
 * row — `FormRow`, `FormFieldRow`, `ScrollableChipRow` — sits on the
 * same vertical grid with a uniform 4dp gutter between rows.
 */
internal val RowVerticalSpacing: Dp = 4.dp

/**
 * Canonical form row for QuickCreate panels.
 *
 * Contract:
 *  - **No wrapper responsibility**: `FormFieldColumn` does not apply
 *    any padding. Every row is responsible for its own horizontal
 *    alignment via this composable.
 *  - **Full sheet width by default**: the row claims the entire width
 *    of its parent (`fillMaxWidth()`) and lays out three slots:
 *      [leading] [content] [trailing]
 *    with the leading slot reserved at [IconColumnWidth] even when
 *    [leading] is null.
 *  - **Minimum height of 48dp**: every row gets a uniform vertical
 *    hit-target regardless of content size.
 *  - **Leading slot is always centered vertically** with the row
 *    content, matching M3 `ListItem` behavior.
 *  - **Outer horizontal padding is 16dp on both sides**: leading
 *    `×` and trailing Create button on the sheet header share the
 *    same left/right gutter.
 *
 * Use this for any row that needs the standard leading icon
 * reservation. For rows that have no icon (e.g. a horizontal
 * scrollable chip row), use [ScrollableChipRow] instead — it bypasses
 * the icon column entirely so the chips can span the full sheet
 * width without a 40dp dead zone on the leading side.
 */
@Composable
fun FormFieldRow(
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = RowHorizontalPadding)
            .heightIn(min = RowMinHeight),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.Start,
    ) {
        // 1. Leading slot — reserved [IconColumnWidth] track, even when
        //    no `leading` composable is provided.
        Box(
            modifier = Modifier.size(IconColumnWidth),
            contentAlignment = Alignment.Center,
        ) {
            leading?.invoke()
        }

        // 2. Gap between leading slot and content
        Spacer(modifier = Modifier.width(IconContentGap))

        // 3. Content cell — fills remaining width via weight
        Row(modifier = Modifier.weight(1f)) {
            content()
        }

        // 4. Optional trailing slot (e.g. an "Open" button)
        if (trailing != null) {
            Spacer(modifier = Modifier.width(IconContentGap))
            trailing()
        }
    }
}

/**
 * Convenience overload: leading slot accepts an icon. Use this when
 * the row needs an `ImageVector` rendered in the leading slot. The
 * icon is sized to [IconColumnWidth] (24dp) and inherits the local
 * content color so it picks up `onSurfaceVariant` from the M3
 * ListItem convention.
 */
@Composable
fun FormFieldRow(
    icon: ImageVector?,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    FormFieldRow(
        leading = icon?.let { ic ->
            {
                Icon(
                    imageVector = ic,
                    contentDescription = null,
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(IconColumnWidth),
                )
            }
        },
        trailing = trailing,
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * Compatibility shim — historical `FormFieldLayout { content }` calls
 * (still used throughout the subpanels) forward onto [FormFieldRow]
 * with no leading icon. New code should call [FormFieldRow] directly.
 */
@Composable
fun FormFieldLayout(
    icon: ImageVector? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable () -> Unit,
) {
    FormFieldRow(
        icon = icon,
        modifier = modifier,
        verticalAlignment = verticalAlignment,
    ) {
        content()
    }
}

/**
 * Convenience overload of [FormFieldLayout] that omits the icon
 * argument (icon-less rows). Forwards to [FormFieldRow] with
 * `icon = null`.
 */
@Composable
fun FormFieldLayout(
    modifier: Modifier = Modifier.fillMaxWidth(),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable () -> Unit,
) {
    FormFieldRow(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
    ) {
        content()
    }
}

/**
 * Column wrapper for QuickCreate panel content.
 *
 * Contract:
 *  - **No padding, no wrapper responsibility**: this is a plain
 *    `Column` that lays out its children top-to-bottom. Each child
 *    is expected to control its own horizontal alignment via
 *    [FormFieldRow] (icon-bearing row), [ScrollableChipRow]
 *    (chrome-less scrolling row), or any other self-contained
 *    composable.
 *  - **Scroll vertically** if needed (panels wire `verticalScroll`).
 *
 * Removing the previous 16dp horizontal padding here is what lets
 * the row family own its gutter: each row applies
 * `padding(horizontal = 16.dp)` itself, so we can selectively
 * override the right-side gutter for rows that need to extend past
 * the sheet edge (e.g. a scrollable chip row whose last chip needs
 * to reach the right edge of the sheet).
 */
@Composable
fun FormFieldColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(RowVerticalSpacing),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}
