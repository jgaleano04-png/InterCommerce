package com.intercommerce.catalog.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** DummyJSON endpoints required by the test. Both list endpoints accept `limit`/`skip`. */
interface ProductApi {

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int,
    ): ProductsPageDto

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("skip") skip: Int,
    ): ProductsPageDto

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDto
}
