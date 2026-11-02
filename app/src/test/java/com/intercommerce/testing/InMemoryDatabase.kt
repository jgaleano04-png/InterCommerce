package com.intercommerce.testing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.intercommerce.core.data.database.AppDatabase

fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
