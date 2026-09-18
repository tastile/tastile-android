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
 * Rule 9: forbid `shadowElevation = N.dp` in `ui/`.
 *
 * Elevation is a theme concern (light source, ambient shadow tone). Hard-coding
 * elevation dp values in screens breaks the unified material metaphor that
 * Material3's tonal-elevation system provides. The design system owns the
 * elevation scale; consumers must reference `LocalTastileElevationTokens.current.*`
 * (or use `Card` / `Surface` which apply the right elevation under the hood).
 *
 * R9 is enabled from Phase 1 day 1; no production file currently uses
 * `shadowElevation = N.dp` in `ui/` so the rule is safe to gate on.
 */
class ShadowElevationRule : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        val path = source.relativePath.orEmpty()
        if (!path.contains("/ui/") && !path.startsWith("ui/")) return

        val text = source.readText()
        val match = SHADOW_ELEVATION_PATTERN.find(text) ?: return
        val raw = match.groupValues[1]
        context.report(
            ISSUE,
            context.getLocation(source),
            "Rule 9: `shadowElevation = ${raw}.dp` is forbidden in `ui/`. " +
                "Use `Card` / `Surface` (which apply tonal elevation) or reference " +
                "`LocalTastileElevationTokens.current.*`.",
        )
    }

    companion object {
        val SHADOW_ELEVATION_PATTERN = Regex("""shadowElevation\s*=\s*(\d+(?:\.\d+)?)\.dp""")

        val ISSUE = Issue.create(
            id = "ShadowElevation",
            briefDescription = "`shadowElevation = N.dp` forbidden in `ui/`",
            explanation = "Elevation is a theme concern. Hard-coding elevation dp values " +
                "in screens breaks the unified material metaphor. Use `Card` / `Surface` " +
                "or reference `LocalTastileElevationTokens.current.*` from the design system.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                ShadowElevationRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}