package com.example.fliztv.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fliztv.data.Channel

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    isOffline: Boolean = false,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    logoLoader: @Composable (String, Modifier) -> Unit = { _, _ -> },
    isTv: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val alpha = if (isOffline) 0.5f else 1f

    Column(
        modifier = Modifier
            .padding(if (isTv) 12.dp else 6.dp)
            .height(if (isTv) 280.dp else 195.dp)
            .clip(RoundedCornerShape(if (isTv) 18.dp else 12.dp))
            .then(if (isFocused) Modifier.border(if (isTv) 4.dp else 2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(if (isTv) 18.dp else 12.dp)) else Modifier)
            .background(
                when {
                    isFocused -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isTv) 160.dp else 110.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clip(RoundedCornerShape(topStart = if (isTv) 18.dp else 12.dp, topEnd = if (isTv) 18.dp else 12.dp))
                .then(if (isOffline) Modifier.alpha(alpha) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotBlank()) {
                logoLoader(channel.logo, Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier
                        .size(if (isTv) 72.dp else 48.dp)
                        .clip(RoundedCornerShape(if (isTv) 16.dp else 10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (isTv) 22.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val qualityLabel = when (channel.quality) {
                "4k" -> "4K"
                "fhd" -> "FHD"
                "hd" -> "HD"
                else -> null
            }
            if (qualityLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(if (isTv) 10.dp else 6.dp)
                        .clip(RoundedCornerShape(if (isTv) 6.dp else 4.dp))
                        .background(Color(0xFF1A1A2E).copy(alpha = 0.85f))
                        .padding(horizontal = if (isTv) 10.dp else 6.dp, vertical = if (isTv) 4.dp else 2.dp)
                ) {
                    Text(
                        text = qualityLabel,
                        fontSize = if (isTv) 14.sp else 9.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val statusText = when {
                isOffline -> "Offline"
                channel.status.isNotEmpty() -> channel.status
                else -> null
            }
            val statusColor = when {
                isOffline -> MaterialTheme.colorScheme.error
                channel.status == "Not 24/7" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            if (statusText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = if (isTv) 10.dp else 6.dp, top = if (qualityLabel != null) (if (isTv) 36.dp else 24.dp) else (if (isTv) 10.dp else 6.dp), end = if (isTv) 10.dp else 6.dp, bottom = if (isTv) 10.dp else 6.dp)
                        .clip(RoundedCornerShape(if (isTv) 6.dp else 4.dp))
                        .background(statusColor)
                        .padding(horizontal = if (isTv) 10.dp else 6.dp, vertical = if (isTv) 4.dp else 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = if (isTv) 14.sp else 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (isTv) 10.dp else 6.dp)
                    .size(if (isTv) 48.dp else 32.dp)
                    .clip(RoundedCornerShape(if (isTv) 24.dp else 16.dp))
                    .background(Color(0x66000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFavoriteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (isTv) 24.dp else 16.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(if (isTv) 14.dp else 8.dp)) {
            Text(
                text = channel.name,
                fontSize = if (isTv) 16.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (isTv) 20.sp else 15.sp
            )

            Spacer(Modifier.height(if (isTv) 8.dp else 4.dp))

            val badge = when {
                channel.country.isNotBlank() -> channel.country
                channel.language.isNotBlank() -> channel.language
                channel.category.isNotBlank() -> channel.category
                else -> null
            }
            if (badge != null) {
                Text(
                    text = badge,
                    fontSize = if (isTv) 14.sp else 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
