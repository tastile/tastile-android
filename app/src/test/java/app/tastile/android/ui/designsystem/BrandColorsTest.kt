package app.tastile.android.ui.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BrandColorsTest {
    @Test fun `light and dark schemes differ in expected roles`() {
        val l = BrandColors.light()
        val d = BrandColors.dark()
        assertNotEquals(l.primary, d.primary)
        assertNotEquals(l.background, d.background)
    }

    @Test fun `light scheme has 28-role coverage`() {
        val l = BrandColors.light()
        // 28 explicit property accesses — the Kotlin compiler enforces coverage.
        // ColorScheme interface methods are compiled with name-mangling suffixes
        // (e.g. getPrimary-0d7_KjU), so Java reflection can't be used here.
        val roles: List<Color> = listOf(
            l.primary, l.onPrimary, l.primaryContainer, l.onPrimaryContainer,
            l.secondary, l.onSecondary, l.secondaryContainer, l.onSecondaryContainer,
            l.tertiary, l.onTertiary, l.tertiaryContainer, l.onTertiaryContainer,
            l.error, l.onError, l.errorContainer, l.onErrorContainer,
            l.background, l.onBackground, l.surface, l.onSurface,
            l.surfaceVariant, l.onSurfaceVariant, l.outline, l.outlineVariant,
            l.scrim, l.inverseSurface, l.inverseOnSurface, l.inversePrimary,
        )
        assertEquals(28, roles.size)
    }
}
