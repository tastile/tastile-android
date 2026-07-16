package app.tastile.android.ui.mobile.tabs

import app.tastile.android.core.CoreTimelineItem
import app.tastile.android.data.api.Workspace

/** Calendar routes edit requests by their v1 source, mirroring Web CalendarMain. */
internal sealed interface CalendarEventTarget {
    data class Placement(val placementId: String, val tileId: String?) : CalendarEventTarget
    data class RecurringTile(val tileId: String) : CalendarEventTarget
}

internal fun calendarEventTarget(item: CoreTimelineItem): CalendarEventTarget {
    return if (item.sourceKind == 1 && !item.tileId.isNullOrBlank()) {
        CalendarEventTarget.RecurringTile(item.tileId)
    } else {
        CalendarEventTarget.Placement(item.id.substringBefore(':'), item.tileId)
    }
}

internal data class ProjectCheckState(val checked: Boolean, val indeterminate: Boolean)

/** id → self plus every descendant, ignoring dangling parent references. */
internal fun projectDescendantMap(workspaces: List<Workspace>): Map<String, Set<String>> {
    val ids = workspaces.mapTo(mutableSetOf()) { it.id }
    val children = workspaces.groupBy { it.parentSubjectId?.takeIf(ids::contains) }
    val result = mutableMapOf<String, Set<String>>()
    fun collect(id: String, visiting: MutableSet<String> = mutableSetOf()): Set<String> {
        result[id]?.let { return it }
        if (!visiting.add(id)) return setOf(id)
        val family = buildSet {
            add(id)
            children[id].orEmpty().forEach { addAll(collect(it.id, visiting)) }
        }
        visiting.remove(id)
        result[id] = family
        return family
    }
    workspaces.forEach { collect(it.id) }
    return result
}

internal fun toggleProjectCascade(
    selected: Set<String>,
    id: String,
    descendants: Map<String, Set<String>>,
): Set<String> {
    val family = descendants[id] ?: setOf(id)
    return if (family.all(selected::contains)) selected - family else selected + family
}

internal fun projectCheckState(
    id: String,
    selected: Set<String>,
    descendants: Map<String, Set<String>>,
): ProjectCheckState {
    val family = descendants[id] ?: setOf(id)
    val count = family.count(selected::contains)
    return ProjectCheckState(checked = count == family.size, indeterminate = count in 1 until family.size)
}
