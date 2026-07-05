package com.example.fliztv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fliztv.data.Channel
import com.example.fliztv.screens.components.ChannelCard
import com.example.fliztv.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    isTv: Boolean = false,
    onChannelClick: (Channel, List<Channel>) -> Unit,
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    viewModel: FavoritesViewModel = viewModel { FavoritesViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Text(
            text = "Favorites",
            fontSize = if (isTv) 32.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = if (isTv) 24.dp else 16.dp, top = if (isTv) 24.dp else 16.dp, bottom = if (isTv) 8.dp else 4.dp)
        )
        Text(
            text = "${state.favorites.size} channels",
            fontSize = if (isTv) 16.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = if (isTv) 24.dp else 16.dp, bottom = if (isTv) 20.dp else 12.dp)
        )

        if (state.favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No favorites yet", fontSize = if (isTv) 22.sp else 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(if (isTv) 16.dp else 8.dp))
                    Text(
                        "Tap the heart icon on any channel to add it here",
                        fontSize = if (isTv) 18.sp else 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTv) 4 else 2),
                contentPadding = PaddingValues(horizontal = if (isTv) 16.dp else 10.dp, vertical = if (isTv) 12.dp else 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.favorites, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.removeFavorite(channel.id) },
                        onClick = { onChannelClick(channel, state.favorites) },
                        logoLoader = logoLoader,
                        isTv = isTv
                    )
                }
            }
        }
    }
}
