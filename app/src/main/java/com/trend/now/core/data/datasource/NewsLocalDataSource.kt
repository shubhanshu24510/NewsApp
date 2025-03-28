package com.trend.now.core.data.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.trend.now.core.data.local.TopicDao
import com.trend.now.core.domain.NewsDataSource
import com.trend.now.core.domain.model.Topic
import com.trend.now.news.domain.response.ApiResult
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.model.NewsResult
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
class NewsLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val topicDao: TopicDao,
    private val gson: Gson
) : NewsDataSource {

    override suspend fun getSupportedTopics(): ApiResult<List<Topic>> =
        ApiResult.Success(topicDao.getAllTopics())

    override suspend fun getTrendingNews(
        topic: String,
        language: String,
        page: Int?,
        country: String?
    ): ApiResult<NewsResult> {
        TODO("Not yet implemented")
    }

    override suspend fun getSupportedCountries(): ApiResult<Map<String, Country>> {
        val content = context.assets.open(SUPPORTED_COUNTRIES_FILE)
            .bufferedReader()
            .use {
                it.readText()
            }
        val countries: List<Country> = gson.fromJson(
            gson.fromJson(content, JsonObject::class.java).get("data"),
            object : TypeToken<List<Country>>() {}.type
        )
        return ApiResult.Success(data = countries.associateBy { it.code })
    }

    companion object {
        private const val SUPPORTED_COUNTRIES_FILE = "supported-countries.json"
    }
}