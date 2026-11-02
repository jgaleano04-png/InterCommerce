package com.intercommerce.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.intercommerce.core.domain.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        // Tracks every validated network so losing Wi-Fi while mobile data is up is not reported as offline.
        val validNetworks = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    validNetworks += network
                } else {
                    validNetworks -= network
                }
                trySend(validNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                validNetworks -= network
                trySend(validNetworks.isNotEmpty())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        trySend(isCurrentlyOnline())
        connectivityManager.registerNetworkCallback(request, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()

    private fun isCurrentlyOnline(): Boolean =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
