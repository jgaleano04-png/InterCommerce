package com.intercommerce.cart.data

import com.intercommerce.cart.data.local.CartDao
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.repository.CartRepository
import com.intercommerce.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room suspend DAOs already run on Room's own background executor, so no dispatcher switch is needed. */
@Singleton
class CartRepositoryImpl @Inject constructor(private val dao: CartDao) : CartRepository {

    override fun observeItems(): Flow<List<CartItem>> = dao.observeAll().map { items -> items.map { it.toDomain() } }

    override suspend fun addOrIncrement(product: Product, maxQuantity: Int): Boolean =
        dao.addOrIncrement(product.toCartEntity(addedAt = System.currentTimeMillis()), maxQuantity)

    override suspend fun changeQuantity(productId: Int, delta: Int, min: Int, max: Int): Boolean =
        dao.changeQuantity(productId, delta, min, max) > 0

    override suspend fun remove(productId: Int) = dao.delete(productId)
}
