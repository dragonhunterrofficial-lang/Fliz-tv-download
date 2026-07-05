package com.example.fliztv.player

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fliztv.data.RecordingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import android.content.Context
import java.io.File

private fun getRecordingDir(context: Context): File {
    return context.getExternalFilesDir("fliztv-recordings")
        ?: File(context.filesDir, "fliztv-recordings")
}

private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
private const val MAX_RECONNECT_ATTEMPTS = 5

private fun detectStreamType(url: String): String {
    val u = url.lowercase()
    return when {
        u.contains(".m3u8") || u.contains("m3u8") -> "HLS"
        u.contains(".mpd") || u.contains("manifest.mpd") -> "DASH"
        u.startsWith("rtsp://") -> "RTSP"
        u.startsWith("rtmp://") -> "RTMP"
        u.startsWith("udp://") || u.startsWith("rtp://") -> "UDP"
        u.contains(".ts") || u.contains("transport") -> "MPEGTS"
        else -> "HTTP"
    }
}

private val STREAM_OPTIONS = mapOf(
    "HLS" to listOf(
        "--hls-max-conn=3",
        "--hls-segment-threads=3",
        "--hls-live-edge=2",
        "--hls-segment-max=30"
    ),
    "DASH" to listOf(
        "--xml-http-request"
    ),
    "RTSP" to listOf(
        "--rtsp-tcp",
        "--rtsp-frame-buffer-size=150000",
        "--rtsp-seq-order=network",
        "--real-rtsp-caching=30000"
    ),
    "RTMP" to listOf(
        "--rtmp-conn-timeout=15",
        "--rtmp-player-path="
    ),
    "UDP" to listOf(
        "--rtp-caching=50000",
        "--udp-caching=50000"
    ),
    "MPEGTS" to listOf(
        "--ts-csa-ck=0",
        "--ts-es-id-pid",
        "--ts-out=duplicate",
        "--ts-pcr=500"
    ),
    "HTTP" to listOf(
        "--http-reconnect"
    )
)

private fun buildEnterpriseOptions(url: String, isLowEnd: Boolean = false, cores: Int = 4): List<String> {
    val type = detectStreamType(url)
    val base = mutableListOf(
        "--no-video-title-show",
        "--network-caching=10000",
        "--live-caching=10000",
        "--avcodec-hw=any",
        "--avcodec-threads=$cores",
        "--ignore-ssl-errors",
        "--drop-late-frames",
        "--skip-frames",
        "--input-fast-seek",
        "--audio-channels=2",
        "--no-spdif",
        "--deinterlace=1",
        "--deinterlace-mode=blend",
        "--audio-time-stretch"
    )
    base.add("--aout=android_audiotrack")
    base.add("--codec=all,avcodec")
    base.addAll(STREAM_OPTIONS[type] ?: emptyList())
    return base
}

@Composable
fun VlcPlayerBox(
    url: String,
    userAgent: String = "",
    referer: String = "",
    isSd: Boolean = false,
    playerState: PlayerState,
    isRecording: Boolean = false,
    recordingChannelName: String = "",
    recordingChannelId: String = "",
    onRecordingError: (String) -> Unit = {},
    onNativeCrash: () -> Unit = {},
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

    var textureView by remember { mutableStateOf<android.view.TextureView?>(null) }
    var surfaceReady by remember { mutableStateOf(false) }
    var hasErrored by remember { mutableStateOf(false) }
    var released by remember { mutableStateOf(false) }
    var reconnectCount by remember { mutableIntStateOf(0) }
    var retryLevel by remember { mutableIntStateOf(0) }
    var recordingId by remember { mutableStateOf<String?>(null) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }
    var wasRecording by remember { mutableStateOf(false) }
    var audioFallbackActive by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val isLowEnd = remember {
        try {
            val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            (am?.memoryClass ?: 128) < 128
        } catch (_: Exception) { false }
    }
    val cores = remember {
        try { Runtime.getRuntime().availableProcessors().coerceIn(2, 8) } catch (_: Exception) { 4 }
    }

    val libVlc = remember {
        val primaryOpts = buildEnterpriseOptions(url, isLowEnd, cores)
        val fbCache = if (isLowEnd) 30000 else 60000
        val fallbackOpts = mutableListOf(
            "--no-video-title-show",
            "--network-caching=$fbCache",
            "--live-caching=$fbCache",
            "--aout=android_audiotrack",
            "--codec=avcodec",
            "--no-spdif",
            "--drop-late-frames",
            "--skip-frames"
        ).also { opts ->
            val t = detectStreamType(url)
            opts.addAll(STREAM_OPTIONS[t] ?: emptyList())
        }
        var vlc: LibVLC? = null
        try {
            vlc = LibVLC(context, primaryOpts)
        } catch (e: Exception) {
            try {
                vlc = LibVLC(context, fallbackOpts)
            } catch (e2: Exception) {
                try {
                    vlc = LibVLC(context)
                } catch (_: Exception) { }
            }
        }
        vlc
    }

    if (libVlc == null) {
        onNativeCrash()
        playerState.error = "Player init failed"
        playerState.isLoading = false
        return
    }

    val mediaPlayer = remember {
        try {
            MediaPlayer(libVlc)
        } catch (_: Exception) {
            null
        }
    }

    if (mediaPlayer == null) {
        try { libVlc.release() } catch (_: Exception) {}
        onNativeCrash()
        playerState.error = "VLC unavailable"
        playerState.isLoading = false
        return
    }

    fun safeFallback() {
        if (!hasErrored && !released) {
            hasErrored = true
            playerState.isLoading = false
            mainHandler.post { onNativeCrash() }
        }
    }

    fun playUrl() {
        if (released || hasErrored) return
        try {
            playerState.isLoading = true
            mediaPlayer.stop()
            val ua = userAgent.ifEmpty { DEFAULT_USER_AGENT }
            val m = Media(libVlc, Uri.parse(url))
            m.addOption(":http-user-agent=$ua")
            m.addOption(":no-ssl-verify")
            m.addOption(":audio-channels=2")
            m.addOption(":no-spdif")
            when (retryLevel) {
                0 -> {
                    m.addOption(":network-caching=60000")
                    m.addOption(":live-caching=60000")
                    m.addOption(":avcodec-hw=any")
                    m.addOption(":codec=all,avcodec")
                    m.addOption(":aout=android_audiotrack")
                }
                1 -> {
                    m.addOption(":network-caching=150000")
                    m.addOption(":live-caching=150000")
                    m.addOption(":avcodec-hw=disabled")
                    m.addOption(":codec=avcodec")
                    m.addOption(":aout=android_audiotrack")
                }
                2 -> {
                    m.addOption(":network-caching=30000")
                    m.addOption(":live-caching=30000")
                    m.addOption(":avcodec-hw=disabled")
                    m.addOption(":codec=avcodec")
                }
                else -> {
                    m.addOption(":network-caching=20000")
                    m.addOption(":live-caching=20000")
                    m.addOption(":avcodec-hw=disabled")
                    m.addOption(":codec=avcodec")
                }
            }
            if (audioFallbackActive) {
                m.addOption(":aout=android_audiotrack")
                m.addOption(":avcodec-hw=disabled")
                m.addOption(":codec=avcodec")
                m.addOption(":no-spdif")
            }
            if (referer.isNotEmpty()) m.addOption(":http-referrer=$referer")
            val type = detectStreamType(url)
            when (type) {
                "HLS" -> {
                    m.addOption(":hls-max-conn=4")
                    m.addOption(":hls-segment-threads=4")
                    m.addOption(":hls-live-edge=3")
                    m.addOption(":hls-segment-max=0")
                }
                "RTSP" -> {
                    m.addOption(":rtsp-tcp")
                    m.addOption(":rtsp-frame-buffer-size=300000")
                }
                "MPEGTS" -> {
                    m.addOption(":ts-es-id-pid")
                    m.addOption(":ts-pcr=500")
                    m.addOption(":ts-trust-pcr")
                }
            }
            mediaPlayer.setScale(0f)
            mediaPlayer.setAspectRatio(null)
            mediaPlayer.setMedia(m)
            mediaPlayer.play()
            mediaPlayer.setVolume(100)
            m.release()
        } catch (_: Exception) {}
    }

    fun applyResizeMode(mode: Int) {
        if (released || hasErrored) return
        try {
            when (mode) {
                RESIZE_FILL -> {
                    mediaPlayer.setScale(0f)
                    mediaPlayer.setAspectRatio("")
                }
                RESIZE_ZOOM -> {
                    mediaPlayer.setScale(1.5f)
                    mediaPlayer.setAspectRatio(null)
                }
                else -> {
                    mediaPlayer.setScale(0f)
                    mediaPlayer.setAspectRatio(null)
                }
            }
            val tv = textureView
            if (tv != null && tv.width > 0 && tv.height > 0) {
                mediaPlayer.getVLCVout().setWindowSize(tv.width, tv.height)
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(mediaPlayer) {
        try {
            mediaPlayer.setEventListener { event ->
                if (released) return@setEventListener
                mainHandler.post {
                    val ps = playerStateRef.value
                    when (event.type) {
                        MediaPlayer.Event.Playing -> {
                            ps.isPlaying = true
                            ps.isLoading = false
                            ps.error = null
                            applyResizeMode(ps.resizeMode)
                        }
                        MediaPlayer.Event.Buffering -> {
                            ps.isLoading = true
                        }
                        MediaPlayer.Event.Stopped -> {
                            ps.isPlaying = false
                        }
                        MediaPlayer.Event.EndReached -> {
                            ps.isPlaying = false
                        }
                        MediaPlayer.Event.EncounteredError -> {
                            ps.isLoading = false
                            if (ps.error == null) {
                                ps.error = "Stream unavailable. The channel may be offline."
                            }
                            if (!released && !hasErrored) {
                                if (!audioFallbackActive) {
                                    audioFallbackActive = true
                                    retryLevel = 0
                                    ps.error = null
                                    playUrl()
                                } else if (retryLevel < 3) {
                                    retryLevel++
                                    ps.error = null
                                    playUrl()
                                } else if (reconnectCount < MAX_RECONNECT_ATTEMPTS) {
                                    reconnectCount++
                                    retryLevel = 0
                                    ps.error = null
                                    playUrl()
                                } else {
                                    safeFallback()
                                }
                            }
                        }
                        MediaPlayer.Event.RecordChanged -> {
                            val isRec = event.getRecording()
                            val path = event.getRecordPath()
                            if (isRec && !path.isNullOrEmpty()) {
                                recordedFilePath = path
                            } else if (isRec && path.isNullOrEmpty()) {
                                // VLC didn't provide a path, use our known path
                            } else if (!isRec) {
                                val rid = recordingId
                                var rPath = recordedFilePath
                                if (rid != null && rPath != null) {
                                    val file = File(rPath)
                                    RecordingManager.completeRecording(rid, rPath, if (file.exists()) file.length() else 0L)
                                } else if (rid != null) {
                                    RecordingManager.failRecording(rid)
                                }
                                recordingId = null
                                recordedFilePath = null
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            mainHandler.post { safeFallback() }
        }
    }

    LaunchedEffect(url, surfaceReady) {
        if (!surfaceReady) return@LaunchedEffect
        if (released || hasErrored) return@LaunchedEffect
        reconnectCount = 0
        retryLevel = 0
        audioFallbackActive = false
        playerState.isLoading = true
        playerState.error = null
        playerState.isPlaying = false
        playerState.position = 0L
        playerState.duration = 0L
        playerState.bufferedPosition = 0L
        playerState.speed = 1f
        playerState.resizeMode = RESIZE_FIT
        playUrl()
    }

    LaunchedEffect(mediaPlayer) {
        playerState.actions = object : PlayerActions {
            override fun togglePlay() {
                if (released || hasErrored) return
                try {
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    else mediaPlayer.play()
                } catch (_: Exception) {}
            }
            override fun seekTo(positionMs: Long) {
                if (released || hasErrored) return
                try { mediaPlayer.setTime(positionMs) } catch (_: Exception) {}
            }
            override fun seekForward(ms: Long) {
                if (released || hasErrored) return
                try { mediaPlayer.setTime(mediaPlayer.time + ms) } catch (_: Exception) {}
            }
            override fun seekBackward(ms: Long) {
                if (released || hasErrored) return
                try { mediaPlayer.setTime((mediaPlayer.time - ms).coerceAtLeast(0)) } catch (_: Exception) {}
            }
            override fun setSpeed(speed: Float) {
                if (released || hasErrored) return
                try {
                    mediaPlayer.setRate(speed)
                    playerState.speed = speed
                } catch (_: Exception) {}
            }
            override fun setVolume(vol: Float) {
                if (released || hasErrored) return
                try {
                    mediaPlayer.setVolume((vol.coerceIn(0f, 1f) * 100).toInt())
                    playerState.volume = vol
                } catch (_: Exception) {}
            }
            override fun setResizeMode(mode: Int) {
                playerState.resizeMode = mode
                applyResizeMode(mode)
            }
            override fun retry() {
                if (released || hasErrored) return
                reconnectCount = 0
                retryLevel = 0
                audioFallbackActive = false
                playerState.error = null
                playerState.isLoading = true
                playUrl()
            }
            override fun release() {
                if (released) return
                released = true
                try {
                    mediaPlayer.setEventListener(null)
                    mediaPlayer.stop()
                    mediaPlayer.getVLCVout().detachViews()
                    mediaPlayer.release()
                    libVlc.release()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2000)
            if (released || hasErrored) break
            try {
                val ps = playerStateRef.value
                val pos = mediaPlayer.getTime().coerceAtLeast(0)
                val dur = mediaPlayer.getLength().coerceAtLeast(0)
                ps.position = pos
                ps.duration = dur
                ps.bufferedPosition = pos
            } catch (_: Exception) {
                if (!hasErrored && !released) {
                    if (!audioFallbackActive) {
                        audioFallbackActive = true
                        retryLevel = 0
                        playUrl()
                    } else if (retryLevel < 3) {
                        retryLevel++
                        playUrl()
                    } else if (reconnectCount < MAX_RECONNECT_ATTEMPTS) {
                        reconnectCount++
                        retryLevel = 0
                        playUrl()
                    } else {
                        safeFallback()
                    }
                }
                break
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (released || hasErrored) return@LaunchedEffect
        if (isRecording && !wasRecording) {
            val dir = getRecordingDir(context)
            dir.mkdirs()
            try {
                val rec = RecordingManager.startRecording(recordingChannelName, recordingChannelId, url)
                recordingId = rec.id
                recordedFilePath = File(dir, "${rec.id}.ts").absolutePath
                val ok = mediaPlayer.record(recordedFilePath!!)
                if (!ok) {
                    RecordingManager.failRecording(rec.id)
                    recordingId = null
                    recordedFilePath = null
                }
            } catch (e: Exception) {
                onRecordingError("Failed to start recording: ${e.message}")
            }
        } else if (!isRecording && wasRecording) {
            try {
                mediaPlayer.record(null)
            } catch (_: Exception) {}
            if (recordingId != null) {
                val rid = recordingId!!
                recordingId = null
                val rPath = recordedFilePath
                recordedFilePath = null
                if (rPath != null) {
                    val file = File(rPath)
                    if (file.exists() && file.length() > 0) {
                        RecordingManager.completeRecording(rid, rPath, file.length())
                    } else {
                        RecordingManager.failRecording(rid)
                    }
                } else {
                    RecordingManager.failRecording(rid)
                }
            }
        }
        wasRecording = isRecording
    }

    LaunchedEffect(playerState.resizeMode, surfaceReady) {
        if (surfaceReady && !released && !hasErrored) {
            applyResizeMode(playerState.resizeMode)
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val rid = recordingId
            val rPath = recordedFilePath
            if (rid != null) {
                try { mediaPlayer.record(null) } catch (_: Exception) {}
                if (rPath != null) {
                    val file = File(rPath)
                    RecordingManager.completeRecording(rid, rPath, if (file.exists()) file.length() else 0L)
                } else {
                    RecordingManager.failRecording(rid)
                }
            }
            if (!released) {
                released = true
                try {
                    mediaPlayer.setEventListener(null)
                    mediaPlayer.stop()
                    mediaPlayer.getVLCVout().detachViews()
                    mediaPlayer.release()
                    libVlc.release()
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).also { tv ->
                    textureView = tv
                    tv.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            if (released || hasErrored) return
                            try {
                                mediaPlayer.getVLCVout().setVideoView(tv)
                                mediaPlayer.getVLCVout().setWindowSize(width, height)
                                mediaPlayer.getVLCVout().attachViews()
                                surfaceReady = true
                                applyResizeMode(playerState.resizeMode)
                            } catch (_: Exception) {}
                        }
                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            if (released || hasErrored) return
                            try {
                                mediaPlayer.getVLCVout().setWindowSize(width, height)
                                applyResizeMode(playerState.resizeMode)
                            } catch (_: Exception) {}
                        }
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            surfaceReady = false
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
