package com.intercommerce.cart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Quantity changes are relative (`quantity + delta`) and bounded in the same statement, so two
 * fast taps can't read the same old value and lose an increment. The bounds come from the domain.
 */
@Dao
abstract class CartDao {

    @Query("SELECT * FROM cart_items ORDER BY addedAt")
    abstract fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM cart_items WHERE productId = :productId)")
    abstract suspend fun contains(productId: Int): Boolean

    @Insert
    abstract suspend fun insert(item: CartItemEntity)

    /** @return rows changed: 0 when the result would fall outside [min]..[max]. */
    @Query(
        """
        UPDATE cart_items SET quantity = quantity + :delta
        WHERE productId = :productId AND quantity + :delta BETWEEN :min AND :max
        """,
    )
    abstract suspend fun changeQuantity(productId: Int, delta: Int, min: Int, max: Int): Int

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    abstract suspend fun delete(productId: Int)

    /** Inserts the item or adds one unit to it. @return false when it was already at [maxQuantity]. */
    @Transaction
    open suspend fun addOrIncrement(item: CartItemEntity, maxQuantity: Int): Boolean {
        if (!contains(item.productId)) {
            insert(item)
            return true
        }
        return changeQuantity(item.productId, delta = 1, min = 1, max = maxQuantity) > 0
    }
}
