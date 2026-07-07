package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.tastile.android.data.repository.AppLocale
import app.tastile.android.data.repository.ThemeMode
import app.tastile.android.ui.theme.TastileTheme

@Composable
fun <T> PickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onPick(value) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
fun LocalePickerDialog(
    current: AppLocale,
    onPick: (AppLocale) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerDialog(
        title = "Locale",
        options = listOf(
            AppLocale.JA to "日本語",
            AppLocale.EN to "English",
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
fun ThemePickerDialog(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerDialog(
        title = "Theme",
        options = listOf(
            ThemeMode.DARK to "Dark (default)",
            ThemeMode.LIGHT to "Light",
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
fun TimeoutPickerDialog(
    currentMinutes: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerDialog(
        title = "Lock timeout",
        options = listOf(5 to "5 min", 15 to "15 min", 60 to "60 min"),
        selected = currentMinutes,
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Preview(showBackground = true)
@Composable
private fun LocalePickerDialogPreview() {
    TastileTheme { LocalePickerDialog(current = AppLocale.JA, onPick = {}, onDismiss = {}) }
}
