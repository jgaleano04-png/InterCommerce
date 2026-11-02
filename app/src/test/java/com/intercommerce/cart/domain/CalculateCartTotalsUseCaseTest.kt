package com.intercommerce.cart.domain

import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.usecase.CalculateCartTotalsUseCase
import com.intercommerce.core.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateCartTotalsUseCaseTest {

    private val calculate = CalculateCartTotalsUseCase()

    private fun item(id: Int, cents: Long, discount: Double, quantity: Int) =
        CartItem(id, "P$id", "", Money(cents), discount, quantity, maxQuantity = 99)

    @Test
    fun `line equals the discounted unit price the catalog shows times quantity`() {
        // 9.99 with 10.48 % -> 8.94 per unit -> 17.88 for two (not 17.89 from rounding the line).
        val cart = calculate(listOf(item(1, cents = 999, discount = 10.48, quantity = 2)))

        assertEquals(Money(894), cart.lines.single().unitNet)
        assertEquals(Money(1788), cart.lines.single().net)
    }

    @Test
    fun `totals add discount and 19 percent tax over the discounted subtotal`() {
        val cart = calculate(
            listOf(
                item(1, cents = 999, discount = 10.48, quantity = 2), // 19.98 - 2.10
                item(2, cents = 1999, discount = 0.0, quantity = 1), // 19.99
            ),
        )

        assertEquals(Money(3997), cart.subtotal)
        assertEquals(Money(210), cart.discount)
        // (39.97 - 2.10) * 0.19 = 7.1953 -> 7.20
        assertEquals(Money(720), cart.tax)
        assertEquals(Money(4507), cart.total)
        assertEquals(3, cart.itemCount)
        assertEquals(cart.total, cart.lines.fold(Money.ZERO) { acc, line -> acc + line.net } + cart.tax)
    }

    @Test
    fun `empty cart is all zeros`() {
        val cart = calculate(emptyList())

        assertTrue(cart.isEmpty)
        assertEquals(Money.ZERO, cart.total)
        assertEquals(0, cart.itemCount)
    }
}
