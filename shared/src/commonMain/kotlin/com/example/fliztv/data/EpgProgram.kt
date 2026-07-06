package com.example.fliztv.data

import com.example.fliztv.currentEpochSeconds

data class EpgProgram(
    val channelId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String = "",
    val category: String = "",
    val episodeTitle: String = "",
    val season: Int = 0,
    val episode: Int = 0
) {
    val isNow: Boolean get() = startTime <= now && endTime > now
    val progress: Float get() {
        val total = endTime - startTime
        if (total <= 0) return 0f
        return ((now - startTime).toFloat() / total).coerceIn(0f, 1f)
    }

    companion object {
        private val now: Long get() = currentEpochSeconds() * 1000
    }
}

data class EpgSource(
    val id: String,
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
    val fallbackUrls: List<String> = emptyList()
)
