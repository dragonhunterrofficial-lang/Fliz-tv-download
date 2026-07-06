package com.example.fliztv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.data.FavoritesRepository
import com.example.fliztv.data.PlaylistManager
import com.example.fliztv.data.PlaylistSource
import com.example.fliztv.data.Recording
import com.example.fliztv.data.UpdateManager
import com.example.fliztv.data.UpdateInfo
import com.example.fliztv.viewmodel.HomeViewModel
import com.example.fliztv.viewmodel.LanguagePrefs
import kotlinx.coroutines.launch

private val gradientPink = Color(0xFFFF0F7B)
private val gradientPurple = Color(0xFF8A2BE2)

@Composable
fun ProfileScreen(
    isTv: Boolean = false,
    onOpenUrl: (String) -> Unit = {},
    onPlayRecording: (Recording) -> Unit = {}
) {
    var showingSettings by remember { mutableStateOf(false) }
    var showPlaylistSources by remember { mutableStateOf(false) }
    var showNotificationToggle by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showLanguageSettings by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showRecordings by remember { mutableStateOf(false) }
    var showUpdateCheck by remember { mutableStateOf(false) }
    val homeViewModel: HomeViewModel = viewModel { HomeViewModel() }

    when {
        showingSettings -> SettingsContent(onBack = { showingSettings = false }, isTv = isTv)
        showPlaylistSources -> PlaylistSourcesScreen(onBack = { showPlaylistSources = false }, isTv = isTv)
        showGuide -> GuideScreen(onBack = { showGuide = false }, isTv = isTv)
        showRecordings -> RecordingsScreen(onBack = { showRecordings = false }, isTv = isTv, onPlayRecording = onPlayRecording)
        showUpdateCheck -> UpdateCheckDialog(onDismiss = { showUpdateCheck = false })
        showNotificationToggle -> NotificationToggleDialog(
            onDismiss = { showNotificationToggle = false }
        )
        showPrivacyPolicy -> PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicy = false }
        )
        showLanguageSettings -> LanguageSettingsDialog(
            onDismiss = {
                showLanguageSettings = false
                homeViewModel.refreshLanguageFilter()
            }
        )
        else -> SettingsMain(
            isTv = isTv,
            onOpenSettings = { showingSettings = true },
            onOpenNotifications = { showNotificationToggle = true },
            onOpenPrivacy = { showPrivacyPolicy = true },
            onOpenLanguage = { showLanguageSettings = true },
            onOpenGuide = { showGuide = true },
            onOpenRecordings = { showRecordings = true },
            onOpenUpdate = { showUpdateCheck = true },
            onOpenPlaylistSources = { showPlaylistSources = true },
            onOpenUrl = onOpenUrl
        )
    }
}

@Composable
private fun SettingsMain(
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLanguage: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenRecordings: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onOpenPlaylistSources: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    isTv: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(if (isTv) 32.dp else 16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 32.dp else 20.dp)
                .clip(RoundedCornerShape(if (isTv) 28.dp else 20.dp))
                .background(
                    Brush.linearGradient(colors = listOf(gradientPink, gradientPurple))
                )
                .padding(if (isTv) 32.dp else 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (isTv) 80.dp else 56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isTv) 40.dp else 28.dp)
                    )
                }
                Spacer(Modifier.width(if (isTv) 24.dp else 16.dp))
                Column {
                    Text(
                        text = "FLIZ TV",
                        fontSize = if (isTv) 28.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(if (isTv) 6.dp else 2.dp))
                    Text(
                        text = "FLIZ TV Member",
                        fontSize = if (isTv) 18.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(if (isTv) 24.dp else 16.dp))

        Column(modifier = Modifier.padding(horizontal = if (isTv) 32.dp else 20.dp)) {
            ProfileSettingItem(
                icon = Icons.Default.Dns,
                label = "Playlist Sources",
                onClick = onOpenPlaylistSources,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.PlayCircle,
                label = "Playback Settings",
                onClick = onOpenSettings,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.Schedule,
                label = "Program Guide (EPG)",
                onClick = onOpenGuide,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.Videocam,
                label = "Recordings",
                onClick = onOpenRecordings,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.Update,
                label = "Check for Updates",
                onClick = onOpenUpdate,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.Language,
                label = "Language Settings",
                onClick = onOpenLanguage,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.PrivacyTip,
                label = "Privacy Policy",
                onClick = onOpenPrivacy,
                isTv = isTv
            )
            Spacer(Modifier.height(if (isTv) 16.dp else 10.dp))
            ProfileSettingItem(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                onClick = onOpenNotifications,
                isTv = isTv
            )
        }

        Spacer(Modifier.height(if (isTv) 40.dp else 24.dp))

        Text(
            text = "v2.0.1",
            fontSize = if (isTv) 16.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(if (isTv) 32.dp else 16.dp))
    }
}

@Composable
private fun SettingsContent(onBack: () -> Unit, isTv: Boolean = false) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var refreshed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var backFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .focusable()
                    .onFocusChanged { backFocused = it.isFocused }
                    .then(
                        if (backFocused) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Playback Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))

                    var refreshFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (isTv) Modifier.focusable().onFocusChanged { refreshFocused = it.isFocused } else Modifier.focusable().onFocusChanged { refreshFocused = it.isFocused })
                            .then(
                                if (isTv && refreshFocused) Modifier.border(
                                    3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                ) else if (!isTv && refreshFocused) Modifier.border(
                                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .clickable {
                                ChannelRepository.clearCache()
                                refreshed = true
                            }
                            .padding(vertical = if (isTv) 16.dp else 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Refresh Channels", fontSize = if (isTv) 18.sp else 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (refreshed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isTv) 28.dp else 20.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    var clearFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (isTv) Modifier.focusable().onFocusChanged { clearFocused = it.isFocused } else Modifier.focusable().onFocusChanged { clearFocused = it.isFocused })
                            .then(
                                if (isTv && clearFocused) Modifier.border(
                                    3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                ) else if (!isTv && clearFocused) Modifier.border(
                                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .clickable { showClearConfirm = true }
                            .padding(vertical = if (isTv) 16.dp else 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clear Favorites", fontSize = if (isTv) 18.sp else 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${FavoritesRepository.size}", fontSize = if (isTv) 18.sp else 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("fliz tv", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2.0.1", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Data Source", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("iptv-org/iptv", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Powered by iptv-org. No video files are stored in this app.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text("Clear Favorites?", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text("Remove all ${FavoritesRepository.size} favorite channels?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = {
                    FavoritesRepository.clear()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun LanguageSettingsDialog(onDismiss: () -> Unit) {
    val languages = remember {
        listOf("Hindi", "English", "Bengali", "Urdu", "Tamil", "Telugu", "Kannada", "Malayalam",
            "Marathi", "Gujarati", "Punjabi", "Odia", "Assamese", "Spanish", "French", "Arabic",
            "Portuguese", "Russian", "German", "Italian", "Japanese", "Chinese", "Korean", "Turkish")
    }
    var selected by remember { mutableStateOf(LanguagePrefs.selectedLanguages) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Languages", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Channels will show for all selected languages",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (lang in selected) selected - lang else selected + lang
                                LanguagePrefs.selectedLanguages = selected
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = lang in selected,
                            onCheckedChange = {
                                selected = if (it) selected + lang else selected - lang
                                LanguagePrefs.selectedLanguages = selected
                            },
                            colors = CheckboxDefaults.colors(checkedColor = gradientPink)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(lang, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = gradientPink)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                LanguagePrefs.selectedLanguages = emptySet()
                selected = emptySet()
            }) {
                Text("Show All", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun NotificationToggleDialog(onDismiss: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Notifications", color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Enable Notifications",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = gradientPink,
                        checkedThumbColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = gradientPink)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Privacy Policy", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Last Updated: June 19, 2026", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(
                    "Welcome to Fliz TV (\"we,\" \"our,\" or \"us\"). This Privacy Policy explains how we collect, use, disclose, and protect your information when you use our mobile application and services.\n\n" +
                    "By using Fliz TV, you agree to the practices described in this Privacy Policy.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("1. Information We Collect", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Personal Information\n\n" +
                    "Fliz TV does not require users to create an account and does not intentionally collect personally identifiable information such as:\n\n" +
                    "• Full Name\n• Address\n• Phone Number\n• Government ID Information\n\n" +
                    "unless voluntarily provided through support communications.\n\n" +
                    "Device Information\n\n" +
                    "We may automatically collect certain information including:\n\n" +
                    "• Device model\n• Operating system version\n• App version\n• IP address\n• Device identifiers\n• Language preferences\n• Crash reports and diagnostics",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("2. How We Use Information", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "We use collected information to:\n\n" +
                    "• Improve app performance\n• Fix bugs and technical issues\n• Monitor service stability\n• Prevent abuse and unauthorized activities\n• Enhance user experience\n• Display relevant advertisements (if applicable)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("3. Live TV Content Disclaimer", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Fliz TV does not host, upload, store, or stream any television channels on its own servers.\n\n" +
                    "All streaming content available within the application is sourced from publicly available third-party providers or content sources.\n\n" +
                    "We do not claim ownership of any trademarks, logos, channels, broadcasts, or media content displayed through the application.\n\n" +
                    "If you are a copyright owner and believe your content is being displayed improperly, please contact us for review.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("4. Third-Party Services", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "The application may use third-party services including:\n\n" +
                    "• Analytics Providers\n• Advertising Networks\n• Streaming Content Providers\n• Cloud Infrastructure Providers\n\n" +
                    "These third parties may collect information according to their own privacy policies.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("5. Advertising", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Fliz TV may display advertisements through third-party advertising partners.\n\n" +
                    "Advertising providers may collect:\n\n" +
                    "• Device identifiers\n• Approximate location information\n• Advertising performance data\n• Usage statistics\n\n" +
                    "for ad delivery and measurement purposes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("6. Cookies and Tracking Technologies", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Some third-party services integrated into the application may use:\n\n" +
                    "• Cookies\n• SDKs\n• Advertising Identifiers\n• Similar tracking technologies\n\n" +
                    "to improve functionality and advertising services.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("7. Data Security", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "We implement reasonable technical and organizational measures to protect information against:\n\n" +
                    "• Unauthorized access\n• Alteration\n• Disclosure\n• Destruction\n\n" +
                    "However, no internet transmission or electronic storage method is completely secure.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("8. Children's Privacy", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Fliz TV is not specifically directed toward children under 13 years of age.\n\n" +
                    "We do not knowingly collect personal information from children. If such information is discovered, it will be removed promptly.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("9. External Links", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "The application may contain links or references to third-party websites or services.\n\n" +
                    "We are not responsible for the privacy practices or content of external services.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("10. Changes to This Privacy Policy", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "We may update this Privacy Policy periodically.\n\n" +
                    "Any modifications will be posted within the application and will become effective immediately upon publication.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text("11. Contact Us", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "For privacy-related questions, copyright concerns, or support requests, contact:\n\n" +
                    "Email: admin@nerroplay.online",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                )
                Text(
                    "Important Note\n\n" +
                    "Fliz TV acts solely as a media access platform. We do not guarantee the availability, legality, quality, or accuracy of any third-party stream or broadcast. Users are responsible for complying with their local laws and regulations regarding media consumption.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontStyle = FontStyle.Italic
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = gradientPink)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun PlaylistSourcesScreen(onBack: () -> Unit, isTv: Boolean = false) {
    var sources by remember { mutableStateOf(PlaylistManager.getSources()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    fun refreshSources() {
        sources = PlaylistManager.getSources()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var backFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier.focusable().onFocusChanged { backFocused = it.isFocused }
                    .then(if (backFocused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Playlist Sources", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Playlists", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))

                    sources.forEach { source ->
                        val isActive = PlaylistManager.getActiveSource()?.id == source.id
                        var sourceFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp)
                                .then(if (isTv) Modifier.focusable().onFocusChanged { sourceFocused = it.isFocused } else Modifier)
                                .then(
                                    if (isTv && sourceFocused) Modifier.border(
                                        3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                                    ) else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.name, fontSize = 14.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (!source.isDefault) {
                                IconButton(onClick = { showDeleteConfirm = source.id }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Box(modifier = Modifier.size(32.dp))
                            }
                            if (!isActive) {
                                TextButton(onClick = {
                                    PlaylistManager.setActiveSource(source.id)
                                    refreshSources()
                                }) {
                                    Text("Activate", fontSize = 12.sp, color = gradientPink)
                                }
                            } else {
                                Text("Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = gradientPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Custom Playlist URL")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Add your own M3U/M3U8 playlist URL. The app will fetch channels from your custom source.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (showAddDialog) {
        var urlInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Playlist URL", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Playlist Name", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("My Playlist", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gradientPink,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("M3U/M3U8 URL", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://example.com/playlist.m3u", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gradientPink,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            val name = nameInput.ifBlank { "Custom ${sources.size}" }
                            PlaylistManager.addSource(name, urlInput.trim())
                            refreshSources()
                            showAddDialog = false
                        }
                    },
                    enabled = urlInput.isNotBlank()
                ) {
                    Text("Add", color = if (urlInput.isNotBlank()) gradientPink else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Remove Source?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to remove this playlist source?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    PlaylistManager.removeSource(id)
                    refreshSources()
                    showDeleteConfirm = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun UpdateCheckDialog(onDismiss: () -> Unit) {
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isChecking by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isChecking = true
        val info = UpdateManager.checkForUpdate()
        updateInfo = info
        isChecking = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check for Updates", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            if (isChecking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = gradientPink)
                    Spacer(Modifier.width(12.dp))
                    Text("Checking for updates...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val info = updateInfo
                if (info == null) {
                    Text("Failed to check for updates. Check your internet connection.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (info.hasUpdate) {
                    Column {
                        Text("New version available: ${info.latestVersion}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("Current version: ${info.currentVersion}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (info.releaseNotes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("What's new:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(info.releaseNotes.take(500), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column {
                        Text("You're up to date!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("Running version ${info.currentVersion}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            val info = updateInfo
            if (info != null && info.hasUpdate) {
                TextButton(onClick = {
                    onDismiss()
                }) {
                    Text("Download", color = gradientPink)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        dismissButton = {
            if (isChecking || (updateInfo?.hasUpdate == true)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun ProfileSettingItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    isTv: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isTv) 24.dp else 16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    if (isTv) 4.dp else 2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(if (isTv) 24.dp else 16.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(if (isTv) 24.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (isTv) 32.dp else 22.dp)
        )
        Spacer(Modifier.width(if (isTv) 20.dp else 14.dp))
        Text(
            text = label,
            fontSize = if (isTv) 20.sp else 15.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(if (isTv) 28.dp else 20.dp)
        )
    }
}
