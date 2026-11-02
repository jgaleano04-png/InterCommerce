package com.intercommerce.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.intercommerce.R
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.core.presentation.designsystem.CartActionButton
import com.intercommerce.core.presentation.designsystem.Dimens
import com.intercommerce.core.presentation.designsystem.LocalIsOnline
import com.intercommerce.core.presentation.designsystem.DiscountBadge
import com.intercommerce.core.presentation.designsystem.MessageState
import com.intercommerce.core.presentation.designsystem.OfflineBanner
import com.intercommerce.core.presentation.designsystem.PriceTag
import com.intercommerce.core.presentation.designsystem.ProductImage
import com.intercommerce.core.presentation.designsystem.RatingLabel
import com.intercommerce.core.presentation.designsystem.shimmer
import com.intercommerce.core.presentation.messageRes
import com.intercommerce.core.presentation.staleDataMessageRes

private const val SKELETON_CELLS = 8
private const val APPEND_SKELETON_CELLS = 2

@Composable
fun CatalogScreen(
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val isOnline = LocalIsOnline.current
    val products = viewModel.products.collectAsLazyPagingItems()

    // Connection back: retry whatever failed. `retry()` is a no-op when nothing failed.
    LaunchedEffect(isOnline) { if (isOnline) products.retry() }

    CatalogContent(
        query = query,
        cartCount = cartCount,
        isOnline = isOnline,
        products = products,
        onQueryChange = viewModel::onQueryChange,
        onProductClick = onProductClick,
        onCartClick = onCartClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogContent(
    query: String,
    cartCount: Int,
    isOnline: Boolean,
    products: LazyPagingItems<Product>,
    onQueryChange: (String) -> Unit,
    onProductClick: (Int) -> Unit,
    onCartClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { CartActionButton(cartCount, onCartClick) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(query, onQueryChange, Modifier.padding(horizontal = Dimens.SpaceL))

            val loadState = products.loadState
            val remoteError = (loadState.mediator?.refresh as? LoadState.Error)?.error
            val isEmpty = products.itemCount == 0
            if (!isEmpty && (remoteError != null || !isOnline)) {
                OfflineBanner(
                    stringResource(staleDataMessageRes(remoteError, isOnline, R.string.showing_saved_data, R.string.update_failed_saved_data)),
                    Modifier.padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS),
                )
            }

            PullToRefreshBox(
                isRefreshing = !isEmpty && loadState.mediator?.refresh is LoadState.Loading,
                onRefresh = products::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    isEmpty && remoteError != null && loadState.source.refresh is LoadState.NotLoading ->
                        MessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(
                                if (query.isBlank()) R.string.catalog_unavailable_title else R.string.search_unavailable_title,
                            ),
                            message = if (query.isBlank()) {
                                stringResource(remoteError.messageRes())
                            } else {
                                stringResource(R.string.search_unavailable_message, query.trim(), stringResource(remoteError.messageRes()))
                            },
                            actionLabel = stringResource(R.string.retry),
                            onAction = products::retry,
                        )

                    isEmpty && loadState.isIdleAndExhausted() ->
                        MessageState(
                            icon = Icons.Outlined.SearchOff,
                            title = stringResource(R.string.no_results_title),
                            message = stringResource(R.string.no_results_message, query.trim()),
                        )

                    isEmpty -> SkeletonGrid()

                    else -> ProductGrid(products, onProductClick)
                }
            }
        }
    }
}

private fun androidx.paging.CombinedLoadStates.isIdleAndExhausted() =
    refresh is LoadState.NotLoading && append.endOfPaginationReached

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

private val gridPadding = PaddingValues(Dimens.SpaceL)

@Composable
private fun ProductGrid(products: LazyPagingItems<Product>, onProductClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimens.GridMinCellWidth),
        state = rememberLazyGridState(),
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            count = products.itemCount,
            key = products.itemKey { it.id },
            contentType = { "product" },
        ) { index ->
            products[index]?.let { ProductCard(it, onClick = { onProductClick(it.id) }) }
        }
        appendFooter(products)
    }
}

private fun LazyGridScope.appendFooter(products: LazyPagingItems<Product>) {
    when (val append = products.loadState.append) {
        is LoadState.Loading -> items(APPEND_SKELETON_CELLS, contentType = { "skeleton" }) { SkeletonCard() }
        is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "error") {
            Column(
                Modifier.fillMaxWidth().padding(Dimens.SpaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(append.error.messageRes()), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = products::retry) { Text(stringResource(R.string.retry)) }
            }
        }
        is LoadState.NotLoading -> Unit
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.SpaceXs / 4),
    ) {
        Box {
            ProductImage(product.thumbnail, contentDescription = product.title, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            if (product.hasDiscount) {
                DiscountBadge(product.discountPercentage, Modifier.align(Alignment.TopStart).padding(Dimens.SpaceS))
            }
        }
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            Text(
                product.brand ?: product.category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            RatingLabel(product.rating)
            PriceTag(product.price, product.finalPrice)
        }
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Dimens.GridMinCellWidth),
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(SKELETON_CELLS) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmer())
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Box(Modifier.fillMaxWidth(0.4f).height(Dimens.SpaceM).shimmer())
            Box(Modifier.fillMaxWidth().height(Dimens.SpaceL).shimmer())
            Box(Modifier.fillMaxWidth(0.6f).height(Dimens.SpaceL).shimmer())
        }
    }
}
