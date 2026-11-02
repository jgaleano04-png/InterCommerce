package com.intercommerce.catalog.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CatalogDao {

    @Query(
        """
        SELECT p.* FROM catalog_entries e
        INNER JOIN products p ON p.id = e.productId
        WHERE e.query = :query
        ORDER BY e.position
        """,
    )
    abstract fun pagingSource(query: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract fun observeProduct(id: Int): Flow<ProductEntity?>

    @Query("SELECT * FROM catalog_remote_keys WHERE query = :query")
    abstract suspend fun remoteKey(query: String): CatalogRemoteKeyEntity?

    @Query("SELECT COUNT(*) FROM catalog_entries WHERE query = :query")
    abstract suspend fun entryCount(query: String): Int

    @Upsert
    abstract suspend fun upsertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEntries(entries: List<CatalogEntryEntity>)

    @Upsert
    abstract suspend fun upsertRemoteKey(key: CatalogRemoteKeyEntity)

    @Query("DELETE FROM catalog_entries WHERE query = :query")
    abstract suspend fun clearEntries(query: String)

    /**
     * Writes one remote page atomically: products, their positions in the listing and the next key.
     * Observers never see products without positions or a key pointing past the stored data.
     * On refresh the listing order is replaced, but product rows are upserted, never deleted.
     */
    @Transaction
    open suspend fun savePage(
        query: String,
        products: List<ProductEntity>,
        startPosition: Int,
        nextSkip: Int?,
        isRefresh: Boolean,
    ) {
        if (isRefresh) clearEntries(query)
        upsertProducts(products)
        insertEntries(
            products.mapIndexed { index, product ->
                CatalogEntryEntity(query = query, productId = product.id, position = startPosition + index)
            },
        )
        upsertRemoteKey(CatalogRemoteKeyEntity(query = query, nextSkip = nextSkip))
    }

    @Query(
        """
        SELECT id FROM products
        WHERE title LIKE '%' || :pattern || '%' ESCAPE '\'
           OR description LIKE '%' || :pattern || '%' ESCAPE '\'
           OR brand LIKE '%' || :pattern || '%' ESCAPE '\'
           OR category LIKE '%' || :pattern || '%' ESCAPE '\'
        ORDER BY title
        """,
    )
    abstract suspend fun searchCachedIds(pattern: String): List<Int>

    /**
     * Offline fallback for a search that was never run online: lists the cached products that match.
     * The key is marked as "end reached" so no remote append is attempted on a local listing; the
     * next successful refresh replaces it with the server's results.
     */
    @Transaction
    open suspend fun seedSearchFromCache(query: String) {
        if (entryCount(query) > 0) return
        val pattern = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        insertEntries(
            searchCachedIds(pattern).mapIndexed { index, id ->
                CatalogEntryEntity(query = query, productId = id, position = index)
            },
        )
        upsertRemoteKey(CatalogRemoteKeyEntity(query = query, nextSkip = null))
    }
}
