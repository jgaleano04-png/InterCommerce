package com.intercommerce.catalog.domain.repository

import androidx.paging.PagingData
import com.intercommerce.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Catalog contract. Implementations must expose the local store as the single source of truth:
 * flows emit cached data first and the network only writes into that store.
 *
 * `PagingData` comes from `paging-common`, a pure Kotlin artifact with no Android dependency.
 */
interface ProductRepository {

    /** Paged products; a blank [query] is the full catalog. */
    fun pagedProducts(query: String): Flow<PagingData<Product>>

    /** Emits `null` while the product has never been stored locally. */
    fun observeProduct(id: Int): Flow<Product?>

    /** Fetches the product and stores it. Throws [com.intercommerce.core.domain.AppError] on failure. */
    suspend fun refreshProduct(id: Int)
}
