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
 * Rule 11: branch names must match `^\d+$` (digits only).
 *
 * ADR-0007 codifies the release-branch workflow: every implementation
 * branch is named after the GitHub Issue number (e.g. `102`, `100`, `101`)
 * and lives on top of `release-<major>-<minor>-<patch>`. Feature / temp
 * branches and worktrees are not allowed. The Gradle task
 * `:app:verifyBranchName` (registered in `app/build.gradle.kts`) is the
 * authoritative gate; this detector exists so editors see the constraint
 * surfaced in `:app:lint` output and unit tests can assert the rule's
 * regex stays in lock-step with the Gradle task.
 */
class BranchNameRule : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        // Branch names are not visible to a per-file scan; the Gradle task
        // `:app:verifyBranchName` (app/build.gradle.kts) owns the check.
        // This stub keeps the rule listed in IssueRegistry so unit tests
        // can register the pattern against future drift.
    }

    companion object {
        const val PATTERN = """^\d+$"""

        val ISSUE = Issue.create(
            id = "BranchName",
            briefDescription = "Branch name must match `^\\d+$` (ADR-0007)",
            explanation = "ADR-0007 requires every implementation branch to be named " +
                "after the GitHub Issue number (digits). The Gradle task " +
                "`:app:verifyBranchName` is the authoritative gate; this lint rule " +
                "mirrors the regex so editors and tests stay aligned.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 4,
            severity = Severity.WARNING,
            implementation = Implementation(
                BranchNameRule::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}