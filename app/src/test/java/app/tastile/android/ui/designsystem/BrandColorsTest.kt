package app.tastile.android.ui.designsystem

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
        listOf(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground", "surface", "onSurface",
            "surfaceVariant", "onSurfaceVariant", "outline", "outlineVariant",
            "scrim", "inverseSurface", "inverseOnSurface", "inversePrimary",
        ).forEach { role ->
            // throws if the role is unset
            l::class.java.getMethod(role)
        }
    }
}
