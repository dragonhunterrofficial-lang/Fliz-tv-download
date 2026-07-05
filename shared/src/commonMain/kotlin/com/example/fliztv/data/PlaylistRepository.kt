package com.example.fliztv.data

import com.example.fliztv.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class PlaylistRepository {
    suspend fun fetchRaw(url: String): String {
        return try {
            httpClient.get(url).bodyAsText()
        } catch (e: Exception) {
            throw Exception("Failed to load channels. Check your internet connection.")
        }
    }

    suspend fun fetchChannels(url: String): List<Channel> {
        return parseM3U(fetchRaw(url))
    }
}
