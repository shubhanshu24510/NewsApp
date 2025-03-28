package com.trend.now.news.presentation.feature.news.topic

import com.trend.now.core.domain.model.Topic

data class TopicsUiState(
    val topics: List<Topic> = listOf(),
    val selectedTopic: String = ""
)