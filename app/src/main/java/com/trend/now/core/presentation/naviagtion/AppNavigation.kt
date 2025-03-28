package com.trend.now.core.presentation.naviagtion

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trend.now.news.presentation.feature.localnews.LocalNewsSettingsScreen
import com.trend.now.news.presentation.feature.localnews.locale.LocaleList
import com.trend.now.news.presentation.feature.localnews.locale.LocaleListScreen
import com.trend.now.news.presentation.feature.main.MainViewModel
import com.trend.now.news.presentation.feature.news.NewsScreen
import com.trend.now.news.presentation.feature.onboarding.OnBoardingScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val mainUiState by mainViewModel.mainUiState.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(LocalDensity.current) { screenWidth.dp.toPx() }.toInt()

    if (!mainUiState.loading) {
        NavHost(
            navController = navController,
            startDestination = if (mainUiState.showOnBoarding) {
                AppRoute.OnBoarding.path
            } else {
                AppRoute.News.path
            },
            enterTransition = { slideInHorizontally(initialOffsetX = { screenWidthPx }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            composable(route = AppRoute.OnBoarding.path) {
                OnBoardingScreen(modifier = modifier,
                    navController = navController)
            }
            composable(route = AppRoute.News.path) {
                NewsScreen(modifier = modifier, navController = navController)
            }
            composable(
                route = AppRoute.LocalNewsSettings.path,
            ) {
                LocalNewsSettingsScreen(modifier = modifier, navController = navController)
            }
            composable(route = AppRoute.CountrySettings.path) {
                LocaleListScreen(
                    localeList = LocaleList.Country,
                    modifier = modifier,
                    navController = navController,
                )
            }
            composable(route = AppRoute.LanguageSettings.path) {
                LocaleListScreen(
                    localeList = LocaleList.Language,
                    modifier = modifier,
                    navController = navController,
                )
            }
        }
    }
}