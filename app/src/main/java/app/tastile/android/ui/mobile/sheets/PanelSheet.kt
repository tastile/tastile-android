package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.ColumnScope
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.ModalBottomSheet
// m2-allow: m3-component
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pure M3 pass-through to [ModalBottomSheet]. Renders only the standard M3
 * drag handle and the supplied [content] — no custom header row, no custom
 * submit affordance, no height constraints. Callers that need a title or
 * submit button must render them inside [content] using standard M3 components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        content = content,
    )
}