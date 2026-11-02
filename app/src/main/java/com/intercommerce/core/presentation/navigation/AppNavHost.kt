package com.intercommerce.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intercommerce.cart.presentation.CartScreen
import com.intercommerce.catalog.presentation.CatalogScreen
import com.intercommerce.productdetail.presentation.ProductDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data object CatalogRoute

@Serializable
data class ProductDetailRoute(val productId: Int)

@Serializable
data object CartRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CatalogRoute) {
        composable<CatalogRoute> {
            CatalogScreen(
                onProductClick = { navController.navigate(ProductDetailRoute(it)) },
                onCartClick = { navController.navigate(CartRoute) { launchSingleTop = true } },
            )
        }
        composable<ProductDetailRoute> {
            ProductDetailScreen(
                onBack = navController::navigateUp,
                onCartClick = { navController.navigate(CartRoute) { launchSingleTop = true } },
            )
        }
        composable<CartRoute> {
            CartScreen(
                onBack = navController::navigateUp,
                onProductClick = { navController.navigate(ProductDetailRoute(it)) },
            )
        }
    }
}
