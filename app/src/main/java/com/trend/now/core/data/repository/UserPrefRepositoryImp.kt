package com.trend.now.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.trend.now.core.presentation.DEFAULT_COUNTRY
import com.trend.now.core.presentation.DEFAULT_LANGUAGE
import com.trend.now.core.presentation.DEFAULT_TOPIC
import com.trend.now.news.domain.repository.UserPrefRepository
import com.trend.now.news.domain.repository.UserPrefRepository.Companion.localNewsEnabledPref
import com.trend.now.news.domain.repository.UserPrefRepository.Companion.newsCountryPref
import com.trend.now.news.domain.repository.UserPrefRepository.Companion.newsLanguagePref
import com.trend.now.news.domain.repository.UserPrefRepository.Companion.selectedTopicPref
import com.trend.now.news.domain.repository.UserPrefRepository.Companion.showOnBoardingPref
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPrefRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPrefRepository {

    override val selectedTopic: Flow<String> = dataStore.data
        .map { it[selectedTopicPref] ?: DEFAULT_TOPIC }

    override suspend fun setSelectedTopic(topicId: String) {
        dataStore.edit { it[selectedTopicPref] = topicId }
    }

    override val newsCountry: Flow<String> = dataStore.data
        .map { it[newsCountryPref] ?: DEFAULT_COUNTRY }

    override suspend fun setNewsCountry(country: String) {
        dataStore.edit { it[newsCountryPref] = country }
    }

    override val newsLanguage: Flow<String> = dataStore.data
        .map { it[newsLanguagePref] ?: DEFAULT_LANGUAGE }

    override suspend fun setNewsLanguage(language: String) {
        dataStore.edit { it[newsLanguagePref] = language }
    }

    override val isShowOnBoarding: Flow<Boolean> = dataStore.data
        .map { it[showOnBoardingPref] ?: true }

    override suspend fun setShowOnBoarding(show: Boolean) {
        dataStore.edit { it[showOnBoardingPref] = show }
    }

    override val isLocalNewsEnabled: Flow<Boolean> = dataStore.data
        .map { it[localNewsEnabledPref] ?: false }

    override suspend fun setLocalNewsEnabled(enabled: Boolean) {
        dataStore.edit {
            it[localNewsEnabledPref] = enabled
        }
    }
}