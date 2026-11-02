package com.intercommerce.productdetail.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.intercommerce.cart.domain.usecase.AddToCartResult
import com.intercommerce.cart.domain.usecase.AddToCartUseCase
import com.intercommerce.cart.domain.usecase.CalculateCartTotalsUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.usecase.ObserveProductUseCase
import com.intercommerce.catalog.domain.usecase.RefreshProductUseCase
import com.intercommerce.core.domain.AppError
import com.intercommerce.testing.MainDispatcherRule
import com.intercommerce.testing.product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric because type-safe `toRoute()` decodes the route through Android's Bundle. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProductDetailViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val stored = MutableStateFlow<Product?>(null)
    private val observeProduct = mockk<ObserveProductUseCase>()
    private val refreshProduct = mockk<RefreshProductUseCase>()
    private val addToCart = mockk<AddToCartUseCase>()
    private val observeCart = mockk<ObserveCartUseCase>()

    @Before
    fun setUp() {
        every { observeProduct(PRODUCT_ID) } returns stored
        every { observeCart() } returns flowOf(CalculateCartTotalsUseCase()(emptyList()))
    }

    private fun viewModel() = ProductDetailViewModel(
        SavedStateHandle(mapOf("productId" to PRODUCT_ID)),
        observeProduct,
        observeCart,
        refreshProduct,
        addToCart,
    )

    private fun ProductDetailViewModel.collectState() =
        kotlinx.coroutines.CoroutineScope(main.dispatcher).launch { uiState.collect {} }

    @Test
    fun `starts loading and shows the product once refreshed`() = runTest(main.dispatcher) {
        coEvery { refreshProduct(PRODUCT_ID) } coAnswers { stored.value = product(id = PRODUCT_ID) }
        val vm = viewModel()
        val job = vm.collectState()

        assertTrue(vm.uiState.value.isRefreshing)
        assertNull(vm.uiState.value.product)
        advanceUntilIdle()

        assertEquals(PRODUCT_ID, vm.uiState.value.product?.id)
        assertFalse(vm.uiState.value.isRefreshing)
        assertNull(vm.uiState.value.error)
        job.cancel()
    }

    @Test
    fun `network failure with a stored product keeps it on screen and exposes the error`() = runTest(main.dispatcher) {
        stored.value = product(id = PRODUCT_ID)
        coEvery { refreshProduct(PRODUCT_ID) } throws AppError.NoConnection()
        val vm = viewModel()
        val job = vm.collectState()
        advanceUntilIdle()

        assertEquals(PRODUCT_ID, vm.uiState.value.product?.id)
        assertTrue(vm.uiState.value.error is AppError.NoConnection)
        job.cancel()
    }

    @Test
    fun `failure without a stored product leaves only the error`() = runTest(main.dispatcher) {
        coEvery { refreshProduct(PRODUCT_ID) } throws AppError.NotFound()
        val vm = viewModel()
        val job = vm.collectState()
        advanceUntilIdle()

        assertNull(vm.uiState.value.product)
        assertTrue(vm.uiState.value.error is AppError.NotFound)
        job.cancel()
    }

    @Test
    fun `retry after reconnection clears the error`() = runTest(main.dispatcher) {
        coEvery { refreshProduct(PRODUCT_ID) } throws AppError.NoConnection()
        val vm = viewModel()
        val job = vm.collectState()
        advanceUntilIdle()

        coEvery { refreshProduct(PRODUCT_ID) } coAnswers { stored.value = product(id = PRODUCT_ID) }
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        coVerify(exactly = 2) { refreshProduct(PRODUCT_ID) }
        job.cancel()
    }

    @Test
    fun `add to cart emits the domain result as a one-shot event`() = runTest(main.dispatcher) {
        stored.value = product(id = PRODUCT_ID)
        coEvery { refreshProduct(PRODUCT_ID) } returns Unit
        coEvery { addToCart(any()) } returnsMany listOf(AddToCartResult.ADDED, AddToCartResult.LIMIT_REACHED)
        val vm = viewModel()
        val job = vm.collectState()
        advanceUntilIdle()

        vm.events.test {
            vm.onAddToCart()
            assertEquals(AddToCartResult.ADDED, awaitItem())
            vm.onAddToCart()
            assertEquals(AddToCartResult.LIMIT_REACHED, awaitItem())
        }
        job.cancel()
    }

    private companion object {
        const val PRODUCT_ID = 5
    }
}
