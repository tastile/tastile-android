package app.tastile.android.ui.mobile.sheets.quickcreate

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
// m2-allow: m3-component
import androidx.compose.material3.LocalTextStyle
// m2-allow: m3-component
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.LocalContentColor
import app.tastile.android.core.designsystem.theme.LocalTastileCardRoleTokens

/**
 * Single-line text field with a quiet bottom underline.
 *
 * Mirrors `tastile-web/src/shared/styles/quick-create.css`'s
 * `.qc-underline-input` rule:
 *  - No `border`, no box outline, no floating label.
 *  - Bottom underline 1dp in [LocalTastileCardRoleTokens.current.neutral.border]
 *    when unfocused, 2dp in [LocalTastileCardRoleTokens.current.actionable.border]
 *    when focused.
 *  - Placeholder text uses [LocalContentColor.current].
 *
 * Used by [QuickCreateHeader] for the title row and by [DateTimeRow]
 * for the date / time inputs.
 */
@Composable
fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    testTag: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> isFocused = true
                is FocusInteraction.Unfocus -> isFocused = false
                else -> Unit
            }
        }
    }

    val underlineColor = if (isFocused) {
        LocalTastileCardRoleTokens.current.actionable.border
    } else {
        LocalTastileCardRoleTokens.current.neutral.border
    }
    val underlineThickness = if (isFocused) 2.dp else 1.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = underlineThickness.toPx()
                val y = size.height - strokeWidth / 2f
                drawLine(
                    color = underlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Square,
                )
            }
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
            .padding(vertical = 4.dp),
        textStyle = textStyle.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(LocalTastileCardRoleTokens.current.actionable.border),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = true,
        visualTransformation = VisualTransformation.None,
        interactionSource = interactionSource,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                androidx.compose.material3.Text(
                    text = placeholder,
                    style = textStyle.copy(color = LocalContentColor.current),
                )
            } else {
                inner()
            }
        },
    )
}

/** Convenience accessor so callers can keep their imports tidy. */
val MaterialThemeLocalTextStyle: TextStyle
    @Composable
    get() = LocalTextStyle.current
