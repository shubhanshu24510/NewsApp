package com.trend.now.news.presentation.feature.localnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trend.now.core.presentation.ui.state.UiState
import com.trend.now.news.domain.model.NewsPreference
import com.trend.now.news.domain.repository.UserPrefRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LocalNewsSettingsViewModel @Inject constructor(
    userPrefRepository: UserPrefRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<NewsPreference>> = combine(
        userPrefRepository.newsLanguage,
        userPrefRepository.newsCountry
    ) { language, country ->
        UiState.Success(NewsPreference(language = language, country = country))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = UiState.Loading
    )
}