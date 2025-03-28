package com.trend.now.news.domain.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

interface UserPrefRepository {
    val selectedTopic: Flow<String>
    suspend fun setSelectedTopic(topicId: String)
    val newsCountry: Flow<String>
    suspend fun setNewsCountry(country: String)
    val newsLanguage: Flow<String>
    suspend fun setNewsLanguage(language: String)
    val isShowOnBoarding: Flow<Boolean>
    suspend fun setShowOnBoarding(show: Boolean)
    val isLocalNewsEnabled: Flow<Boolean>
    suspend fun setLocalNewsEnabled(enabled: Boolean)
    val isForegroundServiceEnabled: Flow<Boolean>
    suspend fun setForegroundServiceEnabled(enabled: Boolean)

    companion object {
        val selectedTopicPref = stringPreferencesKey("selected_topic")
        val newsCountryPref = stringPreferencesKey("news_country")
        val newsLanguagePref = stringPreferencesKey("news_language")
        val showOnBoardingPref = booleanPreferencesKey("show_onboarding")
        val localNewsEnabledPref = booleanPreferencesKey("local_news_enabled")
    }
}