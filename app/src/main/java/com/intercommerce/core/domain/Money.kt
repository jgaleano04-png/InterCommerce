package com.intercommerce.core.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Amount in minor units (cents). Integer arithmetic avoids the binary rounding
 * drift that `Double` introduces when summing cart lines.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(cents + other.cents)

    operator fun minus(other: Money) = Money(cents - other.cents)

    operator fun times(quantity: Int) = Money(cents * quantity)

    /** Applies [rate] (e.g. 0.19) rounding half-up to the nearest cent. */
    fun percentOf(rate: BigDecimal): Money =
        Money(BigDecimal.valueOf(cents).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact())

    /** Price after a percentage discount such as DummyJSON's `discountPercentage` (10.48 = 10.48 %). */
    fun discountedBy(percentage: Double): Money = this - percentOf(percentageToRate(percentage))

    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)

        fun fromDecimal(amount: Double): Money =
            Money(BigDecimal(amount.toString()).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact())

        fun percentageToRate(percentage: Double): BigDecimal = BigDecimal(percentage.toString()).movePointLeft(2)
    }
}
