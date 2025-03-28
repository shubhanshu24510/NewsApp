package com.trend.now.core.network

import com.trend.now.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor : Interceptor {
    val apiKey = BuildConfig.API_KEY
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("x-rapidapi-host", "news-api14.p.rapidapi.com")
                // set your api key here
                .addHeader("x-rapidapi-key", apiKey)
                .build()
        )
}