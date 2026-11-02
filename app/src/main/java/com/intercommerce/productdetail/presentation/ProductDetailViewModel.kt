package com.intercommerce.productdetail.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.intercommerce.cart.domain.usecase.AddToCartResult
import com.intercommerce.cart.domain.usecase.AddToCartUseCase
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.usecase.ObserveProductUseCase
import com.intercommerce.catalog.domain.usecase.RefreshProductUseCase
import com.intercommerce.core.domain.AppError
import com.intercommerce.core.presentation.navigation.ProductDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** Immutable snapshot; `AppError` is never mutated after creation, which Compose can't infer for an Exception. */
@Immutable
data class ProductDetailUiState(
    val product: Product? = null,
    val isRefreshing: Boolean = true,
    /** Last refresh failure. With a stored product it is a banner; without one it is the screen. */
    val error: AppError? = null,
    val quantityInCart: Int = 0,
    val cartItemCount: Int = 0,
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeProduct: ObserveProductUseCase,
    observeCart: ObserveCartUseCase,
    private val refreshProduct: RefreshProductUseCase,
    private val addToCart: AddToCartUseCase,
) : ViewModel() {

    private val productId = savedStateHandle.toRoute<ProductDetailRoute>().productId
    private val refreshState = MutableStateFlow(RefreshState())

    /** One-shot results (snackbar + haptics); buffered so none is lost during a configuration change. */
    private val _events = Channel<AddToCartResult>(Channel.BUFFERED)
    val events: Flow<AddToCartResult> = _events.receiveAsFlow()

    val uiState: StateFlow<ProductDetailUiState> = combine(
        observeProduct(productId),
        observeCart(),
        refreshState,
    ) { product, cart, refresh ->
        ProductDetailUiState(
            product = product,
            isRefreshing = refresh.isRefreshing,
            error = refresh.error,
            quantityInCart = cart.lines.firstOrNull { it.item.productId == productId }?.item?.quantity ?: 0,
            cartItemCount = cart.itemCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProductDetailUiState())

    init {
        refresh()
    }

    /** Stored data is already on screen through [uiState]; this only tries to bring it up to date. */
    fun refresh() {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val error = try {
                refreshProduct(productId)
                null
            } catch (e: AppError) {
                e
            }
            refreshState.update { it.copy(isRefreshing = false, error = error) }
        }
    }

    fun onAddToCart() {
        val product = uiState.value.product ?: return
        viewModelScope.launch { _events.send(addToCart(product)) }
    }

    private data class RefreshState(val isRefreshing: Boolean = false, val error: AppError? = null)
}
