package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * Canonical 3-column row for QuickCreate forms.
 *
 * Enforces M3 `ListItem`-compliant leading alignment so the body's
 * icon column shares its centerline with the sheet's header × icon
 * (M3 IconButton, 48dp touch target):
 *  - Outer horizontal padding: 16dp (applied by this row directly,
 *    matching `FormFieldRow`; `FormFieldColumn` provides no padding)
 *  - Icon column: 24dp (fixed, reserved even if icon is null)
 *  - Gap between icon and content: 16dp
 *  - Content cell: fills remaining width
 *  - Total content offset from screen edge: 16 + 24 + 16 = 56dp
 *  - Icon column centerline: 16 + 12 = 28dp (matches the header ×)
 *
 * The icon column is **always** reserved with a visible vertical guide
 * line (low-opacity outline) so even rows without an explicit icon
 * clearly display the column boundary, matching the web's CSS Grid
 * guide.
 *
 * Every row also enforces the panel-wide **48dp minimum height**
 * ([RowMinHeight]) and a uniform 4dp vertical gutter between rows
 * ([RowVerticalSpacing], applied by [FormFieldColumn]) so the body
 * sits on a consistent vertical grid regardless of content height.
 */
private val RowIconColumnWidth: androidx.compose.ui.unit.Dp = 24.dp
private val RowIconContentGap: androidx.compose.ui.unit.Dp = 16.dp

@Composable
fun FormRow(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // 1. Icon column (fixed 24dp, reserved track)
        Box(
            modifier = Modifier
                .size(RowIconColumnWidth)
                .drawBehind {
                    // Draw a faint vertical guide line on the icon column to
                    // visually indicate the reserved 24dp track even when no
                    // icon is provided.
                    val strokeWidth = 0.5.dp.toPx()
                    drawRect(
                        color = androidx.compose.ui.graphics.Color
                            .Gray.copy(alpha = 0.18f),
                        topLeft = Offset(size.width - strokeWidth / 2f, 0f),
                        size = Size(strokeWidth, size.height),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            icon?.invoke()
        }

        // 2. Gap (16dp)
        Box(modifier = Modifier.size(width = RowIconContentGap, height = 1.dp))

        // 3. Content + Trailing area
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}
