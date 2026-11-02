package com.intercommerce.catalog.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Cached catalog data. Shared by every listing (full catalog and each search). Never referenced by the cart. */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val brand: String?,
    val priceCents: Long,
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
    val reviews: List<StoredReview>,
)

/** Reviews are only ever read together with their product, so they are stored inline as JSON. */
@Serializable
data class StoredReview(
    val rating: Int,
    val comment: String,
    val date: String,
    val reviewerName: String,
)

/**
 * Membership and order of a product inside one listing. `query = ""` is the full catalog.
 * Keeping order per query lets a search refresh without reshuffling the catalog, and vice versa.
 */
@Entity(
    tableName = "catalog_entries",
    primaryKeys = ["query", "productId"],
    indices = [Index(value = ["query", "position"])],
)
data class CatalogEntryEntity(
    val query: String,
    val productId: Int,
    val position: Int,
)

/** Where the next remote page of a listing starts; `nextSkip = null` means the end was reached. */
@Entity(tableName = "catalog_remote_keys")
data class CatalogRemoteKeyEntity(
    @PrimaryKey val query: String,
    val nextSkip: Int?,
)
