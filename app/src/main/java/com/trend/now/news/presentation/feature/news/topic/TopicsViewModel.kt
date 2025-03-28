package com.trend.now.news.presentation.feature.news.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.repository.UserPrefRepository
import com.trend.now.news.domain.response.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPrefRepository: UserPrefRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState

    private var loading: Boolean = false

    init {
        viewModelScope.launch {
            userPrefRepository.selectedTopic
                .filter { it.isNotBlank() }
                .take(1)
                .collect {
                    fetchTopics()
                }
        }

        viewModelScope.launch {
            userPrefRepository.selectedTopic
                .filter { it.isNotBlank() }
                .drop(1)
                .distinctUntilChanged()
                .collect { topic ->
                    _uiState.update {
                        _uiState.value.copy(selectedTopic = topic)
                    }
                }
        }
    }

    fun fetchTopics() {
        if (loading) return

        loading = true
        viewModelScope.launch {
            val result = newsRepository.fetchSupportedTopics()
            if (result is ApiResult.Success) {
                _uiState.update {
                    _uiState.value.copy(
                        topics = result.data,
                        selectedTopic = userPrefRepository.selectedTopic.first()
                    )
                }
            }
            loading = false
        }
    }

    fun selectTopic(topicId: String) = viewModelScope.launch {
        userPrefRepository.setSelectedTopic(topicId)
    }

    fun indexOfTopic(topicId: String): Int {
        return _uiState.value.topics.indexOfFirst { it.id == topicId }
            .takeIf { it >= 0 } ?: 0
    }
}