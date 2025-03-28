package com.trend.now.core.cache

import androidx.core.net.toUri
import com.trend.now.core.cache.CacheConfig.MAX_NEWS_CACHE_IN_HOURS
import com.trend.now.core.data.local.NewsCacheDao
import com.trend.now.core.domain.model.NewsCache
import com.trend.now.core.util.fromMs
import okhttp3.Cache
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsCacheManager @Inject constructor(
    private val cache: Cache,
    private val newsCacheDao: NewsCacheDao,
    private val currentDateProvider: () -> Calendar = { Calendar.getInstance() }
) {
    suspend fun isCacheAvailable(url: String): Boolean {
        val responseCache = cache.urls().find { it == url }
        if (responseCache == null) {
            return false
        }

        val cacheDateInMillis = newsCacheDao.getNewsCache(url)?.createdAt
        if (cacheDateInMillis != null) {
            val todayCalendar = currentDateProvider()
            val cacheCalendar = Calendar.getInstance().apply {
                timeInMillis = cacheDateInMillis
                timeZone = todayCalendar.timeZone
            }
            val diffInHours = TimeUnit.HOURS.fromMs(
                todayCalendar.timeInMillis - cacheCalendar.timeInMillis
            )
            if (diffInHours < 0) {
                return false
            }
            return (diffInHours < MAX_NEWS_CACHE_IN_HOURS)
                && (todayCalendar.day() == cacheCalendar.day()
                && todayCalendar.month() == cacheCalendar.month()
                && todayCalendar.year() == cacheCalendar.year())

        }
        return false
    }
    suspend fun addNewsCache(url: String) {
        val parentUrl = getParentUrl(url)
        if (parentUrl == url) {
            newsCacheDao.deleteNewsCache(parentUrl = parentUrl)
        }
        val cache = NewsCache(
            url = url,
            parentUrl = parentUrl,
            createdAt = currentDateProvider().timeInMillis
        )
        newsCacheDao.insertNewsCache(cache)
    }
    fun getParentUrl(url: String): String = try {
        val uri = url.toUri()
        if (uri.query?.isBlank() == true || uri.path != TRENDINGS_PATH) {
            url
        } else {
            val regex = Regex("([&?])${PAGE_QUERY_NAME}=\\d+(&?)")
            regex.replace(url) { matchResult ->
                if (matchResult.groupValues[2].isEmpty()) matchResult.groupValues[1] else ""
            }
        }
    } catch (e: Exception) {
        url
    }

    companion object {
        private const val TRENDINGS_PATH = "/v2/trendings"
        private const val PAGE_QUERY_NAME = "page"
    }
}

fun <T> Iterator<T>.find(predicate: (T) -> Boolean): T? {
    var result: T? = null
    var hasNext = hasNext()
    while (hasNext) {
        result = next()
        hasNext = if (predicate(result)) {
            false
        } else {
            result = null
            hasNext()
        }
    }
    return result
}

private fun Calendar.day() = get(Calendar.DAY_OF_MONTH)

private fun Calendar.month() = get(Calendar.MONTH)

private fun Calendar.year() = get(Calendar.YEAR)