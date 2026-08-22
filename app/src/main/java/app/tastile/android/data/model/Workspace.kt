package app.tastile.android.data.model

/**
 * Domain model for an owner scope (= Workspace / Project in the v1
 * server-side model).
 *
 * Sourced from `GET /v1/access/subjects` (the one endpoint the
 * tastile-web client hits to populate the Project tab on every
 * page that needs one). Mirrors the web `Workspace` interface in
 * `tastile-web/src/shared/hooks/use-workspaces.ts`.
 *
 * Two kinds coexist under the same shape:
 *  - `kind == 0` = USER subject = Personal scope (always exactly one
 *    per signed-in user; server pins it to the top of the list).
 *  - `kind == 1` = WORKSPACE subject = Project (one per created
 *    workspace, optional, can have parent_subject_id forming a
 *    hierarchy).
 *
 * The Android Tasks view exposes these as its tab row. Personal is
 * always rendered first and its label is always the localized
 * "Personal" string — the server returns an empty `display_name` for
 * it so we don't read that field at all.
 */
data class Workspace(
    val id: String,
    val kind: Int,
    val displayName: String,
    val slug: String?,
    val email: String?,
    val parentSubjectId: String?,
    val color: String?,
    val ownerUserId: String?,
    val disabledAt: String?,
    val createdAt: String,
    val updatedAt: String,
) {
    /** `kind == 0` — the signed-in user's personal scope. */
    val isPersonal: Boolean get() = kind == 0

    /** `kind == 1` — a user-created project. */
    val isProject: Boolean get() = kind == 1

    /** Disabled workspaces are hidden by the web client; mirror that. */
    val isActive: Boolean get() = disabledAt.isNullOrBlank()

    companion object {
        const val KIND_USER: Int = 0
        const val KIND_WORKSPACE: Int = 1
    }
}
