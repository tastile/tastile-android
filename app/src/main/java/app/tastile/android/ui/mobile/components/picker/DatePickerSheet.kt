package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: state-holder
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaDatePicker
import app.tastile.android.core.designsystem.component.NiaDatePickerDialog
import app.tastile.android.core.designsystem.component.NiaTextButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.picker_date_start,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    NiaDatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            NiaButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@NiaButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date)
                },
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
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        }
        NiaDatePicker(state = state)
    }
}
