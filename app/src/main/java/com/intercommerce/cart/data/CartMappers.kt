package com.intercommerce.cart.data

import com.intercommerce.cart.data.local.CartItemEntity
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.core.domain.Money

fun Product.toCartEntity(addedAt: Long) = CartItemEntity(
    productId = id,
    title = title,
    thumbnail = thumbnail,
    unitPriceCents = price.cents,
    discountPercentage = discountPercentage,
    quantity = 1,
    maxQuantity = stock,
    addedAt = addedAt,
)

fun CartItemEntity.toDomain() = CartItem(
    productId = productId,
    title = title,
    thumbnail = thumbnail,
    unitPrice = Money(unitPriceCents),
    discountPercentage = discountPercentage,
    quantity = quantity,
    maxQuantity = maxQuantity,
)
