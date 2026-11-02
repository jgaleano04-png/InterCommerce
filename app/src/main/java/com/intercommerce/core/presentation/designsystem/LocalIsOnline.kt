package com.intercommerce.core.presentation.designsystem

import androidx.compose.runtime.staticCompositionLocalOf

/** App-wide connectivity, provided once at the root so leaf components can react to reconnection. */
val LocalIsOnline = staticCompositionLocalOf { true }
