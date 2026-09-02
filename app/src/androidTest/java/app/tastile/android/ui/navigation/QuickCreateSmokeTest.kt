package app.tastile.android.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.tastile.android.util.MainActivityTestRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

/**
 * M3 Expressive Phase 3 — Task 3.2 (plan §"Task 3.2: Add instrumented
 * QuickCreate smoke test") + Task 2.1 (`quick-create-fab` testTag wiring
 * contract) regression coverage.
 *
 * End-to-end path under a real [app.tastile.android.MainActivity]: launch
 * the app, navigate to TimelineScreen, tap the `quick-create-fab` testTag
 * installed by Phase 2 commit `8085e77`, and assert that the
 * `quick-create-close` testTag rendered by
 * [app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSheetMobile]
 * is displayed.
 *
 * Plan note: the plan's literal `MainActivityTestRule(createAndroidComposeRule())`
 * would not compile against the actual abstract rule at
 * `app/src/androidTest/java/app/tastile/android/util/MainActivityTestRule.kt:27`
 * (no constructor args; `composeTestRule` is exposed as a `@get:Rule`-decorated
 * property). The plan §Step 3 explicitly authorizes adjusting the test to
 * match the rule's actual API — this file follows that authorization using
 * the `class … : MainActivityTestRule()` pattern established by the sibling
 * `MainActivityAuthGateTest`.
 *
 * Run with:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *     --tests "app.tastile.android.ui.navigation.QuickCreateSmokeTest"
 */
@HiltAndroidTest
class QuickCreateSmokeTest : MainActivityTestRule() {

    @Test
    fun timelineQuickCreateFab_opensSheet() {
        composeTestRule.onNodeWithTag("quick-create-fab").performClick()
        composeTestRule.onNodeWithTag("quick-create-close").assertIsDisplayed()
    }
}
