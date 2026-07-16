package app.tastile.android.ui.mobile.panels.projects

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.tastile.android.data.api.Workspace
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectFormRobolectricTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun newProjectForm_exposesFreeColorAndParentProjectControls() {
        compose.setContent {
            NewProjectForm(
                busy = false,
                errorText = null,
                workspaces = listOf(Workspace(id = "parent", displayName = "Parent")),
                onSubmit = { _, _, _, _ -> },
                onCancel = {},
            )
        }

        compose.onNodeWithTag("project-create-name").fetchSemanticsNode()
        compose.onNodeWithTag("project-create-slug").fetchSemanticsNode()
        compose.onNodeWithTag("project-create-color").fetchSemanticsNode()
        compose.onNodeWithTag("project-create-parent").fetchSemanticsNode()
        compose.onNodeWithTag("project-create-submit").fetchSemanticsNode()
    }
}
