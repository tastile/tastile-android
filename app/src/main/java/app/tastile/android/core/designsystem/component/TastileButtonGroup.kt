@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package app.tastile.android.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.theme.TastileButtonGroupTokens

private val tokens = TastileButtonGroupTokens.Default

@Composable
private fun textStyleFor(size: ButtonGroupSize): TextStyle = when (size) {
    ButtonGroupSize.Xs -> MaterialTheme.typography.labelSmall
    ButtonGroupSize.S -> MaterialTheme.typography.labelSmall
    ButtonGroupSize.M -> MaterialTheme.typography.labelMedium
    ButtonGroupSize.L -> MaterialTheme.typography.labelLarge
    ButtonGroupSize.Xl -> MaterialTheme.typography.labelLarge
}

@Composable
fun TastileButtonGroup(
    items: List<ButtonGroupItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    size: ButtonGroupSize = ButtonGroupSize.M,
    modifier: Modifier = Modifier,
) {
    require(items.isNotEmpty()) { "TastileButtonGroup requires at least one item" }
    require(selectedIndex in items.indices) {
        "selectedIndex=$selectedIndex is out of range for items of size ${items.size}"
    }

    val minHeight: Dp = tokens.height(size)
    val minTouchTarget: Dp = 48.dp
    val outerHeight: Dp = if (minHeight < minTouchTarget) minTouchTarget else minHeight

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        val rowScope: SingleChoiceSegmentedButtonRowScope = this
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides if (minHeight < minTouchTarget) minTouchTarget else minHeight,
            ) {
                Box(
                    modifier = Modifier
                        .height(outerHeight)
                        .testTag("button-group-item-$index-touch"),
                ) {
                    rowScope.SegmentedButton(
                        selected = isSelected,
                        onClick = if (item.enabled) ({ onItemSelected(index) }) else ({ /* disabled */ }),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = minTouchTarget)
                            .testTag("button-group-item-$index")
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = tokens.horizontalPadding(size)),
                            ) {
                                item.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(tokens.iconSize(size)),
                                    )
                                }
                                Text(
                                    text = item.label,
                                    style = textStyleFor(size),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
