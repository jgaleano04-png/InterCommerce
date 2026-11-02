package com.intercommerce

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intercommerce.core.domain.NetworkMonitor
import com.intercommerce.core.presentation.designsystem.InterCommerceTheme
import com.intercommerce.core.presentation.designsystem.LocalIsOnline
import com.intercommerce.core.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)
            CompositionLocalProvider(LocalIsOnline provides isOnline) {
                InterCommerceTheme { AppNavHost() }
            }
        }
    }
}
