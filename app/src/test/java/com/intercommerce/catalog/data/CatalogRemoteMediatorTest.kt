package com.intercommerce.catalog.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.intercommerce.catalog.data.local.CatalogDao
import com.intercommerce.catalog.data.local.ProductEntity
import com.intercommerce.core.data.database.AppDatabase
import com.intercommerce.core.domain.AppError
import com.intercommerce.testing.inMemoryDatabase
import com.intercommerce.testing.jsonResponse
import com.intercommerce.testing.pageJson
import com.intercommerce.testing.productApi
import com.intercommerce.testing.productEntity
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
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

/** Real Retrofit + real Room; only the server is fake. */
@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class CatalogRemoteMediatorTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase
    private lateinit var dao: CatalogDao
    private val prefetcher = mockk<ThumbnailPrefetcher>(relaxed = true)

    private val state = PagingState<Int, ProductEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 2, initialLoadSize = 4),
        leadingPlaceholderCount = 0,
    )

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        db = inMemoryDatabase()
        dao = db.catalogDao()
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    private fun mediator(query: String = "") =
        CatalogRemoteMediator(query, server.productApi(), dao, prefetcher, Dispatchers.Unconfined)

    private suspend fun storedIds(query: String = ""): List<Int> {
        val result = dao.pagingSource(query).load(PagingSource.LoadParams.Refresh(null, 50, false))
        return (result as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Test
    fun `refresh then append requests the right window and stores both pages in order`() = runTest {
        server.enqueue(jsonResponse(pageJson(listOf(1, 2, 3, 4), skip = 0, total = 6)))
        server.enqueue(jsonResponse(pageJson(listOf(5, 6), skip = 4, total = 6)))

        val refresh = mediator().load(LoadType.REFRESH, state)
        val append = mediator().load(LoadType.APPEND, state)

        assertEquals("/products?limit=4&skip=0", server.takeRequest().path)
        assertEquals("/products?limit=2&skip=4", server.takeRequest().path)
        assertTrue(refresh is MediatorResult.Success && !refresh.endOfPaginationReached)
        assertTrue(append is MediatorResult.Success && append.endOfPaginationReached)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), storedIds())
        verify { prefetcher.prefetch(match { it.size == 4 }) }
    }

    @Test
    fun `search uses the search endpoint`() = runTest {
        server.enqueue(jsonResponse(pageJson(listOf(9), skip = 0, total = 1)))

        mediator(query = "phone").load(LoadType.REFRESH, state)

        assertEquals("/products/search?q=phone&limit=4&skip=0", server.takeRequest().path)
        assertEquals(listOf(9), storedIds("phone"))
    }

    @Test
    fun `server error keeps the cached listing and reports Http`() = runTest {
        dao.savePage("", listOf(productEntity(1), productEntity(2)), 0, 2, isRefresh = true)
        server.enqueue(jsonResponse("{}", code = 500))

        val result = mediator().load(LoadType.REFRESH, state)

        assertEquals(500, ((result as MediatorResult.Error).throwable as AppError.Http).code)
        assertEquals(listOf(1, 2), storedIds())
    }

    @Test
    fun `malformed payload keeps the cached listing and reports Serialization`() = runTest {
        dao.savePage("", listOf(productEntity(1)), 0, 1, isRefresh = true)
        server.enqueue(jsonResponse("""{"products":[{"id":"not-a-number"}]}"""))

        val result = mediator().load(LoadType.REFRESH, state)

        assertTrue((result as MediatorResult.Error).throwable is AppError.Serialization)
        assertEquals(listOf(1), storedIds())
    }

    @Test
    fun `no response becomes Timeout`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        val result = mediator().load(LoadType.REFRESH, state)

        assertTrue((result as MediatorResult.Error).throwable is AppError.Timeout)
    }

    @Test
    fun `connection dropped mid-response becomes NoConnection and stores nothing`() = runTest {
        server.enqueue(
            jsonResponse(pageJson(listOf(1, 2, 3, 4), skip = 0, total = 6))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )

        val result = mediator().load(LoadType.REFRESH, state)

        assertTrue((result as MediatorResult.Error).throwable is AppError.NoConnection)
        assertEquals(emptyList<Int>(), storedIds())
    }

    @Test
    fun `offline search never run before falls back to matching cached products`() = runTest {
        dao.upsertProducts(listOf(productEntity(1).copy(title = "iPhone 9"), productEntity(2).copy(title = "Lamp")))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = mediator(query = "phone").load(LoadType.REFRESH, state)
        val append = mediator(query = "phone").load(LoadType.APPEND, state)

        assertTrue((result as MediatorResult.Error).throwable is AppError.NoConnection)
        assertEquals(listOf(1), storedIds("phone"))
        // The local listing is final: no remote append is attempted on it.
        assertTrue(append is MediatorResult.Success && append.endOfPaginationReached)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `server error does not trigger the local search fallback`() = runTest {
        dao.upsertProducts(listOf(productEntity(1).copy(title = "iPhone 9")))
        server.enqueue(jsonResponse("{}", code = 503))

        mediator(query = "phone").load(LoadType.REFRESH, state)

        assertEquals(emptyList<Int>(), storedIds("phone"))
    }

    @Test
    fun `successful refresh replaces a locally seeded search with server results`() = runTest {
        dao.upsertProducts(listOf(productEntity(1).copy(title = "iPhone 9")))
        dao.seedSearchFromCache("phone")
        server.enqueue(jsonResponse(pageJson(listOf(7, 8), skip = 0, total = 2)))

        mediator(query = "phone").load(LoadType.REFRESH, state)

        assertEquals(listOf(7, 8), storedIds("phone"))
    }
}
