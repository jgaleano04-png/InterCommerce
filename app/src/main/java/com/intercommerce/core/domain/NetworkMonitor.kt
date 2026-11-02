package com.intercommerce.core.domain

import kotlinx.coroutines.flow.Flow

/** Connectivity as seen by the app. Implemented on top of the Android framework in the data layer. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
