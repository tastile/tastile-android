package app.tastile.android.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 AA contrast guard for [TastileStatusTokens]. Every
 * container/onContainer pair that ships in [TastileStatusTokens] must
 * clear a 4.5:1 contrast ratio against itself in both the light and dark
 * Material 3 default schemes. The pair is tested against the same scheme
 * because the container paints behind the on-container text on the same
 * surface.
 *
 * Phase 1 (Issue #100, release:0-6-0) adds three new container pairs
 * (success / warning / danger) bound to M3 color roles
 * (secondaryContainer / tertiaryContainer / errorContainer). The four
 * legacy lifecycle slots (ready / started / done / archived) are also
 * covered to catch regressions in their container/onContainer pairs.
 */
class TastileStatusTokensContrastTest {

    /**
     * WCAG 2.1 relative luminance for an sRGB-encoded color. Returns a
     * value in `[0, 1]`.
     */
    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val rLinear = srgbLinearize(r)
        val gLinear = srgbLinearize(g)
        val bLinear = srgbLinearize(b)
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    private fun srgbLinearize(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92
        else Math.pow((channel + 0.055) / 1.055, 2.4)

    /**
     * WCAG 2.1 contrast ratio `(L_lighter + 0.05) / (L_darker + 0.05)`.
     * Always >= 1.
     */
    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lFg = relativeLuminance(foreground)
        val lBg = relativeLuminance(background)
        val lighter = maxOf(lFg, lBg)
        val darker = minOf(lFg, lBg)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertMeetsAA(label: String, fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$label: expected contrast >= 4.5 but was %.3f (fg=%s, bg=%s)"
                .format(ratio, fg.toArgb().toString(16), bg.toArgb().toString(16)),
            ratio >= 4.5,
        )
    }

    private fun containerPairs(
        scheme: ColorScheme,
    ): List<Pair<String, Pair<Color, Color>>> {
        val tokens = TastileStatusTokens.default(scheme)
        return listOf(
            "ready.container / ready.onContainer" to
                (tokens.ready.container to tokens.ready.onContainer),
            "started.container / started.onContainer" to
                (tokens.started.container to tokens.started.onContainer),
            "done.container / done.onContainer" to
                (tokens.done.container to tokens.done.onContainer),
            "archived.container / archived.onContainer" to
                (tokens.archived.container to tokens.archived.onContainer),
            "successContainer / onSuccessContainer" to
                (tokens.successContainer to tokens.onSuccessContainer),
            "warningContainer / onWarningContainer" to
                (tokens.warningContainer to tokens.onWarningContainer),
            "dangerContainer / onDangerContainer" to
                (tokens.dangerContainer to tokens.onDangerContainer),
        )
    }

    @Test fun `light scheme - all container pairs meet WCAG AA 4_5`() {
        val pairs = containerPairs(lightColorScheme())
        pairs.forEach { (label, colors) ->
            assertMeetsAA("light $label", colors.first, colors.second)
        }
    }

    @Test fun `dark scheme - all container pairs meet WCAG AA 4_5`() {
        val pairs = containerPairs(darkColorScheme())
        pairs.forEach { (label, colors) ->
            assertMeetsAA("dark $label", colors.first, colors.second)
        }
    }

    @Test fun `light scheme - danger errorContainer onErrorContainer meets AA`() {
        val scheme = lightColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        // The danger pair must bind to the M3 errorContainer / onErrorContainer
        // roles so dynamic color and dark mode adjust automatically.
        assertEquals(scheme.errorContainer, tokens.dangerContainer)
        assertEquals(scheme.onErrorContainer, tokens.onDangerContainer)
        assertMeetsAA(
            "light dangerContainer / onDangerContainer",
            tokens.onDangerContainer,
            tokens.dangerContainer,
        )
    }

    @Test fun `dark scheme - danger errorContainer onErrorContainer meets AA`() {
        val scheme = darkColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        assertEquals(scheme.errorContainer, tokens.dangerContainer)
        assertEquals(scheme.onErrorContainer, tokens.onDangerContainer)
        assertMeetsAA(
            "dark dangerContainer / onDangerContainer",
            tokens.onDangerContainer,
            tokens.dangerContainer,
        )
    }

    @Test fun `light scheme - success secondaryContainer onSecondaryContainer meets AA`() {
        val scheme = lightColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        assertEquals(scheme.secondaryContainer, tokens.successContainer)
        assertEquals(scheme.onSecondaryContainer, tokens.onSuccessContainer)
        assertMeetsAA(
            "light successContainer / onSuccessContainer",
            tokens.onSuccessContainer,
            tokens.successContainer,
        )
    }

    @Test fun `dark scheme - success secondaryContainer onSecondaryContainer meets AA`() {
        val scheme = darkColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        assertEquals(scheme.secondaryContainer, tokens.successContainer)
        assertEquals(scheme.onSecondaryContainer, tokens.onSuccessContainer)
        assertMeetsAA(
            "dark successContainer / onSuccessContainer",
            tokens.onSuccessContainer,
            tokens.successContainer,
        )
    }

    @Test fun `light scheme - warning tertiaryContainer onTertiaryContainer meets AA`() {
        val scheme = lightColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        assertEquals(scheme.tertiaryContainer, tokens.warningContainer)
        assertEquals(scheme.onTertiaryContainer, tokens.onWarningContainer)
        assertMeetsAA(
            "light warningContainer / onWarningContainer",
            tokens.onWarningContainer,
            tokens.warningContainer,
        )
    }

    @Test fun `dark scheme - warning tertiaryContainer onTertiaryContainer meets AA`() {
        val scheme = darkColorScheme()
        val tokens = TastileStatusTokens.default(scheme)
        assertEquals(scheme.tertiaryContainer, tokens.warningContainer)
        assertEquals(scheme.onTertiaryContainer, tokens.onWarningContainer)
        assertMeetsAA(
            "dark warningContainer / onWarningContainer",
            tokens.onWarningContainer,
            tokens.warningContainer,
        )
    }

    @Test fun `contrast ratio helper is symmetric`() {
        val a = Color(0xFF112233)
        val b = Color(0xFFEEDDCC)
        val ratioAB = contrastRatio(a, b)
        val ratioBA = contrastRatio(b, a)
        assertEquals(ratioAB, ratioBA, 0.0001)
    }

    @Test fun `contrast ratio of identical colors is 1 to 1`() {
        val c = Color(0xFF808080)
        val ratio = contrastRatio(c, c)
        assertEquals(1.0, ratio, 0.0001)
    }
}
