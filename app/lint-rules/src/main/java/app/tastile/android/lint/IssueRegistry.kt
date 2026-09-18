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
        // Rule 5 — project-wide `// m2-allow:` budget (baseline 522, +50 headroom).
        M2AllowBudgetRule.ISSUE,
        // Rule 6 — ViewModel must publish exactly one `val uiState: StateFlow<*>`.
        SingleUiStateRule.ISSUE,
        // Rule 7 — tile mutation use-cases must route through `ui/tile/TileComposer.kt`.
        SingleComposerRule.ISSUE,
        // Rule 8 — raw `<N>.dp` literals forbidden in `ui/` (exceptions 0.dp / 1.dp / 0.5.dp).
        NoRawDpInUiRule.ISSUE,
        // Rule 9 — `shadowElevation = N.dp` forbidden in `ui/`.
        ShadowElevationRule.ISSUE,
        // Rule 10 — `Color(0xFF...)` literals forbidden in `ui/` (only `designsystem/theme/Color.kt` exempt).
        HardcodedColorRule.ISSUE,
        // Rule 11 — branch name must match `^\d+$` (ADR-0007); authoritative gate is `:app:verifyBranchName`.
        BranchNameRule.ISSUE,
        // Rule 12 — SourceTileRead wire-shape contract gate; authoritative gate is `:app:verifyWireShapeContract`.
        WireShapeContractGate.ISSUE,
    )
    override val api = CURRENT_API
    override val minApi = 14
    override val vendor = Vendor(
        vendorName = "Tastile",
        identifier = "app.tastile.android.lint",
        feedbackUrl = "https://github.com/tastile/tastile-android/issues",
    )
}