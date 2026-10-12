package com.kisimoni.topline.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.Response

object NetworkLoadingTracker {
    private val activeRequests = MutableStateFlow(0)
    val isLoading = activeRequests.asStateFlow()

    internal fun requestStarted() {
        activeRequests.update { it + 1 }
    }

    internal fun requestFinished() {
        activeRequests.update { (it - 1).coerceAtLeast(0) }
    }
}

class NetworkLoadingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.endsWith("/messages/presence")) {
            return chain.proceed(request)
        }

        NetworkLoadingTracker.requestStarted()
        return try {
            chain.proceed(request)
        } finally {
            NetworkLoadingTracker.requestFinished()
        }
    }
}
