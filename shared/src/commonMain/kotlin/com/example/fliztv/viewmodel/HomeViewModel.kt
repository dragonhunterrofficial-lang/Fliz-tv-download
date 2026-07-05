package com.example.fliztv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fliztv.data.Channel
import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.data.FavoritesRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.example.fliztv.network.httpClient

object LanguagePrefs {
    var selectedLanguages: Set<String> = emptySet()
}

val MAIN_CATEGORIES = listOf("Entertainment", "Movies", "Music", "News", "Kids", "Devotional", "Geographical", "History", "Sports", "Animal Planet", "Other")

data class HomeUiState(
    val indianChannels: List<Channel> = emptyList(),
    val englishChannels: List<Channel> = emptyList(),
    val allChannels: List<Channel> = emptyList(),
    val lastWatchedChannel: Channel? = null,
    val languages: List<String> = emptyList(),
    val selectedLanguages: Set<String> = emptySet(),
    val categories: List<String> = MAIN_CATEGORIES,
    val channelsByCategory: Map<String, List<Channel>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val favoriteIds: Set<String> = emptySet(),
    val channelHealth: Map<String, Boolean> = emptyMap()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(selectedLanguages = LanguagePrefs.selectedLanguages)
        loadChannels()
    }

    companion object {
    }

    fun getLanguages(): List<String> = _uiState.value.languages

    fun toggleLanguage(language: String) {
        val current = LanguagePrefs.selectedLanguages
        val updated = if (language in current) current - language else current + language
        LanguagePrefs.selectedLanguages = updated
        applyLanguageFilter(updated)
    }

    fun selectAllLanguages() {
        LanguagePrefs.selectedLanguages = emptySet()
        applyLanguageFilter(emptySet())
    }

    fun refreshLanguageFilter() {
        applyLanguageFilter(LanguagePrefs.selectedLanguages)
    }

    private fun applyLanguageFilter(selected: Set<String>) {
        val state = _uiState.value.copy(selectedLanguages = selected)
        val languageChannels = getLanguageChannels(state)
        val channelsByCategory = languageChannels
            .groupBy { mapToMainCategory(it.category) }
            .mapValues { (_, chs) -> chs.sortedWith(compareBy(Channel::statusOrder, Channel::name)) }
        val categories = buildList {
            addAll(MAIN_CATEGORIES.filter { it in channelsByCategory })
            if (state.englishChannels.isNotEmpty()) add("International")
        }
        _uiState.value = state.copy(categories = categories, channelsByCategory = channelsByCategory)
    }

    private fun getLanguageChannels(state: HomeUiState): List<Channel> {
        val selected = state.selectedLanguages
        return if (selected.isEmpty()) state.indianChannels
        else state.indianChannels.filter { it.language in selected }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val allChannels = withContext(Dispatchers.Default) {
                    ChannelRepository.getAllChannels()
                }
                val indian = allChannels.filter { it.country == "India" }
                val english = allChannels.filter {
                    val lang = it.language.lowercase()
                    val country = it.country.lowercase()
                    (lang == "english" || country in listOf("united states", "united kingdom", "uk", "canada", "australia", "new zealand", "ireland")) &&
                    country != "india"
                }
                val languages = indian.map { it.language }.filter { it.isNotBlank() }.distinct().sorted()
                val lastWatchedId = ChannelRepository.getLastWatchedChannelId()
                val lastWatched = if (lastWatchedId.isNotBlank()) {
                    allChannels.find { it.id == lastWatchedId }
                } else null
                val baseState = _uiState.value.copy(
                    indianChannels = indian,
                    englishChannels = english,
                    allChannels = allChannels,
                    lastWatchedChannel = lastWatched,
                    languages = languages,
                    favoriteIds = FavoritesRepository.getFavorites().toSet(),
                    isLoading = false
                )
                val languageChannels = getLanguageChannels(baseState)
                val channelsByCategory = languageChannels
                    .groupBy { mapToMainCategory(it.category) }
                    .mapValues { (_, chs) -> chs.sortedWith(compareBy(Channel::statusOrder, Channel::name)) }
                val categories = buildList {
                    addAll(MAIN_CATEGORIES.filter { it in channelsByCategory })
                    if (english.isNotEmpty()) add("International")
                }
                _uiState.value = baseState.copy(
                    categories = categories,
                    channelsByCategory = channelsByCategory
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load channels"
                )
            }
        }
    }

    fun checkCategoryHealth(category: String) {
        val channels = _uiState.value.channelsByCategory[category] ?: return
        viewModelScope.launch {
            channels.chunked(5).forEach { batch ->
                val results = batch.map { chan ->
                    async {
                        chan.url to isStreamReachable(chan.url, chan.userAgent, chan.referer)
                    }
                }
                results.forEach { deferred ->
                    val (url, healthy) = deferred.await()
                    _uiState.value = _uiState.value.copy(
                        channelHealth = _uiState.value.channelHealth + (url to healthy)
                    )
                }
            }
        }
    }

    private suspend fun isStreamReachable(url: String, userAgent: String = "", referer: String = ""): Boolean {
        val ua = userAgent.ifEmpty { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36" }
        return try {
            withTimeout(15000) {
                httpClient.get(url) {
                    header("User-Agent", ua)
                    header("Range", "bytes=0-0")
                    if (referer.isNotEmpty()) header("Referer", referer)
                }.bodyAsText()
                true
            }
        } catch (_: Exception) {
            try {
                withTimeout(10000) {
                    httpClient.get(url) {
                        header("User-Agent", ua)
                        if (referer.isNotEmpty()) header("Referer", referer)
                    }.bodyAsText()
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun refresh() {
        ChannelRepository.clearCache()
        loadChannels()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredCategories(): List<String> {
        val state = _uiState.value
        val query = state.searchQuery
        val cats = state.categories
        return if (query.isBlank()) cats
        else cats.filter { it.contains(query, ignoreCase = true) }
    }

    fun getChannelsForCategory(category: String): List<Channel> {
        if (category == "International") return _uiState.value.englishChannels
        return _uiState.value.channelsByCategory[category] ?: emptyList()
    }

    fun getCategoryChannelCount(category: String): Int {
        return _uiState.value.channelsByCategory[category]?.size ?: 0
    }

    fun toggleFavorite(channelId: String) {
        FavoritesRepository.toggleFavorite(channelId)
        _uiState.value = _uiState.value.copy(
            favoriteIds = FavoritesRepository.getFavorites().toSet()
        )
    }

    fun isFavorite(channelId: String): Boolean = FavoritesRepository.isFavorite(channelId)

    private fun mapToMainCategory(originalCategory: String): String {
        if (originalCategory.isBlank()) return "Other"
        val lower = originalCategory.lowercase()
        return when {
            lower.contains("news") -> "News"
            lower.contains("sport") -> "Sports"
            lower.contains("history") || lower.contains("documentary") -> "History"
            lower.contains("animal") || lower.contains("wildlife") -> "Animal Planet"
            lower.contains("travel") || lower.contains("geograph") || lower.contains("outdoor") ||
                lower.contains("science") || lower.contains("nature") || lower.contains("adventure") -> "Geographical"
            lower.contains("music") -> "Music"
            lower.contains("movie") || lower.contains("cinema") || lower.contains("film") ||
                lower.contains("action") || lower.contains("thriller") || lower.contains("horror") ||
                lower.contains("bollywood") || lower.contains("hollywood") -> "Movies"
            lower.contains("kids") || lower.contains("animation") || lower.contains("cartoon") ||
                lower.contains("children") || lower.contains("preschool") || lower.contains("junior") -> "Kids"
            lower.contains("devotional") || lower.contains("religious") || lower.contains("spiritual") ||
                lower.contains("prayer") || lower.contains("bhajan") || lower.contains("aarti") ||
                lower.contains("temple") || lower.contains("gita") || lower.contains("islamic") ||
                lower.contains("faith") || lower.contains("god") || lower.contains("divine") -> "Devotional"
            lower.contains("entertainment") || lower.contains("comedy") ||
                lower.contains("show") ||
                lower.contains("education") || lower.contains("cooking") ||
                lower.contains("lifestyle") || lower.contains("culture") || lower.contains("general") ||
                lower.contains("classic") || lower.contains("drama") || lower.contains("series") ||
                lower.contains("business") || lower.contains("health") -> "Entertainment"
            else -> "Other"
        }
    }
}

private fun Channel.statusOrder(): Int = when (status) {
    "" -> 0
    "Not 24/7" -> 1
    else -> 2
}
