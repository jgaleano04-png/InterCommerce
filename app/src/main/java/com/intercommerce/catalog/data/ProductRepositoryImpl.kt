package com.intercommerce.catalog.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.intercommerce.catalog.data.local.CatalogDao
import com.intercommerce.catalog.data.remote.ProductApi
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.repository.ProductRepository
import com.intercommerce.core.data.network.safeApiCall
import com.intercommerce.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Page size of the test's reference URL (`limit=10`). */
private const val PAGE_SIZE = 10
private const val PREFETCH_DISTANCE = 4

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApi,
    private val dao: CatalogDao,
    private val thumbnailPrefetcher: ThumbnailPrefetcher,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProductRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedProducts(query: String): Flow<PagingData<Product>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE * 2,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        remoteMediator = CatalogRemoteMediator(query, api, dao, thumbnailPrefetcher, ioDispatcher),
        pagingSourceFactory = { dao.pagingSource(query) },
    ).flow.map { page -> page.map { it.toDomain() } }

    override fun observeProduct(id: Int): Flow<Product?> =
        dao.observeProduct(id).distinctUntilChanged().map { it?.toDomain() }

    override suspend fun refreshProduct(id: Int) {
        val product = withContext(ioDispatcher) { safeApiCall { api.getProduct(id) }.toEntity() }
        dao.upsertProducts(listOf(product))
    }
}
