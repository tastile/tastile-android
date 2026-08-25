package app.tastile.android.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TastileStatusTokensTest {

    @Test fun `default light tokens resolve to Material3 lightColorScheme slots`() {
        val tokens = TastileStatusTokens.default(lightColorScheme())
        assertEquals(lightColorScheme().surfaceVariant, tokens.ready.container)
        assertEquals(lightColorScheme().onSurfaceVariant, tokens.ready.onContainer)
        assertEquals(lightColorScheme().primary, tokens.ready.icon)
    }

    @Test fun `default tokens differ between light and dark schemes`() {
        val light = TastileStatusTokens.default(lightColorScheme())
        val dark = TastileStatusTokens.default(darkColorScheme())
        assertNotEquals(light.ready.icon, dark.ready.icon)
    }

    @Test fun `all four lifecycle slots are populated`() {
        val tokens = TastileStatusTokens.default(lightColorScheme())
        // Each branch reads at least one slot; assert it is not transparent.
        listOf(
            tokens.ready.icon,
            tokens.started.icon,
            tokens.done.icon,
            tokens.archived.icon,
        ).forEach { color ->
            assertNotEquals(androidx.compose.ui.graphics.Color.Transparent, color)
        }
    }
}
