package app.tastile.android.ui.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointsCatalogTest {

    @Test
    fun `catalog has 22 endpoints`() {
        assertEquals(22, EndpointsCatalog.entries.size)
    }

    @Test
    fun `every endpoint has a unique operationId`() {
        val ids = EndpointsCatalog.entries.map { it.operationId }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every endpoint has non-empty label and operationId`() {
        EndpointsCatalog.entries.forEach { e ->
            assertTrue(e.label.isNotBlank())
            assertTrue(e.operationId.isNotBlank())
        }
    }
}
