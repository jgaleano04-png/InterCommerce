package com.intercommerce.catalog.data

import com.intercommerce.catalog.data.remote.ProductDto
import com.intercommerce.catalog.data.remote.ProductsPageDto
import com.intercommerce.core.domain.Money
import com.intercommerce.testing.productDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMappersTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `dto to entity to domain keeps every field and stores price in cents`() {
        val product = productDto(price = 9.99, discountPercentage = 10.48).toEntity().toDomain()

        assertEquals(Money(999), product.price)
        assertEquals(Money(894), product.finalPrice)
        assertEquals("Essence", product.brand)
        assertEquals(1, product.reviews.size)
        assertEquals("Ana", product.reviews.first().reviewerName)
        assertTrue(product.hasDiscount)
    }

    @Test
    fun `blank brand becomes null and empty gallery falls back to thumbnail`() {
        val product = productDto(brand = "  ").copy(images = emptyList()).toEntity().toDomain()

        assertNull(product.brand)
        assertEquals(listOf(product.thumbnail), product.images)
    }

    @Test
    fun `real payload without brand and with unknown fields deserializes`() {
        val payload = """
            {"products":[{"id":7,"title":"Apple","price":1.99,"brand":null,"rating":null,
              "dimensions":{"width":1},"meta":{"barcode":"1"}}],"total":194,"skip":0,"limit":1}
        """.trimIndent()

        val page = json.decodeFromString<ProductsPageDto>(payload)
        val dto: ProductDto = page.products.single()

        assertNull(dto.brand)
        assertEquals(0.0, dto.rating, 0.0)
        assertEquals(194, page.total)
    }
}
