package com.intercommerce.cart.domain.model

import com.intercommerce.core.domain.Money

/** A product line in the cart. Prices are the snapshot taken when the product was added. */
data class CartItem(
    val productId: Int,
    val title: String,
    val thumbnail: String,
    val unitPrice: Money,
    val discountPercentage: Double,
    val quantity: Int,
    val maxQuantity: Int,
)
