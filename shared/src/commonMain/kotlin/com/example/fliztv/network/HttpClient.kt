package com.example.fliztv.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout

expect fun createPlatformEngine(): HttpClientEngine

val httpClient: HttpClient by lazy {
    HttpClient(createPlatformEngine()) {
        install(HttpRedirect)
        install(HttpTimeout) {
            requestTimeoutMillis = 180000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 180000
        }
    }
}

fun createHttpClient(): HttpClient = httpClient
