package com.intercommerce.core.domain

/**
 * Controlled failures that cross the data → domain → presentation boundary.
 *
 * Extends [Exception] on purpose: Paging 3 reports failures as `LoadState.Error(Throwable)`,
 * so the same type travels through the paged catalog and through suspend calls alike.
 */
sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** True when the device could not reach the server, as opposed to the server answering badly. */
    val isConnectivity: Boolean get() = this is NoConnection || this is Timeout

    /** No route to the server: airplane mode, DNS failure, connection reset mid-request. */
    class NoConnection(cause: Throwable? = null) : AppError("No connection", cause)

    class Timeout(cause: Throwable? = null) : AppError("Request timed out", cause)

    class NotFound : AppError("Resource not found")

    class Http(val code: Int) : AppError("HTTP $code")

    /** The server answered but the payload did not match the expected contract. */
    class Serialization(cause: Throwable? = null) : AppError("Malformed response", cause)

    class Unknown(cause: Throwable? = null) : AppError("Unexpected error", cause)
}
