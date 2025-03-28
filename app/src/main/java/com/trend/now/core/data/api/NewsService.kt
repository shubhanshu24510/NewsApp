package com.trend.now.core.data.api

import com.trend.now.core.data.api.response.BasicResponse
import com.trend.now.core.data.api.response.PaginationResponse
import com.trend.now.core.domain.model.Topic
import com.trend.now.news.domain.model.News
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsService {
    @GET("trendings")
    suspend fun fetchTrendingNews(
        @Query("topic") topic: String,
        @Query("language") language: String,
        @Query("country") country: String?,
        @Query("page") page: Int?
    ): Response<PaginationResponse<List<News>>>

    @GET("info/topics")
    suspend fun fetchSupportedTopics(): Response<BasicResponse<List<Topic>>>
}