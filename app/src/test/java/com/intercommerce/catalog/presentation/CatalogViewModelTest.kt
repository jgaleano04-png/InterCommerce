package com.intercommerce.catalog.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import com.intercommerce.cart.domain.usecase.CalculateCartTotalsUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.catalog.domain.usecase.GetPagedProductsUseCase
import com.intercommerce.core.domain.Money
import com.intercommerce.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val getPagedProducts = mockk<GetPagedProductsUseCase>()
    private val observeCart = mockk<ObserveCartUseCase>()

    @Before
    fun setUp() {
        every { getPagedProducts(any()) } returns flowOf(PagingData.empty())
        val items = listOf(CartItem(1, "A", "", Money(100), 0.0, quantity = 2, maxQuantity = 5))
        every { observeCart() } returns flowOf(CalculateCartTotalsUseCase()(items))
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CatalogViewModel(handle, getPagedProducts, observeCart)

    @Test
    fun `catalog loads immediately without debounce`() = runTest(main.dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.products.collect {} }
        advanceUntilIdle()

        verify(exactly = 1) { getPagedProducts("") }
    }

    @Test
    fun `typing is debounced and equivalent queries start a single search`() = runTest(main.dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.products.collect {} }
        advanceUntilIdle()

        vm.onQueryChange("P")
        advanceTimeBy(100)
        vm.onQueryChange("Phone ")
        advanceTimeBy(100)
        vm.onQueryChange("phone")
        advanceUntilIdle()
        vm.onQueryChange("PHONE")
        advanceUntilIdle()

        verify(exactly = 0) { getPagedProducts("p") }
        verify(exactly = 1) { getPagedProducts("phone") }
    }

    @Test
    fun `query survives process death through SavedStateHandle`() = runTest(main.dispatcher) {
        val vm = viewModel(SavedStateHandle(mapOf("query" to "lamp")))
        backgroundScope.launch { vm.products.collect {} }
        advanceUntilIdle()

        assertEquals("lamp", vm.query.value)
        verify { getPagedProducts("lamp") }
    }

    @Test
    fun `cart badge reflects the persisted cart`() = runTest(main.dispatcher) {
        viewModel().cartItemCount.test {
            assertEquals(0, awaitItem())
            assertEquals(2, awaitItem())
        }
    }
}
