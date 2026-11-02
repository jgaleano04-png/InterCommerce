package com.intercommerce.cart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Transactional user data. Holds a snapshot of the product taken when it was added and has no
 * foreign key to `products`: catalog refreshes can never cascade into, overwrite or delete it.
 */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val title: String,
    val thumbnail: String,
    val unitPriceCents: Long,
    val discountPercentage: Double,
    val quantity: Int,
    val maxQuantity: Int,
    val addedAt: Long,
)
