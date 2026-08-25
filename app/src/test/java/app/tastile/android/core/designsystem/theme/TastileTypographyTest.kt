package app.tastile.android.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotNull
import org.junit.Test

class TastileTypographyTest {

    @Test fun `every display and headline TextStyle has a font family`() {
        assertNotNull(TastileTypography.displayLarge.fontFamily)
        assertNotNull(TastileTypography.displayMedium.fontFamily)
        assertNotNull(TastileTypography.displaySmall.fontFamily)
        assertNotNull(TastileTypography.headlineLarge.fontFamily)
        assertNotNull(TastileTypography.headlineMedium.fontFamily)
        assertNotNull(TastileTypography.headlineSmall.fontFamily)
    }

    @Test fun `every title body and label TextStyle has a font family`() {
        assertNotNull(TastileTypography.titleLarge.fontFamily)
        assertNotNull(TastileTypography.titleMedium.fontFamily)
        assertNotNull(TastileTypography.titleSmall.fontFamily)
        assertNotNull(TastileTypography.bodyLarge.fontFamily)
        assertNotNull(TastileTypography.bodyMedium.fontFamily)
        assertNotNull(TastileTypography.bodySmall.fontFamily)
        assertNotNull(TastileTypography.labelLarge.fontFamily)
        assertNotNull(TastileTypography.labelMedium.fontFamily)
        assertNotNull(TastileTypography.labelSmall.fontFamily)
    }
}