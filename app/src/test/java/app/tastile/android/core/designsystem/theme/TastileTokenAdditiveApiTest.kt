package app.tastile.android.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the additive-only contract that [TastileCardRoleTokens] and
 * [TastileStatusTokens] document: appending fields must keep every existing
 * named and positional construction compiling, and an omitted argument must
 * resolve to the value the previous shape already exposed.
 *
 * This is a compile-time guarantee, so the value assertions below matter less
 * than the fact that the file compiles at the legacy arity. If a future change
 * drops a default from any of these fields, this file stops compiling.
 */
class TastileTokenAdditiveApiTest {

    private val cardColors = TastileCardRoleColors(
        container = androidx.compose.ui.graphics.Color(0xFF112233),
        border = androidx.compose.ui.graphics.Color(0xFF334455),
    )

    @Test
    fun `card role tokens keep the legacy three argument positional construction`() {
        val tokens = TastileCardRoleTokens(
            cardColors,
            cardColors,
            cardColors,
        )

        assertEquals(cardColors, tokens.neutral)
        assertEquals(cardColors, tokens.actionable)
        assertEquals(cardColors, tokens.completed)
    }

    @Test
    fun `card role tokens default the appended fields from the existing ones`() {
        val neutral = TastileCardRoleColors(
            container = androidx.compose.ui.graphics.Color(0xFF000000),
            border = androidx.compose.ui.graphics.Color(0xFF111111),
        )
        val actionable = TastileCardRoleColors(
            container = androidx.compose.ui.graphics.Color(0xFF222222),
            border = androidx.compose.ui.graphics.Color(0xFF333333),
        )
        val completed = TastileCardRoleColors(
            container = androidx.compose.ui.graphics.Color(0xFF444444),
            border = androidx.compose.ui.graphics.Color(0xFF555555),
        )

        val tokens = TastileCardRoleTokens(
            neutral = neutral,
            actionable = actionable,
            completed = completed,
        )

        assertEquals(actionable, tokens.cardAccent)
        assertEquals(neutral, tokens.cardNeutral)
    }

    @Test
    fun `status tokens keep the legacy four argument positional construction`() {
        val ready = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF000000),
            onContainer = androidx.compose.ui.graphics.Color(0xFF111111),
            icon = androidx.compose.ui.graphics.Color(0xFF222222),
        )
        val started = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF333333),
            onContainer = androidx.compose.ui.graphics.Color(0xFF444444),
            icon = androidx.compose.ui.graphics.Color(0xFF555555),
        )
        val done = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF666666),
            onContainer = androidx.compose.ui.graphics.Color(0xFF777777),
            icon = androidx.compose.ui.graphics.Color(0xFF888888),
        )
        val archived = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF999999),
            onContainer = androidx.compose.ui.graphics.Color(0xFFAAAAAA),
            icon = androidx.compose.ui.graphics.Color(0xFFBBBBBB),
        )

        val tokens = TastileStatusTokens(ready, started, done, archived)

        assertEquals(ready, tokens.ready)
        assertEquals(started, tokens.started)
        assertEquals(done, tokens.done)
        assertEquals(archived, tokens.archived)
    }

    @Test
    fun `status tokens default the appended fields from the existing ones`() {
        val ready = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF000000),
            onContainer = androidx.compose.ui.graphics.Color(0xFF111111),
            icon = androidx.compose.ui.graphics.Color(0xFF222222),
        )
        val started = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF333333),
            onContainer = androidx.compose.ui.graphics.Color(0xFF444444),
            icon = androidx.compose.ui.graphics.Color(0xFF555555),
        )
        val done = TastileStatusColors(
            container = androidx.compose.ui.graphics.Color(0xFF666666),
            onContainer = androidx.compose.ui.graphics.Color(0xFF777777),
            icon = androidx.compose.ui.graphics.Color(0xFF888888),
        )

        val tokens = TastileStatusTokens(
            ready = ready,
            started = started,
            done = done,
            archived = ready,
        )

        assertEquals(done.container, tokens.successContainer)
        assertEquals(done.onContainer, tokens.onSuccessContainer)
        assertEquals(started.container, tokens.warningContainer)
        assertEquals(started.onContainer, tokens.onWarningContainer)
        assertEquals(ready.container, tokens.dangerContainer)
        assertEquals(ready.onContainer, tokens.onDangerContainer)
    }

    @Test
    fun `default scheme binding wins over the derived fallbacks`() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { scheme ->
            val status = TastileStatusTokens.default(scheme)
            assertEquals(scheme.secondaryContainer, status.successContainer)
            assertEquals(scheme.onSecondaryContainer, status.onSuccessContainer)
            assertEquals(scheme.tertiaryContainer, status.warningContainer)
            assertEquals(scheme.onTertiaryContainer, status.onWarningContainer)
            assertEquals(scheme.errorContainer, status.dangerContainer)
            assertEquals(scheme.onErrorContainer, status.onDangerContainer)

            val card = TastileCardRoleTokens.default(scheme)
            assertEquals(scheme.primaryContainer, card.cardAccent.container)
            assertEquals(scheme.primary, card.cardAccent.border)
            assertEquals(scheme.surfaceContainer, card.cardNeutral.container)
            assertEquals(scheme.outlineVariant, card.cardNeutral.border)
        }
    }
}
