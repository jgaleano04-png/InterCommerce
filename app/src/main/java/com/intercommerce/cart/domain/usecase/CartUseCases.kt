package com.intercommerce.cart.domain.usecase

import com.intercommerce.cart.domain.model.Cart
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.repository.CartRepository
import com.intercommerce.catalog.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val MIN_QUANTITY = 1

class ObserveCartUseCase @Inject constructor(
    private val repository: CartRepository,
    private val calculateTotals: CalculateCartTotalsUseCase,
) {
    operator fun invoke(): Flow<Cart> = repository.observeItems().map(calculateTotals::invoke)
}

enum class AddToCartResult { ADDED, LIMIT_REACHED, OUT_OF_STOCK }

/** The cart can never hold more units than the stock known when the product was added. */
class AddToCartUseCase @Inject constructor(private val repository: CartRepository) {
    suspend operator fun invoke(product: Product): AddToCartResult = when {
        !product.isInStock -> AddToCartResult.OUT_OF_STOCK
        repository.addOrIncrement(product, maxQuantity = product.stock) -> AddToCartResult.ADDED
        else -> AddToCartResult.LIMIT_REACHED
    }
}

/** Moves the quantity by [delta] within 1..stock. Going below 1 is a removal, which is explicit. */
class ChangeCartItemQuantityUseCase @Inject constructor(private val repository: CartRepository) {
    suspend operator fun invoke(item: CartItem, delta: Int): Boolean =
        repository.changeQuantity(item.productId, delta, min = MIN_QUANTITY, max = item.maxQuantity)
}

class RemoveFromCartUseCase @Inject constructor(private val repository: CartRepository) {
    suspend operator fun invoke(productId: Int) = repository.remove(productId)
}
