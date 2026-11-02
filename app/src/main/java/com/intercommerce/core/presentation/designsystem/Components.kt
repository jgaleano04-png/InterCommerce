package com.intercommerce.core.presentation.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import com.intercommerce.R
import com.intercommerce.core.domain.Money
import com.intercommerce.core.presentation.formatCents

private const val SHIMMER_DURATION_MS = 1100
private const val SHIMMER_WIDTH = 600f

/** Animated skeleton fill. A single infinite transition per element; no layout work per frame. */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceContainerLow
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -SHIMMER_WIDTH,
        targetValue = SHIMMER_WIDTH * 2,
        animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(Brush.linearGradient(listOf(base, highlight, base), start = Offset(x, 0f), end = Offset(x + SHIMMER_WIDTH, 0f)))
}

/** Remote image with a neutral placeholder and an explicit error state (Coil caches in memory and disk). */
@Composable
fun ProductImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    // A failure while offline is retried as soon as connectivity comes back.
    var failed by remember(url, LocalIsOnline.current) { mutableStateOf(false) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (failed || url.isBlank()) {
            Icon(
                Icons.Outlined.ImageNotSupported,
                contentDescription = stringResource(R.string.image_unavailable),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(Dimens.SpaceS),
            )
        }
    }
}

@Composable
fun PriceTag(price: Money, finalPrice: Money, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.headlineSmall else PriceTextStyle
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text(finalPrice.formatted(), style = style, color = MaterialTheme.colorScheme.onSurface)
        if (finalPrice < price) {
            Text(
                price.formatted(),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DiscountBadge(percentage: Double, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondary) {
        Text(
            stringResource(R.string.discount_badge, percentage.roundToInt().coerceAtLeast(1)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs / 2),
        )
    }
}

@Composable
fun RatingLabel(rating: Double, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Dimens.SpaceL))
        Text(
            stringResource(R.string.rating_value, rating),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        FilledTonalIconButton(onClick = onDecrease, enabled = canDecrease, modifier = Modifier.size(Dimens.StepperButton)) {
            Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.decrease_quantity))
        }
        Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = onIncrease, enabled = canIncrease, modifier = Modifier.size(Dimens.StepperButton)) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.increase_quantity))
        }
    }
}

/** Full-screen message with an optional action: used for errors without cache and for empty results. */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(Dimens.SpaceXxl),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.IconLarge))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Shown while the app serves stored data because the network is unavailable. */
@Composable
fun OfflineBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(Dimens.SpaceL + Dimens.SpaceXs))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

fun Money.formatted(): String = formatCents(cents)

@Composable
fun CartActionButton(itemCount: Int, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        androidx.compose.material3.BadgedBox(
            badge = { if (itemCount > 0) androidx.compose.material3.Badge { Text(itemCount.toString()) } },
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Outlined.ShoppingCart,
                contentDescription = androidx.compose.ui.res.pluralStringResource(R.plurals.cart_content_description, itemCount, itemCount),
            )
        }
    }
}
