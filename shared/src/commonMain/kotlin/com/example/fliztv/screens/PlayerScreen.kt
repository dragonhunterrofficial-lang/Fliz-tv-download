package com.example.fliztv.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fliztv.data.Channel
import com.example.fliztv.data.EpgManager
import com.example.fliztv.data.EpgProgram
import com.example.fliztv.player.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit,
    isTv: Boolean = false,
    onVolumeUp: () -> Unit = {},
    onVolumeDown: () -> Unit = {},
    onBrightnessUp: () -> Unit = {},
    onBrightnessDown: () -> Unit = {},
    onNextChannel: (() -> Unit)? = null,
    onPrevChannel: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    isRecording: Boolean = false,
    recordingStartTimeMs: Long = 0L,
    onFavoriteClick: () -> Unit = {},
    onRecordClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
    playerState: PlayerState? = null,
    playerContent: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Loading stream...",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }
        }
    }
) {
    var showControls by remember { mutableStateOf(true) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun autoHide() {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(8000)
            showControls = false
        }
    }

    fun resetAutoHide() {
        if (showControls) autoHide()
    }

    LaunchedEffect(Unit) { autoHide() }

    val toggleShowControls: () -> Unit = {
        showControls = !showControls
        if (showControls) autoHide()
    }

    PortraitPlayerLayout(
        channel = channel,
        onBack = onBack,
        isTv = isTv,
        onVolumeUp = onVolumeUp,
        onVolumeDown = onVolumeDown,
        onBrightnessUp = onBrightnessUp,
        onBrightnessDown = onBrightnessDown,
        onNextChannel = onNextChannel,
        onPrevChannel = onPrevChannel,
        isFavorite = isFavorite,
        isRecording = isRecording,
        recordingStartTimeMs = recordingStartTimeMs,
        onFavoriteClick = onFavoriteClick,
        onRecordClick = onRecordClick,
        onCastClick = onCastClick,
        playerState = playerState,
        showControls = showControls,
        onToggleShowControls = toggleShowControls,
        onResetAutoHide = { resetAutoHide() },
        playerContent = playerContent
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PortraitPlayerLayout(
    channel: Channel,
    onBack: () -> Unit,
    isTv: Boolean = false,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onNextChannel: (() -> Unit)? = null,
    onPrevChannel: (() -> Unit)? = null,
    isFavorite: Boolean,
    isRecording: Boolean = false,
    recordingStartTimeMs: Long = 0L,
    onFavoriteClick: () -> Unit,
    onRecordClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
    playerState: PlayerState?,
    showControls: Boolean,
    onToggleShowControls: () -> Unit,
    onResetAutoHide: () -> Unit,
        playerContent: @Composable (Modifier) -> Unit
    ) {
        val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
        val context = androidx.compose.ui.platform.LocalContext.current
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            LaunchedEffect(Unit) {
                activity?.window?.let { window ->
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    activity?.window?.decorView?.let {
                        @Suppress("DEPRECATION")
                        it.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isTv) Modifier
                            .focusable()
                            .onKeyEvent { event ->
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        onPrevChannel?.let { it(); true } ?: false
                                    }
                                    Key.DirectionRight -> {
                                        onNextChannel?.let { it(); true } ?: false
                                    }
                                    Key.DirectionCenter, Key.Enter -> {
                                        onToggleShowControls()
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        onVolumeUp(); true
                                    }
                                    Key.DirectionDown -> {
                                        onVolumeDown(); true
                                    }
                                    Key.Back -> {
                                        onBack(); true
                                    }
                                    else -> false
                                }
                            }
                        else Modifier.pointerInput(Unit) {
                            detectTapGestures { onToggleShowControls() }
                        }
                    )
                    .then(
                        if (!isTv) Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                val am = audioManager ?: return@detectVerticalDragGestures
                                val xFraction = change.position.x / size.width
                                if (xFraction < 0.5f) {
                                    val window = activity?.window ?: return@detectVerticalDragGestures
                                    val lp = window.attributes
                                    lp.screenBrightness = (lp.screenBrightness - dragAmount / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0.01f, 1f)
                                    window.attributes = lp
                                } else {
                                    val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                    val currentVol = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                                    val change = (-dragAmount / size.height.toFloat().coerceAtLeast(1f) * maxVol * 1.5f).toInt()
                                    am.setStreamVolume(
                                        android.media.AudioManager.STREAM_MUSIC,
                                        (currentVol + change).coerceIn(0, maxVol),
                                        android.media.AudioManager.FLAG_SHOW_UI
                                    )
                                }
                            }
                        } else Modifier
                    )
            ) {
                playerContent(Modifier.fillMaxSize())
            }
            PlayerOverlay(
                playerState = playerState,
                modifier = Modifier.fillMaxSize(),
                isTv = isTv
            )

            var recordingDurationText by remember(isRecording) { mutableStateOf("") }
            LaunchedEffect(isRecording) {
                if (isRecording && recordingStartTimeMs > 0L) {
                    while (true) {
                        val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                        val totalSec = elapsed / 1000
                        val h = totalSec / 3600
                        val m = (totalSec % 3600) / 60
                        val s = totalSec % 60
                        recordingDurationText = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
                        delay(1000)
                    }
                } else {
                    recordingDurationText = ""
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it / 4 },
                exit = fadeOut() + slideOutVertically { -it / 4 }
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(horizontal = if (isTv) 12.dp else 4.dp, vertical = if (isTv) 12.dp else 4.dp)
                        .statusBarsPadding()
                        .padding(top = if (isTv) 58.dp else 54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableIconButton(
                        onClick = { onBack(); onResetAutoHide() },
                        isTv = isTv
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                                .padding(if (isTv) 14.dp else 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(if (isTv) 32.dp else 24.dp))
                        }
                    }
                    Spacer(Modifier.width(if (isTv) 12.dp else 4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            fontSize = if (isTv) 22.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val nowProg = EpgManager.getNowPlaying(channel.id)
                        val nextProg = EpgManager.getNextProgram(channel.id)
                        if (nowProg != null) {
                            Text(
                                text = "Now: ${nowProg.title}",
                                fontSize = if (isTv) 14.sp else 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (nowProg == null && nextProg != null) {
                            Text(
                                text = "Next: ${nextProg.title}",
                                fontSize = if (isTv) 14.sp else 10.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (channel.status == "Not 24/7") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(if (isTv) 8.dp else 4.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .padding(horizontal = if (isTv) 12.dp else 6.dp, vertical = if (isTv) 6.dp else 2.dp)
                        ) {
                            Text("Offline?", color = Color.White, fontSize = if (isTv) 18.sp else 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    FocusableIconButton(
                        onClick = { onRecordClick(); onResetAutoHide() },
                        isTv = isTv
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop recording" else "Start recording",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(if (isTv) 36.dp else 24.dp)
                        )
                    }
                    if (isRecording && recordingDurationText.isNotEmpty()) {
                        Text(
                            text = recordingDurationText,
                            color = Color(0xFFFF4444),
                            fontSize = if (isTv) 18.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = if (isTv) 8.dp else 4.dp)
                        )
                    }
                    FocusableIconButton(
                        onClick = { onCastClick(); onResetAutoHide() },
                        isTv = isTv
                    ) {
                        Icon(
                            Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = Color.White,
                            modifier = Modifier.size(if (isTv) 36.dp else 24.dp)
                        )
                    }
                    FocusableIconButton(
                        onClick = { onFavoriteClick(); onResetAutoHide() },
                        isTv = isTv
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.size(if (isTv) 36.dp else 24.dp)
                        )
                    }
                }
            }

            // TV channel navigation indicator
            if (isTv) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.key == Key.DirectionLeft || event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                onPrevChannel?.let { it(); true } ?: false
                            } else false
                        }
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous channel",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.key == Key.DirectionRight || event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                onNextChannel?.let { it(); true } ?: false
                            } else false
                        }
                        .padding(12.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next channel",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Original banner ad at the top
            var bannerWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
            AndroidView(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(if (isTv) 72.dp else 50.dp)
                    .statusBarsPadding()
                    .alpha(0.1f),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        bannerWebView = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.allowContentAccess = false
                        settings.allowFileAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        }
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean = false
                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean = false
                            override fun onReceivedError(view: android.webkit.WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                postDelayed({ loadBannerAd(this@apply) }, 5000)
                            }
                            override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                handler?.cancel()
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onCreateWindow(view: android.webkit.WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false
                        }
                        android.webkit.WebView.setWebContentsDebuggingEnabled(false)
                        loadBannerAd(this)
                    }
                },
                update = { }
            )
            // Auto-refresh banner every 30s
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(30000)
                    bannerWebView?.let { loadBannerAd(it) }
                }
            }

            var showFullAd by remember { mutableStateOf(false) }
            if (showFullAd) {
                var adWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
                DisposableEffect(Unit) {
                    onDispose {
                        adWebView?.let { wv ->
                            wv.stopLoading()
                            wv.loadUrl("about:blank")
                            (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                            wv.destroy()
                        }
                        adWebView = null
                    }
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.1f),
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            adWebView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            settings.setSupportMultipleWindows(false)
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.allowContentAccess = false
                            settings.allowFileAccess = false
                            settings.allowFileAccessFromFileURLs = false
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            }
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean = false
                                @Suppress("DEPRECATION")
                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean = false
                                override fun onReceivedError(view: android.webkit.WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                    postDelayed({ loadFullScreenAd(this@apply) }, 5000)
                                }
                                override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                    handler?.cancel()
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onCreateWindow(view: android.webkit.WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false
                            }
                            android.webkit.WebView.setWebContentsDebuggingEnabled(false)
                            loadFullScreenAd(this)
                        }
                    },
                    update = { }
                )
            }
            // Ad cycle: first ad after 10min, show 10s, hide 5min, repeat
            LaunchedEffect(Unit) {
                delay(600_000)
                while (isActive) {
                    showFullAd = true
                    delay(10_000)
                    showFullAd = false
                    delay(300_000)
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    val bwv = bannerWebView
                    bannerWebView = null
                    bwv?.let { wv ->
                        wv.stopLoading()
                        wv.loadUrl("about:blank")
                        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                }
            }
        }
    }

private fun loadFullScreenAd(webView: android.webkit.WebView) {
    webView.loadUrl("https://duepose.com/aj52gvh5m?key=36ca7c430c662195566c67e2cc9a0072")
}

private fun loadBannerAd(webView: android.webkit.WebView) {
    val html = """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
    <style>
      *{margin:0;padding:0;box-sizing:border-box}
      body{background:transparent;text-align:center;overflow:hidden}
    </style>
    </head>
    <body>
    <script type="text/javascript">
    atOptions = {
        'key' : '987ed23dbca340d3c9cfe54e10920a64',
        'format' : 'iframe',
        'height' : 50,
        'width' : 320,
        'params' : {}
    };
    </script>
    <script type="text/javascript" src="https://duepose.com/987ed23dbca340d3c9cfe54e10920a64/invoke.js"></script>
    </body>
    </html>
    """.trimIndent()
    webView.loadDataWithBaseURL("https://duepose.com", html, "text/html", "UTF-8", null)
}

@Composable
private fun PlayerOverlay(playerState: PlayerState?, modifier: Modifier = Modifier, isTv: Boolean = false) {
    val state = playerState ?: return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (state.isLoading && !state.isPlaying) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(if (isTv) 56.dp else 36.dp))
                Spacer(Modifier.height(if (isTv) 20.dp else 12.dp))
                Text("Buffering...", color = Color.White.copy(alpha = 0.7f), fontSize = if (isTv) 20.sp else 13.sp)
            }
        }
        state.error?.let { errorMsg ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(if (isTv) 72.dp else 48.dp))
                Spacer(Modifier.height(if (isTv) 20.dp else 12.dp))
                Text(errorMsg, fontSize = if (isTv) 20.sp else 14.sp, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(if (isTv) 8.dp else 4.dp))
                Text("Tap to retry", fontSize = if (isTv) 18.sp else 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(if (isTv) 24.dp else 16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isTv) 14.dp else 8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            state.actions?.retry()
                        }
                        .padding(horizontal = if (isTv) 40.dp else 24.dp, vertical = if (isTv) 16.dp else 8.dp)
                ) {
                    Text("Retry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isTv) 22.sp else 14.sp)
                }
            }
        }
    }
}

@Composable
private fun FocusableIconButton(
    onClick: () -> Unit,
    isTv: Boolean,
    content: @Composable () -> Unit
) {
    if (isTv) {
        var isFocused by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .focusable()
                .onFocusChanged { isFocused = it.isFocused }
                .then(
                    if (isFocused) Modifier.border(
                        3.dp, Color.White, RoundedCornerShape(8.dp)
                    ) else Modifier
                )
                .clickable(onClick = onClick)
        ) {
            content()
        }
    } else {
        IconButton(onClick = onClick) {
            content()
        }
    }
}
