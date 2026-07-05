package com.example.fliztv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fliztv.data.Channel
import com.example.fliztv.screens.components.ChannelCard
import com.example.fliztv.viewmodel.HomeViewModel

@Composable
fun CategoriesScreen(
    isTv: Boolean = false,
    onChannelClick: (Channel, List<Channel>) -> Unit,
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(if (isTv) 16.dp else 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Channels",
                    fontSize = if (isTv) 32.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!state.isLoading) {
                    Text(
                        text = "${state.indianChannels.size} channels",
                        fontSize = if (isTv) 16.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isTv) 32.dp else 24.dp)
                )
            }
        }

        Spacer(Modifier.height(if (isTv) 16.dp else 8.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search channels...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (isTv) 18.sp else 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isTv) 28.dp else 20.dp)) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 24.dp else 16.dp, vertical = if (isTv) 12.dp else 6.dp)
                .focusable(),
            singleLine = true,
            textStyle = if (isTv) MaterialTheme.typography.bodyLarge else androidx.compose.material3.LocalTextStyle.current,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(if (isTv) 16.dp else 12.dp)
        )

        Spacer(Modifier.height(if (isTv) 12.dp else 4.dp))

        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> ErrorDisplay(state.error!!) { viewModel.loadChannels() }
            else -> {
                val query = state.searchQuery
                val allChannels = remember(state.indianChannels, query) {
                    if (query.isBlank()) state.indianChannels
                    else state.indianChannels.filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.language.contains(query, ignoreCase = true)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isTv) 4 else 2),
                    contentPadding = PaddingValues(horizontal = if (isTv) 16.dp else 10.dp, vertical = if (isTv) 12.dp else 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allChannels, key = { it.id + it.url }) { channel ->
                        val isOffline = state.channelHealth[channel.url] == false
                        ChannelCard(
                            channel = channel,
                            isFavorite = channel.id in state.favoriteIds,
                            isOffline = isOffline,
                            onFavoriteClick = { viewModel.toggleFavorite(channel.id) },
                            onClick = { onChannelClick(channel, allChannels) },
                            logoLoader = logoLoader,
                            isTv = isTv
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("Loading channels...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorDisplay(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
