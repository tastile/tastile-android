package app.tastile.android.util

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import app.tastile.android.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * R17 (audit 2026-07-16): base rule pair for instrumented UI navigation tests.
 *
 * Subclasses run on a real device or emulator under [TastileTestRunner], which
 * swaps the production [app.tastile.android.TastileApp] for
 * [dagger.hilt.android.testing.HiltTestApplication]. Use `@TestInstallIn`
 * modules on the subclass to override repositories when exercising
 * authenticated code paths.
 */
@LargeTest
@HiltAndroidTest
@RunWith(JUnit4::class)
abstract class MainActivityTestRule {

    @get:Rule(order = 0)
    val hiltRule: HiltAndroidRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    @Before
    fun injectHilt() {
        hiltRule.inject()
    }
}
