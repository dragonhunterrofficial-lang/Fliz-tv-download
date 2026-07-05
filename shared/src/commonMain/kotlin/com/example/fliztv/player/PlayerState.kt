package com.example.fliztv.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface PlayerActions {
    fun togglePlay()
    fun seekTo(positionMs: Long)
    fun seekForward(ms: Long)
    fun seekBackward(ms: Long)
    fun setSpeed(speed: Float)
    fun setVolume(vol: Float)
    fun setResizeMode(mode: Int)
    fun retry()
    fun release()
}

class PlayerState {
    var isPlaying by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var bufferedPosition by mutableLongStateOf(0L)
    var volume by mutableFloatStateOf(1f)
    var speed by mutableFloatStateOf(1f)
    var resizeMode by mutableIntStateOf(0)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var actions: PlayerActions? = null

    val positionFormatted: String get() = formatTime(position)
    val durationFormatted: String get() = formatTime(duration)
    val bufferedPercent: Float get() = if (duration > 0) (bufferedPosition.toFloat() / duration) else 0f
    val progress: Float get() = if (duration > 0) (position.toFloat() / duration) else 0f

    companion object {
        fun formatTime(ms: Long): String {
            val totalSec = ms / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s)
            else "%d:%02d".format(m, s)
        }
    }
}

const val RESIZE_FIT = 0
const val RESIZE_FILL = 1
const val RESIZE_ZOOM = 2
