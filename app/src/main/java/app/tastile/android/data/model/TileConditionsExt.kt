package app.tastile.android.data.model

private val projectLabelRegex = Regex("\"project:([^\"]+)\"")

/**
 * Returns the `project:NAME` token found in the tile's annotation
 * conditions. Two shapes are tolerated:
 *
 *  - `annotationConditions["project"] = "project:NAME"` (string)
 *  - `annotationConditions["labels"]  = ["project:NAME", ...]` (array)
 *
 * Whichever key is present wins; if both are present the dedicated
 * `project` field takes precedence.
 */
fun Tile.projectLabel(): String? {
    val direct = annotationConditions?.get("project")?.toString()
    val directMatch = direct?.let { projectLabelRegex.find(it)?.groupValues?.getOrNull(1) }
    if (!directMatch.isNullOrBlank()) return directMatch

    val labels = annotationConditions?.get("labels")?.toString().orEmpty()
    return projectLabelRegex.find(labels)?.groupValues?.getOrNull(1)
}

/**
 * Returns the ISO date prefix (YYYY-MM-DD) of the tile's due_at value,
 * or null if not set. Falls back across the same set of condition
 * fields used elsewhere in the UI.
 */
fun Tile.dueAtDate(): String? {
    val raw = temporalConditions?.get("due_at")?.toString()?.trim('"')
        ?: annotationConditions?.get("due_at")?.toString()?.trim('"')
    return raw?.takeIf { it.length >= 10 }?.take(10)
}

fun Tile.isRecurring(): Boolean =
    annotationConditions?.containsKey("recurrence") == true ||
        temporalConditions?.containsKey("recurrence") == true