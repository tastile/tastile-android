package app.tastile.android.ui.mobile.sheets

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tastile.android.core.designsystem.theme.TastileTheme
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreatePanelContent
import app.tastile.android.ui.mobile.sheets.quickcreate.QuickCreateSubpanel
import app.tastile.android.ui.mobile.sheets.quickcreate.quickCreateSubmissionValidation
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke coverage for the QuickCreate 4-peer-editor + stacked-subpanel
 * architecture.
 *
 * 2026-09-08: rewrote the prior monolithic-base suite (which asserted
 * against tags like `quick-create-base`, `quick-create-organize-row`,
 * `quick-create-tasks-header` etc.) against the current production
 * layout. Each test mounts the canonical entry point for the surface
 * under test:
 *  - workflow-level surfaces mount [QuickCreatePanelContent] (the
 *    dispatcher that routes to Event / Task / Recurring / Detailed);
 *  - subpanel surfaces mount [QuickCreateSubpanel] directly so the
 *    subpanel content composes without depending on the host's
 *    stacked-sheet plumbing.
 *
 * Interactions inside horizontally-scrolling `ScrollableChipRow`s are
 * exercised through the store mutation path rather than the click
 * gesture (Robolectric's horizontal scroll viewport does not advance
 * in lockstep with the parent column under the v1 dispatcher).
 */
@RunWith(AndroidJUnit4::class)
class QuickCreatePanelsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val projects = listOf(
        QuickCreateProject("workspace-focus", "Focus"),
        QuickCreateProject("workspace-home", "Home"),
    )

    @Test
    fun `event workflow panel composes its event header and workflow batch`() {
        val store = QuickCreateStateStore()
        rule.setContent { TastileTheme { QuickCreatePanelContent(store, {}, projects, emptyList()) } }

        // Event is the default workflow, so the Event panel mounts on
        // first composition.
        rule.onNodeWithTag("quick-create-event").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-event-header").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-event-batch").assertIsDisplayed()
        // The active panel stays at Base — the workflow-level panel
        // exposes open-affordance rows but does not navigate to a
        // subpanel itself.
        assertEquals(QuickCreatePanel.Base, store.state.value.activePanel)
    }

    @Test
    fun `recurring workflow panel composes its recurring header and workflow batch`() {
        // Seed the store with the Recurring workflow so the dispatcher
        // mounts the recurring panel on first composition (the panel
        // dispatcher reads `draft.workflow` synchronously via
        // `store.draft`, which is NOT a Compose State, so seeding the
        // workflow in the initial draft is the only reliable way to
        // compose a non-default workflow in this test harness).
        val store = QuickCreateStateStore(
            QuickCreateDraftState(workflow = WorkflowKind.Recurring),
        )
        rule.setContent { TastileTheme { QuickCreatePanelContent(store, {}, projects, emptyList()) } }

        rule.onNodeWithTag("quick-create-recurring").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-recurring-header").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-recurring-batch").assertIsDisplayed()
    }

    @Test
    fun `subpanel shell mounts the identity panel with description and color chips`() {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Identity,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = emptyList(),
                )
            }
        }
        rule.onNodeWithTag("quick-create-subpanel-Identity").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-description").assertIsDisplayed()
        rule.onAllNodesWithTag("quick-create-color-3b82f6", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .let { nodes -> assertTrue("default blue swatch must render", nodes.isNotEmpty()) }
    }

    @Test
    fun `subpanel shell mounts the duration panel with the no-duration row`() {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Duration,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = emptyList(),
                )
            }
        }
        rule.onNodeWithTag("quick-create-subpanel-Duration").assertIsDisplayed()
        rule.onNodeWithTag("quick-create-duration-none").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `subpanel shell mounts the meta panel with project catalog tag chips and memo`() {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Meta,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = listOf("health", "weekly"),
                )
            }
        }
        rule.onNodeWithTag("quick-create-subpanel-Meta").assertIsDisplayed()
        rule.onNodeWithTag("meta-project-catalog").assertIsDisplayed()
        rule.onNodeWithTag("meta-tag-chips").assertIsDisplayed()
        rule.onNodeWithTag("meta-memo").assertIsDisplayed()
        rule.onNodeWithTag("meta-clear").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("meta-apply").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `subpanel shell mounts the time panel with the when mode picker`() {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Time,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = emptyList(),
                )
            }
        }
        rule.onNodeWithTag("quick-create-subpanel-Time").assertIsDisplayed()
        // The when-mode picker exposes a "none" chip alongside Day /
        // Range / Reference; verify at least the none chip is reachable
        // so the picker cannot silently disappear during refactors.
        rule.onNodeWithTag("quick-create-when-none").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `submission validation reflects the base draft regardless of the mounted subpanel`() {
        // An invalid draft (no title, no start/end span) stays invalid
        // no matter which subpanel the user has open.
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Identity,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = emptyList(),
                )
            }
        }
        assertTrue(!quickCreateSubmissionValidation(store.state.value).isValid)

        // After supplying title + a valid span via the store, validation
        // flips without requiring the subpanel route to change.
        store.updateIdentity(store.state.value.identity.copy(title = "Plan review"))
        store.updateTime(
            store.state.value.time.copy(
                span = QuickCreateSpan("2026-07-19T09:00:00Z", "2026-07-19T10:00:00Z"),
            ),
        )
        assertTrue(quickCreateSubmissionValidation(store.state.value).isValid)
    }

    @Test
    fun `completion term appended through the store survives recomposition`() {
        val store = QuickCreateStateStore()
        rule.setContent {
            val draft by store.state.collectAsStateWithLifecycle()
            TastileTheme {
                QuickCreateSubpanel(
                    panel = QuickCreatePanel.Completion,
                    draft = draft,
                    store = store,
                    onBack = { store.backToBase() },
                    projects = projects,
                    knownTags = emptyList(),
                )
            }
        }
        val before = store.state.value.plan.completion.root.children.size
        rule.runOnUiThread {
            store.appendCompletionTerm(
                JsonObject(
                    mapOf(
                        "kind" to JsonPrimitive("task"),
                        "value" to JsonObject(
                            mapOf(
                                "taskId" to JsonPrimitive("task_default"),
                                "state" to JsonPrimitive(2),
                            )
                        ),
                    )
                )
            )
        }
        rule.waitForIdle()
        // The default completion root already contains one term
        // (`defaultTermCondition`); appendCompletionTerm adds one more
        // child. Verify the size grew by exactly one.
        assertEquals(before + 1, store.state.value.plan.completion.root.children.size)
    }
}
