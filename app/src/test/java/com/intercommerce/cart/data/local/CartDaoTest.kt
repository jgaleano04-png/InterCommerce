package com.intercommerce.cart.data.local

import com.intercommerce.cart.data.toCartEntity
import com.intercommerce.core.data.database.AppDatabase
import com.intercommerce.testing.inMemoryDatabase
import com.intercommerce.testing.product
import com.intercommerce.testing.productEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CartDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.cartDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun quantityOf(id: Int) = dao.observeAll().first().single { it.productId == id }.quantity

    @Test
    fun `addOrIncrement inserts then increments up to the limit`() = runTest {
        val item = product(id = 1, stock = 2).toCartEntity(addedAt = 0)

        assertTrue(dao.addOrIncrement(item, maxQuantity = 2))
        assertTrue(dao.addOrIncrement(item, maxQuantity = 2))
        assertFalse(dao.addOrIncrement(item, maxQuantity = 2))
        assertEquals(2, quantityOf(1))
    }

    @Test
    fun `changeQuantity refuses to leave bounds`() = runTest {
        dao.insert(product(id = 1).toCartEntity(addedAt = 0))

        assertEquals(0, dao.changeQuantity(1, delta = -1, min = 1, max = 5))
        assertEquals(1, dao.changeQuantity(1, delta = 1, min = 1, max = 5))
        assertEquals(2, quantityOf(1))
    }

    @Test
    fun `catalog sync never touches cart rows`() = runTest {
        dao.insert(product(id = 1, price = 9.99).toCartEntity(addedAt = 0))
        dao.changeQuantity(1, delta = 2, min = 1, max = 5)

        db.catalogDao().savePage("", listOf(productEntity(1, price = 99.0, stock = 0)), 0, null, isRefresh = true)

        val item = dao.observeAll().first().single()
        assertEquals(3, item.quantity)
        assertEquals(999L, item.unitPriceCents)
    }
}
