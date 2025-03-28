package com.trend.now.core.domain

import com.trend.now.core.domain.model.Topic
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.model.NewsResult
import com.trend.now.news.domain.response.ApiResult

interface NewsDataSource {
    suspend fun getSupportedTopics(): ApiResult<List<Topic>>
    suspend fun getTrendingNews(
        topic: String,
        language: String,
        page: Int? = null,
        country: String? = null
    ): ApiResult<NewsResult>

    suspend fun getSupportedCountries(): ApiResult<Map<String, Country>>
}