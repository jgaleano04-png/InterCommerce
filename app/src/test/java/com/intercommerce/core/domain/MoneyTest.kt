package com.intercommerce.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `fromDecimal converts without binary drift`() {
        assertEquals(999L, Money.fromDecimal(9.99).cents)
        assertEquals(1_899_999L, Money.fromDecimal(18999.99).cents)
        assertEquals(30L, Money.fromDecimal(0.1 + 0.2).cents)
    }

    @Test
    fun `discountedBy applies the percentage rounding half up`() {
        // 9.99 * 10.48 % = 1.046952 -> 1.05
        assertEquals(Money(894), Money(999).discountedBy(10.48))
        assertEquals(Money(999), Money(999).discountedBy(0.0))
    }

    @Test
    fun `percentOf rounds half up to the cent`() {
        assertEquals(Money(19), Money(100).percentOf(BigDecimal("0.19")))
        assertEquals(Money(1), Money(5).percentOf(BigDecimal("0.10")))
    }
}
