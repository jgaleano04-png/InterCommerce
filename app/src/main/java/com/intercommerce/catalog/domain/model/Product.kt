package com.intercommerce.catalog.domain.model

import com.intercommerce.core.domain.Money

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val brand: String?,
    val price: Money,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val thumbnail: String,
    val images: List<String>,
    val tags: List<String>,
    val availabilityStatus: String?,
    val warrantyInformation: String?,
    val shippingInformation: String?,
    val returnPolicy: String?,
    val reviews: List<Review>,
) {
    val finalPrice: Money get() = price.discountedBy(discountPercentage)
    val hasDiscount: Boolean get() = discountPercentage > 0.0
    val isInStock: Boolean get() = stock > 0
}

data class Review(
    val rating: Int,
    val comment: String,
    val date: String,
    val reviewerName: String,
)
