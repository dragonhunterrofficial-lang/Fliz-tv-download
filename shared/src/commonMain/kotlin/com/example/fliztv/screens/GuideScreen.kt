package com.example.fliztv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fliztv.currentEpochSeconds
import com.example.fliztv.data.EpgManager
import com.example.fliztv.data.EpgProgram
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun GuideScreen(
    channelName: String = "",
    channelId: String = "",
    isTv: Boolean = false,
    onBack: () -> Unit
) {
    var programs by remember { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentTime by remember { mutableLongStateOf(currentEpochSeconds() * 1000) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(channelId) {
        isLoading = true
        error = null
        val epg = if (channelId.isNotBlank()) {
            EpgManager.getProgramsForChannel(channelId)
        } else {
            emptyList()
        }
        if (epg.isNotEmpty()) {
            programs = epg
        } else {
            val fetched = EpgManager.fetchPrograms()
            programs = if (channelId.isNotBlank()) {
                EpgManager.getProgramsForChannel(channelId)
            } else {
                EpgManager.getAllPrograms()
            }
        }
        if (programs.isEmpty()) error = if (channelId.isNotBlank()) "No EPG data available for this channel" else "No EPG data loaded"
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30000)
            currentTime = currentEpochSeconds() * 1000
        }
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
            Text("Program Guide", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (channelName.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text("- $channelName", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
        } else if (programs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No program data available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("EPG data not loaded yet", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(programs, key = { "${it.channelId}_${it.startTime}" }) { program ->
                    val isNow = program.startTime <= currentTime && program.endTime > currentTime
                    val isPast = program.endTime <= currentTime
                    ProgramCard(
                        program = program,
                        isNow = isNow,
                        isPast = isPast,
                        currentTime = currentTime,
                        isTv = isTv
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(program: EpgProgram, isNow: Boolean, isPast: Boolean, currentTime: Long, isTv: Boolean = false) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isNow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isNow) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(if (isNow) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(12.dp)) else Modifier)
            .then(if (isTv) Modifier.focusable().onFocusChanged { isFocused = it.isFocused } else Modifier)
            .then(
                if (isTv && isFocused) Modifier.border(
                    3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimeRange(program.startTime, program.endTime),
                    fontSize = 12.sp,
                    color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal
                )
                if (isNow) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = program.title,
                fontSize = 15.sp,
                fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (program.episodeTitle.isNotBlank()) {
                Text(
                    text = program.episodeTitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (program.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = program.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isNow) {
                Spacer(Modifier.height(6.dp))
                val progress = ((currentTime - program.startTime).toFloat() /
                        (program.endTime - program.startTime).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

private fun formatTimeRange(startMs: Long, endMs: Long): String {
    return "${formatTime(startMs)} - ${formatTime(endMs)}"
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = (totalSec / 3600) % 24
    val m = (totalSec % 3600) / 60
    return "%02d:%02d".format(h, m)
}
