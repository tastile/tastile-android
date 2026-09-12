package app.tastile.android.lint

import com.android.tools.lint.client.api.JavaContext
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/**
 * Rule 5: budget check for `// m2-allow:` markers.
 *
 * Every `// m2-allow:` marker exempts the *next* import line from Rule 1
 * (no Material3 in M3-unified screens). The markers are an escape hatch
 * that must be spent sparingly; Phase 1 establishes the baseline (522) and
 * enforces a phase-end cap of `baseline + 50 = 572`.
 *
 * Unlike the other Rule 4-12 detectors this rule is *also* surfaced as a
 * Gradle task (`verifyM2AllowBudget` in `app/build.gradle.kts`) so the
 * budget can fail the build even when lint is suppressed. The Gradle
 * gate is the source of truth; this detector simply mirrors the count
 * so editors see the budget at the offending line.
 */
class M2AllowBudgetRule : Detector(), Detector.SourceCodeScanner {

    override fun visitResource(context: JavaContext, resource: com.android.tools.lint.client.api.ResourceFile) {
        // No-op: per-file text scan handled by visitSourceCode.
    }

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        val text = source.readText()
        if (text.contains("// m2-allow:")) {
            // Per-line reporting would be noisy; the Gradle task owns the
            // project-wide count. Just surface a single hint per file.
            context.report(
                ISSUE,
                context.getLocation(source),
                "Rule 5: `// m2-allow:` marker present. The project-wide budget " +
                    "is enforced by `:app:verifyM2AllowBudget` " +
                    "(baseline $BASELINE + $BUDGET_DELTA = $LIMIT).",
            )
        }
    }

    companion object {
        const val BASELINE = 522
        const val BUDGET_DELTA = 50
        const val LIMIT = BASELINE + BUDGET_DELTA

        val ISSUE = Issue.create(
            id = "M2AllowBudget",
            briefDescription = "`// m2-allow:` marker budget at risk",
            explanation = "Every `// m2-allow:` marker exempts one line from " +
                "Rule 1 (no Material3 in M3-unified screens). The project-wide " +
                "budget is $LIMIT (baseline $BASELINE + $BUDGET_DELTA delta). " +
                "The Gradle task `:app:verifyM2AllowBudget` owns the count.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 4,
            severity = Severity.WARNING,
            implementation = Implementation(
                M2AllowBudgetRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}