package com.intercommerce.testing

import com.intercommerce.catalog.data.remote.ProductApi
import com.intercommerce.core.data.network.NetworkModule
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Same Json configuration as production, pointed at a local server with a short read timeout. */
fun MockWebServer.productApi(): ProductApi = Retrofit.Builder()
    .baseUrl(url("/"))
    .client(OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build())
    .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
    .build()
    .create(ProductApi::class.java)

private val json = Json { encodeDefaults = true }

fun pageJson(ids: List<Int>, skip: Int, total: Int): String =
    """{"products":[${ids.joinToString(",") { productJson(it) }}],"total":$total,"skip":$skip,"limit":${ids.size}}"""

fun productJson(id: Int, price: Double = 9.99): String =
    json.encodeToString(com.intercommerce.catalog.data.remote.ProductDto.serializer(), productDto(id = id, price = price))

fun jsonResponse(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setBody(body)
