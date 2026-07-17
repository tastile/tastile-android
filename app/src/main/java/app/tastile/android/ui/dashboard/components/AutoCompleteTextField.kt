package app.tastile.android.ui.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
// m2-allow: m3-component
import androidx.compose.material3.DropdownMenuItem
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuBox
// m2-allow: m3-component
import androidx.compose.material3.ExposedDropdownMenuDefaults
// m2-allow: m3-component
import androidx.compose.material3.MenuAnchorType
// m2-allow: m3-component
import androidx.compose.material3.OutlinedTextField
// m2-allow: primitive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = expanded && suggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = visible,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = visible) },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = visible,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionSelected(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}
