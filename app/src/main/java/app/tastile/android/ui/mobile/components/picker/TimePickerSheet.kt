package app.tastile.android.ui.mobile.components.picker

// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: state-holder
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.core.designsystem.component.NiaTimePicker
import app.tastile.android.core.designsystem.component.NiaTimePickerDialog
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.picker_duration_label,
) {
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    NiaTimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NiaButton(
                onClick = { onConfirm(LocalTime.of(timeState.hour, timeState.minute)) },
                text = { Text(stringResource(R.string.common_confirm)) },
            )
        },
        dismissButton = {
            NiaTextButton(
                onClick = onDismiss,
                text = { Text(stringResource(R.string.common_cancel)) },
            )
        },
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        NiaTimePicker(state = timeState)
    }
}
