package com.intercommerce.catalog.data

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the thumbnails of every stored page into Coil's disk cache, so products that were
 * fetched but never scrolled into view still show their image offline.
 *
 * Only thumbnails (~8 KB each, ~1.5 MB for the whole catalog) and straight to disk: the memory
 * cache is skipped so prefetching never evicts what is on screen. Gallery images are not
 * prefetched; they are cached when the detail screen shows them.
 */
@Singleton
class ThumbnailPrefetcher @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun prefetch(urls: List<String>) {
        val loader = SingletonImageLoader.get(context)
        urls.filter { it.isNotBlank() }.forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
            )
        }
    }
}
