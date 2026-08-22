/*
 * Tastile Tasks-view scope selector. The mobile Tasks view is now
 * project-axis: tab row holds ALL / STARRED / UNASSIGNED + any per-project
 * sections. Time-axis grouping is sorted inside each section via
 * [SortOrder], not driven through tabs.
 */
package app.tastile.android.ui.dashboard

import androidx.annotation.StringRes
import app.tastile.android.R
import app.tastile.android.data.model.Tile

/**
 * How a ProjectSection is rendered.
 */
enum class SortOrder(val id: String, @StringRes val labelRes: Int) {
    BY_TIME_ASC("time_asc", R.string.tasks_sort_by_time_asc),
    BY_TIME_DESC("time_desc", R.string.tasks_sort_by_time_desc),
    BY_TITLE("title", R.string.tasks_sort_by_title);

    companion object {
        val DEFAULT: SortOrder = BY_TIME_ASC
        fun fromId(id: String?): SortOrder =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * A grouping displayed inside the Tasks view as either a tab option (when
 * listed alongside its peers) or a row of tiles (when [selected]).
 *
 * Each [id] is stable across recompositions so it can drive Compose
 * `key = ...` and `testTag`s.
 */
data class ProjectSection(
    val id: String,
    val label: String,
    val tiles: List<Tile>,
)

/**
 * Reserved section IDs for non-tile pseudo-tabs in the Tasks view.
 *
 * Historically the Tasks view exposed ALL / STARRED / UNASSIGNED as
 * fixed scopes above the project list. With the v1 source-tiles API
 * we now treat every project as a first-class section and there is no
 * "All" / "Unassigned" catch-all. The only pseudo-tab we still need is
 * a "+ New List" entry that opens the project-creation overlay, plus
 * a sentinel that the ViewModel uses when nothing is selected yet.
 *
 * Kept as a single object so legacy callers that read [DEFAULT] still
 * resolve.
 */
enum class FixedTasksScope(val id: String, @StringRes val labelRes: Int) {
    NEW_LIST("new_list", R.string.tasks_tab_new_list),
    NONE("", R.string.tasks_scope_all);

    companion object {
        /**
         * Default selected scope. There is no real "default" project —
         * the first project becomes the default once projects are
         * loaded. Until then we expose the empty-sentinel id.
         */
        val DEFAULT: FixedTasksScope = NONE

        fun fromId(id: String?): FixedTasksScope =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Reserved section IDs that the ViewModel appends to [ProjectSection]
 * lists but which aren't backed by tiles. The UI uses these IDs to
 * trigger creation actions (e.g. "new list" → open the quick-create
 * overlay) instead of selecting an existing scope.
 */
object TasksScopeActions {
    /** Final "new project list" tab — tap opens the new-list overlay. */
    const val NEW_LIST: String = "new_list"
}
