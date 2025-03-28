package com.trend.now.core.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trend.now.core.data.api.response.ErrorResponse
import com.trend.now.news.domain.response.ApiResult
import com.trend.now.core.presentation.ui.state.UiState
import retrofit2.Response

object ApiResultUtil {
    fun <T> toApiResultError(gson: Gson, response: Response<T>): ApiResult.Error {
        return response.errorBody()?.string()?.let { errorBody ->
            val error = gson.fromJson<ErrorResponse>(
                errorBody,
                object : TypeToken<ErrorResponse>() {}.type
            )
            ApiResult.Error(error.code, error.message)
        } ?: run {
            ApiResult.Error(response.code(), response.message())
        }
    }
}

fun <T> ApiResult<T>.toUiState(): UiState<T> {
    return when (this) {
        is ApiResult.Success -> UiState.Success(this.data)
        is ApiResult.Error -> UiState.Error(this.code, this.message)
    }
}