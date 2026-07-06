package com.example.fliztv.data

import com.example.fliztv.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class PlaylistRepository {
    suspend fun fetchRaw(url: String, fallbackUrls: List<String> = emptyList()): String {
        val urlsToTry = buildList {
            add(url)
            addAll(fallbackUrls)
        }
        for (u in urlsToTry) {
            try {
                return httpClient.get(u).bodyAsText()
            } catch (_: Exception) { }
        }
        throw Exception("Failed to load channels. Check your internet connection.")
    }

    suspend fun fetchChannels(url: String, fallbackUrls: List<String> = emptyList()): List<Channel> {
        return parseM3U(fetchRaw(url, fallbackUrls))
    }
}
