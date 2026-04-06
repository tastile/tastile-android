package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun DurationPickerDialog(
    initialHours: Int,
    initialMinutes: Int,
    title: String,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hours by remember { mutableIntStateOf(initialHours.coerceIn(0, 99)) }
    var minutes by remember { mutableIntStateOf(initialMinutes.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d", hours, minutes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { hours = (hours + 1).coerceIn(0, 99) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                        Text(String.format(Locale.US, "%02d", hours), style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { hours = (hours - 1).coerceIn(0, 99) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minutes = (minutes + 5).coerceIn(0, 59) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                        Text(String.format(Locale.US, "%02d", minutes), style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { minutes = (minutes - 5).coerceIn(0, 59) }) {
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
                                hours = totalMinutes / 60
                                minutes = totalMinutes % 60
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours, minutes) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
