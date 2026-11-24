package com.kakuma.resourcelink.core.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiErrorEnvelope(val error: ApiErrorBody)

@JsonClass(generateAdapter = true)
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
)

/** DRF page: `{count, next, previous, results}`. */
@JsonClass(generateAdapter = true)
data class PageDto<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>,
)
