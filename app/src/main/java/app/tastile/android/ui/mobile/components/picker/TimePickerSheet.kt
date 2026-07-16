package app.tastile.android.ui.mobile.components.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: state-holder
import androidx.compose.material3.rememberModalBottomSheetState
// m2-allow: state-holder
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaModalBottomSheet
import app.tastile.android.core.designsystem.component.NiaTextButton
import app.tastile.android.core.designsystem.component.NiaTimePicker
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.picker_duration_label,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )

    NiaModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            NiaTimePicker(state = timeState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NiaTextButton(
                    onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                    modifier = Modifier.weight(1f),
                    text = { Text(stringResource(R.string.common_cancel)) },
                )
                NiaButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onConfirm(LocalTime.of(timeState.hour, timeState.minute))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = { Text(stringResource(R.string.common_confirm)) },
                )
            }
        }
    }
}
