@file:OptIn(ExperimentalPermissionsApi::class)

package com.trend.now.news.presentation.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.trend.now.R
import com.trend.now.core.presentation.DEFAULT_COUNTRY
import com.trend.now.core.presentation.naviagtion.AppRoute
import com.trend.now.core.util.countryCode
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    navController: NavController, // Add NavController for navigation
    viewModel: OnBoardingViewModel = hiltViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val context = LocalContext.current

    val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

    // Request multiple permissions at once
    val multiplePermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )
    )

    val hasRequestedPermission = rememberSaveable { mutableStateOf(false) }
    val allPermissionsGranted = multiplePermissions.permissions.all { it.status.isGranted }
    val locationFetched = remember { mutableStateOf(false) }

    // Fetch location and update news before navigating
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            viewModel.setLoading(true) // Start loading while fetching data

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

//            val address = location?.countryCode(context)
            val countryCode = location?.countryCode(context) ?: DEFAULT_COUNTRY

            viewModel.setupLocalNews(
                language = Locale.getDefault().language,
                country = countryCode.toString(),
            ) {
                // Navigate only after preferences are saved
                navController.navigate(AppRoute.News.path)
                viewModel.setLoading(false)
            }
        }
    }

    // Navigate to NewsScreen only after location is fetched
    LaunchedEffect(locationFetched.value) {
        if (locationFetched.value) {
            viewModel.setLoading(false) // Stop loading
            navController.navigate(AppRoute.News.path) // Navigate to NewsScreen
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Button(onClick = {
                    viewModel.setLoading(true) // Show loading state while saving preferences
                    viewModel.setupLocalNews(language = Locale.getDefault().language) {
                        navController.navigate(AppRoute.News.path) // Navigate only after preferences are saved
                        viewModel.setLoading(false) // Stop loading
                    }
                }) {
                    Text(text = stringResource(R.string.skip))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.img_location),
                    contentDescription = "image-location-permission",
                    modifier = Modifier.size(280.dp)
                )
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = stringResource(
                        R.string.onboarding_desc,
                        "\"${stringResource(R.string.app_name)}\""
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            AnimatedButton(
                text = if (multiplePermissions.permissions.any { it.status.shouldShowRationale }
                    && !hasRequestedPermission.value
                ) {
                    stringResource(R.string.go_to_settings)
                } else {
                    stringResource(R.string.allow_permission)
                },
                loading = loading
            ) {
                if (multiplePermissions.permissions.any { it.status.shouldShowRationale }
                    && !hasRequestedPermission.value
                ) {
                    // Open App Settings if permissions were permanently denied
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } else {
                    viewModel.setLoading(true)

                    // Request all permissions at once
                    multiplePermissions.launchMultiplePermissionRequest()
                    hasRequestedPermission.value = true

                    // Save foreground service permission state
                    val isForegroundServiceGranted = multiplePermissions.permissions.any {
                        it.permission == Manifest.permission.FOREGROUND_SERVICE && it.status.isGranted
                    }
                    viewModel.saveForegroundServicePermission(isForegroundServiceGranted)

                    //  Navigate to NewsScreen if foreground service is granted
                    if (isForegroundServiceGranted) {
                        navController.navigate(AppRoute.News.path)
                        viewModel.setLoading(false) // Stop loading after navigation
                    }
                }
            }
//            AnimatedButton(
//                text = if (multiplePermissions.permissions.any { it.status.shouldShowRationale }
//                    && !hasRequestedPermission.value
//                ) {
//                    stringResource(R.string.go_to_settings)
//                } else {
//                    stringResource(R.string.allow_permission)
//                },
//                loading = loading
//            ) {
//                if (multiplePermissions.permissions.any { it.status.shouldShowRationale }
//                    && !hasRequestedPermission.value
//                ) {
//                    // TODO: Open App Settings if permissions were denied permanently
//                } else {
//                    viewModel.setLoading(true)
//
//                    // Request all permissions at once
//                    multiplePermissions.launchMultiplePermissionRequest()
//                    hasRequestedPermission.value = true
//
//                    // Save foreground service permission state
//                    val isForegroundServiceGranted = multiplePermissions.permissions.any {
//                        it.permission == Manifest.permission.FOREGROUND_SERVICE && it.status.isGranted
//                    }
//                    viewModel.saveForegroundServicePermission(isForegroundServiceGranted)
//                }
//            }
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
fun AnimatedButton(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val widthFraction by animateFloatAsState(
        targetValue = if (loading) 0f else 0.9f,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )
    val buttonText = remember {
        derivedStateOf { if (widthFraction == 0.9f) text else "" }
    }

    Button(
        modifier = modifier
            .height(48.dp)
            .requiredWidthIn(min = 48.dp)
            .fillMaxWidth(fraction = widthFraction),
        contentPadding = PaddingValues(0.dp),
        onClick = {
            if (!loading) onClick()
        }
    ) {
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.surface,
                strokeWidth = 3.dp
            )
        }
        if (!loading) {
            Text(
                text = buttonText.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}