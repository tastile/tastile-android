package app.tastile.android.ui.mobile.panels.projects

import app.tastile.android.data.api.Workspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectsTreeTest {
    private fun workspace(id: String, name: String, parent: String? = null) = Workspace(
        id = id,
        displayName = name,
        parentSubjectId = parent,
    )

    @Test
    fun orderWorkspaceTree_keeps_parent_before_child_and_sorts_siblings() {
        val ordered = orderWorkspaceTree(listOf(
            workspace("child", "Child", "parent"),
            workspace("second", "Zebra"),
            workspace("parent", "Alpha"),
        ))

        assertEquals(listOf("parent", "child", "second"), ordered.map { it.workspace.id })
        assertEquals(listOf(0, 1, 0), ordered.map { it.depth })
    }

    @Test
    fun descendantIds_excludes_self_children_and_deeper_descendants_from_parent_picker() {
        val workspaces = listOf(
            workspace("root", "Root"),
            workspace("child", "Child", "root"),
            workspace("grandchild", "Grandchild", "child"),
            workspace("other", "Other"),
        )

        val blocked = descendantIds("root", workspaces) + "root"

        assertTrue(setOf("root", "child", "grandchild").all(blocked::contains))
        assertTrue("other" !in blocked)
    }
}
