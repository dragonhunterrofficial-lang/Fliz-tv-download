package com.example.fliztv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fliztv.data.Channel
import com.example.fliztv.screens.components.ChannelCard
import com.example.fliztv.viewmodel.HomeViewModel

@Composable
fun CategoryChannelsScreen(
    category: String,
    isTv: Boolean = false,
    onChannelClick: (Channel, List<Channel>) -> Unit,
    onBack: () -> Unit,
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = remember(category, state.indianChannels) {
        viewModel.getChannelsForCategory(category)
    }

    LaunchedEffect(category) {
        viewModel.checkCategoryHealth(category)
    }

    val sortedChannels = remember(channels, state.channelHealth) {
        channels.sortedBy { channel ->
            when (state.channelHealth[channel.url]) {
                true -> 0
                null -> 1
                false -> 2
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(if (isTv) 8.dp else 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 12.dp else 4.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isTv) 32.dp else 24.dp)
                )
            }
            Spacer(Modifier.width(if (isTv) 12.dp else 4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    fontSize = if (isTv) 24.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${channels.size} channels",
                    fontSize = if (isTv) 16.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No channels in this category", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (isTv) 18.sp else 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTv) 4 else 2),
                contentPadding = PaddingValues(horizontal = if (isTv) 16.dp else 10.dp, vertical = if (isTv) 12.dp else 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedChannels, key = { it.id + it.url }) { channel ->
                    val isOffline = state.channelHealth[channel.url] == false
                    ChannelCard(
                            channel = channel,
                            isFavorite = channel.id in state.favoriteIds,
                            isOffline = isOffline,
                            onFavoriteClick = { viewModel.toggleFavorite(channel.id) },
                            onClick = { onChannelClick(channel, sortedChannels) },
                            logoLoader = logoLoader,
                            isTv = isTv
                        )
                }
            }
        }
    }
}
