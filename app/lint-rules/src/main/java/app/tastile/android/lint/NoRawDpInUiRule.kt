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
 * Rule 8: forbid raw `<N>.dp` literals in `ui/`.
 *
 * Numeric dp values must go through the design-system token layer
 * (`LocalTastileLayoutTokens.current.*`, etc.) so layout scales with the
 * user's accessibility / theme settings. The detector allows the
 * structural exceptions `0.dp`, `1.dp`, and `0.5.dp` (used for hairline
 * strokes, neutral padding, and exact-half hairlines respectively) but
 * every other value is a violation.
 *
 * R8 is enabled from Phase 1 day 1 (`verifyDesignSystemImports` calls
 * the underlying text scan) because no production file currently uses
 * forbidden raw dp in `ui/` and we want drift to fail the build fast.
 */
class NoRawDpInUiRule : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        val path = source.relativePath.orEmpty()
        // Only the `ui/` consumer tree; the design system itself owns dp tokens.
        if (!path.contains("/ui/") && !path.startsWith("ui/")) return

        val text = source.readText()
        val matches = RAW_DP_PATTERN.findAll(text)
        val firstBad = matches.firstOrNull() ?: return
        val raw = firstBad.groupValues[1]
        val numeric = raw.toDoubleOrNull() ?: return
        if (numeric <= 1.0 || numeric == 0.5) return

        context.report(
            ISSUE,
            context.getLocation(source),
            "Rule 8: raw `${raw}.dp` literal in `ui/`. Route through " +
                "`LocalTastileLayoutTokens.current.*` instead. " +
                "(0.dp / 1.dp / 0.5.dp are exempt.)",
        )
    }

    companion object {
        // Matches `<number>.dp` where `<number>` is one or more digits
        // optionally followed by `.digits`. `0.5` and `0`/`1` are filtered
        // by the numeric guard above.
        val RAW_DP_PATTERN = Regex("""(\d+(?:\.\d+)?)\.dp""")

        val ISSUE = Issue.create(
            id = "NoRawDpInUi",
            briefDescription = "Raw `<N>.dp` literal forbidden in `ui/`",
            explanation = "Numeric dp values must go through the design-system " +
                "token layer (`LocalTastileLayoutTokens.current.*`) so layout scales " +
                "with the user's accessibility / theme settings. Exceptions: " +
                "`0.dp`, `1.dp`, `0.5.dp` (hairlines / neutral padding).",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                NoRawDpInUiRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}