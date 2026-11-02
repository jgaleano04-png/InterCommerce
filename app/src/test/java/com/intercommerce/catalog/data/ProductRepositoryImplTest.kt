package com.intercommerce.catalog.data

import com.intercommerce.core.data.database.AppDatabase
import com.intercommerce.core.domain.AppError
import com.intercommerce.core.domain.Money
import com.intercommerce.testing.inMemoryDatabase
import com.intercommerce.testing.jsonResponse
import com.intercommerce.testing.productApi
import com.intercommerce.testing.productEntity
import com.intercommerce.testing.productJson
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var repository: ProductRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        db = inMemoryDatabase()
        repository = ProductRepositoryImpl(server.productApi(), db.catalogDao(), mockk(relaxed = true), Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun `refresh stores the product and observers receive it`() = runTest {
        server.enqueue(jsonResponse(productJson(id = 3, price = 19.99)))

        repository.refreshProduct(3)

        assertEquals("/products/3", server.takeRequest().path)
        assertEquals(Money(1999), repository.observeProduct(3).first()!!.price)
    }

    @Test
    fun `failed refresh keeps the stored product readable`() = runTest {
        db.catalogDao().upsertProducts(listOf(productEntity(3, price = 5.0)))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = runCatching { repository.refreshProduct(3) }.exceptionOrNull()

        assertTrue(error is AppError.NoConnection)
        assertEquals(Money(500), repository.observeProduct(3).first()!!.price)
    }

    @Test
    fun `unknown product maps 404 to NotFound`() = runTest {
        server.enqueue(jsonResponse("""{"message":"Product with id '999' not found"}""", code = 404))

        val error = runCatching { repository.refreshProduct(999) }.exceptionOrNull()

        assertTrue(error is AppError.NotFound)
    }
}
