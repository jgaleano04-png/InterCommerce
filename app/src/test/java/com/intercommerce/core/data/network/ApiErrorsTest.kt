package com.intercommerce.core.data.network

import com.intercommerce.core.domain.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorsTest {

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody()))

    @Test
    fun `maps infrastructure exceptions to controlled errors`() {
        assertTrue(SocketTimeoutException().toAppError() is AppError.Timeout)
        assertTrue(UnknownHostException().toAppError() is AppError.NoConnection)
        assertTrue(IOException("reset").toAppError() is AppError.NoConnection)
        assertTrue(httpError(404).toAppError() is AppError.NotFound)
        assertEquals(500, (httpError(500).toAppError() as AppError.Http).code)
        assertTrue(SerializationException("bad").toAppError() is AppError.Serialization)
        assertTrue(IllegalStateException().toAppError() is AppError.Unknown)
    }

    @Test
    fun `safeApiCall rethrows mapped error`() = runTest {
        val error = runCatching { safeApiCall { throw SocketTimeoutException() } }.exceptionOrNull()
        assertTrue(error is AppError.Timeout)
    }

    @Test(expected = CancellationException::class)
    fun `safeApiCall never swallows cancellation`() = runTest {
        safeApiCall { throw CancellationException("cancelled") }
    }
}
