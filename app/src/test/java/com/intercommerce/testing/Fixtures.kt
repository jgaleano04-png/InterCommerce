package com.intercommerce.testing

import com.intercommerce.catalog.data.remote.ProductDto
import com.intercommerce.catalog.data.remote.ReviewDto
import com.intercommerce.catalog.data.toDomain
import com.intercommerce.catalog.data.toEntity

fun productDto(
    id: Int = 1,
    price: Double = 9.99,
    discountPercentage: Double = 10.0,
    stock: Int = 5,
    brand: String? = "Essence",
) = ProductDto(
    id = id,
    title = "Product $id",
    price = price,
    description = "Description $id",
    category = "beauty",
    brand = brand,
    discountPercentage = discountPercentage,
    rating = 4.5,
    stock = stock,
    thumbnail = "https://cdn.dummyjson.com/$id/thumbnail.webp",
    images = listOf("https://cdn.dummyjson.com/$id/1.webp"),
    tags = listOf("beauty"),
    availabilityStatus = "In Stock",
    reviews = listOf(ReviewDto(rating = 5, comment = "Great", date = "2025-01-01", reviewerName = "Ana")),
)

fun productEntity(id: Int = 1, price: Double = 9.99, stock: Int = 5) =
    productDto(id = id, price = price, stock = stock).toEntity()

fun product(id: Int = 1, price: Double = 9.99, discountPercentage: Double = 10.0, stock: Int = 5) =
    productDto(id = id, price = price, discountPercentage = discountPercentage, stock = stock).toEntity().toDomain()
