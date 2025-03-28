@file:OptIn(ExperimentalMaterial3Api::class)

package com.trend.now.news.presentation.feature.news

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.trend.now.R
import com.trend.now.core.domain.services.NewsForegroundService
import com.trend.now.core.util.isListAtTop
import com.trend.now.news.domain.repository.UserPrefRepository
import com.trend.now.news.presentation.feature.news.component.NewsCard
import com.trend.now.news.presentation.feature.news.localnews.LocalNewsSection
import com.trend.now.news.presentation.feature.news.localnews.LocalNewsViewModel
import com.trend.now.news.presentation.feature.news.topic.TopicSection
import com.trend.now.news.presentation.feature.news.topic.TopicsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    localNewsViewModel: LocalNewsViewModel = hiltViewModel(),
    topicsViewModel: TopicsViewModel = hiltViewModel(),
    newsViewModel: NewsViewModel = hiltViewModel()
) {
    // Lazy list state to track scroll position
    val newsListState = rememberLazyListState()

    // Elevation for AppBar: Elevates when scrolling
    val appBarElevation by remember {
        derivedStateOf { if (newsListState.isListAtTop()) 0.dp else 8.dp }
    }

    // Floating action button visibility: Shows when scrolled down
    val showFab by remember {
        derivedStateOf { newsListState.firstVisibleItemIndex > 1 }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val trendingNewsUiState by newsViewModel.trendingNewsUiState.collectAsState()
    val isForegroundServiceEnabled by newsViewModel.isForegroundServiceEnabled.collectAsState()

    LaunchedEffect(isForegroundServiceEnabled) {
        if (isForegroundServiceEnabled) {
            val serviceIntent = Intent(context, NewsForegroundService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    LaunchedEffect(newsListState) {
        snapshotFlow { newsListState.layoutInfo.visibleItemsInfo }
            .map { visibleItems ->
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: -1
                val totalItems = newsListState.layoutInfo.totalItemsCount
                Pair(lastVisibleItemIndex, totalItems)
            }
            .distinctUntilChanged() // Prevent unnecessary recompositions
            .collect { (lastVisibleItemIndex, totalItems) ->
                if (trendingNewsUiState.data.isNotEmpty() &&
                    lastVisibleItemIndex >= totalItems - 1 &&
                    !trendingNewsUiState.loading // Prevent multiple calls
                ) {
                    newsViewModel.loadMoreTrendingNews()
                }
            }
    }

    //  Improved error handling: Prevents multiple snackbars showing at once
    LaunchedEffect(trendingNewsUiState.success) {
        if (!trendingNewsUiState.success && trendingNewsUiState.data.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.unable_to_load_more_news)
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = appBarElevation, // Dynamic shadow based on scroll position
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Black
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                val coroutineScope = rememberCoroutineScope()
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            newsListState.animateScrollToItem(0) // Scroll to top on FAB click
                        }
                    },
                ) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        // Pull-to-refresh functionality
        PullToRefreshBox(
            isRefreshing = trendingNewsUiState.refreshing,
            onRefresh = {
                topicsViewModel.fetchTopics()
                newsViewModel.onPullToRefresh()
            },
            modifier = modifier.padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.animateContentSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp),
                state = newsListState,
            ) {
                item(key = "local-news") {
                    LocalNewsSection(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        navController = navController,
                        viewModel = localNewsViewModel
                    )
                }
                item(key = "topics-section") {
                    TopicSection(
                        modifier = Modifier.fillParentMaxWidth(),
                        viewModel = topicsViewModel
                    )
                }
                if (trendingNewsUiState.loading && trendingNewsUiState.data.isEmpty()) {
                    // Show full-screen loading if the first page is still loading
                    item(key = "loading") {
                        Loading(modifier = Modifier.fillParentMaxSize())
                    }
                }
                if (trendingNewsUiState.success || trendingNewsUiState.data.isNotEmpty()) {
                    items(items = trendingNewsUiState.data, key = { it.url.hashCode() }) { news ->
                        NewsCard(
                            modifier = Modifier.fillParentMaxWidth(),
                            news = news
                        ) {
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            customTabsIntent.launchUrl(context, news.url.toUri())
                        }
                    }
                    if (trendingNewsUiState.showLoadMore) {
                        item(key = "loading-more") {
                            Loading(modifier = Modifier.fillParentMaxWidth())
                        }
                    }
                } else if (!trendingNewsUiState.loading) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillParentMaxSize()
                        ) {
                            Text(
                                text = trendingNewsUiState.message.takeIf { it.isNotBlank() }
                                    ?: run {
                                        stringResource(R.string.unable_to_load_news)
                                    }
                            )
                            Button(onClick = { newsViewModel.fetchTrendingNews() }) {
                                Text(text = stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}