package com.example.fliztv.data

import com.example.fliztv.currentEpochSeconds
import com.example.fliztv.network.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EpgManager {
    private val epgSources = listOf(
        EpgSource("in", "India", "https://iptv-epg.org/files/epg-in.xml", isDefault = true,
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/in.xml",
                "https://iptv-org.github.io/epg/guides/in.xml"
            )),
        EpgSource("us", "United States", "https://iptv-epg.org/files/epg-us.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/us.xml",
                "https://iptv-org.github.io/epg/guides/us.xml"
            )),
        EpgSource("gb", "United Kingdom", "https://iptv-epg.org/files/epg-gb.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/gb.xml",
                "https://iptv-org.github.io/epg/guides/gb.xml"
            )),
        EpgSource("ca", "Canada", "https://iptv-epg.org/files/epg-ca.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/ca.xml",
                "https://iptv-org.github.io/epg/guides/ca.xml"
            )),
        EpgSource("au", "Australia", "https://iptv-epg.org/files/epg-au.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/au.xml",
                "https://iptv-org.github.io/epg/guides/au.xml"
            )),
        EpgSource("de", "Germany", "https://iptv-epg.org/files/epg-de.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/de.xml",
                "https://iptv-org.github.io/epg/guides/de.xml"
            )),
        EpgSource("fr", "France", "https://iptv-epg.org/files/epg-fr.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/fr.xml",
                "https://iptv-org.github.io/epg/guides/fr.xml"
            )),
        EpgSource("it", "Italy", "https://iptv-epg.org/files/epg-it.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/it.xml",
                "https://iptv-org.github.io/epg/guides/it.xml"
            )),
        EpgSource("es", "Spain", "https://iptv-epg.org/files/epg-es.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/es.xml",
                "https://iptv-org.github.io/epg/guides/es.xml"
            )),
        EpgSource("pt", "Portugal", "https://iptv-epg.org/files/epg-pt.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/pt.xml",
                "https://iptv-org.github.io/epg/guides/pt.xml"
            )),
        EpgSource("br", "Brazil", "https://iptv-epg.org/files/epg-br.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/br.xml",
                "https://iptv-org.github.io/epg/guides/br.xml"
            )),
        EpgSource("ar", "Argentina", "https://iptv-epg.org/files/epg-ar.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/ar.xml",
                "https://iptv-org.github.io/epg/guides/ar.xml"
            )),
        EpgSource("bd", "Bangladesh", "https://iptv-epg.org/files/epg-bd.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/bd.xml",
                "https://iptv-org.github.io/epg/guides/bd.xml"
            )),
        EpgSource("pk", "Pakistan", "https://iptv-epg.org/files/epg-pk.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/pk.xml",
                "https://iptv-org.github.io/epg/guides/pk.xml"
            )),
        EpgSource("lk", "Sri Lanka", "https://iptv-epg.org/files/epg-lk.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/lk.xml",
                "https://iptv-org.github.io/epg/guides/lk.xml"
            )),
        EpgSource("np", "Nepal", "https://iptv-epg.org/files/epg-np.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/np.xml",
                "https://iptv-org.github.io/epg/guides/np.xml"
            )),
        EpgSource("ae", "UAE", "https://iptv-epg.org/files/epg-ae.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/ae.xml",
                "https://iptv-org.github.io/epg/guides/ae.xml"
            )),
        EpgSource("sa", "Saudi Arabia", "https://iptv-epg.org/files/epg-sa.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/sa.xml",
                "https://iptv-org.github.io/epg/guides/sa.xml"
            )),
        EpgSource("za", "South Africa", "https://iptv-epg.org/files/epg-za.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/za.xml",
                "https://iptv-org.github.io/epg/guides/za.xml"
            )),
        EpgSource("ng", "Nigeria", "https://iptv-epg.org/files/epg-ng.xml",
            fallbackUrls = listOf(
                "https://raw.githubusercontent.com/iptv-org/epg/master/guides/ng.xml",
                "https://iptv-org.github.io/epg/guides/ng.xml"
            ))
    )
    @Volatile
    private var cachedPrograms: List<EpgProgram>? = null
    @Volatile
    private var cacheTime: Long = 0
    private var epgSourceUrl: String = epgSources.first().url
    private val cacheDurationMs = 30 * 60 * 1000L

    private val globalFallbackUrls = listOf(
        "https://iptv-org.github.io/epg/guides/in.xml",
        "https://raw.githubusercontent.com/iptv-org/epg/master/guides/all.xml",
        "https://epg.pm/epg.xml"
    )

    fun getSources(): List<EpgSource> = epgSources

    fun setActiveSource(url: String) {
        synchronized(this) {
            epgSourceUrl = url
            cachedPrograms = null
            cacheTime = 0
        }
    }

    suspend fun fetchPrograms(forceRefresh: Boolean = false): List<EpgProgram> {
        val now = currentEpochSeconds() * 1000
        val cached = cachedPrograms
        if (!forceRefresh && cached != null && (now - cacheTime) < cacheDurationMs) {
            return cached
        }
        return withContext(Dispatchers.Default) {
            val urlsToTry = buildList {
                add(epgSourceUrl)
                val source = epgSources.find { it.url == epgSourceUrl }
                if (source != null) {
                    addAll(source.fallbackUrls)
                }
                addAll(globalFallbackUrls)
            }

            for (url in urlsToTry) {
                try {
                    val xml = httpClient.get(url).bodyAsText()
                    val programs = XmltvParser.parse(xml)
                    if (programs.isNotEmpty()) {
                        cachedPrograms = programs
                        cacheTime = now
                        return@withContext programs
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            cachedPrograms ?: emptyList()
        }
    }

    suspend fun fetchProgramsForChannel(channelId: String, tvgUrl: String): List<EpgProgram> {
        if (tvgUrl.isBlank()) return emptyList()
        val norm = normalizeId(channelId)
        val cached = cachedPrograms?.filter { normalizeId(it.channelId) == norm }
        if (cached != null && cached.isNotEmpty()) return cached

        return withContext(Dispatchers.Default) {
            try {
                val xml = httpClient.get(tvgUrl).bodyAsText()
                val programs = XmltvParser.parse(xml).filter { normalizeId(it.channelId) == norm }
                if (programs.isNotEmpty()) {
                    cachedPrograms = (cachedPrograms ?: emptyList()) + programs
                }
                programs
            } catch (_: Exception) {
                cachedPrograms?.filter { normalizeId(it.channelId) == norm } ?: emptyList()
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
