package com.example.fliztv.data

object PlaylistManager {
    private val sources = mutableListOf(
        PlaylistSource("iptv-org", "iptv-org (Indian)", "https://iptv-org.github.io/iptv/index.m3u", isDefault = true)
    )
    private var activeSourceId: String = "iptv-org"
    private var counter = 0L
    private val lock = Any()

    fun getSources(): List<PlaylistSource> = synchronized(lock) { sources.toList() }
    fun getActiveSource(): PlaylistSource? = synchronized(lock) { sources.firstOrNull { it.id == activeSourceId } }

    fun setActiveSource(id: String) {
        synchronized(lock) {
            if (sources.any { it.id == id }) {
                activeSourceId = id
                ChannelRepository.clearCache()
            }
        }
    }

    fun addSource(name: String, url: String): PlaylistSource {
        synchronized(lock) {
            counter++
            val id = "custom_$counter"
            val source = PlaylistSource(id, name, url)
            sources.add(source)
            return source
        }
    }

    fun removeSource(id: String) {
        synchronized(lock) {
            sources.removeAll { it.id == id }
            if (activeSourceId == id && sources.isNotEmpty()) {
                activeSourceId = sources.first().id
                ChannelRepository.clearCache()
            }
        }
    }

    fun getActiveChannelsUrl(): String? = getActiveSource()?.url
}
