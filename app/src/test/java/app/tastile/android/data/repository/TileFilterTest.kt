package app.tastile.android.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileFilterTest {

    @Test
    fun default_emitsOnlyViewModeAndLimit() {
        val params = TileFilter.DEFAULT.toQueryParameters()
        assertEquals(
            mapOf("view_mode" to "list", "limit" to "20"),
            params,
        )
    }

    @Test
    fun lifecycle_addsKeyWhenProvided() {
        val params = TileFilter(lifecycle = "started").toQueryParameters()
        assertEquals("started", params["lifecycle"])
        assertTrue(params.containsKey("view_mode"))
        assertTrue(params.containsKey("limit"))
    }

    @Test
    fun lifecycle_blankIsOmitted() {
        val params = TileFilter(lifecycle = "   ").toQueryParameters()
        assertFalse(params.containsKey("lifecycle"))
    }

    @Test
    fun excludeFuture_addsKeyOnlyWhenTrue() {
        val off = TileFilter(excludeFuture = false).toQueryParameters()
        assertFalse(off.containsKey("exclude_future"))

        val on = TileFilter(excludeFuture = true).toQueryParameters()
        assertEquals("true", on["exclude_future"])
    }

    @Test
    fun ownerIds_joinsCommaAndOmitsWhenEmpty() {
        val none = TileFilter().toQueryParameters()
        assertFalse(none.containsKey("owner_ids"))

        val two = TileFilter(ownerIds = listOf("u1", "u2")).toQueryParameters()
        assertEquals("u1,u2", two["owner_ids"])
    }

    @Test
    fun rangeAndGranularityAndSearch_allAppearWhenSet() {
        val params = TileFilter(
            range = "today",
            granularity = "min_5m",
            search = "meeting",
        ).toQueryParameters()
        assertEquals("today", params["range"])
        assertEquals("min_5m", params["granularity"])
        assertEquals("meeting", params["search"])
    }

    @Test
    fun blankNullableStringsAreOmitted() {
        val params = TileFilter(
            lifecycle = null,
            search = null,
            range = "",
            granularity = "   ",
        ).toQueryParameters()
        assertFalse(params.containsKey("lifecycle"))
        assertFalse(params.containsKey("search"))
        assertFalse(params.containsKey("range"))
        assertFalse(params.containsKey("granularity"))
    }
}