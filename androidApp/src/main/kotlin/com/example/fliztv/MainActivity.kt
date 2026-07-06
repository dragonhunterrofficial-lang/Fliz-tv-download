package com.example.fliztv

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.data.FavoritesRepository
import com.example.fliztv.data.RecordingManager
import com.example.fliztv.player.AdaptivePlayerBox
import com.example.fliztv.player.CastManager
import com.example.fliztv.player.ChannelLogoImage
import com.example.fliztv.player.PlayerState
import com.example.fliztv.player.RecordingDownloader

class MainActivity : ComponentActivity() {
    private var currentChannelName: String = ""
    private var onChannelUp: (() -> Unit)? = null
    private var onChannelDown: (() -> Unit)? = null

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP,
            android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                onChannelUp?.invoke()
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
            android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                onChannelDown?.invoke()
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                true // handled by Compose onKeyEvent
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        hideSystemBars()

        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        if (isTv) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        val prefs = getSharedPreferences("fliztv_prefs", Context.MODE_PRIVATE)
        val savedFavorites = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        FavoritesRepository.loadFavorites(savedFavorites)
        FavoritesRepository.setOnChangedListener { ids ->
            prefs.edit().putStringSet("favorites", ids).apply()
        }

        val cachePrefs = getSharedPreferences("fliztv_cache", Context.MODE_PRIVATE)
        ChannelRepository.setCacheCallbacks(
            load = { cachePrefs.getString("m3u_data", "") ?: "" },
            save = { data: String -> cachePrefs.edit().putString("m3u_data", data).apply() }
        )
        ChannelRepository.setLastWatchedCallbacks(
            load = { cachePrefs.getString("last_watched", "") ?: "" },
            save = { id: String -> cachePrefs.edit().putString("last_watched", id).apply() }
        )

        ChannelRepository.setScope(lifecycleScope)

        val recPrefs = getSharedPreferences("fliztv_recordings", Context.MODE_PRIVATE)
        RecordingManager.setPersistenceCallbacks(
            save = { data: String -> recPrefs.edit().putString("recordings", data).apply() },
            load = { recPrefs.getString("recordings", "") ?: "" }
        )

        val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager

        CastManager.init(this)

        setContent {
            var showExitDialog by remember { mutableStateOf(false) }

            Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            App(
                isTv = isTv,
                onOpenUrl = { url ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) { }
                },
                onEnterPip = { enterPip() },
                onVolumeUp = { audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI) },
                onVolumeDown = { audioManager?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI) },
                onBrightnessUp = {
                    val lp = window.attributes
                    lp.screenBrightness = (lp.screenBrightness + 0.05f).coerceIn(0.01f, 1f)
                    window.attributes = lp
                },
                onBrightnessDown = {
                    val lp = window.attributes
                    lp.screenBrightness = (lp.screenBrightness - 0.05f).coerceIn(0.01f, 1f)
                    window.attributes = lp
                },
                playerProvider = { channel, modifier, playerState, isRec, recName, recId, onRecErr ->
                    CastManager.loadUrl(channel.url, channel.name, channel.userAgent, channel.referer)
                    Box(modifier = modifier) {
                        if (CastManager.isCasting) {
                            com.example.fliztv.player.CastingOverlay(
                                deviceName = CastManager.deviceName,
                                channelName = channel.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AdaptivePlayerBox(
                                url = channel.url,
                                userAgent = channel.userAgent,
                                referer = channel.referer,
                                isSd = channel.isSd,
                                quality = channel.quality,
                                playerState = playerState,
                                isRecording = isRec,
                                recordingChannelName = recName,
                                recordingChannelId = recId,
                                onRecordingError = { msg ->
                                    runOnUiThread {
                                        android.widget.Toast.makeText(this@MainActivity, "Recording: $msg", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                },
                logoLoader = { url, modifier ->
                    ChannelLogoImage(url = url, modifier = modifier)
                },
                backHandler = { enabled, onBack ->
                    BackHandler(enabled = enabled) {
                        if (enabled) onBack()
                        else showExitDialog = true
                    }
                },
                onPlayerActiveChanged = { isActive ->
                    if (!isTv) {
                        requestedOrientation = if (isActive) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }
                },
                onCastClick = {
                    if (CastManager.isCasting) CastManager.disconnect()
                    else CastManager.showRouteSelector()
                },
                isCasting = CastManager.isCasting,
                castDeviceName = CastManager.deviceName
            )

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Exit App") },
                    text = { Text("Are you sure you want to exit?") },
                    confirmButton = {
                        TextButton(onClick = { finish() }) {
                            Text("Exit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }


            }
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                try { enterPictureInPictureMode(params) } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CastManager.release()
        RecordingDownloader.cleanup()
    }
}
