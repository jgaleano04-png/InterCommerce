package com.intercommerce.core.data.network

import com.intercommerce.core.domain.AppError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/** Single place where infrastructure exceptions become [AppError]. Order matters: timeouts are IOExceptions too. */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is SocketTimeoutException -> AppError.Timeout(this)
    is IOException -> AppError.NoConnection(this)
    is HttpException ->
        if (code() == HttpURLConnection.HTTP_NOT_FOUND) AppError.NotFound() else AppError.Http(code())
    is SerializationException -> AppError.Serialization(this)
    else -> AppError.Unknown(this)
}

/** Runs a remote call translating any failure into [AppError]; cancellation is never swallowed. */
suspend inline fun <T> safeApiCall(crossinline call: suspend () -> T): T =
    try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw e.toAppError()
    }
