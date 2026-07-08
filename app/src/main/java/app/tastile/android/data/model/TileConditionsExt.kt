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
 *
 * Legacy path. New code should prefer [projectLabels] (top-level
 * `tile.labels`) once every consumer migrates.
 */
fun Tile.projectLabel(): String? {
    val direct = annotationConditions?.get("project")?.toString()
    val directMatch = direct?.let { projectLabelRegex.find(it)?.groupValues?.getOrNull(1) }
    if (!directMatch.isNullOrBlank()) return directMatch

    val labels = annotationConditions?.get("labels")?.toString().orEmpty()
    return projectLabelRegex.find(labels)?.groupValues?.getOrNull(1)
}

/**
 * All `project:NAME`-prefixed labels on the tile. Empty if none.
 * Combines the v1 top-level [Tile.labels] with the legacy
 * `annotationConditions["labels"]` fallback.
 */
fun Tile.projectLabels(): List<String> {
    val fromV1 = labels.filter { it.startsWith("project:") }.map { it.removePrefix("project:") }
    if (fromV1.isNotEmpty()) return fromV1
    val legacy = projectLabel()
    return legacy?.let { listOf(it) }.orEmpty()
}

/**
 * Returns the ISO date prefix (YYYY-MM-DD) of the tile's due_at value,
 * or null if not set. Prefers the v1 top-level [Tile.dueAt] (added in
 * C1); falls back across the same set of legacy condition fields used
 * elsewhere in the UI.
 */
fun Tile.dueAtDate(): String? {
    val v1 = dueAt?.takeIf { it.length >= 10 }?.take(10)
    if (!v1.isNullOrBlank()) return v1
    val raw = temporalConditions?.get("due_at")?.toString()?.trim('"')
        ?: annotationConditions?.get("due_at")?.toString()?.trim('"')
    return raw?.takeIf { it.length >= 10 }?.take(10)
}

/**
 * Whether the tile has a recurrence definition. Reads from the v1
 * `tile.isRecurring` flag (set when the list-view reports a
 * `recurrence` block). Falls back to legacy JSON detection.
 */
fun Tile.isRecurring(): Boolean =
    isRecurring ||
        annotationConditions?.containsKey("recurrence") == true ||
        temporalConditions?.containsKey("recurrence") == true
