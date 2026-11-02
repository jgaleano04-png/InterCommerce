package com.intercommerce.cart.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.intercommerce.R
import com.intercommerce.cart.domain.model.Cart
import com.intercommerce.cart.domain.model.CartItem
import com.intercommerce.cart.domain.model.CartLine
import com.intercommerce.core.domain.Money
import com.intercommerce.core.presentation.designsystem.Dimens
import com.intercommerce.core.presentation.designsystem.MessageState
import com.intercommerce.core.presentation.designsystem.ProductImage
import com.intercommerce.core.presentation.designsystem.QuantityStepper
import com.intercommerce.core.presentation.designsystem.formatted

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CartContent(
        state = state,
        onBack = onBack,
        onProductClick = onProductClick,
        onIncrease = viewModel::increase,
        onDecrease = viewModel::decrease,
        onRemove = viewModel::remove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartContent(
    state: CartUiState,
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
) {
    val cart = (state as? CartUiState.Content)?.cart
    // Wide screens show the summary beside the list; a bottom bar would take most of a landscape phone.
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val twoPane = windowWidth >= Dimens.TwoPaneMinWidth
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = { if (!twoPane && cart != null && !cart.isEmpty) CartSummary(cart, Modifier.navigationBarsPadding()) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                cart == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                cart.isEmpty -> MessageState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = stringResource(R.string.empty_cart_title),
                    message = stringResource(R.string.empty_cart_message),
                    actionLabel = stringResource(R.string.explore_products),
                    onAction = onBack,
                )
                twoPane -> Row(Modifier.fillMaxSize()) {
                    CartLines(cart, Modifier.weight(1f), onProductClick, onIncrease, onDecrease, onRemove)
                    CartSummary(cart, Modifier.width(Dimens.SidePaneWidth).fillMaxHeight())
                }
                else -> CartLines(cart, Modifier.widthIn(max = Dimens.ContentMaxWidth), onProductClick, onIncrease, onDecrease, onRemove)
            }
        }
    }
}

@Composable
private fun CartLines(
    cart: Cart,
    modifier: Modifier,
    onProductClick: (Int) -> Unit,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        items(cart.lines, key = { it.item.productId }, contentType = { "line" }) { line ->
            CartLineCard(
                line = line,
                onClick = { onProductClick(line.item.productId) },
                onIncrease = { onIncrease(line.item) },
                onDecrease = { onDecrease(line.item) },
                onRemove = { onRemove(line.item) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = line.item
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(Dimens.SpaceM), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            ProductImage(
                item.thumbnail,
                contentDescription = item.title,
                modifier = Modifier.size(Dimens.CartThumbnail).clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable(onClick = onClick),
                    )
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.remove_item, item.title))
                    }
                }
                Text(
                    stringResource(R.string.unit_price, line.unitNet.formatted()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(
                        quantity = item.quantity,
                        canDecrease = item.quantity > 1,
                        canIncrease = item.quantity < item.maxQuantity,
                        onDecrease = onDecrease,
                        onIncrease = onIncrease,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(line.net.formatted(), style = MaterialTheme.typography.titleMedium)
                        if (line.discount > Money.ZERO) {
                            Text(
                                stringResource(R.string.line_savings, line.discount.formatted()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartSummary(cart: Cart, modifier: Modifier = Modifier) {
    Surface(modifier, tonalElevation = Dimens.SpaceXs, shadowElevation = Dimens.SpaceS) {
        Column(
            Modifier.fillMaxWidth().padding(Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            SummaryRow(stringResource(R.string.subtotal_items, cart.itemCount), cart.subtotal.formatted())
            SummaryRow(stringResource(R.string.discounts), "−${cart.discount.formatted()}")
            SummaryRow(stringResource(R.string.tax_label, cart.taxRate.movePointRight(2).toInt()), cart.tax.formatted())
            HorizontalDivider()
            SummaryRow(stringResource(R.string.total), cart.total.formatted(), emphasized = true)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = style)
        Text(value, style = style)
    }
}
