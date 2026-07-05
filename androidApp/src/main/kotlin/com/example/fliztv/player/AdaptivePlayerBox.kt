package com.example.fliztv.player

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class PlayerEngine {
    EXO_PLAYER, VLC
}

private fun suggestEngine(url: String): PlayerEngine {
    return PlayerEngine.VLC
}

@Composable
fun AdaptivePlayerBox(
    url: String,
    userAgent: String = "",
    referer: String = "",
    isSd: Boolean = false,
    quality: String = "unknown",
    playerState: PlayerState,
    isRecording: Boolean = false,
    recordingChannelName: String = "",
    recordingChannelId: String = "",
    onRecordingError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentEngine by remember { mutableStateOf(suggestEngine(url)) }
    var lastUrl by remember { mutableStateOf("") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(url) {
        if (url != lastUrl) {
            lastUrl = url
            currentEngine = suggestEngine(url)
        }
    }

    when (currentEngine) {
        PlayerEngine.EXO_PLAYER -> {
            ExoPlayerBox(
                url = url,
                userAgent = userAgent,
                referer = referer,
                playerState = playerState,
                onFallback = {
                    mainHandler.post { currentEngine = PlayerEngine.VLC }
                },
                modifier = modifier
            )
        }
        PlayerEngine.VLC -> {
            VlcPlayerBox(
                url = url,
                userAgent = userAgent,
                referer = referer,
                isSd = isSd,
                playerState = playerState,
                isRecording = isRecording,
                recordingChannelName = recordingChannelName,
                recordingChannelId = recordingChannelId,
                onRecordingError = onRecordingError,
                onNativeCrash = {
                    mainHandler.post { currentEngine = PlayerEngine.EXO_PLAYER }
                },
                modifier = modifier
            )
        }
    }
}
