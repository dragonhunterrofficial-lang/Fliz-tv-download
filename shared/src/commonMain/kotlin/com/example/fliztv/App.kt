package com.example.fliztv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fliztv.data.Channel
import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.player.PlayerState
import com.example.fliztv.data.FavoritesRepository
import kotlinx.coroutines.delay
import com.example.fliztv.screens.CategoriesScreen
import com.example.fliztv.screens.CategoryChannelsScreen
import com.example.fliztv.screens.FavoritesScreen
import com.example.fliztv.screens.HomeScreen
import com.example.fliztv.screens.PlayerScreen
import com.example.fliztv.screens.ProfileScreen

@Composable
private fun TvNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                else Modifier
            )
            .then(
                if (isFocused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class Screen {
    HOME, LIVE_TV, FAVORITES, SETTINGS
}

private val ModernDarkScheme = darkColorScheme(
    primary = Color(0xFFFF0F7B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0535),
    onPrimaryContainer = Color(0xFFFFD6E6),
    secondary = Color(0xFFB03CFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0A7A),
    onSecondaryContainer = Color(0xFFE8D0FF),
    tertiary = Color(0xFFFFB86C),
    onTertiary = Color(0xFF2D1A00),
    background = Color(0xFF0A0A18),
    onBackground = Color(0xFFE2E2F0),
    surface = Color(0xFF161625),
    onSurface = Color(0xFFE2E2F0),
    surfaceVariant = Color(0xFF1E1E30),
    onSurfaceVariant = Color(0xFFB8B8CC),
    outline = Color(0xFF2D2D48),
    outlineVariant = Color(0xFF1E1E38),
    error = Color(0xFFFF5A5F),
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun App(
    isTv: Boolean = false,
    playerProvider: @Composable (Channel, Modifier, PlayerState, Boolean, String, String, (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> },
    onEnterPip: () -> Unit = {},
    onVolumeUp: () -> Unit = {},
    onVolumeDown: () -> Unit = {},
    onBrightnessUp: () -> Unit = {},
    onBrightnessDown: () -> Unit = {},
    onPlayerActiveChanged: (Boolean) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onCastClick: () -> Unit = {},
    isCasting: Boolean = false,
    castDeviceName: String = "",
    colorScheme: ColorScheme = ModernDarkScheme
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var channelList by remember { mutableStateOf<List<Channel>>(emptyList()) }

    fun getNextChannel(): Channel? {
        val idx = channelList.indexOf(selectedChannel)
        if (idx >= 0 && idx < channelList.size - 1) return channelList[idx + 1]
        return null
    }

    fun getPrevChannel(): Channel? {
        val idx = channelList.indexOf(selectedChannel)
        if (idx > 0) return channelList[idx - 1]
        return null
    }

    fun handleChannelClick(channel: Channel, list: List<Channel> = emptyList()) {
        if (list.isNotEmpty()) channelList = list
        ChannelRepository.saveLastWatchedChannel(channel.id)
        selectedChannel = channel
    }

    val isPlayerActive = selectedChannel != null
    LaunchedEffect(isPlayerActive) {
        onPlayerActiveChanged(isPlayerActive)
    }

    MaterialTheme(colorScheme = colorScheme) {
        val canGoBack = selectedChannel != null || selectedCategory != null
        backHandler(canGoBack) {
            when {
                selectedChannel != null -> selectedChannel = null
                selectedCategory != null -> selectedCategory = null
            }
        }
        if (selectedChannel != null) {
            val currentChannel = selectedChannel!!
            val playerState = remember(currentChannel) { PlayerState() }

            var isFav by remember { mutableStateOf(FavoritesRepository.isFavorite(currentChannel.id)) }

            var isRecording by remember(currentChannel.id) { mutableStateOf(false) }
            var recordingStartTimeMs by remember(currentChannel.id) { mutableLongStateOf(0L) }
            var recordingError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(recordingError) {
                recordingError?.let {
                    delay(5000)
                    recordingError = null
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                key(currentChannel.id) {
                    PlayerScreen(
                        channel = currentChannel,
                        onBack = { selectedChannel = null },
                        isTv = isTv,
                        onVolumeUp = onVolumeUp,
                        onVolumeDown = onVolumeDown,
                        onBrightnessUp = onBrightnessUp,
                        onBrightnessDown = onBrightnessDown,
                        onNextChannel = {
                            getNextChannel()?.let { ch ->
                                selectedChannel = ch
                            }
                        },
                        onPrevChannel = {
                            getPrevChannel()?.let { ch ->
                                selectedChannel = ch
                            }
                        },
                        isFavorite = isFav,
                        isRecording = isRecording,
                        recordingStartTimeMs = recordingStartTimeMs,
                        onFavoriteClick = {
                            FavoritesRepository.toggleFavorite(currentChannel.id)
                            isFav = !isFav
                        },
                        onRecordClick = {
                            isRecording = !isRecording
                            if (isRecording) {
                                recordingStartTimeMs = currentEpochSeconds() * 1000
                            } else {
                                recordingStartTimeMs = 0L
                            }
                        },
                        onCastClick = onCastClick,
                        playerState = playerState,
                        playerContent = { modifier ->
                            playerProvider(currentChannel, modifier, playerState, isRecording, currentChannel.name, currentChannel.id) { msg ->
                                recordingError = msg
                            }
                        }
                    )
                }
            }
        } else if (selectedCategory != null) {
            val currentCategory = selectedCategory!!
            CategoryChannelsScreen(
                category = currentCategory,
                onChannelClick = { channel, list ->
                    handleChannelClick(channel, list)
                },
                onBack = { selectedCategory = null },
                logoLoader = logoLoader
            )
        } else if (isTv) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "FLIZ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    TvNavItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        selected = currentScreen == Screen.HOME,
                        onClick = { currentScreen = Screen.HOME }
                    )
                    TvNavItem(
                        icon = Icons.Default.PlayArrow,
                        label = "Live TV",
                        selected = currentScreen == Screen.LIVE_TV,
                        onClick = { currentScreen = Screen.LIVE_TV }
                    )
                    TvNavItem(
                        icon = Icons.Default.Favorite,
                        label = "Favorites",
                        selected = currentScreen == Screen.FAVORITES,
                        onClick = { currentScreen = Screen.FAVORITES }
                    )
                    TvNavItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        selected = currentScreen == Screen.SETTINGS,
                        onClick = { currentScreen = Screen.SETTINGS }
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.background)) {
                    when (currentScreen) {
                        Screen.HOME -> HomeScreen(
                            isTv = true,
                            onCategoryClick = { category ->
                                selectedCategory = category
                            },
                            onChannelClick = { channel, list ->
                                handleChannelClick(channel, list)
                            },
                            logoLoader = logoLoader
                        )
                        Screen.LIVE_TV -> CategoriesScreen(
                            isTv = true,
                            onChannelClick = { channel, list ->
                                handleChannelClick(channel, list)
                            },
                            logoLoader = logoLoader
                        )
                        Screen.FAVORITES -> FavoritesScreen(
                            isTv = true,
                            onChannelClick = { channel, list ->
                                handleChannelClick(channel, list)
                            },
                            logoLoader = logoLoader
                        )
                        Screen.SETTINGS -> ProfileScreen(
                            isTv = true,
                            onOpenUrl = onOpenUrl,
                            onPlayRecording = { recording ->
                                selectedChannel = com.example.fliztv.data.Channel(
                                    id = recording.id,
                                    name = recording.channelName,
                                    url = recording.filePath.ifEmpty { recording.streamUrl },
                                    logo = "",
                                    country = "",
                                    category = "Recordings",
                                    language = "",
                                    userAgent = "",
                                    referer = ""
                                )
                            }
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.HOME,
                            onClick = { currentScreen = Screen.HOME },
                            icon = { Icon(Icons.Default.Home, contentDescription = null, tint = if (currentScreen == Screen.HOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.LIVE_TV,
                            onClick = { currentScreen = Screen.LIVE_TV },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (currentScreen == Screen.LIVE_TV) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { Text("Live TV", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.FAVORITES,
                            onClick = { currentScreen = Screen.FAVORITES },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = if (currentScreen == Screen.FAVORITES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { Text("Favorites", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.SETTINGS,
                            onClick = { currentScreen = Screen.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = if (currentScreen == Screen.SETTINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreen(
                                onCategoryClick = { category ->
                                    selectedCategory = category
                                },
                                onChannelClick = { channel, list ->
                                    handleChannelClick(channel, list)
                                },
                                logoLoader = logoLoader
                            )
                            Screen.LIVE_TV -> CategoriesScreen(
                                onChannelClick = { channel, list ->
                                    handleChannelClick(channel, list)
                                },
                                logoLoader = logoLoader
                            )
                            Screen.FAVORITES -> FavoritesScreen(
                                onChannelClick = { channel, list ->
                                    handleChannelClick(channel, list)
                                },
                                logoLoader = logoLoader
                            )
                            Screen.SETTINGS -> ProfileScreen(
                                onOpenUrl = onOpenUrl,
                                onPlayRecording = { recording ->
                                    selectedChannel = com.example.fliztv.data.Channel(
                                        id = recording.id,
                                        name = recording.channelName,
                                        url = recording.filePath.ifEmpty { recording.streamUrl },
                                        logo = "",
                                        country = "",
                                        category = "Recordings",
                                        language = "",
                                        userAgent = "",
                                        referer = ""
                                    )
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
