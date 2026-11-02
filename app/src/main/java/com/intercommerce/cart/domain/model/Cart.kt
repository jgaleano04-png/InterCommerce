package com.intercommerce.cart.domain.model

import com.intercommerce.core.domain.Money
import java.math.BigDecimal

data class CartLine(
    val item: CartItem,
    /** Unit price × quantity, before discount. */
    val gross: Money,
    val discount: Money,
) {
    val net: Money get() = gross - discount

    /** Discounted unit price; equals `Product.finalPrice` for the same snapshot. */
    val unitNet: Money get() = item.unitPrice.discountedBy(item.discountPercentage)
}

data class Cart(
    val lines: List<CartLine>,
    val subtotal: Money,
    val discount: Money,
    val tax: Money,
    val total: Money,
    val itemCount: Int,
    val taxRate: BigDecimal,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}
