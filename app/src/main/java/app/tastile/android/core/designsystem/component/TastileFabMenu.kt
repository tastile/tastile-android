@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

private const val MAX_MENU_ITEMS = 6

@Composable
fun TastileFabMenu(
    mainIcon: ImageVector,
    mainLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier,
) {
    require(items.isNotEmpty()) { "TastileFabMenu requires at least one item" }
    val clippedItems = items.take(MAX_MENU_ITEMS)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fab-rotation",
    )
    val stateDesc = if (expanded) "expanded" else "collapsed"

    BackHandler(enabled = expanded) { onExpandedChange(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                clippedItems.forEachIndexed { index, item ->
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (item) {
                                is FabMenuItem.Action -> item.onClick()
                            }
                        },
                        modifier = Modifier
                            .testTag("fab-menu-item-$index")
                            .semantics { role = Role.Button },
                    ) {
                        Icon(item.icon, contentDescription = null)
                        Text(item.label)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.semantics {
                role = Role.Button
                stateDescription = stateDesc
            },
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = mainLabel,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}
