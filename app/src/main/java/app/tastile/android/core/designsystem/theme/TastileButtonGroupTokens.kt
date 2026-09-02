package app.tastile.android.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tastile.android.core.designsystem.component.ButtonGroupSize

@Immutable
data class TastileButtonGroupTokens(
    val heights: Map<ButtonGroupSize, Dp>,
    val horizontalPaddings: Map<ButtonGroupSize, Dp>,
    val iconSizes: Map<ButtonGroupSize, Dp>,
) {
    fun height(size: ButtonGroupSize): Dp = heights.getValue(size)
    fun horizontalPadding(size: ButtonGroupSize): Dp = horizontalPaddings.getValue(size)
    fun iconSize(size: ButtonGroupSize): Dp = iconSizes.getValue(size)

    companion object {
        val Default = TastileButtonGroupTokens(
            heights = mapOf(
                ButtonGroupSize.Xs to 32.dp,
                ButtonGroupSize.S to 40.dp,
                ButtonGroupSize.M to 48.dp,
                ButtonGroupSize.L to 56.dp,
                ButtonGroupSize.Xl to 64.dp,
            ),
            horizontalPaddings = mapOf(
                ButtonGroupSize.Xs to 8.dp,
                ButtonGroupSize.S to 12.dp,
                ButtonGroupSize.M to 16.dp,
                ButtonGroupSize.L to 20.dp,
                ButtonGroupSize.Xl to 24.dp,
            ),
            iconSizes = mapOf(
                ButtonGroupSize.Xs to 16.dp,
                ButtonGroupSize.S to 18.dp,
                ButtonGroupSize.M to 20.dp,
                ButtonGroupSize.L to 24.dp,
                ButtonGroupSize.Xl to 28.dp,
            ),
        )
    }
}
