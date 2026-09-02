package app.tastile.android.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Adapter that exposes TastileShapeTokens through MaterialTheme.shapes.
 * Component code that reads MaterialTheme.shapes.<key> automatically picks up
 * the values from LocalTastileShapeTokens.
 */
val TastileShapes: Shapes
    @Composable
    @ReadOnlyComposable
    get() = Shapes(
        extraSmall = RoundedCornerShape(LocalTastileShapeTokens.current.xs),
        small = RoundedCornerShape(LocalTastileShapeTokens.current.s),
        medium = RoundedCornerShape(LocalTastileShapeTokens.current.m),
        large = RoundedCornerShape(LocalTastileShapeTokens.current.large),
        extraLarge = RoundedCornerShape(LocalTastileShapeTokens.current.xl),
        largeIncreased = RoundedCornerShape(LocalTastileShapeTokens.current.largeIncreased),
        extraLargeIncreased = RoundedCornerShape(LocalTastileShapeTokens.current.extraLargeIncreased),
        extraExtraLarge = RoundedCornerShape(LocalTastileShapeTokens.current.extraExtraLarge),
    )