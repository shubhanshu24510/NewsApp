package com.trend.now.core.data.repository

import com.trend.now.core.cache.CacheConfig.MAX_TOPICS_CACHE_IN_DAYS
import com.trend.now.core.cache.NewsCacheManager
import com.trend.now.core.data.datasource.NewsLocalDataSource
import com.trend.now.core.data.datasource.NewsRemoteDataSource
import com.trend.now.core.data.local.TopicDao
import com.trend.now.core.domain.model.Topic
import com.trend.now.core.presentation.DEFAULT_TOPIC
import com.trend.now.core.util.fromMs
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.model.News
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.response.ApiResult
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

@ActivityRetainedScoped
class NewsRepositoryImpl @Inject constructor(
    private val remoteDataSource: NewsRemoteDataSource,
    private val localDataSource: NewsLocalDataSource,
    private val topicDao: TopicDao,
    private val newsCacheManager: NewsCacheManager,
    private val currentDateProvider: () -> Calendar = { Calendar.getInstance() }
) : NewsRepository {
    override suspend fun fetchSupportedTopics(): ApiResult<List<Topic>> =
        withContext(Dispatchers.IO) {
            val localResult = localDataSource.getSupportedTopics()
            (localResult as? ApiResult.Success)?.let { local ->
                local.data.firstOrNull()?.let { first ->
                    val diffInDays = TimeUnit.DAYS.fromMs(
                        abs(currentDateProvider().timeInMillis - first.createdAt)
                    )
                    if (diffInDays < MAX_TOPICS_CACHE_IN_DAYS) {
                        return@withContext moveGeneralToFirst(local)
                    }
                }
            }
            val remoteResult = remoteDataSource.getSupportedTopics()
            if (remoteResult is ApiResult.Success) {
                topicDao.insertAll(
                    remoteResult.data.map {
                        it.copy(createdAt = currentDateProvider().timeInMillis)
                    }
                )
            }
            return@withContext moveGeneralToFirst(remoteResult)
        }

    override suspend fun fetchTrendingNews(
        topic: String,
        language: String,
        country: String?,
        page: Int?
    ): ApiResult<List<News>> = withContext(Dispatchers.IO) {
        val result = remoteDataSource.getTrendingNews(
            topic = topic,
            language = language,
            country = country,
            page = page
        )
        when (result) {
            is ApiResult.Success -> {
                if (!result.data.fromCache) {
                    newsCacheManager.addNewsCache(result.data.url)
                }
                ApiResult.Success(result.data.data, result.meta)
            }

            is ApiResult.Error -> result
        }
    }

    override suspend fun fetchSupportedCountries(): ApiResult<Map<String, Country>> =
        withContext(Dispatchers.IO) {
            localDataSource.getSupportedCountries()
        }

    private fun moveGeneralToFirst(result: ApiResult<List<Topic>>): ApiResult<List<Topic>> {
        if (result is ApiResult.Success) {
            val topics = result.data
            val general = topics.find { it.id == DEFAULT_TOPIC }
            val data = if (general != null) {
                listOf(general).plus(topics.minus(general))
            } else {
                topics
            }
            return ApiResult.Success(data, result.meta)
        }
        return result
    }
}