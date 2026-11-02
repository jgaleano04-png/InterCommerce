package com.intercommerce.cart.domain

import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.repository.CartRepository
import com.intercommerce.cart.domain.usecase.AddToCartResult
import com.intercommerce.cart.domain.usecase.AddToCartUseCase
import com.intercommerce.cart.domain.usecase.CalculateCartTotalsUseCase
import com.intercommerce.cart.domain.usecase.ChangeCartItemQuantityUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.cart.domain.usecase.RemoveFromCartUseCase
import com.intercommerce.core.domain.Money
import com.intercommerce.testing.product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CartUseCasesTest {

    private val repository = mockk<CartRepository>(relaxUnitFun = true)
    private val item = CartItem(1, "P1", "", Money(1000), 0.0, quantity = 2, maxQuantity = 3)

    @Test
    fun `add uses the product stock as the limit`() = runTest {
        val product = product(id = 1, stock = 4)
        coEvery { repository.addOrIncrement(product, 4) } returns true

        assertEquals(AddToCartResult.ADDED, AddToCartUseCase(repository)(product))
    }

    @Test
    fun `add reports the limit when the repository refuses another unit`() = runTest {
        val product = product(id = 1, stock = 2)
        coEvery { repository.addOrIncrement(product, 2) } returns false

        assertEquals(AddToCartResult.LIMIT_REACHED, AddToCartUseCase(repository)(product))
    }

    @Test
    fun `out of stock product never reaches the repository`() = runTest {
        assertEquals(AddToCartResult.OUT_OF_STOCK, AddToCartUseCase(repository)(product(stock = 0)))
        coVerify(exactly = 0) { repository.addOrIncrement(any(), any()) }
    }

    @Test
    fun `quantity changes are bounded between one and the stored stock`() = runTest {
        coEvery { repository.changeQuantity(1, 1, 1, 3) } returns true
        coEvery { repository.changeQuantity(1, -2, 1, 3) } returns false

        val change = ChangeCartItemQuantityUseCase(repository)
        change(item, delta = 1)
        assertFalse(change(item, delta = -2))

        coVerify { repository.changeQuantity(1, 1, min = 1, max = 3) }
    }

    @Test
    fun `remove deletes by product id`() = runTest {
        RemoveFromCartUseCase(repository)(7)

        coVerify { repository.remove(7) }
    }

    @Test
    fun `observe cart prices whatever the repository emits`() = runTest {
        every { repository.observeItems() } returns flowOf(listOf(item))

        val cart = ObserveCartUseCase(repository, CalculateCartTotalsUseCase())().first()

        assertEquals(Money(2000), cart.subtotal)
        assertEquals(Money(380), cart.tax)
        assertEquals(Money(2380), cart.total)
    }
}
