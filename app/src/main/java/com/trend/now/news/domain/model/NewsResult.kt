package com.trend.now.news.domain.model

data class NewsResult(
    val data: List<News>,
    val fromCache: Boolean,
    val url: String
)