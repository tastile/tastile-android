package app.tastile.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for the "reference overlay" toggles surfaced in the
 * mobile side panel (`SidePanelSection.References`).
 *
 * Mirrors the additive `enabled: string[]` set on
 * `tastile-web/src/lib/stores/reference-overlay-store.ts`:
 *  - **default** = empty set (no overlays enabled)
 *  - [toggle] adds when absent, removes when present
 *  - [setEnabled] is the bulk-write companion used by tests
 *
 * Backed by [SharedPreferences] (not DataStore — `androidx.datastore` is
 * not in this module's deps yet) via a dedicated prefs file. The store
 * also exposes an in-memory [StateFlow] mirror so Compose can observe
 * the current set via `collectAsState` without re-reading prefs on
 * every recomposition.
 *
 * Labels themselves are not sensitive; they live in plain prefs on
 * purpose. A user clearing app data will reset the overlay set, which
 * matches the web UX (same store, same `localStorage` semantics).
 */
@Singleton
class ReferenceOverlayStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = prefs(context)

    private val _enabled = MutableStateFlow(readInitial())
    /** Live set of label-overlays the user has enabled. */
    val enabled: StateFlow<Set<String>> = _enabled.asStateFlow()

    /** Flow-shaped accessor — kept for parity with the test API. */
    fun getEnabled(): StateFlow<Set<String>> = enabled

    /** Add [label] when absent, remove it when present. Idempotent. */
    suspend fun toggle(label: String) {
        val current = _enabled.value
        val next = if (label in current) current - label else current + label
        write(next)
    }

    /** Replace the entire overlay set. Used by tests and bulk-restore flows. */
    suspend fun setEnabled(labels: Set<String>) {
        write(labels)
    }

    private fun write(next: Set<String>) {
        prefs.edit { putStringSet(KEY_ENABLED, next.toSet()) }
        _enabled.value = next
    }

    private fun readInitial(): Set<String> =
        prefs.getStringSet(KEY_ENABLED, emptySet())?.toSet() ?: emptySet()

    companion object {
        internal const val PREFS_NAME = "tastile-reference-overlay"
        internal const val KEY_ENABLED = "reference_overlay_enabled"

        /** Visible-for-testing accessor for the underlying prefs file. */
        internal fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}