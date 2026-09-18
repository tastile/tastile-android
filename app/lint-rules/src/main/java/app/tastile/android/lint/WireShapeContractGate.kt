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
 * Rule 12: the wire-shape contract gate for `SourceTileRead`.
 *
 * The canonical key set for `SourceTileRead` lives in
 * `app/src/main/assets/source_tile_canonical_keys.json`. Every test
 * fixture under `app/src/test/resources/wire_fixtures/source_tile/*.json`
 * must contain exactly that set of top-level keys. Drift (added /
 * removed / renamed keys) silently breaks the wire decoder that consumes
 * those fixtures, so the Gradle task `:app:verifyWireShapeContract` is
 * wired into `:app:check` and fails fast on mismatch.
 *
 * The lint detector stub here exists so the rule is registered in
 * `IssueRegistry` and unit tests can lock the regex / JSON schema path
 * against accidental relocation.
 */
class WireShapeContractGate : Detector(), SourceCodeScanner {

    override fun visitSourceCode(context: JavaContext, source: com.android.tools.lint.client.api.SourceFile) {
        // The Gradle task `:app:verifyWireShapeContract` owns the comparison.
        // This stub keeps the rule listed in IssueRegistry so the file
        // reference / canonical-keys path can be regression-locked.
    }

    companion object {
        const val CANONICAL_KEYS_PATH =
            "app/src/main/assets/source_tile_canonical_keys.json"
        const val FIXTURE_GLOB = "app/src/test/resources/wire_fixtures/source_tile/*.json"

        val ISSUE = Issue.create(
            id = "WireShapeContract",
            briefDescription = "SourceTileRead wire-shape contract drift gate",
            explanation = "The canonical key set for `SourceTileRead` lives in " +
                "`app/src/main/assets/source_tile_canonical_keys.json`. Every test " +
                "fixture under `app/src/test/resources/wire_fixtures/source_tile/*.json` " +
                "must match it exactly. The Gradle task `:app:verifyWireShapeContract` " +
                "owns the comparison; this lint rule mirrors the contract path so " +
                "editors and tests stay aligned.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 4,
            severity = Severity.WARNING,
            implementation = Implementation(
                WireShapeContractGate::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}