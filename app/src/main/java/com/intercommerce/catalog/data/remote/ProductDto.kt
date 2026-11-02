package com.intercommerce.catalog.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProductsPageDto(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0,
)

/**
 * Only `id`, `title` and `price` are treated as mandatory. Everything else has a default because
 * the live API omits fields per product (92 of 194 have no `brand`), and one missing field must
 * not fail a whole page.
 */
@Serializable
data class ProductDto(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String = "",
    val category: String = "",
    val brand: String? = null,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val availabilityStatus: String? = null,
    val warrantyInformation: String? = null,
    val shippingInformation: String? = null,
    val returnPolicy: String? = null,
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
data class ReviewDto(
    val rating: Int = 0,
    val comment: String = "",
    val date: String = "",
    val reviewerName: String = "",
)
