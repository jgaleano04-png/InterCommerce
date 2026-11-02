package com.intercommerce.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.intercommerce.cart.data.local.CartDao
import com.intercommerce.cart.data.local.CartItemEntity
import com.intercommerce.catalog.data.local.CatalogDao
import com.intercommerce.catalog.data.local.CatalogEntryEntity
import com.intercommerce.catalog.data.local.CatalogRemoteKeyEntity
import com.intercommerce.catalog.data.local.ProductEntity
import com.intercommerce.catalog.data.local.StoredReview
import kotlinx.serialization.json.Json

/**
 * Single SQLite database. Schema is exported to `app/schemas` so future versions ship explicit
 * migrations; destructive fallback is deliberately not enabled because it would wipe the cart.
 */
@Database(
    entities = [ProductEntity::class, CatalogEntryEntity::class, CatalogRemoteKeyEntity::class, CartItemEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun cartDao(): CartDao

    companion object {
        const val NAME = "intercommerce.db"
    }
}

class Converters {
    @TypeConverter
    fun fromStrings(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStrings(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromReviews(value: List<StoredReview>): String = json.encodeToString(value)

    @TypeConverter
    fun toReviews(value: String): List<StoredReview> = json.decodeFromString(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
