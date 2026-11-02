package com.intercommerce.catalog.domain

import androidx.paging.PagingData
import com.intercommerce.catalog.domain.repository.ProductRepository
import com.intercommerce.catalog.domain.usecase.GetPagedProductsUseCase
import com.intercommerce.catalog.domain.usecase.ObserveProductUseCase
import com.intercommerce.catalog.domain.usecase.RefreshProductUseCase
import com.intercommerce.core.domain.AppError
import com.intercommerce.testing.product
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogUseCasesTest {

    private val repository = mockk<ProductRepository>()

    @Test
    fun `paged products normalizes the query so equal searches share one listing`() {
        every { repository.pagedProducts(any()) } returns flowOf(PagingData.empty())

        GetPagedProductsUseCase(repository)("  PhOne ")
        GetPagedProductsUseCase(repository)("")

        verify { repository.pagedProducts("phone") }
        verify { repository.pagedProducts("") }
    }

    @Test
    fun `observe product emits stored product and null when never stored`() = runTest {
        every { repository.observeProduct(1) } returns flowOf(product(id = 1))
        every { repository.observeProduct(2) } returns flowOf(null)

        assertEquals(1, ObserveProductUseCase(repository)(1).first()!!.id)
        assertNull(ObserveProductUseCase(repository)(2).first())
    }

    @Test(expected = AppError.NotFound::class)
    fun `refresh product propagates controlled errors`() = runTest {
        coEvery { repository.refreshProduct(99) } throws AppError.NotFound()

        RefreshProductUseCase(repository)(99)
    }

    @Test
    fun `refresh product delegates to the repository`() = runTest {
        coEvery { repository.refreshProduct(1) } returns Unit

        RefreshProductUseCase(repository)(1)

        coVerify(exactly = 1) { repository.refreshProduct(1) }
    }
}
