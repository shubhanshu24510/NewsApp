package com.trend.now.core.data.api.response

data class BasicResponse<T>(val success: Boolean, val data: T)