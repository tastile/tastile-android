package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: m3-component
import androidx.compose.material3.DatePicker
// m2-allow: m3-component
import androidx.compose.material3.DatePickerDialog
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tastile.android.R
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppTertiaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
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

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppPrimaryButton(
                text = stringResource(R.string.common_confirm),
                onClick = {
                    val millis = state.selectedDateMillis ?: return@AppPrimaryButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onConfirm(date)
                },
            )
        },
        dismissButton = {
            AppTertiaryButton(text = stringResource(R.string.common_cancel), onClick = onDismiss)
        },
    ) {
        Column(modifier = Modifier.padding(MobileSpacing.md)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        }
        DatePicker(state = state)
    }
}
