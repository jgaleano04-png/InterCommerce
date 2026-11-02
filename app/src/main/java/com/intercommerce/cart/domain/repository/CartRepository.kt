package com.intercommerce.cart.domain.repository

import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

/** Local-only store: no operation depends on connectivity. */
interface CartRepository {

    fun observeItems(): Flow<List<CartItem>>

    /** Adds [product] or one more unit of it. @return false when it is already at [maxQuantity]. */
    suspend fun addOrIncrement(product: Product, maxQuantity: Int): Boolean

    /** @return false when the resulting quantity would fall outside [min]..[max]. */
    suspend fun changeQuantity(productId: Int, delta: Int, min: Int, max: Int): Boolean

    suspend fun remove(productId: Int)
}
