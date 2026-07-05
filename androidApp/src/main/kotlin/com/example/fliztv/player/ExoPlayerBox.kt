package com.example.fliztv.player

import android.net.Uri
import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

@UnstableApi
@Composable
fun ExoPlayerBox(
    url: String,
    userAgent: String = "",
    referer: String = "",
    playerState: PlayerState,
    onFallback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    if (url.isBlank()) {
        playerState.error = "Invalid stream URL"
        playerState.isLoading = false
        return
    }

    val playerStateRef = remember { mutableStateOf(playerState) }
    playerStateRef.value = playerState

    var currentPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var reconnectCount by remember { mutableIntStateOf(0) }
    var lastErrorTime by remember { mutableLongStateOf(0L) }
    var audioTrackAttempt by remember { mutableIntStateOf(0) }
    var fallbackTriggered by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun releasePlayer(player: ExoPlayer?) {
        player?.run {
            try {
                stop()
                clearMediaItems()
                release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(url) {
        playerView?.player = null
        releasePlayer(currentPlayer)
        currentPlayer = null
        reconnectCount = 0
        lastErrorTime = 0L
        audioTrackAttempt = 0
        fallbackTriggered = false

        val newPlayer = try {
            ExoPlayer.Builder(context)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(15000, 120000, 5000, 10000)
                        .setBackBuffer(30000, false)
                        .setTargetBufferBytes(-1)
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build()
                )
                .build()
                .apply {
                    trackSelectionParameters = trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        .setForceHighestSupportedBitrate(false)
                        .setPreferredAudioRoleFlags(C.ROLE_FLAG_MAIN)
                        .build()
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_ONE
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val ps = playerStateRef.value
                            ps.isLoading = playbackState == Player.STATE_BUFFERING
                            if (playbackState == Player.STATE_READY) {
                                ps.isLoading = false
                            }
                        }
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            val ps = playerStateRef.value
                            ps.isPlaying = isPlaying
                            if (isPlaying) {
                                ps.error = null
                                ps.isLoading = false
                            }
                        }
                        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                            Log.d("ExoAudio", "trackGroups=${tracks.groups.size}")
                            val ac3MimeTypes = setOf("audio/ac-3", "audio/eac-3", "audio/ac3", "audio/eac3", "audio/vnd.dolby.dd-raw", "audio/vnd.dolby.dd")
                            for (group in tracks.groups) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    if (format.sampleMimeType in ac3MimeTypes) {
                                        Log.d("ExoAudio", "AC-3 audio detected: ${format.sampleMimeType}")
                                        if (!fallbackTriggered) {
                                            fallbackTriggered = true
                                            mainHandler.post { onFallback() }
                                        }
                                        return
                                    }
                                }
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            val ps = playerStateRef.value
                            ps.isLoading = false
                            ps.error = "Stream error: ${error.message ?: "Unknown"}"
                        }
                    })
                }
        } catch (e: Exception) {
            playerState.error = "Player init failed: ${e.message}"
            playerState.isLoading = false
            return@LaunchedEffect
        }

        currentPlayer = newPlayer
        playerView?.player = newPlayer

        try {
            playerState.isLoading = true
            playerState.error = null
            playerState.isPlaying = false
            playerState.position = 0L
            playerState.duration = 0L
            playerState.bufferedPosition = 0L
            playerState.volume = 1f
            playerState.speed = 1f
            playerState.resizeMode = RESIZE_FIT

            val ua = userAgent.ifEmpty { DEFAULT_USER_AGENT }
            val headers = mutableMapOf("User-Agent" to ua)
            if (referer.isNotEmpty()) headers["Referer"] = referer

            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                DefaultHttpDataSource.Factory()
                    .setUserAgent(ua)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(60000)
                    .setReadTimeoutMs(90000)
                    .setDefaultRequestProperties(headers)
            )

            val playUri = if (url.startsWith("/")) Uri.parse("file://$url") else Uri.parse(url)
            val mediaItem = MediaItem.fromUri(playUri)
            val mediaSource = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
            newPlayer.setMediaSource(mediaSource)
            newPlayer.prepare()
        } catch (e: Exception) {
            playerState.error = "Stream error: ${e.message ?: "Failed to load"}"
            playerState.isLoading = false
            releasePlayer(newPlayer)
            currentPlayer = null
        }
    }

    fun retryPlayback() {
        val player = currentPlayer ?: return
        try {
            playerState.error = null
            playerState.isLoading = true
            player.stop()
            player.clearMediaItems()
            player.seekTo(0)
            player.prepare()
        } catch (_: Exception) {}
    }

    fun switchAudioTrack() {
        val player = currentPlayer ?: return
        try {
            val tracks = player.currentTracks
            val audioTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            if (audioTracks.isEmpty()) return
            audioTrackAttempt++
            val trackIdx = audioTrackAttempt % audioTracks.size
            val params = player.trackSelectionParameters
                .buildUpon()
                .clearOverrides()
                .addOverride(TrackSelectionOverride(audioTracks[trackIdx].mediaTrackGroup, 0))
                .build()
            player.trackSelectionParameters = params
        } catch (_: Exception) {}
    }

    LaunchedEffect(currentPlayer) {
        val player = currentPlayer ?: return@LaunchedEffect
        playerState.actions = object : PlayerActions {
            override fun togglePlay() {
                try { player.playWhenReady = !player.playWhenReady } catch (_: Exception) {}
            }
            override fun seekTo(positionMs: Long) {
                try { player.seekTo(positionMs) } catch (_: Exception) {}
            }
            override fun seekForward(ms: Long) {
                try { player.seekTo(player.currentPosition + ms) } catch (_: Exception) {}
            }
            override fun seekBackward(ms: Long) {
                try { player.seekTo((player.currentPosition - ms).coerceAtLeast(0)) } catch (_: Exception) {}
            }
            override fun setSpeed(speed: Float) {
                try {
                    player.setPlaybackSpeed(speed)
                    playerState.speed = speed
                } catch (_: Exception) {}
            }
            override fun setVolume(vol: Float) {
                try {
                    player.volume = vol
                    playerState.volume = vol
                } catch (_: Exception) {}
            }
            override fun setResizeMode(mode: Int) {
                playerState.resizeMode = mode
            }
            override fun retry() {
                reconnectCount = 0
                retryPlayback()
            }
            override fun release() {
                releasePlayer(player)
            }
        }
    }

    LaunchedEffect(playerState.error) {
        val err = playerState.error ?: return@LaunchedEffect
        if (fallbackTriggered) return@LaunchedEffect
        if (reconnectCount >= 3) {
            fallbackTriggered = true
            mainHandler.post { onFallback() }
            return@LaunchedEffect
        }
        val now = System.currentTimeMillis()
        if (now - lastErrorTime < 10000) return@LaunchedEffect
        lastErrorTime = now
        reconnectCount++
        audioTrackAttempt = 0
        delay(2000)
        retryPlayback()
    }

    fun applyResizeMode() {
        playerView?.resizeMode = when (playerState.resizeMode) {
            RESIZE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            RESIZE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(500)
            val p = currentPlayer ?: continue
            try {
                playerState.position = p.currentPosition
                playerState.duration = p.duration.coerceAtLeast(0)
                playerState.bufferedPosition = p.bufferedPosition
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(playerState.resizeMode, playerView) {
        applyResizeMode()
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            releasePlayer(currentPlayer)
            currentPlayer = null
            playerView?.player = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = currentPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }.also { playerView = it }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
