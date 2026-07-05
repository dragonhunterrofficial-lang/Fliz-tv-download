package com.example.fliztv.data

import com.example.fliztv.currentEpochSeconds
import com.example.fliztv.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EpgManager {
    private val epgSources = listOf(
        EpgSource("in", "India", "https://iptv-epg.org/files/epg-in.xml", isDefault = true),
        EpgSource("us", "United States", "https://iptv-epg.org/files/epg-us.xml"),
        EpgSource("gb", "United Kingdom", "https://iptv-epg.org/files/epg-gb.xml"),
        EpgSource("ca", "Canada", "https://iptv-epg.org/files/epg-ca.xml"),
        EpgSource("au", "Australia", "https://iptv-epg.org/files/epg-au.xml"),
        EpgSource("de", "Germany", "https://iptv-epg.org/files/epg-de.xml"),
        EpgSource("fr", "France", "https://iptv-epg.org/files/epg-fr.xml"),
        EpgSource("it", "Italy", "https://iptv-epg.org/files/epg-it.xml"),
        EpgSource("es", "Spain", "https://iptv-epg.org/files/epg-es.xml"),
        EpgSource("pt", "Portugal", "https://iptv-epg.org/files/epg-pt.xml"),
        EpgSource("br", "Brazil", "https://iptv-epg.org/files/epg-br.xml"),
        EpgSource("ar", "Argentina", "https://iptv-epg.org/files/epg-ar.xml"),
        EpgSource("bd", "Bangladesh", "https://iptv-epg.org/files/epg-bd.xml"),
        EpgSource("pk", "Pakistan", "https://iptv-epg.org/files/epg-pk.xml"),
        EpgSource("lk", "Sri Lanka", "https://iptv-epg.org/files/epg-lk.xml"),
        EpgSource("np", "Nepal", "https://iptv-epg.org/files/epg-np.xml"),
        EpgSource("ae", "UAE", "https://iptv-epg.org/files/epg-ae.xml"),
        EpgSource("sa", "Saudi Arabia", "https://iptv-epg.org/files/epg-sa.xml"),
        EpgSource("za", "South Africa", "https://iptv-epg.org/files/epg-za.xml"),
        EpgSource("ng", "Nigeria", "https://iptv-epg.org/files/epg-ng.xml")
    )
    private var cachedPrograms: List<EpgProgram>? = null
    private var cacheTime: Long = 0
    private var epgSourceUrl: String = epgSources.first().url
    private val cacheDurationMs = 30 * 60 * 1000L

    fun getSources(): List<EpgSource> = epgSources

    fun setActiveSource(url: String) {
        epgSourceUrl = url
        cachedPrograms = null
        cacheTime = 0
    }

    suspend fun fetchPrograms(forceRefresh: Boolean = false): List<EpgProgram> {
        val now = currentEpochSeconds() * 1000
        if (!forceRefresh && cachedPrograms != null && (now - cacheTime) < cacheDurationMs) {
            return cachedPrograms ?: emptyList()
        }
        return withContext(Dispatchers.Default) {
            try {
                val xml = httpClient.get(epgSourceUrl).bodyAsText()
                val programs = XmltvParser.parse(xml)
                if (programs.isNotEmpty()) {
                    cachedPrograms = programs
                    cacheTime = now
                }
                programs
            } catch (e: Exception) {
                cachedPrograms ?: emptyList()
            }
        }
    }

    private fun normalizeId(id: String): String {
        return id.substringBefore("@").trim().lowercase()
    }

    private fun nowMs(): Long = currentEpochSeconds() * 1000

    fun getProgramsForChannel(channelId: String): List<EpgProgram> {
        val norm = normalizeId(channelId)
        return cachedPrograms?.filter { normalizeId(it.channelId) == norm }?.sortedBy { it.startTime } ?: emptyList()
    }

    fun getAllPrograms(): List<EpgProgram> {
        val now = nowMs()
        return cachedPrograms?.filter { it.endTime > now - 86400000 && it.startTime < now + 172800000 }
            ?.sortedBy { it.startTime } ?: emptyList()
    }

    fun getNowPlaying(channelId: String): EpgProgram? {
        val now = nowMs()
        val norm = normalizeId(channelId)
        return cachedPrograms?.find { normalizeId(it.channelId) == norm && it.startTime <= now && it.endTime > now }
    }

    fun getNextProgram(channelId: String): EpgProgram? {
        val now = nowMs()
        val norm = normalizeId(channelId)
        return cachedPrograms?.filter { normalizeId(it.channelId) == norm && it.startTime > now }?.minByOrNull { it.startTime }
    }

    fun clearCache() {
        cachedPrograms = null
    }
}
