package com.trend.now.core.data.api.response

data class PaginationResponse<T>(
    val success: Boolean,
    val size: Int,
    val page: Int,
    val totalPages: Int,
    val data: T,
)