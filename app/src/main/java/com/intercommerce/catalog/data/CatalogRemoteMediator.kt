package com.intercommerce.catalog.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.intercommerce.catalog.data.local.CatalogDao
import com.intercommerce.catalog.data.local.ProductEntity
import com.intercommerce.catalog.data.remote.ProductApi
import com.intercommerce.core.data.network.safeApiCall
import com.intercommerce.core.domain.AppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Fetches pages from DummyJSON into Room. It never hands data to the UI: the UI reads the
 * Room `PagingSource`, which Room invalidates after each write. A failed load leaves the
 * stored listing untouched, so cached products stay on screen.
 *
 * Paging 3 runs at most one load per direction at a time, which rules out duplicated requests.
 */
@OptIn(ExperimentalPagingApi::class)
class CatalogRemoteMediator(
    private val query: String,
    private val api: ProductApi,
    private val dao: CatalogDao,
    private val thumbnailPrefetcher: ThumbnailPrefetcher,
    private val ioDispatcher: CoroutineDispatcher,
) : RemoteMediator<Int, ProductEntity>() {

    /** Cached data is shown immediately while the first page is refreshed from the network. */
    override suspend fun initialize() = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, ProductEntity>): MediatorResult {
        val skip = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val key = dao.remoteKey(query)
                if (key != null && key.nextSkip == null) return MediatorResult.Success(endOfPaginationReached = true)
                key?.nextSkip ?: dao.entryCount(query)
            }
        }
        val limit = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize

        return try {
            val page = withContext(ioDispatcher) {
                safeApiCall {
                    if (query.isEmpty()) api.getProducts(limit, skip) else api.searchProducts(query, limit, skip)
                }
            }
            val entities = page.products.map { it.toEntity() }
            val loaded = skip + page.products.size
            val nextSkip = loaded.takeIf { page.products.isNotEmpty() && it < page.total }
            dao.savePage(
                query = query,
                products = entities,
                startPosition = skip,
                nextSkip = nextSkip,
                isRefresh = loadType == LoadType.REFRESH,
            )
            thumbnailPrefetcher.prefetch(entities.map { it.thumbnail })
            MediatorResult.Success(endOfPaginationReached = nextSkip == null)
        } catch (error: AppError) {
            if (loadType == LoadType.REFRESH && query.isNotEmpty() && error.isConnectivity) {
                dao.seedSearchFromCache(query)
            }
            MediatorResult.Error(error)
        }
    }
}
