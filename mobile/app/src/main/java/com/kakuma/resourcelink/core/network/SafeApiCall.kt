package com.kakuma.resourcelink.core.network

import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Runs an API call and converts transport/HTTP failures into [AppError]s. */
suspend fun <T> safeApiCall(moshi: Moshi, block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    AppResult.Failure(mapHttpError(moshi, e.code(), e.response()?.errorBody()?.string()))
} catch (e: IOException) {
    AppResult.Failure(AppError.Network)
} catch (e: Exception) {
    AppResult.Failure(AppError.Unknown(e.message))
}

fun mapHttpError(moshi: Moshi, status: Int, rawBody: String?): AppError {
    val body = rawBody?.let {
        runCatching { moshi.adapter(ApiErrorEnvelope::class.java).fromJson(it)?.error }.getOrNull()
    }
    return when {
        status == 401 -> AppError.Unauthorized
        status == 403 -> AppError.Forbidden
        status == 404 -> AppError.NotFound
        status == 409 -> AppError.Conflict(body?.message)
        status == 429 -> AppError.RateLimited
        status == 400 -> AppError.Validation(body?.message, flattenDetails(body?.details.orEmpty()))
        status == 503 -> AppError.Unavailable(body?.message)
        status >= 500 -> AppError.Server(body?.message)
        else -> AppError.Unknown(body?.message)
    }
}

/** `{"email": ["msg"], "profile": {"bio": ["msg"]}}` → `{"email": "msg", "profile.bio": "msg"}`. */
internal fun flattenDetails(details: Map<String, Any?>, prefix: String = ""): Map<String, String> =
    buildMap {
        details.forEach { (key, value) ->
            val name = if (prefix.isEmpty()) key else "$prefix.$key"
            when (value) {
                is String -> put(name, value)
                is List<*> -> value.firstOrNull()?.let { first ->
                    if (first is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        putAll(flattenDetails(first as Map<String, Any?>, name))
                    } else put(name, first.toString())
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    putAll(flattenDetails(value as Map<String, Any?>, name))
                }
            }
        }
    }
