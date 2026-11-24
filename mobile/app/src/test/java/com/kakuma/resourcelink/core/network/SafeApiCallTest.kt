package com.kakuma.resourcelink.core.network

import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SafeApiCallTest {
    private val moshi = Moshi.Builder().build()

    private fun httpError(code: Int, body: String = "") =
        HttpException(Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())))

    @Test fun `success is wrapped`() = runTest {
        assertEquals(AppResult.Success(42), safeApiCall(moshi) { 42 })
    }

    @Test fun `io failures map to Network`() = runTest {
        assertEquals(AppResult.Failure(AppError.Network), safeApiCall(moshi) { throw IOException("offline") })
    }

    @Test fun `backend error envelope is mapped with flattened field errors`() = runTest {
        val body = """{"error":{"code":"validation_error","message":"Check the form","details":{"email":["Taken"],"profile":{"bio":["Too long"]}}}}"""

        val result = safeApiCall(moshi) { throw httpError(400, body) }

        assertEquals(
            AppResult.Failure(AppError.Validation("Check the form", mapOf("email" to "Taken", "profile.bio" to "Too long"))),
            result,
        )
    }

    @Test fun `status codes map to typed errors`() {
        assertEquals(AppError.Unauthorized, mapHttpError(moshi, 401, null))
        assertEquals(AppError.Forbidden, mapHttpError(moshi, 403, null))
        assertEquals(AppError.NotFound, mapHttpError(moshi, 404, null))
        assertEquals(AppError.RateLimited, mapHttpError(moshi, 429, null))
        assertEquals(AppError.Unavailable("AI is off"), mapHttpError(moshi, 503, """{"error":{"code":"ai_unavailable","message":"AI is off"}}"""))
        assertEquals(AppError.Server(null), mapHttpError(moshi, 500, "<html>oops</html>"))
    }
}
