package com.trend.now.news.presentation.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.news.domain.model.NewsPreference
import com.trend.now.news.domain.response.ApiResult
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.repository.UserPrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPrefRepository: UserPrefRepository,
) : ViewModel() {
    private val fetchTrendingNewsMutex = Mutex()
    private var trendingNewsJob: Job? = null
    private val _trendingNewsUiState = MutableStateFlow(NewsUiState(loading = true))
    val trendingNewsUiState: StateFlow<NewsUiState> = _trendingNewsUiState
    private var loadingMore: Boolean = false

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
        if (loadingMore) {
            return
        }
        if (!_trendingNewsUiState.value.showLoadMore) {
            return
        }
        loadingMore = true
        internalFetchTrendingNews {
            loadingMore = false
        }
    }

    fun onPullToRefresh() {
        if (_trendingNewsUiState.value.refreshing) return

        _trendingNewsUiState.value = _trendingNewsUiState.value.copy(
            refreshing = true
        )
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
                            } else {
                                null
                            }
                        )

                        val data = _trendingNewsUiState.value.data
                        when (result) {
                            is ApiResult.Success -> {
                                val meta = result.meta
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
                                        showLoadMore =
                                            meta.page > 0 && meta.page < meta.totalPages,
                                        page = meta.page
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
}