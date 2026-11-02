package com.intercommerce.catalog.data

import com.intercommerce.catalog.data.local.ProductEntity
import com.intercommerce.catalog.data.local.StoredReview
import com.intercommerce.catalog.data.remote.ProductDto
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.model.Review
import com.intercommerce.core.domain.Money

// Remote data never reaches the domain directly: DTO → entity (stored) → domain (observed).

fun ProductDto.toEntity() = ProductEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    brand = brand?.takeIf { it.isNotBlank() },
    priceCents = Money.fromDecimal(price).cents,
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    thumbnail = thumbnail,
    images = images,
    tags = tags,
    availabilityStatus = availabilityStatus,
    warrantyInformation = warrantyInformation,
    shippingInformation = shippingInformation,
    returnPolicy = returnPolicy,
    reviews = reviews.map { StoredReview(it.rating, it.comment, it.date, it.reviewerName) },
)

fun ProductEntity.toDomain() = Product(
    id = id,
    title = title,
    description = description,
    category = category,
    brand = brand,
    price = Money(priceCents),
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    thumbnail = thumbnail,
    images = images.ifEmpty { listOfNotNull(thumbnail.takeIf { it.isNotBlank() }) },
    tags = tags,
    availabilityStatus = availabilityStatus,
    warrantyInformation = warrantyInformation,
    shippingInformation = shippingInformation,
    returnPolicy = returnPolicy,
    reviews = reviews.map { Review(it.rating, it.comment, it.date, it.reviewerName) },
)
