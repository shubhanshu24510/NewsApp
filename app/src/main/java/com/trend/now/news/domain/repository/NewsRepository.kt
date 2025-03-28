package com.trend.now.news.domain.repository

import com.trend.now.news.domain.response.ApiResult
import com.trend.now.core.domain.model.Topic
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.model.News

interface NewsRepository {
    suspend fun fetchSupportedTopics(): ApiResult<List<Topic>>
    suspend fun fetchTrendingNews(
        topic: String,
        language: String,
        country: String? = null,
        page: Int? = null
    ): ApiResult<List<News>>
    suspend fun fetchSupportedCountries(): ApiResult<Map<String, Country>>
}