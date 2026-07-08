package app.tastile.android.ui.mobile.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tastile.android.ui.designsystem.AppSpacing
import app.tastile.android.ui.mobile.designsystem.MobileTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PanelSheet(
    title: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background.copy(alpha = MobileTokens.SurfaceAlpha.strongSelected),
        scrimColor = Color.Black.copy(alpha = MobileTokens.SurfaceAlpha.scrim),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                modifier = Modifier.padding(
                    top = AppSpacing.xs,
                    bottom = AppSpacing.md,
                ),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                content = content,
            )
        }
    }
}