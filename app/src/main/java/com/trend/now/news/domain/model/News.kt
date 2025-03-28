package com.trend.now.news.domain.model

data class News(
    val title: String,
    val url: String,
    val excerpt: String,
    val thumbnail: String,
    val date: String,
    val publisher: Publisher
)

data class Publisher(
    val name: String,
    val url: String,
    val favicon: String
)