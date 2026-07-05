package com.example.fliztv.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun createPlatformEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        followRedirects(true)
        followSslRedirects(true)
        retryOnConnectionFailure(true)
        connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
    }
}
