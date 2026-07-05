package com.example.fliztv.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fliztv.data.Channel
import com.example.fliztv.data.FavoritesRepository
import com.example.fliztv.screens.components.ChannelCard
import com.example.fliztv.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

private val gradientPink = Color(0xFFFF0F7B)
private val gradientPurple = Color(0xFF8A2BE2)

@Composable
fun HomeScreen(
    isTv: Boolean = false,
    onCategoryClick: (String) -> Unit,
    onChannelClick: (Channel, List<Channel>) -> Unit,
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel { HomeViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedChip by remember { mutableStateOf("All") }

    val chips = remember(state.categories) {
        listOf("All") + state.categories
    }
    val popularChannels = remember(state.channelsByCategory) {
        val targetCategories = listOf("Movies", "News", "Music", "Sports", "Entertainment")
        targetCategories.flatMap { category ->
            state.channelsByCategory[category]
                ?.filter { it.status == "" }
                ?.shuffled()
                ?.take(2)
                ?: emptyList()
        }.shuffled()
    }
    val sportsChannels = remember(state.channelsByCategory) {
        state.channelsByCategory["Sports"]?.take(10) ?: emptyList()
    }

    val tvFactor = if (isTv) 1.5f else 1f
    val hPadding = if (isTv) 32.dp else 20.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = if (isTv) 32.dp else 16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPadding, vertical = if (isTv) 24.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FLIZ TV",
                    fontSize = if (isTv) 42.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradientPink,
                    modifier = Modifier.weight(1f)
                )

            }
        }

        // Hero Section — rotating featured channels
        item {
            val heroCategories = listOf("Movies", "News", "Sports", "Entertainment")
            val heroChannels = remember(state.channelsByCategory) {
                heroCategories.flatMap { category ->
                    state.channelsByCategory[category]?.filter { it.status == "" } ?: emptyList()
                }.let { liveChannels ->
                    if (liveChannels.isEmpty()) state.indianChannels.take(5)
                    else if (liveChannels.size <= 5) liveChannels
                    else liveChannels.shuffled().take(5)
                }
            }
            var currentIndex by remember { mutableStateOf(0) }

            LaunchedEffect(heroChannels) {
                if (heroChannels.size > 1) {
                    while (true) {
                        delay(7000)
                        currentIndex = (currentIndex + 1) % heroChannels.size
                    }
                }
            }

            if (heroChannels.isNotEmpty()) {
                var heroFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = hPadding)
                        .height(if (isTv) 320.dp else 200.dp)
                        .then(if (isTv) Modifier.focusable().onFocusChanged { heroFocused = it.isFocused } else Modifier)
                ) {
                    Crossfade(
                        targetState = currentIndex,
                        animationSpec = tween(700, easing = FastOutSlowInEasing),
                        label = "heroCrossfade"
                    ) { index ->
                        val heroChannel = heroChannels[index]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .then(
                                    if (isTv && heroFocused) Modifier.border(
                                        4.dp, Color.White, RoundedCornerShape(28.dp)
                                    ) else Modifier
                                )
                                .background(
                                    Brush.linearGradient(
                                        colors = when (index % 4) {
                                            0 -> listOf(gradientPink, gradientPurple)
                                            1 -> listOf(Color(0xFF1A237E), Color(0xFF4A148C))
                                            2 -> listOf(Color(0xFF004D40), Color(0xFF00695C))
                                            3 -> listOf(Color(0xFFE65100), Color(0xFFBF360C))
                                            else -> listOf(gradientPink, gradientPurple)
                                        }
                                    )
                                )
                                .clickable { onChannelClick(heroChannel, state.indianChannels) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(25.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (heroChannel.logo.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            logoLoader(heroChannel.logo, Modifier.fillMaxSize().padding(8.dp))
                                        }
                                        Spacer(Modifier.height(12.dp))
                                    }
                                    Text(
                                        text = heroChannel.name,
                                        fontSize = if (isTv) 30.sp else 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(if (isTv) 12.dp else 6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(if (isTv) 22.dp else 16.dp)
                                        )
                                        Spacer(Modifier.width(if (isTv) 10.dp else 6.dp))
                                        Text(
                                            text = if (heroChannel.category.isNotBlank()) heroChannel.category else "Live Channel",
                                            fontSize = if (isTv) 16.sp else 13.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(Modifier.height(if (isTv) 24.dp else 16.dp))
                                    Button(
                                        onClick = { onChannelClick(heroChannel, state.indianChannels) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = gradientPink
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(if (isTv) 56.dp else 42.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Watch Now",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dot indicators
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        heroChannels.indices.forEach { i ->
                            val isActive = i == currentIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 24.dp else 8.dp, 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isActive) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                                    .clickable { currentIndex = i }
                            )
                        }
                    }
                }
            }
        }

        // Section title: Categories
        item {
            Spacer(Modifier.height(if (isTv) 40.dp else 28.dp))
            Text(
                text = "Categories",
                fontSize = if (isTv) 28.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = hPadding)
            )
            Spacer(Modifier.height(if (isTv) 20.dp else 14.dp))
        }

        // Category chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = hPadding),
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 16.dp else 10.dp)
            ) {
                items(chips, key = { it }) { chip ->
                    val isActive = chip == selectedChip
                    var isFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (isActive) Modifier.background(
                                    Brush.linearGradient(listOf(gradientPink, gradientPurple))
                                ) else Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                            .focusable()
                            .onFocusChanged { isFocused = it.isFocused }
                            .clickable {
                                selectedChip = chip
                                if (chip != "All") {
                                    onCategoryClick(chip)
                                }
                            }
                            .padding(horizontal = if (isTv) 28.dp else 18.dp, vertical = if (isTv) 16.dp else 10.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = if (isTv) 18.sp else 14.sp,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Live Sports section
        if (sportsChannels.isNotEmpty()) {
            item {
                Spacer(Modifier.height(if (isTv) 40.dp else 28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = hPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isTv) 32.dp else 24.dp)
                    )
                    Spacer(Modifier.width(if (isTv) 12.dp else 8.dp))
                    Text(
                        text = "Live Sports",
                        fontSize = if (isTv) 28.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onCategoryClick("Sports") }) {
                        Text("View All", fontSize = if (isTv) 16.sp else 12.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(if (isTv) 18.dp else 14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(if (isTv) 20.dp else 14.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = hPadding),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 20.dp else 14.dp)
                ) {
                    items(sportsChannels, key = { it.url }) { channel ->
                        ContinueCard(
                            channel = channel,
                            onClick = { onChannelClick(channel, state.indianChannels) },
                            logoLoader = logoLoader,
                            isTv = isTv
                        )
                    }
                }
            }
        }

        // Continue Watching
        item {
            Spacer(Modifier.height(if (isTv) 40.dp else 28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = hPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Continue Watching",
                    fontSize = if (isTv) 28.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onCategoryClick(state.categories.firstOrNull() ?: "") }) {
                    Text("View All", fontSize = if (isTv) 16.sp else 12.sp, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(if (isTv) 18.dp else 14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(if (isTv) 20.dp else 14.dp))
        }

        // Continue Watching row
        item {
            val lastWatched = state.lastWatchedChannel
            val continueChannels = remember(state.indianChannels, lastWatched) {
                if (lastWatched != null) listOf(lastWatched)
                else emptyList()
            }
            if (continueChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Start watching channels to see them here",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(continueChannels, key = { it.id }) { channel ->
                        ContinueCard(
                            channel = channel,
                            onClick = { onChannelClick(channel, state.indianChannels) },
                            logoLoader = logoLoader,
                            isTv = isTv
                        )
                    }
                }
            }
        }

        // Section title: Popular Channels
        item {
            Spacer(Modifier.height(if (isTv) 40.dp else 28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = hPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Channels",
                    fontSize = if (isTv) 28.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onCategoryClick(state.categories.firstOrNull() ?: "") }) {
                    Text("View All", fontSize = if (isTv) 16.sp else 12.sp, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(if (isTv) 18.dp else 14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(if (isTv) 20.dp else 14.dp))
        }

        // Popular Channels grid
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading channels...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        } else if (state.error != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            val gridColumns = if (isTv) 3 else 2
            items(
                popularChannels.chunked(gridColumns),
                key = { it.joinToString("") { ch -> ch.id + ch.url } }
            ) { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = hPadding),
                    horizontalArrangement = Arrangement.spacedBy(if (isTv) 20.dp else 14.dp)
                ) {
                    rowChannels.forEach { channel ->
                        PopularCard(
                            channel = channel,
                            onClick = { onChannelClick(channel, state.indianChannels) },
                            modifier = Modifier.weight(1f),
                            logoLoader = logoLoader,
                            isTv = isTv
                        )
                    }
                    if (rowChannels.size < gridColumns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(if (isTv) 20.dp else 14.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(channel: Channel, onClick: () -> Unit, logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> }, isTv: Boolean = false) {
    var isFocused by remember { mutableStateOf(false) }
    val cardWidth = if (isTv) 260.dp else 180.dp
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(if (isTv) 28.dp else 22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isFocused) Modifier.border(
                    if (isTv) 4.dp else 2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(if (isTv) 28.dp else 22.dp)
                ) else Modifier
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(if (isTv) 24.dp else 18.dp)
    ) {
        LiveBadge()
        Spacer(Modifier.height(if (isTv) 18.dp else 12.dp))
        Box(
            modifier = Modifier
                .size(if (isTv) 90.dp else 60.dp)
                .clip(RoundedCornerShape(if (isTv) 18.dp else 12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                logoLoader(channel.logo, Modifier.fillMaxSize())
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isTv) 24.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(if (isTv) 18.dp else 12.dp))
        Text(
            text = channel.name,
            fontSize = if (isTv) 20.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(if (isTv) 8.dp else 4.dp))
        Text(
            text = if (channel.category.isNotBlank()) channel.category else "Live Channel",
            fontSize = if (isTv) 16.sp else 13.sp,
            color = Color(0xFF999999),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PopularCard(channel: Channel, onClick: () -> Unit, modifier: Modifier = Modifier, logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> }, isTv: Boolean = false) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isTv) 28.dp else 20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isFocused) Modifier.border(
                    if (isTv) 4.dp else 2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(if (isTv) 28.dp else 20.dp)
                ) else Modifier
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(if (isTv) 24.dp else 16.dp)
    ) {
        LiveBadge()
        Spacer(Modifier.height(if (isTv) 20.dp else 12.dp))
        Box(
            modifier = Modifier
                .size(if (isTv) 100.dp else 72.dp)
                .clip(RoundedCornerShape(if (isTv) 20.dp else 14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                logoLoader(channel.logo, Modifier.fillMaxSize())
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isTv) 26.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(if (isTv) 20.dp else 12.dp))
        Text(
            text = channel.name,
            fontSize = if (isTv) 18.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFFF3B3B))
        )
        Text(
            text = "LIVE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF3B3B)
        )
    }
}
