package com.intercommerce.core.di

import com.intercommerce.cart.data.CartRepositoryImpl
import com.intercommerce.cart.domain.repository.CartRepository
import com.intercommerce.catalog.data.ProductRepositoryImpl
import com.intercommerce.catalog.domain.repository.ProductRepository
import com.intercommerce.core.data.connectivity.AndroidNetworkMonitor
import com.intercommerce.core.domain.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The only place that knows which implementation backs each domain contract. */
@Module
@InstallIn(SingletonComponent::class)
interface BindingsModule {
    @Binds
    fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
}
