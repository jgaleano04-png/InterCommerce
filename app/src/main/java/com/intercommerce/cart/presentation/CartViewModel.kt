package com.intercommerce.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intercommerce.cart.domain.model.Cart
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.usecase.ChangeCartItemQuantityUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.cart.domain.usecase.RemoveFromCartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

sealed interface CartUiState {
    data object Loading : CartUiState
    data class Content(val cart: Cart) : CartUiState
}

/**
 * Holds no cart copy of its own: every action writes to Room and the new state arrives through
 * [ObserveCartUseCase], so what is shown is always what is persisted.
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    observeCart: ObserveCartUseCase,
    private val changeQuantity: ChangeCartItemQuantityUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = observeCart()
        .map<Cart, CartUiState> { CartUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CartUiState.Loading)

    fun increase(item: CartItem) {
        viewModelScope.launch { changeQuantity(item, delta = 1) }
    }

    fun decrease(item: CartItem) {
        viewModelScope.launch { changeQuantity(item, delta = -1) }
    }

    fun remove(item: CartItem) {
        viewModelScope.launch { removeFromCart(item.productId) }
    }
}
