package com.intercommerce.catalog.domain.usecase

import androidx.paging.PagingData
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPagedProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    /** "  Phone " and "phone" are the same search, so they share one cached listing. */
    operator fun invoke(query: String): Flow<PagingData<Product>> = repository.pagedProducts(normalize(query))

    companion object {
        fun normalize(query: String) = query.trim().lowercase()
    }
}

class ObserveProductUseCase @Inject constructor(private val repository: ProductRepository) {
    operator fun invoke(id: Int): Flow<Product?> = repository.observeProduct(id)
}

class RefreshProductUseCase @Inject constructor(private val repository: ProductRepository) {
    /** @throws com.intercommerce.core.domain.AppError when the product could not be fetched. */
    suspend operator fun invoke(id: Int) = repository.refreshProduct(id)
}
