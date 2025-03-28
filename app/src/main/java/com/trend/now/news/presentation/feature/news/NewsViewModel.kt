package com.trend.now.news.presentation.feature.news

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.R
import com.trend.now.news.domain.model.NewsPreference
import com.trend.now.news.domain.response.ApiResult
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.repository.UserPrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPrefRepository: UserPrefRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val fetchTrendingNewsMutex = Mutex()
    private var trendingNewsJob: Job? = null
    private val _trendingNewsUiState = MutableStateFlow(NewsUiState(loading = true))
    val trendingNewsUiState: StateFlow<NewsUiState> = _trendingNewsUiState
    private var loadingMore: Boolean = false

    val isForegroundServiceEnabled = userPrefRepository.isForegroundServiceEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        viewModelScope.launch {
            combine(
                userPrefRepository.selectedTopic,
                userPrefRepository.newsCountry,
                userPrefRepository.newsLanguage
            ) { topic, country, language ->
                Pair(topic, NewsPreference(country = country, language = language))
            }.filter {
                it.first.isNotBlank()
                        && it.second.language.isNotBlank()
                        && it.second.country.isNotBlank()
            }.distinctUntilChanged { old, new ->
                old.first == new.first && old.second == new.second
            }.collect {
                fetchTrendingNews()
            }
        }
    }

    fun fetchTrendingNews() {
        _trendingNewsUiState.update {
            _trendingNewsUiState.value.copy(loading = true)
        }
        internalFetchTrendingNews()
    }


    fun loadMoreTrendingNews() {
        if (loadingMore || !_trendingNewsUiState.value.showLoadMore) return
        loadingMore = true
        internalFetchTrendingNews() { loadingMore = false }
    }

    fun onPullToRefresh() {
        if (_trendingNewsUiState.value.refreshing) return
        _trendingNewsUiState.value = _trendingNewsUiState.value.copy(refreshing = true)

        viewModelScope.launch {
            delay(300)
            internalFetchTrendingNews()
        }
    }

    private fun internalFetchTrendingNews(onFetchDone: () -> Unit = {}) {
        trendingNewsJob?.cancel()
        viewModelScope.launch {
            fetchTrendingNewsMutex.withLock {
                trendingNewsJob = launch {
                    try {
                        val result = newsRepository.fetchTrendingNews(
                            topic = userPrefRepository.selectedTopic.first(),
                            language = userPrefRepository.newsLanguage.first(),
                            country = userPrefRepository.newsCountry.first(),
                            page = if (loadingMore) {
                                _trendingNewsUiState.value.page + 1
                            } else null
                        )

                        val data = _trendingNewsUiState.value.data
                        when (result) {
                            is ApiResult.Success -> {
                                val meta = result.meta
                                val latestNews = result.data.firstOrNull() // Get latest news

                                _trendingNewsUiState.update {
                                    _trendingNewsUiState.value.copy(
                                        data = if (loadingMore) {
                                            data.plus(result.data)
                                        } else {
                                            result.data
                                        },
                                        loading = false,
                                        refreshing = false,
                                        success = true,
                                        showLoadMore = meta.page > 0 && meta.page < meta.totalPages,
                                        page = meta.page
                                    )
                                }
                                //  Pass context from UI to show notification
                                latestNews?.let { news ->
                                    showNewsNotification(
                                        context = context.applicationContext,
                                        imageUrl = news.url,
                                        title = news.title,
                                        message = news.excerpt
                                    )
                                }
                            }

                            is ApiResult.Error -> _trendingNewsUiState.value =
                                _trendingNewsUiState.value.copy(
                                    success = false,
                                    loading = false,
                                    refreshing = false,
                                    message = result.message,
                                )
                        }
                        onFetchDone()
                    } finally {
                        fetchTrendingNewsMutex.withLock { trendingNewsJob = null }
                    }
                }
            }
        }
    }

    fun showNewsNotification(context: Context, imageUrl: String?, title: String, message: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channelId = "news_channel"
        val notificationId = 1

        // Create Notification Channel (for Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            "News Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Get latest news updates"
        }
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.news_icon) // Replace with your icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        //  Add Large Image (if available)
        imageUrl?.let { url ->
            val bitmap = loadBitmapFromUrl(url)
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            }
        }

        // Check permission before showing notification
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, builder.build())
        }
    }


    fun loadBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}