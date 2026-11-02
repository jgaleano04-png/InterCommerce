package com.intercommerce.cart.presentation

import app.cash.turbine.test
import com.intercommerce.cart.data.CartRepositoryImpl
import com.intercommerce.cart.domain.usecase.AddToCartUseCase
import com.intercommerce.cart.domain.usecase.CalculateCartTotalsUseCase
import com.intercommerce.cart.domain.usecase.ChangeCartItemQuantityUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.cart.domain.usecase.RemoveFromCartUseCase
import com.intercommerce.core.data.database.AppDatabase
import com.intercommerce.core.domain.Money
import com.intercommerce.testing.MainDispatcherRule
import com.intercommerce.testing.inMemoryDatabase
import com.intercommerce.testing.product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration: ViewModel → use cases → repository → real Room. No mocks, so the test proves that
 * what the screen shows is what is persisted, and that a new ViewModel (process restart) reads it back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CartViewModelTest {

    @get:Rule val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var repository: CartRepositoryImpl

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = CartRepositoryImpl(db.cartDao())
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel() = CartViewModel(
        ObserveCartUseCase(repository, CalculateCartTotalsUseCase()),
        ChangeCartItemQuantityUseCase(repository),
        RemoveFromCartUseCase(repository),
    )

    private suspend fun app.cash.turbine.ReceiveTurbine<CartUiState>.awaitContent(): CartUiState.Content {
        var state = awaitItem()
        while (state !is CartUiState.Content) state = awaitItem()
        return state
    }

    @Test
    fun `starts loading then shows an empty cart`() = runTest {
        viewModel().uiState.test {
            assertEquals(CartUiState.Loading, awaitItem())
            assertTrue(awaitContent().cart.isEmpty)
        }
    }

    @Test
    fun `increase decrease and remove update the observed totals`() = runTest {
        AddToCartUseCase(repository)(product(id = 1, price = 10.0, discountPercentage = 0.0, stock = 3))
        val vm = viewModel()

        vm.uiState.test {
            val item = awaitContent().cart.lines.single().item
            vm.increase(item)
            assertEquals(Money(2380), awaitContent().cart.total) // 2 × 10.00 + 19 % IVA

            vm.decrease(item.copy(quantity = 2))
            assertEquals(1, awaitContent().cart.itemCount)

            vm.remove(item)
            assertTrue(awaitContent().cart.isEmpty)
        }
    }

    @Test
    fun `quantity never exceeds stock or drops below one`() = runTest {
        AddToCartUseCase(repository)(product(id = 1, stock = 1))
        val vm = viewModel()

        vm.uiState.test {
            val item = awaitContent().cart.lines.single().item
            vm.increase(item)
            vm.decrease(item)
            expectNoEvents()
            assertEquals(1, (vm.uiState.value as CartUiState.Content).cart.itemCount)
        }
    }

    @Test
    fun `a new ViewModel reads back the persisted cart`() = runTest {
        AddToCartUseCase(repository)(product(id = 1, stock = 5))
        AddToCartUseCase(repository)(product(id = 1, stock = 5))
        AddToCartUseCase(repository)(product(id = 2, stock = 5))

        viewModel().uiState.test {
            val cart = awaitContent().cart
            assertEquals(3, cart.itemCount)
            assertEquals(listOf(1, 2), cart.lines.map { it.item.productId })
        }
    }
}
