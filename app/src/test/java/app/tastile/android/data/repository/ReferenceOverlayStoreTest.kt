package app.tastile.android.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [ReferenceOverlayStore].
 *
 * Backed by Robolectric so that `SharedPreferences` can resolve a real
 * in-memory prefs file. Each test clears the prefs up front so we always
 * observe the round-trip behaviour of [ReferenceOverlayStore.toggle] and
 * [ReferenceOverlayStore.setEnabled] from a known-empty baseline.
 */
@RunWith(AndroidJUnit4::class)
class ReferenceOverlayStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        prefs().edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs().edit().clear().commit()
    }

    @Test
    fun getEnabled_defaultsToEmptySet() = runTest {
        val store = ReferenceOverlayStore(context)
        assertEquals(emptySet<String>(), store.getEnabled().first())
    }

    @Test
    fun toggle_addsLabelWhenAbsent() = runTest {
        val store = ReferenceOverlayStore(context)
        store.toggle("work")
        assertEquals(setOf("work"), store.getEnabled().first())
    }

    @Test
    fun toggle_removesLabelWhenPresent() = runTest {
        val store = ReferenceOverlayStore(context)
        store.toggle("work")
        store.toggle("work")
        assertEquals(emptySet<String>(), store.getEnabled().first())
    }

    @Test
    fun toggle_supportsMultipleLabels() = runTest {
        val store = ReferenceOverlayStore(context)
        store.toggle("work")
        store.toggle("urgent")
        assertEquals(setOf("work", "urgent"), store.getEnabled().first())
    }

    @Test
    fun setEnabled_replacesExistingSet() = runTest {
        val store = ReferenceOverlayStore(context)
        store.toggle("work")
        store.setEnabled(setOf("a", "b", "c"))
        assertEquals(setOf("a", "b", "c"), store.getEnabled().first())
    }

    @Test
    fun setEnabled_emptyClearsAll() = runTest {
        val store = ReferenceOverlayStore(context)
        store.toggle("work")
        store.toggle("urgent")
        store.setEnabled(emptySet())
        assertEquals(emptySet<String>(), store.getEnabled().first())
    }

    @Test
    fun enabled_persistsAcrossInstances() = runTest {
        ReferenceOverlayStore(context).toggle("work")
        ReferenceOverlayStore(context).toggle("urgent")
        val reread = ReferenceOverlayStore(context)
        assertEquals(setOf("work", "urgent"), reread.getEnabled().first())
    }

    private fun prefs() = ReferenceOverlayStore.prefs(context)
}