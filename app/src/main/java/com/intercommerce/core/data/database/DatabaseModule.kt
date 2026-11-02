package com.intercommerce.core.data.database

import android.content.Context
import androidx.room.Room
import com.intercommerce.cart.data.local.CartDao
import com.intercommerce.catalog.data.local.CatalogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideCatalogDao(database: AppDatabase): CatalogDao = database.catalogDao()

    @Provides
    fun provideCartDao(database: AppDatabase): CartDao = database.cartDao()
}
