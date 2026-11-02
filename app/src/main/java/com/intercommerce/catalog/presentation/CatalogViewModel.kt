package com.intercommerce.catalog.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.intercommerce.cart.domain.usecase.ObserveCartUseCase
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.usecase.GetPagedProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 350L
private const val STOP_TIMEOUT_MS = 5_000L
private const val QUERY_KEY = "query"

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getPagedProducts: GetPagedProductsUseCase,
    observeCart: ObserveCartUseCase,
) : ViewModel() {

    /** Survives process death, so the user returns to the same search. */
    val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")

    /**
     * Typing is debounced and only normalized changes start a new Pager; `flatMapLatest` cancels
     * the previous search's requests, and `cachedIn` keeps pages across configuration changes.
     */
    val products: Flow<PagingData<Product>> = query
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .map(GetPagedProductsUseCase::normalize)
        .distinctUntilChanged()
        .flatMapLatest { getPagedProducts(it) }
        .cachedIn(viewModelScope)

    val cartItemCount: StateFlow<Int> = observeCart()
        .map { it.itemCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    fun onQueryChange(value: String) {
        savedStateHandle[QUERY_KEY] = value
    }
}
