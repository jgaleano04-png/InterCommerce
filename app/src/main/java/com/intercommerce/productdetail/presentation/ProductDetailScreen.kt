package com.intercommerce.productdetail.presentation

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intercommerce.R
import com.intercommerce.cart.domain.usecase.AddToCartResult
import com.intercommerce.catalog.domain.model.Product
import com.intercommerce.catalog.domain.model.Review
import com.intercommerce.core.domain.AppError
import com.intercommerce.core.presentation.designsystem.CartActionButton
import com.intercommerce.core.presentation.designsystem.Dimens
import com.intercommerce.core.presentation.designsystem.DiscountBadge
import com.intercommerce.core.presentation.designsystem.MessageState
import com.intercommerce.core.presentation.designsystem.OfflineBanner
import com.intercommerce.core.presentation.designsystem.PriceTag
import com.intercommerce.core.presentation.designsystem.ProductImage
import com.intercommerce.core.presentation.designsystem.RatingLabel
import com.intercommerce.core.presentation.messageRes
import com.intercommerce.core.presentation.staleDataMessageRes
import com.intercommerce.core.presentation.designsystem.LocalIsOnline

@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    val resources = LocalResources.current
    val isOnline = LocalIsOnline.current

    // Connection back after a failed refresh: try again without asking the user.
    LaunchedEffect(isOnline) { if (isOnline && state.error != null) viewModel.refresh() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { result ->
            view.performAddToCartHaptic(result)
            val message = when (result) {
                AddToCartResult.ADDED -> R.string.added_to_cart
                AddToCartResult.LIMIT_REACHED -> R.string.stock_limit_reached
                AddToCartResult.OUT_OF_STOCK -> R.string.out_of_stock
            }
            snackbarHostState.currentSnackbarData?.dismiss()
            val action = if (result == AddToCartResult.ADDED) resources.getString(R.string.view_cart) else null
            if (snackbarHostState.showSnackbar(resources.getString(message), actionLabel = action) == SnackbarResult.ActionPerformed) {
                onCartClick()
            }
        }
    }

    ProductDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onCartClick = onCartClick,
        onRetry = viewModel::refresh,
        onAddToCart = viewModel::onAddToCart,
    )
}

/** Native haptics through the View: no VIBRATE permission needed, and it respects the user's system setting. */
private fun View.performAddToCartHaptic(result: AddToCartResult) {
    val success = result == AddToCartResult.ADDED
    val constant = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            if (success) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        success -> HapticFeedbackConstants.VIRTUAL_KEY
        else -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailContent(
    state: ProductDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onRetry: () -> Unit,
    onAddToCart: () -> Unit,
) {
    val product = state.product
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.title.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = { CartActionButton(state.cartItemCount, onCartClick) },
            )
        },
        bottomBar = { if (product != null) AddToCartBar(product, state.quantityInCart, onAddToCart) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                product != null -> ProductDetailBody(product, state.error)
                state.isRefreshing -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> MessageState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.product_unavailable_title),
                    message = stringResource((state.error ?: AppError.Unknown()).messageRes()),
                    actionLabel = stringResource(R.string.retry).takeIf { state.error !is AppError.NotFound },
                    onAction = onRetry,
                )
            }
        }
    }
}

/** Compact: gallery above the information. Wide: gallery and scrolling information side by side. */
@Composable
private fun ProductDetailBody(product: Product, error: AppError?) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val banner: @Composable () -> Unit = {
            if (error != null) {
                OfflineBanner(
                    stringResource(
                        staleDataMessageRes(error, LocalIsOnline.current, R.string.showing_saved_product, R.string.update_failed_saved_product),
                    ),
                    Modifier.padding(horizontal = Dimens.SpaceL, vertical = Dimens.SpaceS),
                )
            }
        }
        if (maxWidth >= Dimens.TwoPaneMinWidth) {
            Row(Modifier.fillMaxSize()) {
                Gallery(product, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    banner()
                    ProductInfo(product)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = Dimens.ContentMaxWidth).fillMaxWidth()) {
                    banner()
                    Gallery(product, Modifier.fillMaxWidth().height(Dimens.DetailGalleryHeight))
                    ProductInfo(product)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductInfo(product: Product) {
    Column(Modifier.padding(Dimens.SpaceL), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        Text(
            listOfNotNull(product.brand, product.category.replaceFirstChar { it.uppercase() }).joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(product.title, style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            RatingLabel(product.rating)
            Text(
                if (product.isInStock) pluralStringResource(R.plurals.stock_units, product.stock, product.stock) else stringResource(R.string.out_of_stock),
                style = MaterialTheme.typography.labelMedium,
                color = if (product.isInStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            PriceTag(product.price, product.finalPrice, emphasized = true)
            if (product.hasDiscount) DiscountBadge(product.discountPercentage)
        }
        Text(product.description, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        InfoRow(Icons.Outlined.VerifiedUser, product.warrantyInformation)
        InfoRow(Icons.Outlined.LocalShipping, product.shippingInformation)
        InfoRow(Icons.Outlined.Replay, product.returnPolicy)
        InfoRow(Icons.Outlined.Inventory2, product.availabilityStatus)
        if (product.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                product.tags.forEach { SuggestionChip(onClick = {}, label = { Text(it) }, enabled = false) }
            }
        }
        if (product.reviews.isNotEmpty()) {
            HorizontalDivider()
            Text(stringResource(R.string.reviews_title, product.reviews.size), style = MaterialTheme.typography.titleMedium)
            product.reviews.forEach { ReviewItem(it) }
        }
        Spacer(Modifier.height(Dimens.SpaceL))
    }
}

@Composable
private fun Gallery(product: Product, modifier: Modifier) {
    val pagerState = rememberPagerState { product.images.size }
    Box(modifier) {
        HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ProductImage(
                url = product.images[page],
                contentDescription = stringResource(R.string.image_of, product.title, page + 1),
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (product.images.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(Dimens.SpaceM),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
            ) {
                repeat(product.images.size) { index ->
                    val color = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(Modifier.size(Dimens.SpaceS).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String?) {
    if (text.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS), verticalAlignment = Alignment.CenterVertically) {
            Text(review.reviewerName, style = MaterialTheme.typography.labelLarge)
            RatingLabel(review.rating.toDouble())
        }
        Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddToCartBar(product: Product, quantityInCart: Int, onAddToCart: () -> Unit) {
    Surface(tonalElevation = Dimens.SpaceXs, shadowElevation = Dimens.SpaceS) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(Dimens.SpaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
        ) {
            PriceTag(product.price, product.finalPrice, Modifier.weight(1f))
            Button(onClick = onAddToCart, enabled = product.isInStock) {
                Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.size(Dimens.SpaceS))
                AnimatedContent(targetState = quantityInCart, label = "cartQuantity") { quantity ->
                    Text(
                        if (quantity == 0) stringResource(R.string.add_to_cart)
                        else stringResource(R.string.add_another, quantity),
                    )
                }
            }
        }
    }
}
