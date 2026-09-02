package app.tastile.android.ui.memo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.model.Tile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoScreen(
    viewModel: MemoViewModel = hiltViewModel()
) {
    val recentTiles by viewModel.recentTiles.collectAsStateWithLifecycle()
    val selectedTileId by viewModel.selectedTileId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var noteText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.memo_saved_message)
    val errorPrefix = stringResource(R.string.memo_error_prefix)

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.clearSaveSuccess()
            noteText = "" // Clear input
        }
    }

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(errorPrefix + it)
            viewModel.clearError()
        }
    }

    val selectedTile = recentTiles.find { it.id == selectedTileId }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = stringResource(R.string.memo_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tile selector dropdown
                if (recentTiles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        // menuAnchor() (no args) was removed in M3 1.5.0-alpha27;
                        // the deprecated overload no longer exists. The
                        // read-only OutlinedTextField below uses the
                        // PrimaryNotEditable anchor type.
                        val anchorModifier: Modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true,
                        )
                        OutlinedTextField(
                            value = selectedTile?.title ?: stringResource(R.string.memo_select_tile),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.memo_tile_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(anchorModifier)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            recentTiles.forEach { tile ->
                                DropdownMenuItem(
                                    text = { Text(tile.title) },
                                    onClick = {
                                        viewModel.selectTile(tile.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Note input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.memo_note_label)) },
                    placeholder = { Text(stringResource(R.string.memo_note_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    minLines = 5
                )

                // Save button
                Button(
                    onClick = {
                        viewModel.saveMemo(noteText)
                    },
                    enabled = noteText.isNotBlank() && selectedTileId != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.memo_save_button))
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
