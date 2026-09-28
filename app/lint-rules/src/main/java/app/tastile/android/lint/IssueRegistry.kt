package app.tastile.android.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class IssueRegistry : IssueRegistry() {
    override val issues = listOf(
        // Rule 1/2 (wrapper design-system parameter order + stability) — original L0 rules.
        WrapperParameterOrderDetector.ISSUE,
        WrapperStabilityDetector.ISSUE,
        // Rule 4 — Screen/Sheet/Frame/Dialog/Panel/Scaffold must import LocalBackgroundTheme or Surface.
        FrameLocalBackgroundRule.ISSUE,
        // Rule 6 — ViewModel must publish exactly one `val uiState: StateFlow<*>`.
        SingleUiStateRule.ISSUE,
        // Rules 5, 8, 9, 10, 11 and 12 are enforced by Gradle tasks in
        // `app/build.gradle.kts` (`verifyM2AllowBudget`, `verifyDesignSystemImports`,
        // `verifyBranchName`, `verifyWireShapeContract`) rather than by a lint
        // detector. Those rules are project-wide budgets, branch metadata, or
        // cross-file wire contracts, none of which lint 32 can express through
        // the UAST-only `SourceCodeScanner`. Rule 7 ships with the Phase 4
        // TileComposer work, when the use cases it inspects exist.
    )
    override val api = CURRENT_API
    override val minApi = 14
    override val vendor = Vendor(
        vendorName = "Tastile",
        identifier = "app.tastile.android.lint",
        feedbackUrl = "https://github.com/tastile/tastile-android/issues",
    )
}