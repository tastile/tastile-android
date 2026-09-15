package app.tastile.android.ui.navigation

import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.QuickCreateSheetMobile
import app.tastile.android.util.HiltTestActivity
import app.tastile.android.util.QuickCreateCanaryBackend
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Platform-level canary for the QuickCreate create journey, motivated by the
 * 2026-09-15 signed-AAB report: the Create button rendered
 * `enabled=true / clickable=true`, yet physical pointer input never produced
 * a POST, while the same sheet's close(X) worked on the same input path.
 *
 * What this test PROVES (fake boundary is the network edge ONLY):
 * real Composable (incl. ModalBottomSheet container) -> real pointer/touch
 * dispatch -> real `onClick` -> real submission ViewModel -> real dispatcher
 * -> fake [V1ApiClient] ([QuickCreateCanaryBackend]), through to sheet close.
 *
 * Why three tests:
 * - [platformClickOnCreate_submitsWrite]: [UiDevice.click] at the node's
 *   display coordinates — travels the real MotionEvent -> Compose gesture
 *   path. The load-bearing journey proof: GREEN means a user tap on Create
 *   really submits.
 * - [semanticsClickOnCreate_submitsWrite]: direct OnClick invocation control
 *   (what `performClick()` does, minus idle waits) — bypasses gesture
 *   dispatch by design. If this is GREEN while the platform test is RED,
 *   the delta points at the gesture/input layer rather than the
 *   ViewModel/dispatcher.
 * - [platformClickOnClose_dismissesSheet]: harness control — a platform tap
 *   on the same sheet's close(X) must dismiss. GREEN proves tap delivery
 *   and coordinate math work end-to-end; if THIS were red too, suspect the
 *   harness/environment, not the app.
 *
 * Verified finding (2026-09-15, API 35 emulator): early runs showed
 * platform RED (calls=0) with the production `dragHandle`-embedded Button,
 * but a control run with the button UNCHANGED also went GREEN once system
 * "… isn't responding" dialogs were dismissed — the RED had been a system
 * alert window swallowing outside taps under software rendering, NOT sheet
 * gesture arbitration. Do NOT cite those early REDs as proof of a dragHandle
 * defect. The physical-device report remains unexplained by this suite and
 * needs the signed-device gate; this canary guards the journey against
 * regressions, it does not reproduce that device-specific fault.
 *
 * Idle-freedom (deliberate): software-rendered CI/emulator frames can take
 * seconds each, and the driven title field keeps a blinking cursor (an
 * infinite animation) once focused — so idle-gated APIs (`waitForIdle`,
 * `waitUntil`, `assert*`) are unusable after focus. The test therefore polls
 * with plain sleeps + [fetchSemanticsNode], invokes the semantics click
 * action directly on the UI thread, and asserts the backend call counter
 * (no Compose involvement). Focus management is unnecessary: the platform
 * tap moves focus itself, and the semantics control never waits for idle.
 *
 * Failure evidence: every stage logs to logcat (tag [TAG]); on failure the
 * test captures a screenshot + window hierarchy into the app's external
 * files dir (`canary/`) before rethrowing. The CI canary job pulls that dir
 * and uploads it with the connected-test reports and logcat.
 *
 * Run with (API 35 emulator, KVM):
 *   ./gradlew :app:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=app.tastile.android.ui.navigation.QuickCreateGestureCanaryTest
 */
@HiltAndroidTest
@RunWith(JUnit4::class)
class QuickCreateGestureCanaryTest {

    @get:Rule(order = 0)
    val hiltRule: HiltAndroidRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var device: UiDevice
    private lateinit var overlay: OverlayViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        QuickCreateCanaryBackend.reset()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun platformClickOnCreate_submitsWrite() {
        withEvidence("platform-click") {
            launchAndSubmit(::deviceClickOnNode)
        }
    }

    @Test
    fun semanticsClickOnCreate_submitsWrite() {
        // Semantics control: bypasses gesture arbitration by design.
        withEvidence("semantics-click") {
            launchAndSubmit(::semanticsClickOnNode)
        }
    }

    @Test
    fun platformClickOnClose_dismissesSheet() {
        // Harness control: a platform tap on the SAME dragHandle row's
        // close(X) must dismiss the sheet. GREEN proves tap delivery and
        // coordinate math work end-to-end; if THIS were red too, the fault
        // would be in the harness, not in the Create button's gesture path.
        withEvidence("platform-close") {
            Log.i(TAG, "stage=launch")
            launchSheetWithValidDraft()
            Log.i(TAG, "stage=click-close")
            deviceClickOnNode("quick-create-close")
            captureState("after-close-click")
            Log.i(TAG, "stage=await-close")
            awaitSheetClosed()
            Log.i(TAG, "stage=done")
        }
    }

    private fun launchAndSubmit(click: (String) -> Unit) {
        Log.i(TAG, "stage=launch")
        launchSheetWithValidDraft()
        Log.i(TAG, "stage=click")
        click("quick-create-handle-submit")
        // Capture the visual state right after the tap lands: proves where
        // the tap went (button vs scrim vs drag pill) even if submit never
        // fires. Pulled via `adb pull .../files/canary` while the app is
        // still installed (the gradle connected task uninstalls afterwards,
        // so prefer install + `am instrument` + pull for diagnosis runs).
        captureState("after-click")
        Log.i(TAG, "stage=await-write")
        awaitWriteBoundary()
        Log.i(TAG, "stage=await-close")
        awaitSheetClosed()
        Log.i(TAG, "stage=done calls=${QuickCreateCanaryBackend.createSourceTileCalls.get()}")
    }

    // ------------------------------------------------------------------
    // Journey steps (shared by both input paths)
    // ------------------------------------------------------------------

    private fun launchSheetWithValidDraft() {
        composeTestRule.setContent {
            TastileTheme {
                overlay = remember { OverlayViewModel() }
                // Dashboard/Projects/Submission ViewModels resolve via
                // hiltViewModel() defaults: REAL production wiring under the
                // HiltTestApplication graph with CanaryApiModule's backend.
                QuickCreateSheetMobile(overlay = overlay)
            }
        }
        composeTestRule.runOnUiThread { overlay.show(Overlay.QuickCreate) }
        pollNode("quick-create-handle-submit", timeoutMs = 240_000)
        Log.i(TAG, "sheet chrome visible")
        // Let the sheet enter animation finish (frames can take seconds
        // under software rendering) before driving input.
        Thread.sleep(30_000)

        // The base sheet seeds a valid next-15-min span via
        // QuickCreateStateStore.openCreate(Event); a non-blank title is the
        // remaining precondition for validation to pass and Create to enable.
        composeTestRule.onNodeWithTag("quick-create-title").performTextInput("Canary tile")
        Log.i(TAG, "title entered")
        Thread.sleep(5_000)

        val deadline = System.currentTimeMillis() + 120_000
        var enabled = false
        while (System.currentTimeMillis() < deadline) {
            enabled = isEnabled(composeTestRule.onNodeWithTag("quick-create-handle-submit"))
            if (enabled) break
            Thread.sleep(2_000)
        }
        Log.i(TAG, "create enabled=$enabled")
        check(enabled) { "Create button is not enabled after entering a valid title" }
        dismissSystemAnrDialogs()
    }

    /**
     * Slow/emulated environments raise "… isn't responding" system dialogs
     * (e.g. Play services under TCG) that sit in a system alert window above
     * the app and swallow outside taps. They are environment noise, not the
     * object under test: dismiss via the dialog's own Wait button right
     * before any platform tap. No-op when no dialog is present (the KVM CI
     * case). UiObject2.click on a SYSTEM dialog is fine — gesture fidelity
     * only matters for taps on the app under test.
     */
    private fun dismissSystemAnrDialogs() {
        repeat(3) {
            val wait = device.findObject(By.text("Wait")) ?: return
            Log.i(TAG, "dismissing system ANR dialog")
            wait.click()
            Thread.sleep(3_000)
        }
    }

    /** The write boundary must be hit EXACTLY once per user-confirmed create. */
    private fun awaitWriteBoundary() {
        val deadline = System.currentTimeMillis() + 240_000
        while (System.currentTimeMillis() < deadline) {
            val calls = QuickCreateCanaryBackend.createSourceTileCalls.get()
            if (calls >= 1) {
                Log.i(TAG, "write boundary hit calls=$calls")
                check(calls == 1) { "expected exactly 1 createSourceTile call, got $calls" }
                return
            }
            Thread.sleep(500)
        }
        error(
            "write boundary never hit: createSourceTile calls=" +
                QuickCreateCanaryBackend.createSourceTileCalls.get(),
        )
    }

    /** Success tears the sheet down (consume + overlay.dismiss). */
    private fun awaitSheetClosed() {
        val deadline = System.currentTimeMillis() + 120_000
        while (System.currentTimeMillis() < deadline) {
            if (isGone("quick-create-handle-submit")) {
                Log.i(TAG, "sheet closed")
                return
            }
            Thread.sleep(500)
        }
        error("sheet did not close after successful submit")
    }

    // ------------------------------------------------------------------
    // Idle-free node helpers (poll via fetchSemanticsNode, never waitIdle)
    // ------------------------------------------------------------------

    private fun pollNode(tag: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!isGone(tag)) return true
            Thread.sleep(500)
        }
        error("node [$tag] never appeared within ${timeoutMs}ms")
    }

    private fun isGone(tag: String): Boolean = runCatching {
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        false
    }.getOrDefault(true)

    private fun isEnabled(node: SemanticsNodeInteraction): Boolean = runCatching {
        val semantics = node.fetchSemanticsNode()
        !semantics.config.contains(SemanticsProperties.Disabled)
    }.getOrDefault(false)

    // ------------------------------------------------------------------
    // Platform input + failure evidence
    // ------------------------------------------------------------------

    /**
     * True platform pointer input: converts the Compose node's window
     * position to display coordinates and sends a tap through UiAutomator
     * (real MotionEvent injection), NOT through Compose semantics. The tap
     * itself moves focus, so no focus management is needed.
     */
    private fun deviceClickOnNode(tag: String) {
        dismissSystemAnrDialogs()
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val position = node.positionInWindow
        val size = node.size
        val decor = composeTestRule.activity.window.decorView
        val location = IntArray(2)
        decor.getLocationOnScreen(location)
        val x = location[0] + position.x + size.width / 2
        val y = location[1] + position.y + size.height / 2
        Log.i(TAG, "device click [$tag] at display=($x,$y)")
        check(device.click(x.toInt(), y.toInt())) {
            "UiDevice.click failed at display coords ($x, $y) for node [$tag]"
        }
    }

    /**
     * Semantics control: invokes the node's OnClick action directly on the
     * UI thread — the same call `performClick()` would make, minus its
     * idle waits (unusable once the title field's cursor blinks). Still
     * bypasses gesture arbitration by design, preserving the control's
     * meaning: GREEN here + RED on the platform path = gesture layer.
     */
    private fun semanticsClickOnNode(tag: String) {
        // fetchSemanticsNode synchronizes and therefore must run on the
        // test thread; only the action invocation goes to the UI thread.
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        val action = runCatching { node.config[SemanticsActions.OnClick] }.getOrNull()
            ?: error("node [$tag] has no OnClick action")
        composeTestRule.runOnUiThread {
            val handled = action.action?.invoke()
                ?: error("node [$tag] OnClick action is null")
            Log.i(TAG, "semantics click [$tag] handled=$handled")
            check(handled) { "node [$tag] OnClick action declined" }
        }
    }

    private fun <T> withEvidence(name: String, block: () -> T): T {
        try {
            return block()
        } catch (failure: Throwable) {
            Log.e(
                TAG,
                "canary [$name] failed: ${failure.message} " +
                    "calls=${QuickCreateCanaryBackend.createSourceTileCalls.get()}",
                failure,
            )
            captureState(name)
            throw failure
        }
    }

    private fun canaryDir(): File = File(
        InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir(null),
        "canary",
    ).apply { mkdirs() }

    private fun captureState(name: String) {
        runCatching {
            val dir = canaryDir()
            val shot = device.takeScreenshot(File(dir, "$name.png"))
            File(dir, "$name-hierarchy.xml").outputStream().use { out ->
                device.dumpWindowHierarchy(out)
            }
            Log.i(TAG, "canary [$name] state captured to ${dir.absolutePath} shot=$shot")
        }.onFailure { evidenceFailure ->
            Log.e(TAG, "canary [$name] state capture failed", evidenceFailure)
        }
    }

    private companion object {
        const val TAG = "QuickCreateCanary"
    }
}
