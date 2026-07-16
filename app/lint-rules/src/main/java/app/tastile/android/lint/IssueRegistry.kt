package app.tastile.android.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class IssueRegistry : IssueRegistry() {
    override val issues = listOf(
        WrapperParameterOrderDetector.ISSUE,
        WrapperStabilityDetector.ISSUE,
    )
    override val api = CURRENT_API
    override val minApi = 14
    override val vendor = Vendor(
        vendorName = "Tastile",
        identifier = "app.tastile.android.lint",
        feedbackUrl = "https://github.com/tastile/tastile-android/issues",
    )
}
