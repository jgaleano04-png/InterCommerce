package com.intercommerce.core.presentation

import androidx.annotation.StringRes
import com.intercommerce.R
import com.intercommerce.core.domain.AppError
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// DummyJSON prices are in US dollars.
// ponytail: NumberFormat isn't thread-safe; fine because formatting only runs during composition on
// the main thread. Use one instance per call or a ThreadLocal if it is ever called off the main thread.
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

@StringRes
fun Throwable.messageRes(): Int = when (this) {
    is AppError.NoConnection -> R.string.error_no_connection
    is AppError.Timeout -> R.string.error_timeout
    is AppError.NotFound -> R.string.error_not_found
    is AppError.Http -> R.string.error_server
    is AppError.Serialization -> R.string.error_serialization
    else -> R.string.error_unknown
}

/** Banner shown above stored data: "offline" only when it really is a connectivity problem. */
@StringRes
fun staleDataMessageRes(error: Throwable?, isOnline: Boolean, @StringRes offline: Int, @StringRes failed: Int): Int =
    if (!isOnline || error == null || (error as? AppError)?.isConnectivity == true) offline else failed

/** Formats from the exact decimal, never through a Double. */
fun formatCents(cents: Long): String = currencyFormat.format(BigDecimal.valueOf(cents, 2))
