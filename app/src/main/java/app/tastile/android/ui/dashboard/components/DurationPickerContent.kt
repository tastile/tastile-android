package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
// m2-allow: m3-component
import androidx.compose.material3.FilterChip
// m2-allow: primitive
import androidx.compose.material3.Icon
// m2-allow: m3-component
import androidx.compose.material3.IconButton
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun DurationPickerContent(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format(Locale.US, "%02d:%02d", hours, minutes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onHoursChange((hours + 1).coerceIn(0, 99)) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
                Text(String.format(Locale.US, "%02d", hours), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onHoursChange((hours - 1).coerceIn(0, 99)) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            Text(":", style = MaterialTheme.typography.headlineMedium)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onMinutesChange((minutes + 5).coerceIn(0, 59)) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
                Text(String.format(Locale.US, "%02d", minutes), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onMinutesChange((minutes - 5).coerceIn(0, 59)) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15 to "15m", 25 to "25m", 45 to "45m", 60 to "1h").forEach { (totalMinutes, label) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        onHoursChange(totalMinutes / 60)
                        onMinutesChange(totalMinutes % 60)
                    },
                    label = { Text(label) },
                )
            }
        }
    }
}
