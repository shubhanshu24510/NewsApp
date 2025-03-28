package com.trend.now.news.presentation.feature.localnews.locale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.news.domain.response.ApiResult
import com.trend.now.core.presentation.ui.state.UiState
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.model.Language
import com.trend.now.news.domain.model.NewsPreference
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.repository.UserPrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocaleListViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPrefRepository: UserPrefRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Map<String, Country>>>(UiState.Loading)
    val uiState: StateFlow<UiState<Map<String, Country>>> = _uiState

    val newsPreference = combine(
        userPrefRepository.newsLanguage,
        userPrefRepository.newsCountry
    ) { language, country ->
        NewsPreference(language = language, country = country)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = NewsPreference()
    )

    init {
        fetchSupportedCountries()
    }

    fun setNewsCountry(
        country: Country,
        onCountrySet: (languageChanged: Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        userPrefRepository.setNewsCountry(country.code)
        val newsLanguage = userPrefRepository.newsLanguage.first()
        val lang = country.languages.find { it.code == newsLanguage }
        if (lang == null) {
            val newLang = country.languages.firstOrNull()
            if (newLang != null) {
                userPrefRepository.setNewsLanguage(newLang.code)
                onCountrySet(true)
                return@launch
            }
        }
        onCountrySet(false)
    }

    fun setNewsLanguage(language: Language) = viewModelScope.launch {
        userPrefRepository.setNewsLanguage(language.code)
    }

    private fun fetchSupportedCountries() = viewModelScope.launch {
        val result = newsRepository.fetchSupportedCountries()
        if (result is ApiResult.Success) {
            _uiState.value = UiState.Success(result.data)
        }
    }
}