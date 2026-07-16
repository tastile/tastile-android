package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: primitive
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: m3-component
import androidx.compose.material3.ModalBottomSheet
// m2-allow: m3-component
import androidx.compose.material3.SheetState
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.NiaFilledTonalButton

/**
 * Shared bottom-sheet wrapper used by every panel sheet. Lays out the M3
 * baseline (opaque `surfaceContainerLow` background, scrim `0.28` black)
 * and a uniform top bar:
 *
 *   [Close X]   [title slot]   [Submit label+icon?]
 *
 * - `title` is the default middle content (static `Text`). Pass `null`
 *   when no title is desired.
 * - `headerContent` overrides `title` when supplied (e.g. QuickCreate uses
 *   a BasicTextField with underline style for the editable tile title).
 * - `onSubmit` enables the right-side FilledTonalButton (`submitLabel` + `submitIcon`).
 *   When `submitEnabled` is `false`, the button is rendered disabled.
 * - The header row sits flush against the M3 drag handle (no vertical padding)
 *   so the submit affordance is the topmost element after the handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onSubmit: (() -> Unit)? = null,
    submitEnabled: Boolean = true,
    submitIcon: ImageVector = Icons.Outlined.Check,
    submitLabel: String = "Submit",
    submitTestTag: String? = null,
    headerContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        scrimColor = colors.scrim.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.onSurface,
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        headerContent != null -> headerContent()
                        title != null -> Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (onSubmit != null) {
                    val submitModifier = if (submitTestTag != null) {
                        Modifier.testTag(submitTestTag)
                    } else Modifier
                    NiaFilledTonalButton(
                        onClick = onSubmit,
                        modifier = submitModifier,
                        text = { Text(submitLabel) },
                        leadingIcon = {
                            Icon(
                                imageVector = submitIcon,
                                contentDescription = null,
                                tint = if (submitEnabled) colors.primary else colors.onSurfaceVariant,
                            )
                        },
                    )
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}
