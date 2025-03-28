package com.trend.now.news.domain.model

data class NewsPreference(
    val language: String = "",
    val country: String = ""
) {
    fun isNotBlank(): Boolean = language.isNotBlank() && country.isNotBlank()
}