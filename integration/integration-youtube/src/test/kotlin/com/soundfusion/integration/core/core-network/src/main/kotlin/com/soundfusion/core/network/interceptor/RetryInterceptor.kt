package com.soundfusion.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        repeat(3) { attempt ->
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code != 429) return response
                response.close()
                Thread.sleep((1000L * (attempt + 1)))
            } catch (e: IOException) {
                lastException = e
                if (attempt < 2) Thread.sleep((1000L * (attempt + 1)))
            }
        }
        throw lastException ?: IOException("Request failed after 3 retries")
    }
}
