package com.trend.now.news.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.core.presentation.DEFAULT_COUNTRY
import com.trend.now.core.presentation.DEFAULT_LANGUAGE
import com.trend.now.news.domain.model.Country
import com.trend.now.news.domain.repository.NewsRepository
import com.trend.now.news.domain.repository.UserPrefRepository
import com.trend.now.news.domain.response.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userPrefRepository: UserPrefRepository
) : ViewModel() {
    private var countries: Map<String, Country> = mapOf()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            fetchSupportedCountries()
        }
    }

    fun saveForegroundServicePermission(isGranted: Boolean) {
        viewModelScope.launch {
            userPrefRepository.setForegroundServiceEnabled(isGranted)
        }
    }

    fun setLoading(loading: Boolean) = viewModelScope.launch {
        _loading.value = loading
    }

    fun setupLocalNews(
        language: String,
        country: String = "",
        onComplete: () -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            if (countries.isEmpty() || (language.isBlank() && country.isBlank())) {
                userPrefRepository.setNewsLanguage(DEFAULT_LANGUAGE)
                userPrefRepository.setNewsCountry(DEFAULT_COUNTRY)
                userPrefRepository.setShowOnBoarding(false)
                _loading.value = false
                onComplete() // Call onComplete() when preferences are saved
                return@launch
            }
            val userCountry = countries[country]
            val userLanguage = userCountry?.languages?.find {
                it.code == language
            } ?: run {
                userCountry?.languages?.firstOrNull()
            }
            if (userCountry != null && userLanguage != null) {
                userPrefRepository.setNewsLanguage(userLanguage.code)
                userPrefRepository.setNewsCountry(userCountry.code)
                userPrefRepository.setLocalNewsEnabled(true)
            } else {
                userPrefRepository.setNewsLanguage(DEFAULT_LANGUAGE)
                userPrefRepository.setNewsCountry(DEFAULT_COUNTRY)
                userPrefRepository.setLocalNewsEnabled(false)
            }
            userPrefRepository.setShowOnBoarding(false)
            _loading.value = false
            onComplete() // Ensure this runs only after preferences are updated
        }
    }

    private fun fetchSupportedCountries() = viewModelScope.launch {
        val result = newsRepository.fetchSupportedCountries()
        delay(2000)
        if (result is ApiResult.Success) {
            countries = result.data
        } else {
            // TODO: handle when failed to get supported countries
        }
        _loading.value = false
    }
}