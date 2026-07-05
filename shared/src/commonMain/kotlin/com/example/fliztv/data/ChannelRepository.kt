package com.example.fliztv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ChannelRepository {
    private val mutex = Mutex()
    private var cachedChannels: List<Channel>? = null
    private var onCachedChannelsChanged: ((String) -> Unit)? = null
    private var loadFromCache: (() -> String)? = null
    private var lastWatchedChannelId: String = ""
    private var onLastWatchedChanged: ((String) -> Unit)? = null
    private var scope: CoroutineScope? = null

    fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    fun setCacheCallbacks(
        load: () -> String,
        save: (String) -> Unit
    ) {
        loadFromCache = load
        onCachedChannelsChanged = save
        val raw = load()
        if (raw.isNotBlank()) {
            cachedChannels = parseM3U(raw)
        }
    }

    fun setLastWatchedCallbacks(
        load: () -> String,
        save: (String) -> Unit
    ) {
        onLastWatchedChanged = save
        lastWatchedChannelId = load()
    }

    fun saveLastWatchedChannel(channelId: String) {
        lastWatchedChannelId = channelId
        onLastWatchedChanged?.invoke(channelId)
    }

    fun getLastWatchedChannelId(): String = lastWatchedChannelId

    private suspend fun fetchFromNetwork(): List<Channel> {
        val repo = PlaylistRepository()
        val url = PlaylistManager.getActiveChannelsUrl() ?: return emptyList()
        val rawM3u = repo.fetchRaw(url)
        val channels = parseM3U(rawM3u)
        mutex.withLock { cachedChannels = channels }
        onCachedChannelsChanged?.invoke(rawM3u)

        val headerTvgUrl = extractHeaderTvgUrl(rawM3u)
        if (headerTvgUrl.isNotBlank() && !headerTvgUrl.endsWith(".gz")) {
            EpgManager.setActiveSource(headerTvgUrl)
        }
        scope?.launch {
            EpgManager.fetchPrograms(forceRefresh = true)
        }

        return channels
    }

    suspend fun getAllChannels(forceRefresh: Boolean = false): List<Channel> {
        mutex.withLock {
            if (cachedChannels != null && !forceRefresh) {
                return cachedChannels ?: emptyList()
            }
        }
        mutex.withLock {
            if (cachedChannels == null && loadFromCache != null) {
                val raw = loadFromCache?.invoke() ?: ""
                if (raw.isNotBlank()) {
                    cachedChannels = parseM3U(raw)
                    val chans = cachedChannels
                    if (chans != null && chans.isNotEmpty()) return chans
                }
            }
        }
        return fetchFromNetwork()
    }

    suspend fun getIndianChannels(forceRefresh: Boolean = false): List<Channel> {
        return getAllChannels(forceRefresh).filter { it.country == "India" }
    }

    suspend fun getEnglishChannels(forceRefresh: Boolean = false): List<Channel> {
        return getAllChannels(forceRefresh).filter {
            val lang = it.language.lowercase()
            val country = it.country.lowercase()
            (lang == "english" || country in listOf("united states", "united kingdom", "uk", "canada", "australia", "new zealand", "ireland")) &&
            country != "india"
        }
    }

    fun clearCache() {
        cachedChannels = null
        EpgManager.clearCache()
    }
}
