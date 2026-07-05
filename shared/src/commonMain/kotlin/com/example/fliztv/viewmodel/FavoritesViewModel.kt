package com.example.fliztv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fliztv.data.Channel
import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.data.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FavoritesUiState(
    val favorites: List<Channel> = emptyList(),
    val isLoading: Boolean = false
)

class FavoritesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val channels = withContext(Dispatchers.Default) {
                    ChannelRepository.getAllChannels()
                }
                val ids = FavoritesRepository.getFavorites()
                val favs = channels.filter { it.id in ids }
                _uiState.value = _uiState.value.copy(favorites = favs, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun removeFavorite(channelId: String) {
        FavoritesRepository.removeFavorite(channelId)
        _uiState.value = _uiState.value.copy(
            favorites = _uiState.value.favorites.filter { it.id != channelId }
        )
    }
}
