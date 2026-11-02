package com.intercommerce.cart.domain.usecase

import com.intercommerce.cart.domain.model.Cart
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.model.CartLine
import com.intercommerce.core.domain.Money
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Pricing rules:
 * 1. The discount is computed per unit (`discountPercentage` of the unit price, rounded half-up
 *    to the cent) and multiplied by quantity, so a line always equals the unit price the user saw
 *    in the catalog × quantity.
 * 2. Tax (IVA) applies to the discounted subtotal, rounded half-up once.
 * 3. Totals are sums of the rounded lines, so what is shown adds up exactly.
 */
class CalculateCartTotalsUseCase @Inject constructor() {

    operator fun invoke(items: List<CartItem>): Cart {
        val lines = items.map { item ->
            val unitDiscount = item.unitPrice.percentOf(Money.percentageToRate(item.discountPercentage))
            CartLine(item, gross = item.unitPrice * item.quantity, discount = unitDiscount * item.quantity)
        }
        val subtotal = lines.fold(Money.ZERO) { acc, line -> acc + line.gross }
        val discount = lines.fold(Money.ZERO) { acc, line -> acc + line.discount }
        val taxable = subtotal - discount
        val tax = taxable.percentOf(TAX_RATE)
        return Cart(
            lines = lines,
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            total = taxable + tax,
            itemCount = items.sumOf { it.quantity },
            taxRate = TAX_RATE,
        )
    }

    companion object {
        /** Colombian IVA general rate. */
        val TAX_RATE: BigDecimal = BigDecimal("0.19")
    }
}
