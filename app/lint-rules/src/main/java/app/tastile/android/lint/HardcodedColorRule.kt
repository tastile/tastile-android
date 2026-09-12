package app.tastile.android.lint

import com.android.tools.lint.client.api.JavaContext
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner

/**
 * Rule 10: forbid `Color(0xFF...)` literals in `ui/` (except `designsystem/theme/Color.kt`).
 *
 * Hex color literals outside the design system defeat the semantic-token layer
 * (primary / secondary / surface / error / etc.). Every screen must read
 * colors via the design-system token API; the only file allowed to declare
 * raw `Color(0x...)` is `core/designsystem/theme/Color.kt` where the tokens
 * themselves live.
 *
 * R10 is enabled from Phase 1 day 1; the design system file is the sole
 * exempt location.
 */
class HardcodedColorRule : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        val path = source.relativePath.orEmpty()
        if (!path.contains("/ui/") && !path.startsWith("ui/")) return

        // Sole exemption: the design-system token declaration file.
        if (path.endsWith("core/designsystem/theme/Color.kt")) return
        if (path.endsWith("designsystem/theme/Color.kt")) return

        val text = source.readText()
        val match = HEX_COLOR_PATTERN.find(text) ?: return
        val raw = match.groupValues[1]
        context.report(
            ISSUE,
            context.getLocation(source),
            "Rule 10: hardcoded `Color(0x${raw}...)` literal in `ui/`. " +
                "Reference a design-system token instead " +
                "(see `core/designsystem/theme/Color.kt`).",
        )
    }

    companion object {
        // Matches `Color(0xFF112233)` / `Color(0xffAABBCC)` / `Color(0xFFFFFFFFL)`.
        val HEX_COLOR_PATTERN = Regex("""Color\(\s*0[xX][0-9A-Fa-f]{6,8}""")

        val ISSUE = Issue.create(
            id = "HardcodedColor",
            briefDescription = "`Color(0xFF...)` literal forbidden in `ui/`",
            explanation = "Hex color literals outside the design system defeat the " +
                "semantic-token layer. Every screen must read colors via the " +
                "design-system token API; only `core/designsystem/theme/Color.kt` " +
                "may declare raw `Color(0x...)` values.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                HardcodedColorRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}