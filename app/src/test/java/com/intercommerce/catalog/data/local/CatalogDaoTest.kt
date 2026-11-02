package com.intercommerce.catalog.data.local

import androidx.paging.PagingSource
import com.intercommerce.core.data.database.AppDatabase
import com.intercommerce.testing.inMemoryDatabase
import com.intercommerce.testing.productEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CatalogDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.catalogDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun loadIds(query: String): List<Int> {
        val result = dao.pagingSource(query).load(PagingSource.LoadParams.Refresh(null, 50, false))
        return (result as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Test
    fun `appended pages keep remote order and the next key`() = runTest {
        dao.savePage("", listOf(productEntity(3), productEntity(1)), startPosition = 0, nextSkip = 2, isRefresh = true)
        dao.savePage("", listOf(productEntity(2)), startPosition = 2, nextSkip = null, isRefresh = false)

        assertEquals(listOf(3, 1, 2), loadIds(""))
        assertNull(dao.remoteKey("")!!.nextSkip)
    }

    @Test
    fun `refresh replaces listing order but keeps product rows`() = runTest {
        dao.savePage("", listOf(productEntity(1), productEntity(2)), 0, 2, isRefresh = true)
        dao.savePage("", listOf(productEntity(5)), 0, 1, isRefresh = true)

        assertEquals(listOf(5), loadIds(""))
        // Product 1 is no longer listed but stays cached for detail screens.
        assertEquals(1, dao.observeProduct(1).first()!!.id)
    }

    @Test
    fun `a search listing never alters the catalog listing`() = runTest {
        dao.savePage("", listOf(productEntity(1), productEntity(2)), 0, 2, isRefresh = true)
        dao.savePage("phone", listOf(productEntity(2, price = 50.0)), 0, null, isRefresh = true)

        assertEquals(listOf(1, 2), loadIds(""))
        assertEquals(listOf(2), loadIds("phone"))
        assertEquals(5000L, dao.observeProduct(2).first()!!.priceCents)
    }
}
