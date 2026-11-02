package com.intercommerce

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

private const val MEMORY_CACHE_PERCENT = 0.25
private const val DISK_CACHE_BYTES = 100L * 1024 * 1024

@HiltAndroidApp
class InterCommerceApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var okHttpClient: OkHttpClient

    /** Images share the app's OkHttp client and keep explicit memory and disk caches, so they render offline. */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()
}
