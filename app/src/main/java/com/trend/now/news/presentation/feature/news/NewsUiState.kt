package com.trend.now.news.presentation.feature.news

import com.trend.now.news.domain.model.News

data class NewsUiState(
    val data: List<News> = listOf(),
    val success: Boolean = false,
    val loading: Boolean = true,
    val showLoadMore: Boolean = false,
    val refreshing: Boolean = false,
    val message: String = "",
    val page: Int = -1
)