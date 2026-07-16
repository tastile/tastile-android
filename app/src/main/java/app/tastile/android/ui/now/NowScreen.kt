package app.tastile.android.ui.now

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowScreen(
    viewModel: NowViewModel = hiltViewModel()
) {
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    var isRefreshing by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadTiles()
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with refresh button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { viewModel.loadTiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            // Active tile (Started)
            val activeTile = tiles.find { it.lifecycle == TileLifecycle.STARTED.value }
            if (activeTile != null) {
                ActiveTileCard(
                    tile = activeTile,
                    onComplete = { viewModel.completeTile(it) }
                )
            }

            // Error message
            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Tiles list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tiles.filter { it.lifecycle != TileLifecycle.STARTED.value }) { tile ->
                    TileCard(
                        tile = tile,
                        onStart = { viewModel.startTile(it) },
                        onComplete = { viewModel.completeTile(it) },
                        onDelete = { deleteCandidate = it }
                    )
                }
            }

            // Create new tile
            CreateTileSection(
                onCreate = { viewModel.createTile(it) }
            )
        }

        // Loading indicator
        if (isLoading && tiles.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        deleteCandidate?.let { id ->
            AlertDialog(
                onDismissRequest = { deleteCandidate = null },
                title = { Text("Delete tile?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTile(id)
                        deleteCandidate = null
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
fun ActiveTileCard(
    tile: Tile,
    onComplete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, Color.Green, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
                
                LifecycleBadge(lifecycle = TileLifecycle.STARTED)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            tile.nextAction?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onComplete(tile.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete")
            }
        }
    }
}

@Composable
fun TileCard(
    tile: Tile,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val lifecycle = TileLifecycle.fromString(tile.lifecycle)
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LifecycleBadge(lifecycle = lifecycle)
            }
            
            Row {
                when (lifecycle) {
                    TileLifecycle.READY -> {
                        IconButton(onClick = { onStart(tile.id) }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TileLifecycle.STARTED -> {
                        IconButton(onClick = { onComplete(tile.id) }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = Color.Green
                            )
                        }
                    }
                    TileLifecycle.DONE -> {
                        // No action for done tiles
                    }
                    TileLifecycle.ARCHIVED -> {
                        // No action for archived tiles
                    }
                }
                
                IconButton(onClick = { onDelete(tile.id) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun LifecycleBadge(lifecycle: TileLifecycle) {
    val (color, text) = when (lifecycle) {
        TileLifecycle.READY -> MaterialTheme.colorScheme.outline to "Ready"
        TileLifecycle.STARTED -> MaterialTheme.colorScheme.primary to "Started"
        TileLifecycle.DONE -> Color.Green to "Done"
        TileLifecycle.ARCHIVED -> MaterialTheme.colorScheme.outlineVariant to "Archived"
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CreateTileSection(
    onCreate: (String) -> Unit
) {
    var newTileTitle by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTileTitle,
                onValueChange = { newTileTitle = it },
                label = { Text("New tile title") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    if (newTileTitle.isNotBlank()) {
                        onCreate(newTileTitle)
                        newTileTitle = ""
                    }
                },
                enabled = newTileTitle.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}
